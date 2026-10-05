-- Session Setting for Functions
SET QUOTED_IDENTIFIER ON;
GO

-- Generate Code Function
CREATE FUNCTION dbo.ufn_generate_code(@id BIGINT) RETURNS BIGINT
AS
BEGIN

    RETURN 10000000 + ((@id % 90000000) * 71234567 + 12345691) % 90000000;

END;
GO

-- Generate Transaction Code Function
CREATE FUNCTION dbo.ufn_generate_transaction_code(@transaction_id BIGINT) RETURNS VARCHAR(255)
AS
BEGIN

    RETURN 'TXN-' + CAST(@transaction_id AS VARCHAR(20)) + '-' + CAST(dbo.ufn_generate_code(@transaction_id) AS VARCHAR(20));

END;
GO
