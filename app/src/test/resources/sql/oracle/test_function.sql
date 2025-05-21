-- Create a function to get employee info
CREATE OR REPLACE FUNCTION get_employee_info(p_emp_id IN NUMBER)
RETURN VARCHAR2 AS
    v_full_name VARCHAR2(100);
BEGIN
    SELECT first_name || ' ' || last_name
    INTO v_full_name
    FROM employees
    WHERE employee_id = p_emp_id;
    
    RETURN v_full_name;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN NULL;
    WHEN OTHERS THEN
        RAISE;
END;
/

-- Test the function
SELECT get_employee_info(1) FROM dual;
/

-- Cleanup
DROP FUNCTION get_employee_info;
/ 