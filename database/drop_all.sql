-- Drop All Functions
DECLARE @sql NVARCHAR(MAX) = N'';

SELECT @sql += 'DROP FUNCTION ' + QUOTENAME(SCHEMA_NAME(schema_id)) + '.' + QUOTENAME(name) + ';' + CHAR(10)
FROM sys.objects
WHERE type IN ('FN', 'IF', 'TF');

EXEC sp_executesql @sql;

SET @sql = N'';

-- Drop All Stored Procedures
SELECT @sql += 'DROP PROCEDURE ' + QUOTENAME(SCHEMA_NAME(schema_id)) + '.' + QUOTENAME(name) + ';' + CHAR(10)
FROM sys.procedures;

EXEC sp_executesql @sql;

SET @sql = N'';

-- Drop All Triggers
SELECT @sql += 'DROP TRIGGER ' + QUOTENAME(SCHEMA_NAME(t.schema_id)) + '.' + QUOTENAME(tr.name) + ';' + CHAR(10)
FROM sys.triggers tr
INNER JOIN sys.tables t
    ON tr.parent_id = t.object_id
WHERE tr.parent_class = 1;

EXEC sp_executesql @sql;

SET @sql = N'';

-- Drop All Foreign Keys
SELECT @sql += 'ALTER TABLE ' + QUOTENAME(SCHEMA_NAME(t.schema_id)) + '.' + QUOTENAME(t.name) + ' DROP CONSTRAINT ' + QUOTENAME(fk.name) + ';' + CHAR(10)
FROM sys.foreign_keys fk
INNER JOIN sys.tables t
    ON fk.parent_object_id = t.object_id;

EXEC sp_executesql @sql;

SET @sql = N'';

-- Drop All Tables
SELECT @sql += 'DROP TABLE ' + QUOTENAME(SCHEMA_NAME(schema_id)) + '.' + QUOTENAME(name) + ';' + CHAR(10)
FROM sys.tables;

EXEC sp_executesql @sql;
