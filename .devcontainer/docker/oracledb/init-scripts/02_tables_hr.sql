ALTER SESSION SET CONTAINER = FREEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = HR;

-- Check if HR schema is the current schema
DECLARE
  v_current_schema VARCHAR2(30);
BEGIN
  SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') INTO v_current_schema FROM DUAL;
  
  IF v_current_schema != 'HR' THEN
    RAISE_APPLICATION_ERROR(-20001, 'Current schema is not HR. Cannot proceed.');
  END IF;
END;
/


-- Check if any tables already exist and exit with error if they do
DECLARE
  v_table_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(v_table_exists, -955); -- ORA-00955: name is already used by an existing object
  
  v_count NUMBER;
  TYPE table_array IS TABLE OF VARCHAR2(30);
  v_tables table_array := table_array('REGIONS', 'COUNTRIES', 'LOCATIONS', 'DEPARTMENTS', 
                                     'JOBS', 'EMPLOYEES', 'JOB_HISTORY');
  i NUMBER := 1;
BEGIN
  -- Check if any of the tables already exist
  WHILE i <= v_tables.COUNT LOOP
    SELECT COUNT(*) INTO v_count
    FROM user_tables
    WHERE table_name = v_tables(i);
    
    IF v_count > 0 THEN
      RAISE_APPLICATION_ERROR(-20003, 'Table ' || v_tables(i) || ' already exists. Cannot proceed.');
    END IF;
    
    i := i + 1;
  END LOOP;
EXCEPTION
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Error checking tables: ' || SQLERRM);
    RAISE;
END;
/

-- Create base tables without constraints or indexes
CREATE TABLE regions (
    region_id NUMBER,
    region_name VARCHAR2(25)
);

CREATE TABLE countries (
    country_id VARCHAR2(2),
    country_name VARCHAR2(40),
    region_id NUMBER
);

CREATE TABLE locations (
    location_id NUMBER,
    street_address VARCHAR2(40),
    postal_code VARCHAR2(12),
    city VARCHAR2(30),
    state_province VARCHAR2(25),
    country_id VARCHAR2(2)
);

CREATE TABLE departments (
    department_id NUMBER,
    department_name VARCHAR2(30),
    manager_id NUMBER,
    location_id NUMBER
);

CREATE TABLE jobs (
    job_id VARCHAR2(10),
    job_title VARCHAR2(35),
    min_salary NUMBER,
    max_salary NUMBER
);

CREATE TABLE employees (
    employee_id NUMBER,
    first_name VARCHAR2(20),
    last_name VARCHAR2(25),
    email VARCHAR2(25),
    phone_number VARCHAR2(20),
    hire_date DATE,
    job_id VARCHAR2(10),
    salary NUMBER,
    commission_pct NUMBER,
    manager_id NUMBER,
    department_id NUMBER
);

CREATE TABLE job_history (
    employee_id NUMBER,
    start_date DATE,
    end_date DATE,
    job_id VARCHAR2(10),
    department_id NUMBER
); 