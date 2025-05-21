-- Create employees table
CREATE TABLE employees (
    employee_id INTEGER PRIMARY KEY,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    salary NUMERIC(10,2)
);

-- Insert test data
INSERT INTO employees (employee_id, first_name, last_name, salary) VALUES (1, 'John', 'Doe', 3000);
INSERT INTO employees (employee_id, first_name, last_name, salary) VALUES (2, 'Jane', 'Smith', 4000);
INSERT INTO employees (employee_id, first_name, last_name, salary) VALUES (3, 'Bob', 'Johnson', 3500);

COMMIT; 