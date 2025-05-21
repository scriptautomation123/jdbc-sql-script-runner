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

-- Load Regions
INSERT INTO regions VALUES (1, 'Europe');
INSERT INTO regions VALUES (2, 'Americas');
INSERT INTO regions VALUES (3, 'Asia');
INSERT INTO regions VALUES (4, 'Middle East and Africa');

-- Load Countries
INSERT INTO countries VALUES ('IT', 'Italy', 1);
INSERT INTO countries VALUES ('JP', 'Japan', 3);
INSERT INTO countries VALUES ('US', 'United States of America', 2);
INSERT INTO countries VALUES ('CA', 'Canada', 2);
INSERT INTO countries VALUES ('CN', 'China', 3);
INSERT INTO countries VALUES ('IN', 'India', 3);
INSERT INTO countries VALUES ('AU', 'Australia', 3);
INSERT INTO countries VALUES ('ZW', 'Zimbabwe', 4);
INSERT INTO countries VALUES ('SG', 'Singapore', 3);
INSERT INTO countries VALUES ('UK', 'United Kingdom', 1);
INSERT INTO countries VALUES ('FR', 'France', 1);
INSERT INTO countries VALUES ('DE', 'Germany', 1);
INSERT INTO countries VALUES ('ZM', 'Zambia', 4);
INSERT INTO countries VALUES ('EG', 'Egypt', 4);
INSERT INTO countries VALUES ('BR', 'Brazil', 2);
INSERT INTO countries VALUES ('CH', 'Switzerland', 1);
INSERT INTO countries VALUES ('NL', 'Netherlands', 1);
INSERT INTO countries VALUES ('MX', 'Mexico', 2);
INSERT INTO countries VALUES ('KW', 'Kuwait', 4);
INSERT INTO countries VALUES ('IL', 'Israel', 4);
INSERT INTO countries VALUES ('DK', 'Denmark', 1);
INSERT INTO countries VALUES ('HK', 'HongKong', 3);
INSERT INTO countries VALUES ('NG', 'Nigeria', 4);
INSERT INTO countries VALUES ('AR', 'Argentina', 2);
INSERT INTO countries VALUES ('BE', 'Belgium', 1);

-- Load sample data for locations
INSERT INTO locations VALUES (1000, '1297 Via Cola di Rie', '00989', 'Roma', NULL, 'IT');
INSERT INTO locations VALUES (1100, '93091 Calle della Testa', '10934', 'Venice', NULL, 'IT');
INSERT INTO locations VALUES (1200, '2017 Shinjuku-ku', '1689', 'Tokyo', 'Tokyo Prefecture', 'JP');
INSERT INTO locations VALUES (1300, '9450 Kamiya-cho', '6823', 'Hiroshima', NULL, 'JP');
INSERT INTO locations VALUES (1400, '2014 Jabberwocky Rd', '26192', 'Southlake', 'Texas', 'US');
INSERT INTO locations VALUES (1700, '2004 Charade Rd', '98199', 'Seattle', 'Washington', 'US');

-- Insert jobs
INSERT INTO jobs VALUES ('AD_PRES', 'President', 20000, 40000);

INSERT INTO jobs VALUES ('AD_VP', 'Administration Vice President', 15000, 30000);
INSERT INTO jobs VALUES ('AD_ASST', 'Administration Assistant', 3000, 6000);
INSERT INTO jobs VALUES ('FI_MGR', 'Finance Manager', 8200, 16000);
INSERT INTO jobs VALUES ('FI_ACCOUNT', 'Accountant', 4200, 9000);
INSERT INTO jobs VALUES ('IT_PROG', 'Programmer', 4000, 10000);
INSERT INTO jobs VALUES ('AC_ACCOUNT', 'Public Accountant', 4200, 9000);
INSERT INTO jobs VALUES ('AC_MGR', 'Accounting Manager', 8200, 16000);

-- Create ALL departments first (before any employee references them)
INSERT INTO departments VALUES (10, 'Administration', NULL, 1100);
INSERT INTO departments VALUES (20, 'Marketing', NULL, 1200);
INSERT INTO departments VALUES (30, 'Purchasing', NULL, 1300);
INSERT INTO departments VALUES (40, 'Human Resources', NULL, 1400);
INSERT INTO departments VALUES (50, 'Shipping', NULL, 1000);
INSERT INTO departments VALUES (60, 'IT', NULL, 1400);
INSERT INTO departments VALUES (90, 'Executive', NULL, 1100);
INSERT INTO departments VALUES (110, 'Accounting', NULL, 1700);

-- Now create employees
-- Create Steven King (President) first as he has no manager
INSERT INTO employees VALUES (100, 'Steven', 'King', 'SKING', '515.123.4567', 
                             TO_DATE('17-JUN-1987', 'dd-MON-yyyy'), 'AD_PRES', 24000, NULL, NULL, 90);

-- Create VPs that report to King
INSERT INTO employees VALUES (101, 'Neena', 'Kochhar', 'NKOCHHAR', '515.123.4568', 
                             TO_DATE('21-SEP-1989', 'dd-MON-yyyy'), 'AD_VP', 17000, NULL, 100, 90);
INSERT INTO employees VALUES (102, 'Lex', 'De Haan', 'LDEHAAN', '515.123.4569', 
                             TO_DATE('13-JAN-1993', 'dd-MON-yyyy'), 'AD_VP', 17000, NULL, 100, 90);

-- Create department managers
INSERT INTO employees VALUES (114, 'Den', 'Raphaely', 'DRAPHEAL', '515.127.4561', 
                             TO_DATE('07-DEC-1994', 'dd-MON-yyyy'), 'AD_VP', 11000, NULL, 100, 30);
INSERT INTO employees VALUES (121, 'Adam', 'Fripp', 'AFRIPP', '650.123.2234', 
                             TO_DATE('10-APR-1997', 'dd-MON-yyyy'), 'AD_VP', 8200, NULL, 100, 50);
INSERT INTO employees VALUES (200, 'Jennifer', 'Whalen', 'JWHALEN', '515.123.4444', 
                             TO_DATE('17-SEP-1987', 'dd-MON-yyyy'), 'AD_ASST', 4400, NULL, 101, 10);
INSERT INTO employees VALUES (201, 'Michael', 'Hartstein', 'MHARTSTE', '515.123.5555', 
                             TO_DATE('17-FEB-1996', 'dd-MON-yyyy'), 'AD_VP', 13000, NULL, 100, 20);
INSERT INTO employees VALUES (203, 'Susan', 'Mavris', 'SMAVRIS', '515.123.7777', 
                             TO_DATE('07-JUN-1994', 'dd-MON-yyyy'), 'AD_VP', 6500, NULL, 101, 40);
INSERT INTO employees VALUES (205, 'Shelley', 'Higgins', 'SHIGGINS', '515.123.8080', 
                             TO_DATE('07-JUN-1994', 'dd-MON-yyyy'), 'AC_MGR', 12000, NULL, 101, 110);

-- Update departments with manager IDs after creating the employees
UPDATE departments SET manager_id = 200 WHERE department_id = 10;
UPDATE departments SET manager_id = 201 WHERE department_id = 20;
UPDATE departments SET manager_id = 114 WHERE department_id = 30;
UPDATE departments SET manager_id = 203 WHERE department_id = 40;
UPDATE departments SET manager_id = 121 WHERE department_id = 50;
UPDATE departments SET manager_id = 102 WHERE department_id = 60;
UPDATE departments SET manager_id = 100 WHERE department_id = 90;
UPDATE departments SET manager_id = 205 WHERE department_id = 110;

-- Create other employees
INSERT INTO employees VALUES (103, 'Alexander', 'Hunold', 'AHUNOLD', '590.423.4567', 
                            TO_DATE('03-JAN-1990', 'dd-MON-yyyy'), 'IT_PROG', 9000, NULL, 102, 60);
INSERT INTO employees VALUES (104, 'Bruce', 'Ernst', 'BERNST', '590.423.4568', 
                            TO_DATE('21-MAY-1991', 'dd-MON-yyyy'), 'IT_PROG', 6000, NULL, 103, 60);

-- Sample job history
INSERT INTO job_history VALUES (102, TO_DATE('13-JAN-1993', 'dd-MON-yyyy'), 
                              TO_DATE('24-JUL-1998', 'dd-MON-yyyy'), 'IT_PROG', 60);
INSERT INTO job_history VALUES (101, TO_DATE('21-SEP-1989', 'dd-MON-yyyy'), 
                              TO_DATE('27-OCT-1993', 'dd-MON-yyyy'), 'AC_ACCOUNT', 110);
INSERT INTO job_history VALUES (101, TO_DATE('28-OCT-1993', 'dd-MON-yyyy'), 
                              TO_DATE('15-MAR-1997', 'dd-MON-yyyy'), 'AC_MGR', 110);

COMMIT; 