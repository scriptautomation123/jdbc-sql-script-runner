CREATE OR REPLACE PROCEDURE test_proc(
    p_id IN NUMBER,
    p_name OUT VARCHAR2
) AS
BEGIN
    IF p_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, 'ID cannot be null');
    END IF;
    
    SELECT name INTO p_name
    FROM test_table
    WHERE id = p_id;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20002, 'No data found for ID: ' || p_id);
END;
/
