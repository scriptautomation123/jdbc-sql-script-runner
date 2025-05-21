-- Drop the function if it exists
BEGIN
   EXECUTE IMMEDIATE 'DROP FUNCTION hr.get_employee_info';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -4043 THEN  -- -4043 is "does not exist"
         RAISE;
      END IF;
END;
/

-- Create the function in HR schema
CREATE OR REPLACE FUNCTION hr.get_employee_info(p_emp_id IN NUMBER) 
RETURN VARCHAR2 AS
BEGIN
    RETURN (SELECT first_name || ' ' || last_name 
            FROM hr.employees 
            WHERE employee_id = p_emp_id);
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN NULL;
    WHEN OTHERS THEN
        RAISE;
END;
/

-- Grant execute permission to public
GRANT EXECUTE ON 
hr.get_employee_info TO PUBLIC;