-- Session Setting for Triggers
SET QUOTED_IDENTIFIER ON;
GO

-- Prevent Deletion of Client With Outstanding Wallet Balance Trigger
CREATE TRIGGER trg_users_au_prevent_deletion_of_client_with_outstanding_wallet_balance
    ON users
    AFTER UPDATE
    AS
BEGIN
    -- Trigger Options
    SET NOCOUNT ON;

    -- Find Deleted Clients With Outstanding Wallet Balance
    IF EXISTS (SELECT 1
               FROM inserted i
               INNER JOIN deleted d
                   ON d.user_id = i.user_id
               WHERE i.role = 'CLIENT'
                 AND d.account_status = 'ACTIVE'
                 AND i.account_status = 'DELETED'
                 AND i.wallet_balance > 0.00)
        BEGIN
            -- Reject the Update
            THROW 50001, 'A client with an outstanding wallet balance cannot be deleted.', 1;
        END;
END;
GO

-- Prevent Deletion of Client, Associate, or Paralegal With an Active Consultation Trigger
CREATE TRIGGER trg_users_au_prevent_deletion_of_user_with_active_consultation
    ON users
    AFTER UPDATE
    AS
BEGIN
    -- Trigger Options
    SET NOCOUNT ON;

    -- Find Deleted Users Assigned to an Active Consultation
    IF EXISTS (SELECT 1
               FROM inserted i
               INNER JOIN deleted d
                   ON d.user_id = i.user_id
               WHERE d.account_status = 'ACTIVE'
                 AND i.account_status = 'DELETED'
                 AND EXISTS (SELECT 1
                             FROM consultations cn
                             WHERE (cn.consultation_status IN ('PENDING', 'ACCEPTED_BY_PARTNER', 'SCHEDULED') OR (cn.consultation_status = 'ACCEPTED_BY_ASSOCIATE' AND NOT EXISTS (SELECT 1
                                                                                                                                                                                   FROM cases cs
                                                                                                                                                                                   WHERE cs.consultation_id = cn.consultation_id)))
                               AND (cn.client_id = i.user_id OR cn.associate_id = i.user_id OR cn.paralegal_id = i.user_id)))
        BEGIN
            -- Reject the Update
            THROW 50002, 'A user assigned to an active consultation cannot be deleted.', 1;
        END;
END;
GO

-- Prevent Deletion of Client, Associate, or Paralegal With an Open Case Trigger
CREATE TRIGGER trg_users_au_prevent_deletion_of_user_with_open_case
    ON users
    AFTER UPDATE
    AS
BEGIN
    -- Trigger Options
    SET NOCOUNT ON;

    -- Find Deleted Users Assigned to an Open Case
    IF EXISTS (SELECT 1
               FROM inserted i
               INNER JOIN deleted d
                   ON d.user_id = i.user_id
               WHERE d.account_status = 'ACTIVE'
                 AND i.account_status = 'DELETED'
                 AND EXISTS (SELECT 1
                             FROM cases cs
                             WHERE cs.case_status = 'OPEN'
                               AND (cs.client_id = i.user_id OR cs.associate_id = i.user_id OR cs.paralegal_id = i.user_id)))
        BEGIN
            -- Reject the Update
            THROW 50003, 'A user assigned to an open case cannot be deleted.', 1;
        END;
END;
GO

-- Prevent Deletion of Client With an Unpaid Invoice Trigger
CREATE TRIGGER trg_users_au_prevent_deletion_of_client_with_unpaid_invoice
    ON users
    AFTER UPDATE
    AS
BEGIN
    -- Trigger Options
    SET NOCOUNT ON;

    -- Find Deleted Clients With an Unpaid Invoice
    IF EXISTS (SELECT 1
               FROM inserted i
               INNER JOIN deleted d
                   ON d.user_id = i.user_id
               WHERE i.role = 'CLIENT'
                 AND d.account_status = 'ACTIVE'
                 AND i.account_status = 'DELETED'
                 AND EXISTS (SELECT 1
                             FROM invoices inv
                             INNER JOIN cases cs
                                 ON cs.case_id = inv.case_id
                             WHERE inv.invoice_status = 'UNPAID'
                               AND cs.client_id = i.user_id))
        BEGIN
            -- Reject the Update
            THROW 50004, 'A client with an unpaid invoice cannot be deleted.', 1;
        END;
END;
GO
