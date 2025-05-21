-- Connect as HR user and set container
ALTER SESSION SET CONTAINER = FREEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = HR;

-- Check if HR schema is the current schema
DECLARE
  v_current_schema VARCHAR2(30);
BEGIN
  SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') INTO v_current_schema FROM DUAL;
  
  IF v_current_schema != 'HR' THEN
    RAISE_APPLICATION_ERROR(-20001, 'Current schema is not HR. Cannot proceed.');
  END IF;
END;
/

-- Drop tables in the correct order by handling constraints dynamically
DECLARE
  v_sql VARCHAR2(200);
BEGIN
  -- First disable all foreign key constraints for HR-owned tables
  FOR constraint_rec IN (
    SELECT c.constraint_name, c.table_name
    FROM user_constraints c
    JOIN all_objects o ON c.table_name = o.object_name
    WHERE c.constraint_type = 'R'  -- R = Foreign Key constraints
      AND o.owner = 'HR'
      AND o.object_type = 'TABLE'
      AND o.generated = 'N'  -- Not system-generated
  ) LOOP
    v_sql := 'ALTER TABLE ' || constraint_rec.table_name || 
             ' DISABLE CONSTRAINT ' || constraint_rec.constraint_name;
    
    BEGIN
      EXECUTE IMMEDIATE v_sql;
      DBMS_OUTPUT.PUT_LINE('Disabled constraint: ' || constraint_rec.constraint_name);
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Failed to disable constraint ' || constraint_rec.constraint_name || 
                           ' on table ' || constraint_rec.table_name || ': ' || SQLERRM);
    END;
  END LOOP;
  
  -- Now drop only real tables (not system-generated) owned by HR
  FOR table_rec IN (
    SELECT object_name as table_name
    FROM all_objects
    WHERE owner = 'HR'
      AND object_type = 'TABLE'
      AND generated = 'N'  -- Not system-generated
  ) LOOP
    v_sql := 'DROP TABLE ' || table_rec.table_name || ' CASCADE CONSTRAINTS';
    
    BEGIN
      EXECUTE IMMEDIATE v_sql;
      DBMS_OUTPUT.PUT_LINE('Table ' || table_rec.table_name || ' dropped successfully');
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Failed to drop table ' || table_rec.table_name || ': ' || SQLERRM);
    END;
  END LOOP;
  
  -- Now drop all sequences owned by HR
  FOR seq_rec IN (
    SELECT object_name as sequence_name
    FROM all_objects
    WHERE owner = 'HR'
      AND object_type = 'SEQUENCE'
      AND generated = 'N'  -- Not system-generated
  ) LOOP
    v_sql := 'DROP SEQUENCE ' || seq_rec.sequence_name;
    
    BEGIN
      EXECUTE IMMEDIATE v_sql;
      DBMS_OUTPUT.PUT_LINE('Sequence ' || seq_rec.sequence_name || ' dropped successfully');
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Failed to drop sequence ' || seq_rec.sequence_name || ': ' || SQLERRM);
    END;
  END LOOP;
EXCEPTION
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Error: ' || SQLERRM);
    RAISE;
END;
/