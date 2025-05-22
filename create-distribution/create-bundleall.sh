#!/bin/bash

# This script must be run in Git Bash on Windows. Archive creation uses PowerShell.

# Exit on error
set -e

# Debug output for troubleshooting
# echo "[DEBUG] PATH: $PATH"
# echo "[DEBUG] which mvn: $(which mvn)"
# echo "[DEBUG] JAVA_HOME: $JAVA_HOME"
# mvn -version

# Ensure consistent behavior across platforms
export MSYS_NO_PATHCONV=1
export MSYS2_ARG_CONV_EXCL="*"

#Default values (can be overridden by args or auto-detection)
APP_NAME=""
APP_VERSION=""
MAIN_CLASS=""
BUNDLE_NAME=""
TEMPLATE_DIR="create-distribution/templates"
OUTPUT_DIR="."
DRIVERS_FILE="create-distribution/drivers.properties"
ARTIFACT=""
ADD_COMMON_DRIVERS=true

# Function to detect the operating system
detect_os() {
    local is_windows=false
    if command -v uname >/dev/null 2>&1; then
        case "$(uname -s)" in
            MINGW*|MSYS*|CYGWIN*)
                is_windows=true
                ;;
        esac
    fi
    if [ "$is_windows" = false ] && [ "${OS:-}" = "Windows_NT" ]; then
        is_windows=true
    fi
    echo "$is_windows"
}

# Initialize global IS_WINDOWS variable
IS_WINDOWS=$(detect_os)

# Colors for output
if [ -t 1 ]; then
    RED='\033[0;31m'
    GREEN='\033[0;32m'
    YELLOW='\033[1;33m'
    PURPLE='\033[0;35m'  # Add purple color
    NC='\033[0m' # No Color
else
    RED=''
    GREEN=''
    YELLOW=''
    PURPLE=''
    NC=''
fi

log_info() {
    echo -e "${PURPLE}[INFO]${NC} $*"  # Change from GREEN to PURPLE
}
log_error() {
    echo -e "${RED}[ERROR]${NC} $*" >&2
}
log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $*"
}

# Help function
show_help() {
cat << EOF
Usage: $(basename "$0") [options]

Options:
    --app-name NAME         Application name (default: auto-detect from pom.xml)
    --app-version VERSION   Application version (default: auto-detect from pom.xml)
    --main-class CLASS      Main class (default: auto-detect from pom.xml)
    --bundle-name NAME      Output bundle directory name (default: auto-detect)
    --template-dir DIR      Directory for templates (default: create-distribution/templates)
    --output-dir DIR        Output directory (default: .)
    --drivers-file FILE     JDBC drivers properties file (default: create-distribution/drivers.properties)
    --artifact PATH         Path to built JAR/WAR (default: auto-detect)
    -a, --add-drivers       Download common JDBC drivers
    -h, --help              Show this help message

Example:
    ./$(basename "$0") --app-name myapp --app-version 1.2.3 --main-class com.example.Main --add-drivers
EOF
    exit 0
}

# Find Maven repository in a cross-platform way
find_maven_home() {
    if [ "$IS_WINDOWS" = true ]; then
        echo "$USERPROFILE/.m2"
    else
        echo "$HOME/.m2"
    fi
}

# Parse command line arguments
parse_args() {
    temp_args=()
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --app-name) APP_NAME="$2"; shift 2;;
            --app-version) APP_VERSION="$2"; shift 2;;
            --main-class) MAIN_CLASS="$2"; shift 2;;
            --bundle-name) BUNDLE_NAME="$2"; shift 2;;
            --template-dir) TEMPLATE_DIR="$2"; shift 2;;
            --output-dir) OUTPUT_DIR="$2"; shift 2;;
            --drivers-file) DRIVERS_FILE="$2"; shift 2;;
            --artifact) ARTIFACT="$2"; shift 2;;
            -a|--add-drivers) ADD_COMMON_DRIVERS=true; shift;;
            -h|--help) show_help;;
            *) temp_args+=("$1"); shift;;
        esac
    done
    set -- "${temp_args[@]}"
}

# Cross-platform compatible file operations
copy_file() {
    local src="$1"
    local dest="$2"
    if [ "$IS_WINDOWS" = true ]; then
        cp "$(cygpath -w "$src")" "$(cygpath -w "$dest")"
    else
        cp "$src" "$dest"
    fi
}

create_directory() {
    local dir="$1"
    if [ "$IS_WINDOWS" = true ]; then
        mkdir -p "$(cygpath -w "$dir")"
    else
        mkdir -p "$dir"
    fi
}

make_executable() {
    local file="$1"
    if [ "$IS_WINDOWS" = false ]; then
        chmod +x "$file"
    fi
}

# Function to detect script directory and project root in a cross-platform way
detect_script_locations() {
    # Get script directory in a cross-platform way
    if [ "$IS_WINDOWS" = true ]; then
        SCRIPT_PATH="${BASH_SOURCE[0]}"
        SCRIPT_DIR=$(powershell -command "Split-Path -Parent '$SCRIPT_PATH'" 2>/dev/null)
        if [ -z "$SCRIPT_DIR" ]; then
            # Fallback for Git Bash
            SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
        fi
        
        # Get project root
        PROJECT_ROOT=$(powershell -command "Split-Path -Parent '$SCRIPT_DIR'" 2>/dev/null)
        if [ -z "$PROJECT_ROOT" ]; then
            # Fallback for Git Bash
            PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
        fi
    else
        SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
        PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
    fi
    
    M2_HOME="$(find_maven_home)"
}

# Function to auto-detect JAVA_HOME across platforms
auto_detect_java_home() {
    if [ -z "$JAVA_HOME" ]; then
        if [ "$IS_WINDOWS" = true ]; then
            # Try to find java.exe using PowerShell on Windows
            JAVA_EXE_PATH=$(powershell -command "(Get-Command java -ErrorAction SilentlyContinue).Path" 2>/dev/null)
            if [ -n "$JAVA_EXE_PATH" ]; then
                # Use PowerShell to get parent directory (equivalent to dirname)
                JAVA_BIN_DIR=$(powershell -command "Split-Path -Parent '$JAVA_EXE_PATH'" 2>/dev/null)
                JAVA_HOME_WIN=$(powershell -command "Split-Path -Parent '$JAVA_BIN_DIR'" 2>/dev/null)
                
                # Convert Windows path to Unix path if cygpath is available
                if command -v cygpath >/dev/null 2>&1; then
                    JAVA_HOME=$(cygpath -u "$JAVA_HOME_WIN")
                else
                    JAVA_HOME=$JAVA_HOME_WIN
                fi
                
                export JAVA_HOME
                log_info "Auto-detected JAVA_HOME as $JAVA_HOME (Windows)"
            fi
        else
            JAVA_BIN=$(which java 2>/dev/null)
            if [ -n "$JAVA_BIN" ]; then
                JAVA_BIN=$(readlink -f "$JAVA_BIN")
                
                # Safe cross-platform dirname
                if [ "$IS_WINDOWS" = true ]; then
                    JAVA_BIN_DIR=$(powershell -command "Split-Path -Parent '$JAVA_BIN'" 2>/dev/null)
                    JAVA_HOME=$(powershell -command "Split-Path -Parent '$JAVA_BIN_DIR'" 2>/dev/null)
                else
                    JAVA_HOME=$(dirname $(dirname "$JAVA_BIN"))
                fi
                
                export JAVA_HOME
                log_info "Auto-detected JAVA_HOME as $JAVA_HOME (Unix)"
            fi
        fi
        if [ -z "$JAVA_HOME" ]; then
            log_warn "Could not auto-detect JAVA_HOME. Some features may not work."
        fi
    fi
}

# Function to copy Java security files
copy_java_security_files() {
    local bundle_dir="$1"
    
    # Create the runtime directory for custom JRE and security files
    create_directory "$bundle_dir/runtime/lib/security"

    # Copy Java security files into the bundle's runtime
    if [ -n "$JAVA_HOME" ] && [ -d "$JAVA_HOME/lib/security" ]; then
        cp -r "$JAVA_HOME/lib/security/"* "$bundle_dir/runtime/lib/security/"
        log_info "Copied Java security files from $JAVA_HOME/lib/security to $bundle_dir/runtime/lib/security"
    else
        log_warn "JAVA_HOME is not set or $JAVA_HOME/lib/security does not exist. Skipping security files copy."
    fi
}

# Function to download JDBC driver from Maven
download_jdbc_driver() {
    local db_type="$1"
    local maven_coords="$2"
    local target_dir="$3"
    
    log_info "Downloading $db_type JDBC driver..."
    
    # Create target directory if it doesn't exist
    create_directory "$target_dir"
    
    # Split Maven coordinates
    IFS=':' read -r groupId artifactId version <<< "$maven_coords"
    
    # Download using Maven with explicit repository path
    export MAVEN_OPTS=""
    /c/usr/bin/apache-maven-3.9.9/bin/mvn dependency:copy \
        -Dartifact="$maven_coords" \
        -DoutputDirectory="$target_dir" \
        -Dmdep.stripVersion=true \
        -Dmaven.repo.local="$M2_HOME/repository" \
        || {
            log_error "Failed to download $db_type driver"
            return 1
        }
    
    # Rename the jar to remove version
    local jar_name="$artifactId.jar"
    if [ -f "$target_dir/$artifactId-$version.jar" ]; then
        mv "$target_dir/$artifactId-$version.jar" "$target_dir/$jar_name"
    fi
    
    log_info "Successfully downloaded $db_type driver to $target_dir/$jar_name"
}

# Function to download all common drivers
download_common_drivers() {
    local bundle_dir="$1"
    local drivers_file="$SCRIPT_DIR/drivers.properties"
    
    if [ ! -f "$drivers_file" ]; then
        log_error "drivers.properties not found at $drivers_file"
        return 1
    fi
    
    # Read and process drivers.properties
    while IFS='=' read -r key value || [ -n "$key" ]; do
        # Skip comments and empty lines
        [[ $key =~ ^#.*$ ]] && continue
        [ -z "$key" ] && continue
        
        # Extract database type from key
        local db_type="${key%%.driver}"
        local target_dir="$bundle_dir/drivers/$db_type"
        
        # Download driver
        download_jdbc_driver "$db_type" "$value" "$target_dir"
    done < "$drivers_file"
}

# Create the bundle directory structure
create_bundle_structure() {
    local BUNDLE_DIR="$1"
    
    # Create directories
    if [ "$IS_WINDOWS" = true ]; then
        # Windows - use PowerShell to create directories with output suppressed
        powershell -Command "New-Item -ItemType Directory -Force -Path \"$BUNDLE_DIR\", \"$BUNDLE_DIR\\app\", \"$BUNDLE_DIR\\runtime\", \"$BUNDLE_DIR\\drivers\", \"$BUNDLE_DIR\\drivers\\oracle\", \"$BUNDLE_DIR\\drivers\\mysql\", \"$BUNDLE_DIR\\drivers\\postgresql\", \"$BUNDLE_DIR\\drivers\\sqlserver\" | Out-Null"
    else
        # Unix/Linux
        mkdir -p "$BUNDLE_DIR"/{app,runtime,drivers/{oracle,mysql,postgresql,sqlserver}}
    fi
    
    # Copy templates with path conversion
    copy_file "$SCRIPT_DIR/templates/run.sh.template" "$BUNDLE_DIR/run.sh"
    copy_file "$SCRIPT_DIR/templates/run.bat.template" "$BUNDLE_DIR/run.bat"
    
    # Make run scripts executable
    make_executable "$BUNDLE_DIR/run.sh"
    
    log_info "Created bundle structure in $BUNDLE_DIR"
}


# Auto-detect from pom.xml if not provided
auto_detect_from_pom() {
    if [ -z "$APP_NAME" ] || [ -z "$APP_VERSION" ] || [ -z "$MAIN_CLASS" ]; then
        if [ -f pom.xml ]; then
            [ -z "$APP_NAME" ] && APP_NAME=$(xmllint --xpath 'string(//project/artifactId)' pom.xml 2>/dev/null || echo "app")
            [ -z "$APP_VERSION" ] && APP_VERSION=$(xmllint --xpath 'string(//project/version)' pom.xml 2>/dev/null || echo "1.0-SNAPSHOT")
            [ -z "$MAIN_CLASS" ] && MAIN_CLASS=$(xmllint --xpath 'string(//mainClass|//project/properties/mainClass)' pom.xml 2>/dev/null || echo "Main")
        fi
    fi
}

# Set bundle name if not provided
set_bundle_name() {
    if [ -z "$BUNDLE_NAME" ]; then
        # First try to use APP_NAME-based bundle name if APP_NAME is available
        if [ -n "$APP_NAME" ]; then
            if [ "$IS_WINDOWS" = true ]; then
                BUNDLE_NAME="${APP_NAME}-bundle-windows"
            else
                BUNDLE_NAME="${APP_NAME}-bundle-linux"
            fi
        else
            # Fall back to default bundle naming
            set_default_bundle_name
        fi
    fi
}

# Set artifact if not provided (try to find JAR/WAR in target/build/libs)
auto_detect_artifact() {
    if [ -z "$ARTIFACT" ]; then
        if [ -f "target/${APP_NAME}-${APP_VERSION}.jar" ]; then
            ARTIFACT="target/${APP_NAME}-${APP_VERSION}.jar"
        elif [ -f "build/libs/${APP_NAME}-${APP_VERSION}.jar" ]; then
            ARTIFACT="build/libs/${APP_NAME}-${APP_VERSION}.jar"
        else
            if [ "$IS_WINDOWS" = true ]; then
                # Use PowerShell to get first file on Windows
                ARTIFACT=$(powershell -command "Get-ChildItem -Path target,build/libs -Filter *.jar -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName" 2>/dev/null)
            else
                ARTIFACT=$(find target build/libs -name "*.jar" | head -n 1)
            fi
        fi
    fi
}

# Check required files
check_required_files() {
    test -f "$ARTIFACT" || { log_error "Artifact not found: $ARTIFACT"; exit 1; }
    test -d "$TEMPLATE_DIR" || { log_error "Template dir not found: $TEMPLATE_DIR"; exit 1; }
}

# Download drivers if requested
download_drivers_if_requested() {
    if [ "$ADD_COMMON_DRIVERS" = true ]; then
        if [ -f "$OLDPWD/$DRIVERS_FILE" ]; then
            while IFS='=' read -r key value; do
                [[ $key =~ ^#.*$ ]] && continue
                [ -z "$key" ] && continue
                db_type="${key%%.driver}"
                target_dir="$BUNDLE_NAME/drivers/$db_type"
                create_directory "$target_dir"
                export MAVEN_OPTS=""
                /c/usr/bin/apache-maven-3.9.9/bin/mvn dependency:copy -Dartifact="$value" -DoutputDirectory="$target_dir" || log_warn "Failed to download $db_type driver"
            done < "$OLDPWD/$DRIVERS_FILE"
        else
            log_warn "Drivers file not found: $DRIVERS_FILE"
        fi
    fi
}

# (Optional) Create custom JRE if jlink is available
create_custom_jre() {
    if command -v jlink >/dev/null 2>&1; then
        log_info "Creating custom JRE"
        # Remove existing runtime directory if it exists
        [ -d "$BUNDLE_NAME/runtime" ] && rm -rf "$BUNDLE_NAME/runtime"
        # Create JRE with all necessary modules for database connections
        jlink --add-modules java.base,java.logging,java.xml,java.management,java.naming,jdk.unsupported,java.sql,java.desktop,java.security.jgss,java.security.sasl,java.net.http,java.compiler,jdk.crypto.ec \
          --output "$BUNDLE_NAME/runtime" \
          --strip-debug --no-man-pages --no-header-files --compress zip-2 || log_warn "jlink failed"
    fi
}

create_bundle_readme() {
    echo "Creating bundle README..."
    if [ "$IS_WINDOWS" = true ]; then
        # Use PowerShell for text replacement on Windows
        README_TEMPLATE="$PROJECT_ROOT/create-distribution/templates/README.md.template"
        README_OUTPUT="$BUNDLE_NAME/README.md"
        # Convert paths to Windows format for PowerShell
        README_TEMPLATE_WIN=$(cygpath -w "$README_TEMPLATE")
        README_OUTPUT_WIN=$(cygpath -w "$README_OUTPUT")
        powershell.exe -Command "(Get-Content -Path '$README_TEMPLATE_WIN') -replace '\\\${APPLICATION_NAME}', '$APP_NAME' -replace '\\\${BUNDLE_NAME}', '$BUNDLE_NAME' | Set-Content -Path '$README_OUTPUT_WIN'"
    else
        # Use sed for Unix/Linux
        sed -e "s/\${APPLICATION_NAME}/${APP_NAME}/g" \
            -e "s/\${BUNDLE_NAME}/${BUNDLE_NAME}/g" \
            "$PROJECT_ROOT/create-distribution/templates/README.md.template" > "$BUNDLE_NAME/README.md"
    fi
}

# Create archive for Windows
create_bundle_archive() {
    if [ "$IS_WINDOWS" = true ]; then
    log_info "Creating Windows bundle archive using PowerShell Compress-Archive..."
    powershell.exe -Command "Compress-Archive -Path '$BUNDLE_NAME' -DestinationPath '${BUNDLE_NAME}.zip' -Force"
    else
        log_info "Creating Unix bundle archive using zip command..."
        zip -rq "${BUNDLE_NAME}.zip" "$BUNDLE_NAME"
    fi
    log_info "Bundle created: ${BUNDLE_NAME}.zip"
}

print_final_instructions() {
    log_info "Bundle creation completed successfully!"
    echo "You can find the bundle in: ${BUNDLE_NAME}.zip"
    echo "To use the application:"
    echo "1. Extract the archive: unzip ${BUNDLE_NAME}.zip"
    echo "2. Run the application:"
    echo "   - On Linux/macOS: ./${BUNDLE_NAME}/run.sh"
    echo "   - On Windows: ${BUNDLE_NAME}\\run.bat"
}

copy_uber_jar_to_bundle() {
    copy_file "$PROJECT_ROOT/app/target/dbscriptrunner-1.0-SNAPSHOT.jar" "$BUNDLE_NAME/app/"
}

copy_bundle_templates() {
    copy_file "$SCRIPT_DIR/templates/run.sh.template" "$BUNDLE_NAME/run.sh"
    copy_file "$SCRIPT_DIR/templates/run.bat.template" "$BUNDLE_NAME/run.bat"
    make_executable "$BUNDLE_NAME/run.sh"
}

copy_sample_sql_scripts() {
    local src_dir="$PROJECT_ROOT/app/src/test/resources/sql"
    local dest_dir="$BUNDLE_NAME/resources/sql"
    if [ -d "$src_dir" ]; then
        create_directory "$dest_dir"
        cp -r "$src_dir/"* "$dest_dir/"
        log_info "Copied sample SQL scripts to $dest_dir"
    else
        log_warn "No sample SQL scripts found at $src_dir"
    fi
}

clean_previous_bundle() {
    if [ -d "$BUNDLE_NAME" ]; then
        log_info "Removing existing bundle directory: $BUNDLE_NAME"
        rm -rf "$BUNDLE_NAME"
    fi
    if [ -f "${BUNDLE_NAME}.zip" ]; then
        log_info "Removing existing bundle archive: ${BUNDLE_NAME}.zip"
        rm -f "${BUNDLE_NAME}.zip"
    fi
}

# Add a new function to copy drivers from target/drivers to the bundle
copy_drivers_from_target() {
    local src_base="$PROJECT_ROOT/create-distribution/target/drivers"
    local dest_base="$BUNDLE_NAME/drivers"
    for db in oracle mysql postgresql sqlserver; do
        local src_dir="$src_base/$db"
        local dest_dir="$dest_base/$db"
        create_directory "$dest_dir"
        if [ -d "$src_dir" ]; then
            cp "$src_dir"/*.jar "$dest_dir/" 2>/dev/null || true
        fi
    done
    log_info "Copied JDBC drivers from $src_base to $dest_base"
}

copy_log4j_config() {
    local src_log4j="$PROJECT_ROOT/app/src/main/resources/log4j2.xml"
    local dest_dir="$BUNDLE_NAME/resources"
    create_directory "$dest_dir"
    if [ -f "$src_log4j" ]; then
        cp "$src_log4j" "$dest_dir/"
        log_info "Copied log4j2.xml to $dest_dir"
    else
        log_warn "log4j2.xml not found at $src_log4j, skipping copy."
    fi
}

copy_application_yaml() {
    local src_yaml="$PROJECT_ROOT/app/src/main/resources/application.yaml"
    local dest_dir="$BUNDLE_NAME/resources"
    create_directory "$dest_dir"
    if [ -f "$src_yaml" ]; then
        cp "$src_yaml" "$dest_dir/"
        log_info "Copied application.yaml to $dest_dir"
    else
        log_warn "application.yaml not found at $src_yaml, skipping copy."
    fi
}

# Main execution flow
main() {
    detect_script_locations
    parse_args "$@"
    auto_detect_from_pom
    auto_detect_java_home
    set_bundle_name
    clean_previous_bundle
    auto_detect_artifact
    check_required_files
    create_bundle_structure "$BUNDLE_NAME"
    copy_log4j_config
    copy_application_yaml
    copy_sample_sql_scripts
    copy_uber_jar_to_bundle
    copy_bundle_templates
    copy_drivers_from_target
    create_custom_jre
    copy_java_security_files "$BUNDLE_NAME"
    create_bundle_readme
    create_bundle_archive
    print_final_instructions
}

main "$@"

