-- Basic query to test connectivity
SELECT * FROM DUAL;

-- Create a test table with primary key and varchar column
CREATE TABLE test_table (
    id NUMBER PRIMARY KEY,
    name VARCHAR2(100)
);

-- Insert test data
INSERT INTO test_table VALUES (1, 'Test Data');
