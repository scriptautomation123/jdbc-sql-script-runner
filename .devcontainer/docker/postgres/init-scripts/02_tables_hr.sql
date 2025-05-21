SET search_path TO hr;

-- Create base tables
CREATE TABLE regions (
    region_id INTEGER PRIMARY KEY,
    region_name VARCHAR(25)
);

CREATE TABLE countries (
    country_id CHAR(2) PRIMARY KEY,
    country_name VARCHAR(40),
    region_id INTEGER REFERENCES regions(region_id)
);

CREATE TABLE locations (
    location_id INTEGER PRIMARY KEY DEFAULT nextval('locations_seq'),
    street_address VARCHAR(40),
    postal_code VARCHAR(12),
    city VARCHAR(30),
    state_province VARCHAR(25),
    country_id CHAR(2) REFERENCES countries(country_id)
);

CREATE TABLE departments (
    department_id INTEGER PRIMARY KEY DEFAULT nextval('departments_seq'),
    department_name VARCHAR(30) UNIQUE,
    manager_id INTEGER,
    location_id INTEGER REFERENCES locations(location_id)
);

CREATE TABLE jobs (
    job_id VARCHAR(10) PRIMARY KEY,
    job_title VARCHAR(35) UNIQUE,
    min_salary NUMERIC,
    max_salary NUMERIC
);

CREATE TABLE employees (
    employee_id INTEGER PRIMARY KEY DEFAULT nextval('employees_seq'),
    first_name VARCHAR(20),
    last_name VARCHAR(25),
    email VARCHAR(25) UNIQUE,
    phone_number VARCHAR(20),
    hire_date DATE,
    job_id VARCHAR(10) REFERENCES jobs(job_id),
    salary NUMERIC CHECK (salary > 0),
    commission_pct NUMERIC,
    manager_id INTEGER REFERENCES employees(employee_id),
    department_id INTEGER REFERENCES departments(department_id)
);

CREATE TABLE job_history (
    employee_id INTEGER,
    start_date DATE,
    end_date DATE,
    job_id VARCHAR(10) REFERENCES jobs(job_id),
    department_id INTEGER REFERENCES departments(department_id),
    PRIMARY KEY (employee_id, start_date),
    CHECK (end_date > start_date)
);

-- Add foreign key for department manager after tables exist
ALTER TABLE departments 
    ADD CONSTRAINT dept_mgr_fk 
    FOREIGN KEY (manager_id) 
    REFERENCES employees(employee_id);