-- Set container and handle HR user setup
ALTER SESSION SET CONTAINER = FREEPDB1;

-- Grant necessary privileges to HR user (whether existing or new)
DECLARE
  v_user_exists NUMBER;
BEGIN
  -- Check if HR user exists
  SELECT COUNT(*) INTO v_user_exists 
  FROM dba_users 
  WHERE username = 'HR';
  
  -- Create user if it doesn't exist
  IF v_user_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE USER HR IDENTIFIED BY HR';
  END IF;
  
  -- Grant necessary privileges (these are idempotent)
  EXECUTE IMMEDIATE 'GRANT CREATE SESSION TO HR';
  EXECUTE IMMEDIATE 'GRANT CREATE TABLE TO HR';
  EXECUTE IMMEDIATE 'GRANT CREATE VIEW TO HR';
  EXECUTE IMMEDIATE 'GRANT CREATE SEQUENCE TO HR';
  EXECUTE IMMEDIATE 'GRANT CREATE PROCEDURE TO HR';
  EXECUTE IMMEDIATE 'GRANT UNLIMITED TABLESPACE TO HR';
EXCEPTION
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Error: ' || SQLERRM);
    RAISE;
END;
/ 