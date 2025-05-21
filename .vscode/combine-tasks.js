// .vscode/combine-tasks.js
import { appendFileSync, existsSync, mkdirSync, readdirSync, readFileSync, writeFileSync, watch } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

// Get paths
const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));
const TASK_CONFIGS_DIR = join(SCRIPT_DIR, 'task-configs');
const TASKS_JSON = join(SCRIPT_DIR, 'tasks.json');
const LOG_FILE = join(SCRIPT_DIR, 'task-watcher.log');

// Function to log messages
function log(message) {
    const timestamp = new Date().toISOString();
    const logMessage = `[${timestamp}] ${message}\n`;
    
    // Write to log file
    appendFileSync(LOG_FILE, logMessage);
    
    // Also write to console
    console.log(message);
}

// Function to combine tasks
function combineTasks() {
    // Create task-configs directory if it doesn't exist
    if (!existsSync(TASK_CONFIGS_DIR)) {
        mkdirSync(TASK_CONFIGS_DIR, { recursive: true });
    }

    // Initialize the tasks structure
    const combinedTasks = {
        version: '2.0.0',
        tasks: []
    };

    try {
        // Read all *-tasks.json files from the task-configs directory
        const configFiles = readdirSync(TASK_CONFIGS_DIR)
            .filter(file => file.endsWith('-tasks.json'));

        // Process each config file
        configFiles.forEach(file => {
            const configPath = join(TASK_CONFIGS_DIR, file);
            try {
                // Read and parse the JSON file
                const fileContent = readFileSync(configPath, 'utf8');
                const taskConfig = JSON.parse(fileContent);

                // Add tasks from the config file
                if (Array.isArray(taskConfig.tasks)) {
                    combinedTasks.tasks.push(...taskConfig.tasks);
                }
            } catch (err) {
                log(`Error processing ${file}: ${err.message}`);
            }
        });

        // Write the combined tasks to tasks.json
        writeFileSync(
            TASKS_JSON,
            JSON.stringify(combinedTasks, null, 2),
            'utf8'
        );

        log(`Tasks combined successfully (${combinedTasks.tasks.length} tasks)`);
        return true;
    } catch (err) {
        log(`Error combining tasks: ${err.message}`);
        return false;
    }
}

// Watch mode function
function watchTasks() {
    log('Task watcher started in background mode');
    
    // Initial combination
    combineTasks();

    // Create a debounced version of combineTasks
    let timeout;
    const debouncedCombine = () => {
        clearTimeout(timeout);
        timeout = setTimeout(combineTasks, 500);
    };

    // Watch the task-configs directory
    watch(TASK_CONFIGS_DIR, (eventType, filename) => {
        if (filename && filename.endsWith('-tasks.json')) {
            log(`Change detected in ${filename}`);
            debouncedCombine();
        }
    });

    // Handle process termination
    process.on('SIGINT', () => {
        log('Task watcher stopped');
        process.exit(0);
    });

    process.on('SIGTERM', () => {
        log('Task watcher stopped');
        process.exit(0);
    });
}

// Check if watch mode is requested
if (process.argv.includes('--watch')) {
    watchTasks();
} else {
    combineTasks();
}