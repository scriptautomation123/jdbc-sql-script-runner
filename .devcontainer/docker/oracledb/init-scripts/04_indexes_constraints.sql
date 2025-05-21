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

-- Create sequences with error tracking
BEGIN
  BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE locations_seq' ||
        ' START WITH 3300' ||
        ' INCREMENT BY 100' ||
        ' MAXVALUE 9900' ||
        ' NOCACHE' ||
        ' NOCYCLE';
    DBMS_OUTPUT.PUT_LINE('SUCCESS: Created sequence locations_seq');
  EXCEPTION
    WHEN OTHERS THEN
      DBMS_OUTPUT.PUT_LINE('FAILED: Create sequence locations_seq - ' || SQLERRM);
  END;

  BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE departments_seq' ||
        ' START WITH 280' ||
        ' INCREMENT BY 10' ||
        ' MAXVALUE 9990' ||
        ' NOCACHE' ||
        ' NOCYCLE';
    DBMS_OUTPUT.PUT_LINE('SUCCESS: Created sequence departments_seq');
  EXCEPTION
    WHEN OTHERS THEN
      DBMS_OUTPUT.PUT_LINE('FAILED: Create sequence departments_seq - ' || SQLERRM);
  END;

  BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE employees_seq' ||
        ' START WITH 207' ||
        ' INCREMENT BY 1' ||
        ' NOCACHE' ||
        ' NOCYCLE';
    DBMS_OUTPUT.PUT_LINE('SUCCESS: Created sequence employees_seq');
  EXCEPTION
    WHEN OTHERS THEN
      DBMS_OUTPUT.PUT_LINE('FAILED: Create sequence employees_seq - ' || SQLERRM);
  END;
END;
/

-- Add constraints with error tracking
DECLARE
  TYPE constraint_rec IS RECORD (
    table_name VARCHAR2(30),
    constraint_name VARCHAR2(30),
    constraint_type VARCHAR2(1),
    constraint_clause VARCHAR2(1000)
  );
  
  TYPE constraint_array IS TABLE OF constraint_rec;
  
  v_constraints constraint_array := constraint_array(
    -- Primary key constraints
    constraint_rec('regions', 'reg_id_pk', 'P', 'PRIMARY KEY (region_id)'),
    constraint_rec('countries', 'country_id_pk', 'P', 'PRIMARY KEY (country_id)'),
    constraint_rec('locations', 'loc_id_pk', 'P', 'PRIMARY KEY (location_id)'),
    constraint_rec('departments', 'dept_id_pk', 'P', 'PRIMARY KEY (department_id)'),
    constraint_rec('jobs', 'job_id_pk', 'P', 'PRIMARY KEY (job_id)'),
    constraint_rec('employees', 'emp_id_pk', 'P', 'PRIMARY KEY (employee_id)'),
    constraint_rec('job_history', 'jhist_id_pk', 'P', 'PRIMARY KEY (employee_id, start_date)'),

    -- Foreign key constraints
    constraint_rec('countries', 'countr_reg_fk', 'F', 'FOREIGN KEY (region_id) REFERENCES regions(region_id)'),
    constraint_rec('locations', 'loc_c_id_fk', 'F', 'FOREIGN KEY (country_id) REFERENCES countries(country_id)'),
    constraint_rec('departments', 'dept_loc_fk', 'F', 'FOREIGN KEY (location_id) REFERENCES locations(location_id)'),
    constraint_rec('departments', 'dept_mgr_fk', 'F', 'FOREIGN KEY (manager_id) REFERENCES employees(employee_id)'),
    constraint_rec('employees', 'emp_dept_fk', 'F', 'FOREIGN KEY (department_id) REFERENCES departments(department_id)'),
    constraint_rec('employees', 'emp_job_fk', 'F', 'FOREIGN KEY (job_id) REFERENCES jobs(job_id)'),
    constraint_rec('employees', 'emp_manager_fk', 'F', 'FOREIGN KEY (manager_id) REFERENCES employees(employee_id)'),
    constraint_rec('job_history', 'jhist_dept_fk', 'F', 'FOREIGN KEY (department_id) REFERENCES departments(department_id)'),
    constraint_rec('job_history', 'jhist_emp_fk', 'F', 'FOREIGN KEY (employee_id) REFERENCES employees(employee_id)'),
    constraint_rec('job_history', 'jhist_job_fk', 'F', 'FOREIGN KEY (job_id) REFERENCES jobs(job_id)'),
    
    -- Check constraints
    constraint_rec('employees', 'emp_salary_min', 'C', 'CHECK (salary > 0)'),
    constraint_rec('job_history', 'jhist_date_interval', 'C', 'CHECK (end_date > start_date)'),

    -- Unique constraints
    constraint_rec('departments', 'dept_name_uk', 'U', 'UNIQUE (department_name)'),
    constraint_rec('jobs', 'job_title_uk', 'U', 'UNIQUE (job_title)'),
    constraint_rec('employees', 'emp_email_uk', 'U', 'UNIQUE (email)')
  );
  
  v_sql VARCHAR2(2000);
  i NUMBER := 1;
BEGIN
  -- Process each constraint
  WHILE i <= v_constraints.COUNT LOOP
    v_sql := 'ALTER TABLE ' || v_constraints(i).table_name || 
             ' ADD CONSTRAINT ' || v_constraints(i).constraint_name || ' ' ||
             v_constraints(i).constraint_clause;
    
    BEGIN
      EXECUTE IMMEDIATE v_sql;
      DBMS_OUTPUT.PUT_LINE('SUCCESS: ' || v_sql);
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FAILED: ' || v_sql || ' - ' || SQLERRM);
    END;
    
    i := i + 1;
  END LOOP;
END;
/ 