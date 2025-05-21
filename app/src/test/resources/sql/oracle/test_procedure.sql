-- Create a procedure to update employee salary
CREATE OR REPLACE PROCEDURE update_employee_salary(
    p_emp_id IN NUMBER,
    p_new_salary IN NUMBER
) AS
BEGIN
    UPDATE employees
    SET salary = p_new_salary
    WHERE employee_id = p_emp_id;
    
    IF SQL%ROWCOUNT = 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Employee not found');
    END IF;
    
    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/

-- Test the procedure
BEGIN
    update_employee_salary(1, 5000);
END;
/

-- Cleanup
DROP PROCEDURE update_employee_salary;
/ 