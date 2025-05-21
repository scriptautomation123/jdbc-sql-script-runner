--NOSONAR

-- Set schema
SELECT set_config('search_path', 'hr', false);

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

-- Load Locations
INSERT INTO locations VALUES (1000, '1297 Via Cola di Rie', '00989', 'Roma', NULL, 'IT');
INSERT INTO locations VALUES (1100, '93091 Calle della Testa', '10934', 'Venice', NULL, 'IT');
INSERT INTO locations VALUES (1200, '2017 Shinjuku-ku', '1689', 'Tokyo', 'Tokyo Prefecture', 'JP');
INSERT INTO locations VALUES (1300, '9450 Kamiya-cho', '6823', 'Hiroshima', NULL, 'JP');
INSERT INTO locations VALUES (1400, '2014 Jabberwocky Rd', '26192', 'Southlake', 'Texas', 'US');

-- Load Departments
INSERT INTO departments VALUES (10, 'Administration', 200, 1100);
INSERT INTO departments VALUES (20, 'Marketing', 201, 1200);
INSERT INTO departments VALUES (30, 'Purchasing', 114, 1300);
INSERT INTO departments VALUES (40, 'Human Resources', 203, 1400);
INSERT INTO departments VALUES (50, 'Shipping', 121, 1000);

-- Load Jobs
INSERT INTO jobs VALUES ('AD_PRES', 'President', 20000, 40000);
INSERT INTO jobs VALUES ('AD_VP', 'Administration Vice President', 15000, 30000);
INSERT INTO jobs VALUES ('AD_ASST', 'Administration Assistant', 3000, 6000);
INSERT INTO jobs VALUES ('FI_MGR', 'Finance Manager', 8200, 16000);
INSERT INTO jobs VALUES ('FI_ACCOUNT', 'Accountant', 4200, 9000);

-- Load Employees
INSERT INTO employees VALUES 
(100, 'Steven', 'King', 'SKING', '515.123.4567', '1987-06-17', 'AD_PRES', 24000, NULL, NULL, 90);
INSERT INTO employees VALUES 
(101, 'Neena', 'Kochhar', 'NKOCHHAR', '515.123.4568', '1989-09-21', 'AD_VP', 17000, NULL, 100, 90);
INSERT INTO employees VALUES 
(102, 'Lex', 'De Haan', 'LDEHAAN', '515.123.4569', '1993-01-13', 'AD_VP', 17000, NULL, 100, 90);
INSERT INTO employees VALUES 
(103, 'Alexander', 'Hunold', 'AHUNOLD', '590.423.4567', '1990-01-03', 'IT_PROG', 9000, NULL, 102, 60);
INSERT INTO employees VALUES 
(104, 'Bruce', 'Ernst', 'BERNST', '590.423.4568', '1991-05-21', 'IT_PROG', 6000, NULL, 103, 60);

-- Load Job History
INSERT INTO job_history VALUES 
(102, '1993-01-13', '1998-07-24', 'IT_PROG', 60);
INSERT INTO job_history VALUES 
(101, '1989-09-21', '1993-10-27', 'AC_ACCOUNT', 110);
INSERT INTO job_history VALUES 
(101, '1993-10-28', '1997-03-15', 'AC_MGR', 110);