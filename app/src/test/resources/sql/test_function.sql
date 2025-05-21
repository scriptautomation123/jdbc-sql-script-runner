CREATE OR REPLACE FUNCTION test_func(p_id IN NUMBER)
RETURN VARCHAR2 AS
    v_name VARCHAR2(100);
BEGIN
    IF p_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, 'ID cannot be null');
    END IF;
    
    SELECT name INTO v_name
    FROM test_table
    WHERE id = p_id;
    RETURN v_name;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20002, 'No data found for ID: ' || p_id);
END;
/
