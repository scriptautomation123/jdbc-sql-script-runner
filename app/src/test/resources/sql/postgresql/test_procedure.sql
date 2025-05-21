-- Create a procedure to update employee salary
CREATE OR REPLACE PROCEDURE update_employee_salary(
    p_emp_id INTEGER,
    p_new_salary NUMERIC
) AS $$
BEGIN
    UPDATE employees
    SET salary = p_new_salary
    WHERE employee_id = p_emp_id;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Employee not found';
    END IF;
    
    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
$$ LANGUAGE plpgsql;

-- Test the procedure
CALL update_employee_salary(1, 5000);

-- Cleanup
DROP PROCEDURE update_employee_salary(INTEGER, NUMERIC); 