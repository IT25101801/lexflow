-- Session Setting for Stored Procedures
SET QUOTED_IDENTIFIER ON;
GO

-- Process Wallet Transaction Procedure
CREATE PROCEDURE usp_record_wallet_transaction @client_id BIGINT,
                                               @amount DECIMAL(38, 2),
                                               @transaction_type VARCHAR(11),
                                               @invoice_id BIGINT,
                                               @occurred_at DATETIME2(3),
                                               @transaction_id BIGINT OUTPUT,
                                               @transaction_code VARCHAR(255) OUTPUT,
                                               @balance DECIMAL(38, 2) OUTPUT,
                                               @status_code INT OUTPUT
AS
BEGIN
    -- Procedure Options
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    -- Reset Outputs
    SET @transaction_id = NULL;
    SET @transaction_code = NULL;
    SET @balance = NULL;

    -- Check Amount
    IF @amount IS NULL OR @amount <= 0.00
        BEGIN
            SET @status_code = 1;
            RETURN;
        END;

    BEGIN TRY
        -- Start Transaction
        BEGIN TRANSACTION;

        -- Find and Lock Client
        DECLARE @current_balance DECIMAL(38, 2);

        SELECT @current_balance = wallet_balance
        FROM users
        WITH (UPDLOCK, ROWLOCK)
        WHERE user_id = @client_id
          AND role = 'CLIENT'
          AND account_status = 'ACTIVE';

        IF @current_balance IS NULL
            BEGIN
                SET @status_code = 2;
                ROLLBACK TRANSACTION;
                RETURN;
            END;

        -- Work Out Balance Change
        DECLARE @delta DECIMAL(38, 2);

        IF @transaction_type IN ('CHARGE', 'WITHDRAWAL')
            SET @delta = -@amount;
        ELSE
            SET @delta = @amount;

        -- Check Sufficient Funds
        IF @delta < 0.00 AND (@current_balance + @delta) < 0.00
            BEGIN
                SET @status_code = 3;
                SET @balance = @current_balance;
                ROLLBACK TRANSACTION;
                RETURN;
            END;

        -- Update Balance
        SET @balance = @current_balance + @delta;

        UPDATE users
        SET wallet_balance = @balance
        WHERE user_id = @client_id;

        -- Insert Ledger Row
        INSERT INTO transactions (client_id, amount, transaction_type, invoice_id, occurred_at)
        VALUES (@client_id, @amount, @transaction_type, @invoice_id, @occurred_at);

        -- Set Transaction Code
        SET @transaction_id = CAST(SCOPE_IDENTITY() AS BIGINT);
        SET @transaction_code = dbo.ufn_generate_transaction_code(@transaction_id);

        UPDATE transactions
        SET transaction_code = @transaction_code
        WHERE transaction_id = @transaction_id;

        -- Commit and Report Success
        COMMIT TRANSACTION;
        SET @status_code = 0;
    END TRY
    -- Error Handling
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            BEGIN
                ROLLBACK TRANSACTION;
            END;

        THROW;
    END CATCH;
END;
GO
