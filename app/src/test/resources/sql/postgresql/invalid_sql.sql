-- Invalid SQL that should raise an error
SELECT * FROM non_existent_table;

-- Invalid PL/pgSQL block
DO $$
BEGIN
    invalid_statement;
END;
$$; 