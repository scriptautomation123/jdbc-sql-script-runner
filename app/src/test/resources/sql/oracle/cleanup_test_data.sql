-- Drop test objects
BEGIN
    EXECUTE IMMEDIATE 'DROP FUNCTION get_employee_info';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -4043 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP PROCEDURE update_employee_salary';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -4043 THEN
            RAISE;
        END IF;
END;
/

DROP TABLE employees; 