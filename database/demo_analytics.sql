-- Clear Database

DECLARE
    @sql NVARCHAR(MAX) = N'';

-- Drop All Functions

SELECT @sql += 'DROP FUNCTION ' + QUOTENAME(SCHEMA_NAME(schema_id)) + '.' + QUOTENAME(name) + ';' + CHAR(10)
FROM sys.objects
WHERE type IN ('FN', 'IF', 'TF');

EXEC sp_executesql @sql;

SET
    @sql = N'';

-- Drop All Stored Procedures

SELECT @sql += 'DROP PROCEDURE ' + QUOTENAME(SCHEMA_NAME(schema_id)) + '.' + QUOTENAME(name) + ';' + CHAR(10)
FROM sys.procedures;

EXEC sp_executesql @sql;

SET
    @sql = N'';

-- Drop All Triggers

SELECT @sql += 'DROP TRIGGER ' + QUOTENAME(SCHEMA_NAME(t.schema_id)) + '.' + QUOTENAME(tr.name) + ';' + CHAR(10)
FROM sys.triggers tr
INNER JOIN sys.tables t
    ON tr.parent_id = t.object_id
WHERE tr.parent_class = 1;

EXEC sp_executesql @sql;

SET
    @sql = N'';

-- Drop All Foreign Keys

SELECT @sql += 'ALTER TABLE ' + QUOTENAME(SCHEMA_NAME(t.schema_id)) + '.' + QUOTENAME(t.name) + ' DROP CONSTRAINT ' + QUOTENAME(fk.name) + ';' + CHAR(10)
FROM sys.foreign_keys fk
INNER JOIN sys.tables t
    ON fk.parent_object_id = t.object_id;

EXEC sp_executesql @sql;

SET
    @sql = N'';

-- Drop All Tables

SELECT @sql += 'DROP TABLE ' + QUOTENAME(SCHEMA_NAME(schema_id)) + '.' + QUOTENAME(name) + ';' + CHAR(10)
FROM sys.tables;

EXEC sp_executesql @sql;


-- Recreate Schema

-- Session Setting for Filtered Unique Indexes
SET
    QUOTED_IDENTIFIER ON;

-- Users Table

CREATE TABLE users (
    user_id        BIGINT IDENTITY (1, 1) NOT NULL,
    role           VARCHAR(24)            NOT NULL,
    user_code      VARCHAR(255),
    email          VARCHAR(255)           NOT NULL,
    password       VARCHAR(255)           NOT NULL,
    name           VARCHAR(MAX)           NOT NULL,
    date_of_birth  DATE                   NOT NULL,
    gender         VARCHAR(6)             NOT NULL,
    nic_number     VARCHAR(12)            NOT NULL,
    account_status VARCHAR(7)             NOT NULL,
    registered_at  DATETIME2(3)           NOT NULL,
    bar_number     VARCHAR(10),
    wallet_balance DECIMAL(38, 2),

    -- Primary Key
    CONSTRAINT pk_users PRIMARY KEY (user_id),

    -- Unique Constraints
    CONSTRAINT uq_users_user_code UNIQUE (user_code),

    -- Check Constraints
    CONSTRAINT ck_users_role CHECK (role IN ('PARTNER', 'ASSOCIATE', 'PARALEGAL', 'CLIENT', 'CLIENT_RELATIONS_OFFICER', 'FINANCE_OFFICER')),
    CONSTRAINT ck_users_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT ck_users_nic_number CHECK (
        nic_number LIKE '[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][VXvx]' OR
        nic_number LIKE '[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]'
        ),
    CONSTRAINT ck_users_account_status CHECK (account_status IN ('ACTIVE', 'DELETED')),
    CONSTRAINT ck_users_bar_number_role CHECK (
        (role IN ('PARTNER', 'ASSOCIATE') AND bar_number IS NOT NULL) OR
        (role NOT IN ('PARTNER', 'ASSOCIATE') AND bar_number IS NULL)
        ),
    CONSTRAINT ck_users_wallet_balance_role CHECK (
        (role = 'CLIENT' AND wallet_balance IS NOT NULL) OR
        (role <> 'CLIENT' AND wallet_balance IS NULL)
        ),
    CONSTRAINT ck_users_wallet_balance_non_negative CHECK (
        wallet_balance IS NULL OR
        wallet_balance >= 0.00
        )
);

-- Filtered Unique Index: Email
CREATE UNIQUE INDEX ux_users_email ON users (email) WHERE account_status = 'ACTIVE';

-- Filtered Unique Index: NIC Number
CREATE UNIQUE INDEX ux_users_nic_number ON users (nic_number) WHERE account_status = 'ACTIVE';

-- Filtered Unique Index: Bar Number
CREATE UNIQUE INDEX ux_users_bar_number ON users (bar_number) WHERE bar_number IS NOT NULL AND account_status = 'ACTIVE';

-- User Addresses Table
CREATE TABLE user_addresses (
    user_id BIGINT       NOT NULL,
    address VARCHAR(800) NOT NULL,
    
    -- Primary Key
    CONSTRAINT pk_user_addresses PRIMARY KEY (user_id, address)
);

-- User Phone Numbers Table
CREATE TABLE user_phone_numbers (
    user_id      BIGINT      NOT NULL,
    phone_number VARCHAR(10) NOT NULL,
    
    -- Primary Key
    CONSTRAINT pk_user_phone_numbers PRIMARY KEY (user_id, phone_number),
    
    -- Check Constraints
    CONSTRAINT ck_user_phone_numbers_phone_number CHECK (phone_number LIKE '0[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]')
);

-- User Addresses Foreign Keys
ALTER TABLE user_addresses
    ADD CONSTRAINT fk_user_addresses_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;

-- User Phone Numbers Foreign Keys
ALTER TABLE user_phone_numbers
    ADD CONSTRAINT fk_user_phone_numbers_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;

-- Consultations Table
CREATE TABLE consultations (
    consultation_id     BIGINT IDENTITY (1, 1) NOT NULL,
    consultation_code   VARCHAR(255),
    client_id           BIGINT                 NOT NULL,
    case_type           VARCHAR(21)            NOT NULL,
    description         VARCHAR(MAX)           NOT NULL,
    consultation_status VARCHAR(21)            NOT NULL,
    submitted_at        DATETIME2(3)           NOT NULL,
    associate_id        BIGINT,
    paralegal_id        BIGINT,
    rejection_reason    VARCHAR(MAX),

    -- Primary Key
    CONSTRAINT pk_consultations PRIMARY KEY (consultation_id),

    -- Unique Constraints
    CONSTRAINT uq_consultations_consultation_code UNIQUE (consultation_code),

    -- Check Constraints
    CONSTRAINT ck_consultations_case_type CHECK (case_type IN
                                                 ('CIVIL', 'CONTRACT_COMMERCIAL', 'CORPORATE', 'CRIMINAL', 'EMPLOYMENT', 'FAMILY', 'IMMIGRATION', 'INTELLECTUAL_PROPERTY', 'PROPERTY', 'OTHER')),
    CONSTRAINT ck_consultations_consultation_status CHECK (consultation_status IN
                                                           ('PENDING', 'REJECTED_BY_PARTNER', 'ACCEPTED_BY_PARTNER', 'SCHEDULED', 'REJECTED_BY_ASSOCIATE', 'ACCEPTED_BY_ASSOCIATE')),
    CONSTRAINT ck_consultations_associate_id_consultation_status CHECK (
        (consultation_status NOT IN ('PENDING', 'REJECTED_BY_PARTNER') AND associate_id IS NOT NULL) OR
        (consultation_status IN ('PENDING', 'REJECTED_BY_PARTNER') AND associate_id IS NULL)
        ),
    CONSTRAINT ck_consultations_paralegal_id_consultation_status CHECK (
        (consultation_status NOT IN ('PENDING', 'REJECTED_BY_PARTNER') AND paralegal_id IS NOT NULL) OR
        (consultation_status IN ('PENDING', 'REJECTED_BY_PARTNER') AND paralegal_id IS NULL)
        ),
    CONSTRAINT ck_consultations_rejection_reason_consultation_status CHECK (
        (consultation_status IN ('REJECTED_BY_PARTNER', 'REJECTED_BY_ASSOCIATE') AND rejection_reason IS NOT NULL) OR
        (consultation_status NOT IN ('REJECTED_BY_PARTNER', 'REJECTED_BY_ASSOCIATE') AND rejection_reason IS NULL)
        )
);

-- Consultation Opposing Parties Table
CREATE TABLE consultation_opposing_parties (
    consultation_id BIGINT       NOT NULL,
    opposing_party  VARCHAR(800) NOT NULL,
    
    -- Primary Key
    CONSTRAINT pk_consultation_opposing_parties PRIMARY KEY (consultation_id, opposing_party)
);

-- Consultations Foreign Keys
ALTER TABLE consultations
    ADD CONSTRAINT fk_consultations_client_id FOREIGN KEY (client_id) REFERENCES users (user_id),
        CONSTRAINT fk_consultations_associate_id FOREIGN KEY (associate_id) REFERENCES users (user_id),
        CONSTRAINT fk_consultations_paralegal_id FOREIGN KEY (paralegal_id) REFERENCES users (user_id);

-- Consultation Opposing Parties Foreign Keys
ALTER TABLE consultation_opposing_parties
    ADD CONSTRAINT fk_consultation_opposing_parties_consultation_id FOREIGN KEY (consultation_id) REFERENCES consultations (consultation_id) ON DELETE CASCADE;

-- Cases Table
CREATE TABLE cases (
    case_id                BIGINT IDENTITY (1, 1) NOT NULL,
    case_code              VARCHAR(255),
    consultation_id        BIGINT                 NOT NULL,
    client_id              BIGINT                 NOT NULL,
    associate_id           BIGINT                 NOT NULL,
    paralegal_id           BIGINT                 NOT NULL,
    case_status            VARCHAR(6)             NOT NULL,
    opened_at              DATETIME2(3)           NOT NULL,
    case_resolution_status VARCHAR(9),
    closed_at              DATETIME2(3),
    
    -- Primary Key
    CONSTRAINT pk_cases PRIMARY KEY (case_id),
    
    -- Unique Constraints
    CONSTRAINT uq_cases_case_code UNIQUE (case_code),
    CONSTRAINT uq_cases_consultation_id UNIQUE (consultation_id),
    
    -- Check Constraints
    CONSTRAINT ck_cases_case_status CHECK (case_status IN ('OPEN', 'CLOSED')),
    CONSTRAINT ck_cases_case_resolution_status CHECK (case_resolution_status IN ('WON', 'LOST', 'SETTLED', 'DISMISSED')),
    CONSTRAINT ck_cases_case_resolution_status_case_status CHECK (
        (case_status = 'CLOSED' AND case_resolution_status IS NOT NULL) OR
        (case_status <> 'CLOSED' AND case_resolution_status IS NULL)
        ),
    CONSTRAINT ck_cases_closed_at_case_status CHECK (
        (case_status = 'CLOSED' AND closed_at IS NOT NULL) OR
        (case_status <> 'CLOSED' AND closed_at IS NULL)
        )
);

-- Cases Foreign Keys
ALTER TABLE cases
    ADD CONSTRAINT fk_cases_consultation_id FOREIGN KEY (consultation_id) REFERENCES consultations (consultation_id),
        CONSTRAINT fk_cases_client_id FOREIGN KEY (client_id) REFERENCES users (user_id),
        CONSTRAINT fk_cases_associate_id FOREIGN KEY (associate_id) REFERENCES users (user_id),
        CONSTRAINT fk_cases_paralegal_id FOREIGN KEY (paralegal_id) REFERENCES users (user_id);

-- Contacts Table
CREATE TABLE contacts (
    contact_id BIGINT IDENTITY (1, 1) NOT NULL,
    case_id    BIGINT                 NOT NULL,
    name       VARCHAR(MAX),
    notes      VARCHAR(MAX),
    added_at   DATETIME2(3)           NOT NULL,

    -- Primary Key
    CONSTRAINT pk_contacts PRIMARY KEY (contact_id)
);

-- Contact Phone Numbers Table
CREATE TABLE contact_phone_numbers (
    contact_id   BIGINT      NOT NULL,
    phone_number VARCHAR(10) NOT NULL,
    
    -- Primary Key
    CONSTRAINT pk_contact_phone_numbers PRIMARY KEY (contact_id, phone_number),
    
    -- Check Constraints
    CONSTRAINT ck_contact_phone_numbers_phone_number CHECK (phone_number LIKE '0[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]')
);

-- Contact Emails Table
CREATE TABLE contact_emails (
    contact_id BIGINT       NOT NULL,
    email      VARCHAR(255) NOT NULL,
    
    -- Primary Key
    CONSTRAINT pk_contact_emails PRIMARY KEY (contact_id, email)
);

-- Contact Addresses Table
CREATE TABLE contact_addresses (
    contact_id BIGINT       NOT NULL,
    address    VARCHAR(800) NOT NULL,
    
    -- Primary Key
    CONSTRAINT pk_contact_addresses PRIMARY KEY (contact_id, address)
);

-- Contacts Foreign Keys
ALTER TABLE contacts
    ADD CONSTRAINT fk_contacts_case_id FOREIGN KEY (case_id) REFERENCES cases (case_id);

-- Contact Phone Numbers Foreign Keys
ALTER TABLE contact_phone_numbers
    ADD CONSTRAINT fk_contact_phone_numbers_contact_id FOREIGN KEY (contact_id) REFERENCES contacts (contact_id) ON DELETE CASCADE;

-- Contact Emails Foreign Keys
ALTER TABLE contact_emails
    ADD CONSTRAINT fk_contact_emails_contact_id FOREIGN KEY (contact_id) REFERENCES contacts (contact_id) ON DELETE CASCADE;

-- Contact Addresses Foreign Keys
ALTER TABLE contact_addresses
    ADD CONSTRAINT fk_contact_addresses_contact_id FOREIGN KEY (contact_id) REFERENCES contacts (contact_id) ON DELETE CASCADE;

-- Invoices Table
CREATE TABLE invoices (
    invoice_id     BIGINT IDENTITY (1, 1) NOT NULL,
    invoice_code   VARCHAR(255),
    case_id        BIGINT                 NOT NULL,
    total_amount   DECIMAL(38, 2)         NOT NULL,
    invoice_status VARCHAR(6)             NOT NULL,
    compiled_at    DATETIME2(3)           NOT NULL,
    paid_at        DATETIME2(3),
    
    -- Primary Key
    CONSTRAINT pk_invoices PRIMARY KEY (invoice_id),
    
    -- Unique Constraints
    CONSTRAINT uq_invoices_invoice_code UNIQUE (invoice_code),
    
    -- Check Constraints
    CONSTRAINT ck_invoices_invoice_status CHECK (invoice_status IN ('UNPAID', 'PAID', 'VOIDED')),
    CONSTRAINT ck_invoices_paid_at_invoice_status CHECK (
        paid_at IS NULL OR
        invoice_status IN ('PAID', 'VOIDED')
        )
);

-- Invoices Foreign Keys
ALTER TABLE invoices
    ADD CONSTRAINT fk_invoices_case_id FOREIGN KEY (case_id) REFERENCES cases (case_id);

-- Transactions Table
CREATE TABLE transactions (
    transaction_id   BIGINT IDENTITY (1, 1) NOT NULL,
    transaction_code VARCHAR(255),
    client_id        BIGINT                 NOT NULL,
    amount           DECIMAL(38, 2)         NOT NULL,
    transaction_type VARCHAR(11)            NOT NULL,
    invoice_id       BIGINT,
    occurred_at      DATETIME2(3)           NOT NULL,
    
    -- Primary Key
    CONSTRAINT pk_transactions PRIMARY KEY (transaction_id),
    
    -- Unique Constraints
    CONSTRAINT uq_transactions_transaction_code UNIQUE (transaction_code),
    
    -- Check Constraints
    CONSTRAINT ck_transactions_transaction_type CHECK (transaction_type IN ('CARD_TOP_UP', 'CASH_TOP_UP', 'CHARGE', 'REFUND', 'WITHDRAWAL')),
    CONSTRAINT ck_transactions_invoice_id_transaction_type CHECK (
        (transaction_type IN ('CHARGE', 'REFUND') AND invoice_id IS NOT NULL) OR
        (transaction_type IN ('CARD_TOP_UP', 'CASH_TOP_UP', 'WITHDRAWAL') AND invoice_id IS NULL)
        )
);

-- Transactions Foreign Keys
ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_client_id FOREIGN KEY (client_id) REFERENCES users (user_id),
        CONSTRAINT fk_transactions_invoice_id FOREIGN KEY (invoice_id) REFERENCES invoices (invoice_id);

-- Tasks Table
CREATE TABLE tasks (
    task_id           BIGINT IDENTITY (1, 1) NOT NULL,
    case_id           BIGINT                 NOT NULL,
    title             VARCHAR(255)           NOT NULL,
    documenting_task  BIT                    NOT NULL,
    scheduling_task   BIT                    NOT NULL,
    billing_task      BIT                    NOT NULL,
    billable_amount   DECIMAL(38, 2),
    billed_invoice_id BIGINT,
    logged_at         DATETIME2(3)           NOT NULL,
    
    -- Primary Key
    CONSTRAINT pk_tasks PRIMARY KEY (task_id),
    
    -- Check Constraints
    CONSTRAINT ck_tasks_billable_amount_billing_task CHECK (
        (billing_task = 1 AND billable_amount IS NOT NULL) OR
        (billing_task = 0 AND billable_amount IS NULL)
        ),
    CONSTRAINT ck_tasks_billed_invoice_id_billing_task CHECK (
        billed_invoice_id IS NULL OR
        billing_task = 1
        )
);

-- Tasks Foreign Keys
ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_case_id FOREIGN KEY (case_id) REFERENCES cases (case_id),
        CONSTRAINT fk_tasks_billed_invoice_id FOREIGN KEY (billed_invoice_id) REFERENCES invoices (invoice_id);

-- Events Table
CREATE TABLE events (
    event_id        BIGINT IDENTITY (1, 1) NOT NULL,
    event_type      VARCHAR(18)            NOT NULL,
    subject         VARCHAR(255)           NOT NULL,
    date            DATE                   NOT NULL,
    start_time      TIME(0)                NOT NULL,
    end_time        TIME(0)                NOT NULL,
    location        VARCHAR(255)           NOT NULL,
    notes           VARCHAR(MAX),
    event_status    VARCHAR(9)             NOT NULL,
    scheduled_at    DATETIME2(3)           NOT NULL,
    completed_at    DATETIME2(3),
    canceled_at     DATETIME2(3),
    consultation_id BIGINT,
    case_id         BIGINT,
    task_id         BIGINT,

    -- Primary Key
    CONSTRAINT pk_events PRIMARY KEY (event_id),

    -- Check Constraints
    CONSTRAINT ck_events_event_type CHECK (event_type IN ('CONSULTATION_EVENT', 'CASE_EVENT')),
    CONSTRAINT ck_events_event_status CHECK (event_status IN ('SCHEDULED', 'COMPLETED', 'CANCELED')),
    CONSTRAINT ck_events_completed_at_event_status CHECK (
        (event_status = 'COMPLETED' AND completed_at IS NOT NULL) OR
        (event_status <> 'COMPLETED' AND completed_at IS NULL)
        ),
    CONSTRAINT ck_events_canceled_at_event_status CHECK (
        (event_status = 'CANCELED' AND canceled_at IS NOT NULL) OR
        (event_status <> 'CANCELED' AND canceled_at IS NULL)
        ),
    CONSTRAINT ck_events_consultation_id_event_type CHECK (
        (event_type = 'CONSULTATION_EVENT' AND consultation_id IS NOT NULL) OR
        (event_type <> 'CONSULTATION_EVENT' AND consultation_id IS NULL)
        ),
    CONSTRAINT ck_events_case_id_event_type CHECK (
        (event_type = 'CASE_EVENT' AND case_id IS NOT NULL) OR
        (event_type <> 'CASE_EVENT' AND case_id IS NULL)
        ),
    CONSTRAINT ck_events_task_id_event_type CHECK (
        (event_type = 'CASE_EVENT' AND task_id IS NOT NULL) OR
        (event_type <> 'CASE_EVENT' AND task_id IS NULL)
        )
);

-- Filtered Unique Index: Task ID
CREATE UNIQUE INDEX ux_events_task_id ON events (task_id) WHERE task_id IS NOT NULL;

-- Events Foreign Keys
ALTER TABLE events
    ADD CONSTRAINT fk_events_consultation_id FOREIGN KEY (consultation_id) REFERENCES consultations (consultation_id),
        CONSTRAINT fk_events_case_id FOREIGN KEY (case_id) REFERENCES cases (case_id),
        CONSTRAINT fk_events_task_id FOREIGN KEY (task_id) REFERENCES tasks (task_id);

-- Documents Table
CREATE TABLE documents (
    document_id       BIGINT IDENTITY (1, 1) NOT NULL,
    document_type     VARCHAR(21)            NOT NULL,
    file_path         VARCHAR(255)           NOT NULL,
    original_filename VARCHAR(255)           NOT NULL,
    uploaded_at       DATETIME2(3)           NOT NULL,
    user_id           BIGINT,
    consultation_id   BIGINT,
    case_id           BIGINT,
    in_staging        BIT,
    task_id           BIGINT,
    invoice_id        BIGINT,
    
    -- Primary Key
    CONSTRAINT pk_documents PRIMARY KEY (document_id),
    
    -- Check Constraints
    CONSTRAINT ck_documents_document_type CHECK (document_type IN ('NIC_DOCUMENT', 'BAR_DOCUMENT', 'CONSULTATION_DOCUMENT', 'CASE_DOCUMENT', 'INVOICE_DOCUMENT')),
    CONSTRAINT ck_documents_user_id_document_type CHECK (
        (document_type IN ('NIC_DOCUMENT', 'BAR_DOCUMENT') AND user_id IS NOT NULL) OR
        (document_type NOT IN ('NIC_DOCUMENT', 'BAR_DOCUMENT') AND user_id IS NULL)
        ),
    CONSTRAINT ck_documents_consultation_id_document_type CHECK (
        (document_type = 'CONSULTATION_DOCUMENT' AND consultation_id IS NOT NULL) OR
        (document_type <> 'CONSULTATION_DOCUMENT' AND consultation_id IS NULL)
        ),
    CONSTRAINT ck_documents_case_id_document_type CHECK (
        (document_type = 'CASE_DOCUMENT' AND case_id IS NOT NULL) OR
        (document_type <> 'CASE_DOCUMENT' AND case_id IS NULL)
        ),
    CONSTRAINT ck_documents_in_staging_document_type CHECK (
        (document_type = 'CASE_DOCUMENT' AND in_staging IS NOT NULL) OR
        (document_type <> 'CASE_DOCUMENT' AND in_staging IS NULL)
        ),
    CONSTRAINT ck_documents_task_id_document_type CHECK (
        task_id IS NULL OR
        document_type = 'CASE_DOCUMENT'
        ),
    CONSTRAINT ck_documents_invoice_id_document_type CHECK (
        (document_type = 'INVOICE_DOCUMENT' AND invoice_id IS NOT NULL) OR
        (document_type <> 'INVOICE_DOCUMENT' AND invoice_id IS NULL)
        )
);

-- Filtered Unique Index: Invoice ID
CREATE UNIQUE INDEX ux_documents_invoice_id ON documents (invoice_id) WHERE invoice_id IS NOT NULL;

-- Documents Foreign Keys
ALTER TABLE documents
    ADD CONSTRAINT fk_documents_user_id FOREIGN KEY (user_id) REFERENCES users (user_id),
        CONSTRAINT fk_documents_consultation_id FOREIGN KEY (consultation_id) REFERENCES consultations (consultation_id),
        CONSTRAINT fk_documents_case_id FOREIGN KEY (case_id) REFERENCES cases (case_id),
        CONSTRAINT fk_documents_task_id FOREIGN KEY (task_id) REFERENCES tasks (task_id),
        CONSTRAINT fk_documents_invoice_id FOREIGN KEY (invoice_id) REFERENCES invoices (invoice_id);

-- Recreate Seed Data
-- Session Setting for Filtered Unique Indexes
SET
    QUOTED_IDENTIFIER ON;

-- Users Data

INSERT INTO users (role, user_code, email, password, name, date_of_birth, gender, nic_number, account_status, registered_at, bar_number, wallet_balance)
VALUES ('PARTNER',                  'PRT-1-00087866',   'asanka.kalansooriya@lexflow.lk',    '$2a$10$exr5DKqKQ8WgOALxZTVVVujnxhEvmpDBMAa6.Jw2XmVuKt246O0MK', 'Asanka Kalansooriya',     '1960-01-01', 'MALE',   '601003000V',   'ACTIVE', '2019-06-03 09:00:00.000', '0000000101', NULL    ),
       ('ASSOCIATE',                'ASC-2-00136137',   'chandana.medagoda@lexflow.lk',      '$2a$10$zT6h3NUKXGHZwgn2e9mx.elatvK3kiMkKVVVJJeQAgHREEgQAUne.', 'Chandana Medagoda',       '1967-02-02', 'MALE',   '671113037V',   'ACTIVE', '2019-06-12 09:00:00.000', '0000000102', NULL    ),
       ('ASSOCIATE',                'ASC-3-00184408',   'kaushalya.thenuwara@lexflow.lk',    '$2a$10$qrH30YWQcMYSAiCZaPXpK.kMLPuc9xy2FTpGqkC0fa/pL6cPDIrfa', 'Kaushalya Thenuwara',     '1974-03-03', 'FEMALE', '741223074V',   'ACTIVE', '2019-06-21 09:00:00.000', '0000000103', NULL    ),
       ('ASSOCIATE',                'ASC-4-00232679',   'gihan.dassanayake@lexflow.lk',      '$2a$10$Gn5pIZ/vAPAJyCaatBT8Juf3kK7AdnnPEmS/zaJauzn03QFiRGiOm', 'Gihan Dassanayake',       '1981-04-04', 'MALE',   '811333111V',   'ACTIVE', '2019-06-30 09:00:00.000', '0000000104', NULL    ),
       ('PARALEGAL',                'PRL-5-00280950',   'nadeeka.karunanayake@lexflow.lk',   '$2a$10$K6JQJio9F.xgV7Q2a4B/Wuv7jb7bzd0xYMsr.3cOjOoLwGtjgJASC', 'Nadeeka Karunanayake',    '1988-05-05', 'FEMALE', '881443148V',   'ACTIVE', '2019-07-09 09:00:00.000', NULL,         NULL    ),
       ('PARALEGAL',                'PRL-6-00329221',   'lasitha.wickremaratne@lexflow.lk',  '$2a$10$bWeLU2s/JBV5vLe/qmXR3e57RYWOqUOTUUoSDeSKOEPPvRQNdwqtq', 'Lasitha Wickremaratne',   '1960-06-06', 'MALE',   '601553185V',   'ACTIVE', '2019-07-18 09:00:00.000', NULL,         NULL    ),
       ('PARALEGAL',                'PRL-7-00377492',   'upeksha.senevirathne@lexflow.lk',   '$2a$10$e2dC5yKsb2qW7kBHv31uPO2CfyMX1l9jrxMG5p4rL9K.pGWN4rsbi', 'Upeksha Senevirathne',    '1967-07-07', 'FEMALE', '671663222V',   'ACTIVE', '2019-07-27 09:00:00.000', NULL,         NULL    ),
       ('CLIENT_RELATIONS_OFFICER', 'CRO-8-00425763',   'shehan.ilangakoon@lexflow.lk',      '$2a$10$TyuWoYjYbisnmRicjtC66uUXP8xL3ousMeMPa8o6rAufE4RPMBqve', 'Shehan Ilangakoon',       '1974-08-08', 'MALE',   '741773259V',   'ACTIVE', '2019-08-05 09:00:00.000', NULL,         NULL    ),
       ('CLIENT_RELATIONS_OFFICER', 'CRO-9-00474034',   'thushara.kotelawala@lexflow.lk',    '$2a$10$IfdZ8vJRzUSbvLTXRZmzMe9eugwJgls4RRBd183S8pGwXmU8fe1.2', 'Thushara Kotelawala',     '1981-09-09', 'FEMALE', '811883296V',   'ACTIVE', '2019-08-14 09:00:00.000', NULL,         NULL    ),
       ('FINANCE_OFFICER',          'FCO-10-00522305',  'pradeep.wettasinghe@lexflow.lk',    '$2a$10$yXj9FAGIaX1SBT/mI1wt2edfwHyWYtITtH8FvCI/9Bnn0C/QCh.Ry', 'Pradeep Wettasinghe',     '1988-10-10', 'MALE',   '881993333V',   'ACTIVE', '2019-08-23 09:00:00.000', NULL,         NULL    ),
       ('FINANCE_OFFICER',          'FCO-11-00570576',  'dhanushka.atapattu@lexflow.lk',     '$2a$10$57CW3p0KsKo1KPkynEbpYe1o.qQWe1PwgDbd5iWjbvJfnv9lHER.a', 'Dhanushka Atapattu',      '1960-11-11', 'MALE',   '602103370V',   'ACTIVE', '2019-09-01 09:00:00.000', NULL,         NULL    ),
       ('CLIENT',                   'CLT-12-00618847',  'anura.kodikara@gmail.com',          '$2a$10$dOgteFp3I5Yh/DRhYUJ9XOnE.pBnRVV4jh8zSqW56CFSMMB19kvvm', 'Anura Kodikara',          '1967-12-12', 'MALE',   '672213407V',   'ACTIVE', '2021-04-15 10:00:00.000', NULL,         0.00    ),
       ('CLIENT',                   'CLT-13-00667118',  'buddhika.hettiarachchi@gmail.com',  '$2a$10$Ca7w3Yl/iIeQ0QDWo2HUEOz2iz9TMo830tk3h9GTEIXaqrWMiEXtW', 'Buddhika Hettiarachchi',  '1974-01-13', 'MALE',   '742323444V',   'ACTIVE', '2021-06-02 10:00:00.000', NULL,         2500.00 ),
       ('CLIENT',                   'CLT-14-00715389',  'chamari.samarasekara@gmail.com',    '$2a$10$tc8fBdSnmVDrGC5ZV9nIeu9/b9lkqTJcWaTXPhwItT7aM7vw5jPMq', 'Chamari Samarasekara',    '1981-02-14', 'FEMALE', '812433481V',   'ACTIVE', '2021-07-20 10:00:00.000', NULL,         5000.00 ),
       ('CLIENT',                   'CLT-15-00763660',  'dilshan.wanigasekara@gmail.com',    '$2a$10$gHcl0BKDLnBEDavSnEG0L.BByhVwEqSmQs9rp8OGiFf8K3dmBKP5O', 'Dilshan Wanigasekara',    '1988-03-15', 'MALE',   '882543518V',   'ACTIVE', '2021-09-06 10:00:00.000', NULL,         0.00    ),
       ('CLIENT',                   'CLT-16-00811931',  'eranga.liyanage@gmail.com',         '$2a$10$57geDsUWc6PJbwke2yXKluxYuXk/RArlzAPSlbDhk.Bvw2zufNT8u', 'Eranga Liyanage',         '1960-04-16', 'MALE',   '602653555V',   'ACTIVE', '2021-10-24 10:00:00.000', NULL,         2500.00 ),
       ('CLIENT',                   'CLT-17-00860202',  'gayan.ranaweera@gmail.com',         '$2a$10$ZzpKlf22Wt.nASPJA5UyG.yrdKYE0y.fAaBxwvbMfFY3RTpm9I1c.', 'Gayan Ranaweera',         '1967-05-17', 'MALE',   '672763592V',   'ACTIVE', '2021-12-11 10:00:00.000', NULL,         5000.00 ),
       ('CLIENT',                   'CLT-18-00908473',  'hiruni.pathirana@gmail.com',        '$2a$10$2RHpJUv.D59ZeH2n6VdI2.Y1HQbQ0KmJvI.llm0Q5WpRubH7aX11y', 'Hiruni Pathirana',        '1974-06-18', 'FEMALE', '742873629V',   'ACTIVE', '2022-01-28 10:00:00.000', NULL,         0.00    ),
       ('CLIENT',                   'CLT-19-00956744',  'isuri.gamage@gmail.com',            '$2a$10$K7oo2Omy.KgVOw5A994hEey10/G7J/s0.nWP7v/XRFu5gsCys0u1S', 'Isuri Gamage',            '1981-07-19', 'FEMALE', '812983666V',   'ACTIVE', '2022-03-17 10:00:00.000', NULL,         5000.00 ),
       ('CLIENT',                   'CLT-20-01005015',  'janaka.munasinghe@gmail.com',       '$2a$10$iOje3MTmsGcjmtt023X.I.mkJh.fT8a/Ay9gSLPKvqobSirzPQQY2', 'Janaka Munasinghe',       '1988-08-20', 'MALE',   '883093703V',   'ACTIVE', '2022-05-04 10:00:00.000', NULL,         0.00    ),
       ('CLIENT',                   'CLT-21-01053286',  'kasuni.abeygunawardena@gmail.com',  '$2a$10$7xcEt3gMe1pJZP5Ss2J7mOJ1bJ4aYsLHpzUDX.pBGphTDRhYEPIhW', 'Kasuni Abeygunawardena',  '1960-09-21', 'FEMALE', '603203740V',   'ACTIVE', '2022-06-21 10:00:00.000', NULL,         0.00    ),
       ('CLIENT',                   'CLT-22-01101557',  'lakmal.kodikara@gmail.com',         '$2a$10$CmuLgv84EDmlZlz0AdoYkuCvkWmN5p3q9wWbgEp1j4.ihx45/xB46', 'Lakmal Kodikara',         '1967-10-22', 'MALE',   '673313777V',   'ACTIVE', '2022-08-08 10:00:00.000', NULL,         5000.00 ),
       ('CLIENT',                   'CLT-23-01149828',  'madhavi.hettiarachchi@gmail.com',   '$2a$10$YC9HVPwbdzd20T6BKSpeuOBbp33psLNM8bmhihNX1hNZkCFXlGz8K', 'Madhavi Hettiarachchi',   '1974-11-23', 'FEMALE', '743423814V',   'ACTIVE', '2022-09-25 10:00:00.000', NULL,         10000.00),
       ('CLIENT',                   'CLT-24-01198099',  'nalaka.samarasekara@gmail.com',     '$2a$10$U8Pm9auDkluJQU3FGx7R..sChmTj1iUh43zDi4jaFHdua4rx.r2Xy', 'Nalaka Samarasekara',     '1981-12-24', 'MALE',   '813533851V',   'ACTIVE', '2022-11-12 10:00:00.000', NULL,         0.00    ),
       ('CLIENT',                   'CLT-25-01246370',  'oshadi.wanigasekara@gmail.com',     '$2a$10$WSLaYfhGqFFhx8g0dbPeJ..bKmD7jJ8h3Joc6t1fQEokFmOt7ryem', 'Oshadi Wanigasekara',     '1988-01-25', 'FEMALE', '883643888V',   'ACTIVE', '2022-12-30 10:00:00.000', NULL,         2500.00 ),
       ('CLIENT',                   'CLT-26-01294641',  'pasindu.liyanage@gmail.com',        '$2a$10$Js5XyOARpBNPKZUOVKewqeeivJqQzHW9oBKYC8LY1KEFIAJErAcQK', 'Pasindu Liyanage',        '1960-02-26', 'MALE',   '603753925V',   'ACTIVE', '2023-02-16 10:00:00.000', NULL,         0.00    ),
       ('CLIENT',                   'CLT-27-01342912',  'rasika.ranaweera@gmail.com',        '$2a$10$OMgIQ1Wcq3vT85mRR3tItOKPgtJUp2xOxy1kWZYi1rHdn5le4CN6G', 'Rasika Ranaweera',        '1967-03-27', 'FEMALE', '673863962V',   'ACTIVE', '2023-04-05 10:00:00.000', NULL,         0.00    ),
       ('CLIENT',                   'CLT-28-01391183',  'sajith.pathirana@gmail.com',        '$2a$10$jf6z5aCCV19POrVUQ/rWoeVhnWqyD3jW0YiMsU0i9r9OKsGOVkANm', 'Sajith Pathirana',        '1974-04-01', 'MALE',   '743973999V',   'ACTIVE', '2023-05-23 10:00:00.000', NULL,         2500.00 ),
       ('CLIENT',                   'CLT-29-01439454',  'tharaka.gamage@gmail.com',          '$2a$10$5B0H9nnMCziTHrvp7z83fOhPGr8SbJsBHY6ryGIXeX6uY6dW1R0TW', 'Tharaka Gamage',          '1981-05-02', 'MALE',   '814084036V',   'ACTIVE', '2023-07-10 10:00:00.000', NULL,         10000.00),
       ('CLIENT',                   'CLT-30-01487725',  'udara.munasinghe@gmail.com',        '$2a$10$NKSGPMlcMY50RAo69oFmXuA6ud4VNQwjhDq8Rc.fwPHAfA6KUEW12', 'Udara Munasinghe',        '1988-06-03', 'MALE',   '884194073V',   'ACTIVE', '2023-08-27 10:00:00.000', NULL,         0.00    ),
       ('CLIENT',                   'CLT-31-01535996',  'vihanga.abeygunawardena@gmail.com', '$2a$10$PZA5bNteSy8Bb81dp2m8Muxqj3wax9Loao6AF.ONJ9f4SXDEXXz0a', 'Vihanga Abeygunawardena', '1960-07-04', 'FEMALE', '604304110V',   'ACTIVE', '2023-10-14 10:00:00.000', NULL,         2500.00 ),
       ('CLIENT',                   'CLT-32-01584267',  'yasas.kodikara@gmail.com',          '$2a$10$9iPeNiloyBm9t94S4bpA/eOWF3Xi990W2i9KMWQuLegzpAhB7xAtu', 'Yasas Kodikara',          '1967-08-05', 'MALE',   '674414147V',   'ACTIVE', '2023-12-01 10:00:00.000', NULL,         0.00    ),
       ('CLIENT',                   'CLT-33-01632538',  'achini.hettiarachchi@gmail.com',    '$2a$10$xmjHPDvpTGOa.HSMVW1i2u4lnUeJ1FMlvTGZGcBdrBl0bBMTvWdMa', 'Achini Hettiarachchi',    '1974-09-06', 'FEMALE', '744524184V',   'ACTIVE', '2024-01-18 10:00:00.000', NULL,         0.00    ),
       ('CLIENT',                   'CLT-34-01680809',  'bimsara.samarasekara@gmail.com',    '$2a$10$MCiHotWunq6nFJJ88eBQEOwFGOqHMBaBup8WUHU/UiKHCFk7Crgle', 'Bimsara Samarasekara',    '1981-10-07', 'FEMALE', '814634221V',   'ACTIVE', '2024-03-06 10:00:00.000', NULL,         5000.00 ),
       ('CLIENT',                   'CLT-35-01729080',  'dulani.wanigasekara@gmail.com',     '$2a$10$z5Ttzf.ZOGZNm2hcod2ze.p0cHRzu/a8KnMq8552LfsQAkNa0zD8S', 'Dulani Wanigasekara',     '1988-11-08', 'FEMALE', '884744258V',   'ACTIVE', '2024-04-23 10:00:00.000', NULL,         10000.00),
       ('CLIENT',                   'CLT-36-01777351',  'hasitha.liyanage@gmail.com',        '$2a$10$14mbL.YUFiQn54lTrXBOduJO2k0idNbjg10G4i5pup0YTUu0kAlc.', 'Hasitha Liyanage',        '1960-12-09', 'MALE',   '604854295V',   'ACTIVE', '2024-06-10 10:00:00.000', NULL,         0.00    ),
       ('CLIENT',                   'CLT-37-01825622',  'kanchana.ranaweera@gmail.com',      '$2a$10$.x/oMVHfidKnOI/zbisuou3pd93XOaiWKbt7gl65Y0kBG0ZeyVSAm', 'Kanchana Ranaweera',      '1967-01-10', 'FEMALE', '674964332V',   'ACTIVE', '2024-07-28 10:00:00.000', NULL,         5000.00 ),
       ('CLIENT',                   'CLT-38-01873893',  'methmi.pathirana@gmail.com',        '$2a$10$Qscjaa/5f7jCZ24PaxO8DeFOCzJTBMKd1FID5EulVaDmZyNJHNb5W', 'Methmi Pathirana',        '1974-02-11', 'FEMALE', '745074369V',   'ACTIVE', '2024-09-14 10:00:00.000', NULL,         0.00    ),
       ('CLIENT',                   'CLT-39-01922164',  'nipun.gamage@gmail.com',            '$2a$10$YxDU6Y1WWGqfrPpeOLgn6O5nfVUNrRxnXcPDwnISA2.xwN8R2Vqo6', 'Nipun Gamage',            '1981-03-12', 'MALE',   '815184406V',   'ACTIVE', '2024-11-01 10:00:00.000', NULL,         0.00    ),
       ('CLIENT',                   'CLT-40-01970435',  'ruvini.munasinghe@gmail.com',       '$2a$10$ifQ/HDukPVIDnOdnclLBOeqgcTTSVxrddlGQuBdWMx0jaCrp4bVoC', 'Ruvini Munasinghe',       '1988-04-13', 'FEMALE', '885294443V',   'ACTIVE', '2024-12-19 10:00:00.000', NULL,         2500.00 ),
       ('PARTNER',                  'PRT-41-93580258',  'nadeesha.perera@lexflow.lk',        '$2a$10$iqCI1UxIPzqvfFn0Vt63GO/7DTxHINE7KMAw.GBffwBqt1k1JlO0K', 'Nadeesha Perera',         '1965-03-12', 'FEMALE', '655712756V',   'ACTIVE', '2025-01-10 09:00:00.000', '0000000001', NULL    ),
       ('PARTNER',                  'PRT-42-74814825',  'ruwan.fernando@lexflow.lk',         '$2a$10$57ME2LhfOInqVs6e2HjPcOtIHxjFGW7610T3HWEKcFMgPX7S93dGi', 'Ruwan Fernando',          '1965-07-22', 'MALE',   '652032818V',   'ACTIVE', '2025-01-11 09:00:00.000', '0000000002', NULL    ),
       ('PARTNER',                  'PRT-43-56049392',  'sanath.wickramasinghe@lexflow.lk',  '$2a$10$ak14JXpZ6qAIcQEMEoVELuOhbY9yFjAX9q/TtAQH4PxTWcrQXxa.6', 'Sanath Wickramasinghe',   '1966-11-05', 'MALE',   '663092890V',   'ACTIVE', '2025-01-12 09:00:00.000', '0000000003', NULL    ),
       ('PARTNER',                  'PRT-44-37283959',  'chaminda.gunawardena@lexflow.lk',   '$2a$10$anOyZdGfbYM1GWtnd9Pc1eMXYVJfSPYFtL1uciONNbLzE002Ddn.C', 'Chaminda Gunawardena',    '1967-02-28', 'MALE',   '670592833V',   'ACTIVE', '2025-01-13 09:00:00.000', '0000000004', NULL    ),
       ('PARTNER',                  'PRT-45-18518526',  'priyantha.rajapaksa@lexflow.lk',    '$2a$10$BmR1BioObG5bYVGlbsmip.43n/V2gc3.uKSvBVG63Dsqwwl6Q5.nm', 'Priyantha Rajapaksa',     '1967-09-14', 'MALE',   '672572910V',   'ACTIVE', '2025-01-14 09:00:00.000', '0000000005', NULL    ),
       ('PARTNER',                  'PRT-46-89753093',  'ajith.bandara@lexflow.lk',          '$2a$10$/hXpqTKZZnk1L5082M2eIOwpnnx8Vo8KyAAxldgix1yFUGv6EEtjW', 'Ajith Bandara',           '1968-04-19', 'MALE',   '681092887V',   'ACTIVE', '2025-01-15 09:00:00.000', '0000000006', NULL    ),
       ('PARTNER',                  'PRT-47-70987660',  'susantha.dissanayake@lexflow.lk',   '$2a$10$zHB6Xnu5/98LBo.IpUjpaOIlhWFExeKIGveNxPbCoiAH00696No3q', 'Susantha Dissanayake',    '1969-12-01', 'MALE',   '693353010V',   'ACTIVE', '2025-01-16 09:00:00.000', '0000000007', NULL    ),
       ('PARTNER',                  'PRT-48-52222227',  'mahinda.weerasinghe@lexflow.lk',    '$2a$10$c.xIom6cqb1gG9K8CP8kKeuvpgGV/cU7Du/cqNo2BmEmxPu3RdKky', 'Mahinda Weerasinghe',     '1969-06-08', 'MALE',   '691592939V',   'ACTIVE', '2025-01-17 09:00:00.000', '0000000008', NULL    ),
       ('PARTNER',                  'PRT-49-33456794',  'ranjith.senanayake@lexflow.lk',     '$2a$10$qJwy3eVg3CNmv1xS5Z4nZeVpCi0H0BOVQbD2rLVEEW5xvDnppoNo2', 'Ranjith Senanayake',      '1970-10-25', 'MALE',   '702983045V',   'ACTIVE', '2025-01-18 09:00:00.000', '0000000009', NULL    ),
       ('PARTNER',                  'PRT-50-14691361',  'gamini.karunaratne@lexflow.lk',     '$2a$10$q3Mn35aFvMoHXJr.H1caIuEdZvUOaqw/Zxk99eyoqRecvYppSCOW2', 'Gamini Karunaratne',      '1971-08-17', 'MALE',   '712293048V',   'ACTIVE', '2025-01-19 09:00:00.000', '0000000010', NULL    ),
       ('ASSOCIATE',                'ASC-51-85925928',  'ruwan.senanayake@lexflow.lk',       '$2a$10$VgZD5mLWrFKhWhTcmHSXJ.WPwtxjvUXpEwSizHiRg6/TeICgiVkI6', 'Ruwan Senanayake',        '1972-05-14', 'MALE',   '721343043V',   'ACTIVE', '2025-02-01 09:00:00.000', '0000000011', NULL    ),
       ('ASSOCIATE',                'ASC-52-67160495',  'tharindu.mendis@lexflow.lk',        '$2a$10$AAj/ZzbX.RTlmTcX37QtkeTwQvcsIffVBy.UTUl2I2GNh0CiD905S', 'Tharindu Mendis',         '1972-03-09', 'MALE',   '720683012V',   'ACTIVE', '2025-02-02 09:00:00.000', '0000000012', NULL    ),
       ('ASSOCIATE',                'ASC-53-48395062',  'dinesh.abeysekara@lexflow.lk',      '$2a$10$7edoDm6bHlXB5M2rTpClWOychCeYsA4Ny/SCFjTNgalvblnf78vpq', 'Dinesh Abeysekara',       '1973-12-20', 'MALE',   '733543177V',   'ACTIVE', '2025-02-03 09:00:00.000', '0000000013', NULL    ),
       ('ASSOCIATE',                'ASC-54-29629629',  'lahiru.wijesinghe@lexflow.lk',      '$2a$10$RjgMie5SG0TwwYiKuTco/ubkdjv1J7.8b8ISjLQtbzm3ohczACUKi', 'Lahiru Wijesinghe',       '1974-07-30', 'MALE',   '742113159V',   'ACTIVE', '2025-02-04 09:00:00.000', '0000000014', NULL    ),
       ('ASSOCIATE',                'ASC-55-10864196',  'sameera.herath@lexflow.lk',         '$2a$10$IzpkccdzYxrreyUCW60XMe4kO4YDdX8fiD4WT7gxwMFmYoStxg64W', 'Sameera Herath',          '1974-01-11', 'FEMALE', '745113062V',   'ACTIVE', '2025-02-05 09:00:00.000', '0000000015', NULL    ),
       ('CLIENT',                   'CLT-56-02742771',  'supun.abeygunawardena@gmail.com',   '$2a$10$EgzOCXAMiNaQPStbr.rcS.Ey6YdL8MmhW4pf30UWLGYmS630tzej6', 'Supun Abeygunawardena',   '1960-05-14', 'MALE',   '605404480V',   'ACTIVE', '2025-02-05 10:00:00.000', NULL,         5000.00 ),
       ('ASSOCIATE',                'ASC-57-82098763',  'roshan.kulasekara@lexflow.lk',      '$2a$10$lPmuAsjfmtfN2k1rSBvRpuwImIphhOstUu2ianL44ekVpq.zYm6D6', 'Roshan Kulasekara',       '1975-09-04', 'MALE',   '752473196V',   'ACTIVE', '2025-02-06 09:00:00.000', '0000000016', NULL    ),
       ('ASSOCIATE',                'ASC-58-63333330',  'chathura.amarasinghe@lexflow.lk',   '$2a$10$F3qV0I/W9R8wq4m2vUMkCennKOn4UNE0WMrL0yvp3H59qTLtC2Lt2', 'Chathura Amarasinghe',    '1976-11-27', 'MALE',   '763313282V',   'ACTIVE', '2025-02-07 09:00:00.000', '0000000017', NULL    ),
       ('ASSOCIATE',                'ASC-59-44567897',  'isuru.rathnayake@lexflow.lk',       '$2a$10$coauFfJbJm/N1HJpdBhZ7.D5A9zOk6g3JH3NTZy9onvVTWs/Y2tbC', 'Isuru Rathnayake',        '1977-06-16', 'MALE',   '771673243V',   'ACTIVE', '2025-02-08 09:00:00.000', '0000000018', NULL    ),
       ('ASSOCIATE',                'ASC-60-25802464',  'nuwan.jayawardena@lexflow.lk',      '$2a$10$Bf9nfTwXjvbLusTgCtQylOa/wzRXBhpzP8hihLhs/GKpr5CAsA/oW', 'Nuwan Jayawardena',       '1977-04-23', 'MALE',   '771133224V',   'ACTIVE', '2025-02-09 09:00:00.000', '0000000019', NULL    ),
       ('ASSOCIATE',                'ASC-61-97037031',  'manoj.fonseka@lexflow.lk',          '$2a$10$AnmhAsXXoLJcF81ibrjLo.GP5ZuB95k40A7tHQioxpMX5iB3zMscO', 'Manoj Fonseka',           '1978-08-02', 'MALE',   '782143292V',   'ACTIVE', '2025-02-10 09:00:00.000', '0000000020', NULL    ),
       ('PARALEGAL',                'PRL-62-78271598',  'ishani.jayawardena@lexflow.lk',     '$2a$10$vbQY0AEWLwp3XcbX.EzgrOtUU77m0SVCxrCE4hn02KlcuKgMOcrHa', 'Ishani Jayawardena',      '1979-02-18', 'FEMALE', '795493267V',   'ACTIVE', '2025-02-15 09:00:00.000', NULL,         NULL    ),
       ('PARALEGAL',                'PRL-63-59506165',  'ishara.sirisena@lexflow.lk',        '$2a$10$0s95RkL4kmWSTBqwZDvS6OFcOCPGuhVFmepSEPpl0gGns8tIH7nhq', 'Ishara Sirisena',         '1979-05-06', 'FEMALE', '796263294V',   'ACTIVE', '2025-02-16 09:00:00.000', NULL,         NULL    ),
       ('PARALEGAL',                'PRL-64-40740732',  'chathurika.peiris@lexflow.lk',      '$2a$10$ggjTNre.q4kfRVHlYFPlD.m5SwtnaY6U00.99.MDFXx/LIvTZ4IXe', 'Chathurika Peiris',       '1980-10-13', 'FEMALE', '807863403V',   'ACTIVE', '2025-02-17 09:00:00.000', NULL,         NULL    ),
       ('PARALEGAL',                'PRL-65-21975299',  'dilani.wijesekara@lexflow.lk',      '$2a$10$usSUL6HWPfRoy2aLWcJDQ.IEPhAG9UJuGavuVivEo6g/OCqrgNk4O', 'Dilani Wijesekara',       '1981-01-29', 'FEMALE', '815293339V',   'ACTIVE', '2025-02-18 09:00:00.000', NULL,         NULL    ),
       ('PARALEGAL',                'PRL-66-93209866',  'sachini.kariyawasam@lexflow.lk',    '$2a$10$dVWnxQFLQHVATBnl1p/Ky.92FtLZEKuUb8t3nvX/Ih2jO3qYRMfw.', 'Sachini Kariyawasam',     '1982-08-21', 'FEMALE', '827333459V',   'ACTIVE', '2025-02-19 09:00:00.000', NULL,         NULL    ),
       ('PARALEGAL',                'PRL-67-74444433',  'piumi.ranasinghe@lexflow.lk',       '$2a$10$VoXbGeei4F8qCMh5ipHkterwWBA9gWbIHcsE1tpto6tOCPVYDf1JO', 'Piumi Ranasinghe',        '1982-03-15', 'FEMALE', '825743388V',   'ACTIVE', '2025-02-20 09:00:00.000', NULL,         NULL    ),
       ('PARALEGAL',                'PRL-68-55679000',  'yohani.wanniarachchi@lexflow.lk',   '$2a$10$aA0zhguaUDVXCTToy7uAOOj9YlfX2UoJwCruXB6k3cdXuLECo8hnW', 'Yohani Wanniarachchi',    '1983-12-09', 'FEMALE', '838433536V',   'ACTIVE', '2025-02-21 09:00:00.000', NULL,         NULL    ),
       ('PARALEGAL',                'PRL-69-36913567',  'vindya.desilva@lexflow.lk',         '$2a$10$WR9SQ66L.wLOy5vcBGXe.eDtjImrE6KMYt.RUrydZRzg4E7jwrsMS', 'Vindya De Silva',         '1984-07-27', 'FEMALE', '847083526V',   'ACTIVE', '2025-02-22 09:00:00.000', NULL,         NULL    ),
       ('PARALEGAL',                'PRL-70-18148134',  'malshi.jayasuriya@lexflow.lk',      '$2a$10$YyXMP1Uih3.y77wg5XlG6u5QIXEo2oL86rYJvtwV.LOYG9GA/sOXe', 'Malshi Jayasuriya',       '1984-04-04', 'FEMALE', '845943464V',   'ACTIVE', '2025-02-23 09:00:00.000', NULL,         NULL    ),
       ('PARALEGAL',                'PRL-71-89382701',  'hansini.rajapaksa@lexflow.lk',      '$2a$10$MkKEfXd/.u4BvQSBM0i66uAZwBxqKbk7ZkI5Cdgg6i/Gg3huuzLI6', 'Hansini Rajapaksa',       '1985-11-11', 'FEMALE', '858153599V',   'ACTIVE', '2025-02-24 09:00:00.000', NULL,         NULL    ),
       ('CLIENT',                   'CLT-72-70617268',  'lal.yapa@gmail.com',                '$2a$10$AN70zz.xTfMZs45f9OVHLuDq0vXBTXQeRHLXWzFynRdR5kxy/F3vq', 'Lal Yapa',                '1986-06-19', 'MALE',   '861703579V',   'ACTIVE', '2025-03-01 09:00:00.000', NULL,         45000.00),
       ('CLIENT',                   'CLT-73-51851835',  'nadun.perera@gmail.com',            '$2a$10$EeWZIz1aS6qTCdzjv1WjN.wX6ZQw38fmm71gfbMyuVUuczt0/.c3G', 'Nadun Perera',            '1987-09-23', 'MALE',   '872663659V',   'ACTIVE', '2025-03-02 09:00:00.000', NULL,         20000.00),
       ('CLIENT',                   'CLT-74-33086402',  'ishani.silva@gmail.com',            '$2a$10$PpeJGwB3P7sX65ryNJhw6ekWN2m/9Ys2zuLW1vIlaIQkVi3KlYiIO', 'Ishani Silva',            '1987-01-05', 'FEMALE', '875053537V',   'ACTIVE', '2025-03-03 09:00:00.000', NULL,         0.00    ),
       ('CLIENT',                   'CLT-75-14320969',  'ruwangi.fernando@gmail.com',        '$2a$10$/ZJik7tfA2G92Y5aMQeog.IGxWDw7bQSQD/eZnYKl2cEV.WGQssuK', 'Ruwangi Fernando',        '1988-04-30', 'FEMALE', '886203638V',   'ACTIVE', '2025-03-04 09:00:00.000', NULL,         40000.00),
       ('CLIENT',                   'CLT-76-85555536',  'ravindu.senarath@gmail.com',        '$2a$10$TT8J4kamxsgoHh7XaBGegODa/CVliIyKN/vIgK36tCr2wwrO4/cYG', 'Ravindu Senarath',        '1989-11-17', 'MALE',   '893213753V',   'ACTIVE', '2025-03-05 09:00:00.000', NULL,         3000.00 ),
       ('CLIENT',                   'CLT-77-66790103',  'sewwandi.wijesinghe@gmail.com',     '$2a$10$l.PQerRQ0VJW9OLXdjsN6uFbCnLoFHsklQ.5eBHFi4zx6RKBLWE3C', 'Sewwandi Wijesinghe',     '1989-02-14', 'FEMALE', '895453633V',   'ACTIVE', '2025-03-06 09:00:00.000', NULL,         5000.00 ),
       ('CLIENT',                   'CLT-78-48024670',  'janith.herath@gmail.com',           '$2a$10$YplitiS7h2ocFHQlqJ8XJuuH3.1DxVSa5L4rFlKUMrhz24IkVKmfu', 'Janith Herath',           '1990-08-08', 'MALE',   '902203742V',   'ACTIVE', '2025-03-07 09:00:00.000', NULL,         0.00    ),
       ('CLIENT',                   'CLT-79-29259237',  'poornima.mendis@gmail.com',         '$2a$10$Wm5pPjsddwILpwlelIyeCuKdKxWEQaMpyXlyWFBED8bHSA5qjXONi', 'Poornima Mendis',         '1991-05-27', 'FEMALE', '916473759V',   'ACTIVE', '2025-03-08 09:00:00.000', NULL,         25000.00),
       ('CLIENT',                   'CLT-80-10493804',  'sanduni.weerasinghe@gmail.com',     '$2a$10$GBZ3erfReI6FfLbBJZSAQ.vXLgeITqbQP3diC.wsrQ.vQFNvqIpu2', 'Sanduni Weerasinghe',     '1992-10-02', 'FEMALE', '927753836V',   'ACTIVE', '2025-03-09 09:00:00.000', NULL,         18000.00),
       ('CLIENT',                   'CLT-81-81728371',  'thilini.senanayake@gmail.com',      '$2a$10$pivl82zf7LvBWSa8J78QUeiXbkZzYElO9bh92tWbBZ9q1UUYbrd0q', 'Thilini Senanayake',      '1992-03-21', 'FEMALE', '925803764V',   'ACTIVE', '2025-03-10 09:00:00.000', NULL,         0.00    ),
       ('CLIENT_RELATIONS_OFFICER', 'CRO-82-62962938',  'malithi.fernando@lexflow.lk',       '$2a$10$LehQKueeIfo978ZZCPJKCeBeyZ7m9N4jkt7nc/LowSTJ/zRh0susS', 'Malithi Fernando',        '1993-06-06', 'FEMALE', '936573825V',   'ACTIVE', '2025-03-15 09:00:00.000', NULL,         NULL    ),
       ('CLIENT_RELATIONS_OFFICER', 'CRO-83-44197505',  'dilini.perera@lexflow.lk',          '$2a$10$hHoFjK0e5pJVtZyv2LEh9.DUC0aQpPjwMGK6aHJZAJ/G7HcIEPJXW', 'Dilini Perera',           '1994-09-19', 'FEMALE', '947623914V',   'ACTIVE', '2025-03-16 09:00:00.000', NULL,         NULL    ),
       ('CLIENT_RELATIONS_OFFICER', 'CRO-84-25432072',  'nethmi.silva@lexflow.lk',           '$2a$10$xa4n1b2C9KZ/.AOxgLUDLOjFU2aA/N.AzyYbISpmsI58T1iXiiTjW', 'Nethmi Silva',            '1994-01-12', 'FEMALE', '945123803V',   'ACTIVE', '2025-03-17 09:00:00.000', NULL,         NULL    ),
       ('CLIENT_RELATIONS_OFFICER', 'CRO-85-96666639',  'kavya.bandara@lexflow.lk',          '$2a$10$pTH2VIrr/5CdNJDAWaRlq.mvu1FZcKAJoHbm7z0z1E.HgrpKE2UlS', 'Kavya Bandara',           '1995-07-25', 'FEMALE', '957063931V',   'ACTIVE', '2025-03-18 09:00:00.000', NULL,         NULL    ),
       ('CLIENT_RELATIONS_OFFICER', 'CRO-86-77901206',  'tharushi.herath@lexflow.lk',        '$2a$10$3MxCdF8hfi/JMyKeC48U3uuVh.WJNvxV0HgVUH8182Sds7qWh8fuq', 'Tharushi Herath',         '1996-04-08', 'FEMALE', '965983912V',   'ACTIVE', '2025-03-19 09:00:00.000', NULL,         NULL    ),
       ('CLIENT_RELATIONS_OFFICER', 'CRO-87-59135773',  'sithara.wijesinghe@lexflow.lk',     '$2a$10$aesyjrdUGVdFwC1emnbxIuvetdwEhdY3buD6YS1eB08g4Nd5j7inK', 'Sithara Wijesinghe',      '1997-11-30', 'FEMALE', '978344062V',   'ACTIVE', '2025-03-20 09:00:00.000', NULL,         NULL    ),
       ('CLIENT_RELATIONS_OFFICER', 'CRO-88-40370340',  'rashmi.mendis@lexflow.lk',          '$2a$10$dUHj3K1g6eGZvwN48jKKf.fuIidX6G6VG0pC/DYuJLLcBrm5aDSAC', 'Rashmi Mendis',           '1997-08-17', 'FEMALE', '977294010V',   'ACTIVE', '2025-03-21 09:00:00.000', NULL,         NULL    ),
       ('CLIENT_RELATIONS_OFFICER', 'CRO-89-21604907',  'nimasha.weerasinghe@lexflow.lk',    '$2a$10$5ALq1cWuf8osNquAF./ktexSBuaJKAqrhnZMFRG1hr5Wo1subzgwi', 'Nimasha Weerasinghe',     '1998-02-04', 'FEMALE', '985353956V',   'ACTIVE', '2025-03-22 09:00:00.000', NULL,         NULL    ),
       ('CLIENT_RELATIONS_OFFICER', 'CRO-90-92839474',  'erandi.senanayake@lexflow.lk',      '$2a$10$g/WSBN7c97GziMKOcfD4MOtCYgWnNyJ1K0RQ.ersKEEeaBuWH/zTG', 'Erandi Senanayake',       '1999-12-22', 'FEMALE', '998564141V',   'ACTIVE', '2025-03-23 09:00:00.000', NULL,         NULL    ),
       ('CLIENT_RELATIONS_OFFICER', 'CRO-91-74074041',  'malki.gunawardena@lexflow.lk',      '$2a$10$k/gbiUVbaOM3QjNSyqABNuOfL9cDbmU3DcEaprmbXSZteDYVhE86.', 'Malki Gunawardena',       '1999-05-29', 'FEMALE', '996494057V',   'ACTIVE', '2025-03-24 09:00:00.000', NULL,         NULL    ),
       ('FINANCE_OFFICER',          'FCO-92-55308608',  'tharindu.weerasinghe@lexflow.lk',   '$2a$10$Uj3RA/Tmw1DDPlxKp.30muMj5Za.IMqefUSk48T7AqivakFKRYIMa', 'Tharindu Weerasinghe',    '2000-03-03', 'MALE',   '200006274042', 'ACTIVE', '2025-04-01 09:00:00.000', NULL,         NULL    ),
       ('FINANCE_OFFICER',          'FCO-93-36543175',  'chinthaka.dissanayake@lexflow.lk',  '$2a$10$zsgrM3xJJJs.HYhFspjwQen0COfSF4jnvdodJKndAQybZv11Lrvnq', 'Chinthaka Dissanayake',   '2001-10-16', 'MALE',   '200128974183', 'ACTIVE', '2025-04-02 09:00:00.000', NULL,         NULL    ),
       ('FINANCE_OFFICER',          'FCO-94-17777742',  'sanjeewa.karunaratne@lexflow.lk',   '$2a$10$AmFP8JWe8EuVh48JBOE4TOahrXTQViBSBGYBOtbJmj4DGLYEuDKm6', 'Sanjeewa Karunaratne',    '2002-06-28', 'MALE',   '200217974180', 'ACTIVE', '2025-04-03 09:00:00.000', NULL,         NULL    ),
       ('FINANCE_OFFICER',          'FCO-95-89012309',  'harsha.amarasinghe@lexflow.lk',     '$2a$10$qBNpXXlVjtKGAEtUhDz0b.CcK2B2USgmPNdAFMRyhFgSILn/NnLt.', 'Harsha Amarasinghe',      '2002-01-09', 'MALE',   '200200974096', 'ACTIVE', '2025-04-04 09:00:00.000', NULL,         NULL    ),
       ('FINANCE_OFFICER',          'FCO-96-70246876',  'kalana.rathnayake@lexflow.lk',      '$2a$10$Y1R2YL1.ozHr9RObope9.OrW0Uw1Db/m44kzciU5Zhbvt.sZyOqBO', 'Kalana Rathnayake',       '2003-09-05', 'MALE',   '200324874233', 'ACTIVE', '2025-04-05 09:00:00.000', NULL,         NULL    ),
       ('FINANCE_OFFICER',          'FCO-97-51481443',  'asela.jayawardena@lexflow.lk',      '$2a$10$V0eiCLGL0M3DqqPgk1U/3.OfriRGsg8Ul4ioTSJQOeclFbXkZc5zy', 'Asela Jayawardena',       '2004-05-13', 'MALE',   '200413374226', 'ACTIVE', '2025-04-06 09:00:00.000', NULL,         NULL    ),
       ('FINANCE_OFFICER',          'FCO-98-32716010',  'rukshan.fonseka@lexflow.lk',        '$2a$10$lpUnI0YRRdm/aacq4uewgeFd3uY4On8qmjU2dhJr9G.O0RLvRl.FK', 'Rukshan Fonseka',         '2004-11-24', 'MALE',   '200432874315', 'ACTIVE', '2025-04-07 09:00:00.000', NULL,         NULL    ),
       ('FINANCE_OFFICER',          'FCO-99-13950577',  'chanaka.kulasekara@lexflow.lk',     '$2a$10$VcLOtprPLpkwq5wg8naaB.lJAxipeqeH21qqPSuUU6bl9lFKWvX2K', 'Chanaka Kulasekara',      '2005-07-07', 'MALE',   '200518874283', 'ACTIVE', '2025-04-08 09:00:00.000', NULL,         NULL    ),
       ('FINANCE_OFFICER',          'FCO-100-85185144', 'prasad.abeysekara@lexflow.lk',      '$2a$10$6f/rFkE30RRFZIEj57x9/.2qYkkzaKutoLQBLxBe05rq7TIyH71vS', 'Prasad Abeysekara',       '2006-02-17', 'MALE',   '200604874265', 'ACTIVE', '2025-04-09 09:00:00.000', NULL,         NULL    ),
       ('FINANCE_OFFICER',          'FCO-101-66419711', 'dushyanthi.peiris@lexflow.lk',      '$2a$10$m8or9XZUDp3AJJtLWYfaw./Y7oqKpJ6PUxDuhnJP.R8mRwdh/kA1W', 'Dushyanthi Peiris',       '2007-12-30', 'FEMALE', '200786474445', 'ACTIVE', '2025-04-10 09:00:00.000', NULL,         NULL    );

-- User Addresses Data
INSERT INTO user_addresses (user_id, address)
VALUES (1,   'No. 10, Temple Road, Kandy'        ),
       (2,   'No. 13, Station Road, Galle'       ),
       (3,   'No. 16, Park Avenue, Kalutara'     ),
       (4,   'No. 19, Lake Road, Kegalle'        ),
       (5,   'No. 22, Flower Road, Kurunegala'   ),
       (6,   'No. 25, School Lane, Negombo'      ),
       (7,   'No. 28, Main Street, Colombo'      ),
       (8,   'No. 31, Hospital Road, Gampaha'    ),
       (9,   'No. 34, Church Street, Matara'     ),
       (10,  'No. 37, Market Road, Ratnapura'    ),
       (11,  'No. 40, Temple Road, Kandy'        ),
       (12,  'No. 43, Station Road, Galle'       ),
       (13,  'No. 46, Park Avenue, Kalutara'     ),
       (14,  'No. 49, Lake Road, Kegalle'        ),
       (15,  'No. 52, Flower Road, Kurunegala'   ),
       (16,  'No. 55, School Lane, Negombo'      ),
       (17,  'No. 58, Main Street, Colombo'      ),
       (18,  'No. 61, Hospital Road, Gampaha'    ),
       (19,  'No. 64, Church Street, Matara'     ),
       (20,  'No. 67, Market Road, Ratnapura'    ),
       (21,  'No. 70, Temple Road, Kandy'        ),
       (22,  'No. 73, Station Road, Galle'       ),
       (23,  'No. 76, Park Avenue, Kalutara'     ),
       (24,  'No. 79, Lake Road, Kegalle'        ),
       (25,  'No. 82, Flower Road, Kurunegala'   ),
       (26,  'No. 85, School Lane, Negombo'      ),
       (27,  'No. 88, Main Street, Colombo'      ),
       (28,  'No. 91, Hospital Road, Gampaha'    ),
       (29,  'No. 94, Church Street, Matara'     ),
       (30,  'No. 97, Market Road, Ratnapura'    ),
       (31,  'No. 100, Temple Road, Kandy'       ),
       (32,  'No. 103, Station Road, Galle'      ),
       (33,  'No. 106, Park Avenue, Kalutara'    ),
       (34,  'No. 109, Lake Road, Kegalle'       ),
       (35,  'No. 112, Flower Road, Kurunegala'  ),
       (36,  'No. 115, School Lane, Negombo'     ),
       (37,  'No. 118, Main Street, Colombo'     ),
       (38,  'No. 121, Hospital Road, Gampaha'   ),
       (39,  'No. 124, Church Street, Matara'    ),
       (40,  'No. 127, Market Road, Ratnapura'   ),
       (41,  'No. 12, Galle Road, Colombo'       ),
       (41,  'No. 45, Hill Street, Colombo'      ),
       (42,  'No. 8, Kandy Road, Colombo'        ),
       (43,  'No. 21, Lake Drive, Colombo'       ),
       (44,  'No. 33, Main Street, Colombo'      ),
       (45,  'No. 7, Temple Lane, Colombo'       ),
       (45,  'No. 30, Flower Road, Colombo'      ),
       (46,  'No. 19, Church Road, Colombo'      ),
       (47,  'No. 56, Station Road, Colombo'     ),
       (48,  'No. 4, Park Avenue, Colombo'       ),
       (49,  'No. 88, Beach Road, Colombo'       ),
       (50,  'No. 15, Hill Street, Colombo'      ),
       (51,  'No. 27, Galle Road, Colombo'       ),
       (51,  'No. 3, Lake Drive, Colombo'        ),
       (52,  'No. 14, Kandy Road, Colombo'       ),
       (53,  'No. 60, Main Street, Colombo'      ),
       (54,  'No. 9, Temple Lane, Colombo'       ),
       (55,  'No. 22, Church Road, Colombo'      ),
       (55,  'No. 8, Ward Place, Colombo'        ),
       (56,  'No. 130, Temple Road, Kandy'       ),
       (57,  'No. 41, Station Road, Colombo'     ),
       (58,  'No. 5, Park Avenue, Colombo'       ),
       (59,  'No. 77, Beach Road, Colombo'       ),
       (60,  'No. 32, Hill Street, Colombo'      ),
       (61,  'No. 18, Galle Road, Colombo'       ),
       (62,  'No. 6, Lake Drive, Colombo'        ),
       (62,  'No. 50, Kandy Road, Colombo'       ),
       (63,  'No. 10, Main Street, Colombo'      ),
       (64,  'No. 24, Temple Lane, Colombo'      ),
       (65,  'No. 37, Church Road, Colombo'      ),
       (66,  'No. 2, Station Road, Colombo'      ),
       (66,  'No. 19, Alfred House Road, Colombo'),
       (67,  'No. 63, Park Avenue, Colombo'      ),
       (68,  'No. 29, Beach Road, Colombo'       ),
       (69,  'No. 11, Hill Street, Colombo'      ),
       (70,  'No. 44, Galle Road, Colombo'       ),
       (71,  'No. 16, Lake Drive, Colombo'       ),
       (72,  'No. 100, Galle Road, Colombo'      ),
       (72,  'No. 25, Beach Road, Colombo'       ),
       (73,  'No. 13, Kandy Road, Colombo'       ),
       (74,  'No. 48, Main Street, Colombo'      ),
       (75,  'No. 20, Temple Lane, Colombo'      ),
       (76,  'No. 35, Church Road, Colombo'      ),
       (76,  'No. 27, Bullers Lane, Colombo'     ),
       (77,  'No. 52, Station Road, Colombo'     ),
       (78,  'No. 17, Park Avenue, Colombo'      ),
       (79,  'No. 64, Beach Road, Colombo'       ),
       (80,  'No. 30, Hill Street, Colombo'      ),
       (81,  'No. 8, Galle Road, Colombo'        ),
       (82,  'No. 12, Lake Drive, Colombo'       ),
       (82,  'No. 40, Kandy Road, Colombo'       ),
       (83,  'No. 26, Main Street, Colombo'      ),
       (84,  'No. 9, Temple Lane, Colombo'       ),
       (85,  'No. 55, Church Road, Colombo'      ),
       (86,  'No. 31, Station Road, Colombo'     ),
       (87,  'No. 47, Park Avenue, Colombo'      ),
       (88,  'No. 3, Beach Road, Colombo'        ),
       (89,  'No. 66, Hill Street, Colombo'      ),
       (90,  'No. 23, Galle Road, Colombo'       ),
       (91,  'No. 38, Lake Drive, Colombo'       ),
       (92,  'No. 42, Kandy Road, Colombo'       ),
       (92,  'No. 7, Main Street, Colombo'       ),
       (93,  'No. 28, Temple Lane, Colombo'      ),
       (94,  'No. 61, Church Road, Colombo'      ),
       (95,  'No. 19, Station Road, Colombo'     ),
       (96,  'No. 34, Park Avenue, Colombo'      ),
       (97,  'No. 50, Beach Road, Colombo'       ),
       (98,  'No. 6, Hill Street, Colombo'       ),
       (99,  'No. 43, Galle Road, Colombo'       ),
       (100, 'No. 15, Lake Drive, Colombo'       ),
       (101, 'No. 57, Kandy Road, Colombo'       );

-- User Phone Numbers Data
INSERT INTO user_phone_numbers (user_id, phone_number)
VALUES (1,   '0705000000'),
       (2,   '0715000731'),
       (3,   '0725001462'),
       (4,   '0735002193'),
       (5,   '0745002924'),
       (6,   '0755003655'),
       (7,   '0765004386'),
       (8,   '0775005117'),
       (9,   '0705005848'),
       (10,  '0715006579'),
       (11,  '0725007310'),
       (12,  '0735008041'),
       (13,  '0745008772'),
       (14,  '0755009503'),
       (15,  '0765010234'),
       (16,  '0775010965'),
       (17,  '0705011696'),
       (18,  '0715012427'),
       (19,  '0725013158'),
       (20,  '0735013889'),
       (21,  '0745014620'),
       (22,  '0755015351'),
       (23,  '0765016082'),
       (24,  '0775016813'),
       (25,  '0705017544'),
       (26,  '0715018275'),
       (27,  '0725019006'),
       (28,  '0735019737'),
       (29,  '0745020468'),
       (30,  '0755021199'),
       (31,  '0765021930'),
       (32,  '0775022661'),
       (33,  '0705023392'),
       (34,  '0715024123'),
       (35,  '0725024854'),
       (36,  '0735025585'),
       (37,  '0745026316'),
       (38,  '0755027047'),
       (39,  '0765027778'),
       (40,  '0775028509'),
       (41,  '0770000001'),
       (41,  '0710000001'),
       (42,  '0770000002'),
       (43,  '0770000003'),
       (43,  '0710000003'),
       (44,  '0770000004'),
       (45,  '0770000005'),
       (46,  '0770000006'),
       (47,  '0770000007'),
       (48,  '0770000008'),
       (49,  '0770000009'),
       (50,  '0770000010'),
       (51,  '0770000011'),
       (51,  '0710000011'),
       (52,  '0770000012'),
       (53,  '0770000013'),
       (53,  '0710000013'),
       (54,  '0770000014'),
       (55,  '0770000015'),
       (56,  '0705029240'),
       (57,  '0770000016'),
       (58,  '0770000017'),
       (59,  '0770000018'),
       (60,  '0770000019'),
       (61,  '0770000020'),
       (62,  '0770000021'),
       (62,  '0710000021'),
       (63,  '0770000022'),
       (64,  '0770000023'),
       (64,  '0710000023'),
       (65,  '0770000024'),
       (66,  '0770000025'),
       (67,  '0770000026'),
       (68,  '0770000027'),
       (69,  '0770000028'),
       (70,  '0770000029'),
       (71,  '0770000030'),
       (72,  '0770000031'),
       (72,  '0710000031'),
       (73,  '0770000032'),
       (74,  '0770000033'),
       (74,  '0710000033'),
       (75,  '0770000034'),
       (76,  '0770000035'),
       (77,  '0770000036'),
       (78,  '0770000037'),
       (79,  '0770000038'),
       (80,  '0770000039'),
       (81,  '0770000040'),
       (82,  '0770000041'),
       (82,  '0710000041'),
       (83,  '0770000042'),
       (84,  '0770000043'),
       (85,  '0770000044'),
       (86,  '0770000045'),
       (87,  '0770000046'),
       (88,  '0770000047'),
       (89,  '0770000048'),
       (90,  '0770000049'),
       (91,  '0770000050'),
       (92,  '0770000051'),
       (92,  '0710000051'),
       (93,  '0770000052'),
       (94,  '0770000053'),
       (95,  '0770000054'),
       (96,  '0770000055'),
       (97,  '0770000056'),
       (98,  '0770000057'),
       (99,  '0770000058'),
       (100, '0770000059'),
       (101, '0770000060');

-- Consultations Data
INSERT INTO consultations (consultation_code, client_id, case_type, description, consultation_status, submitted_at, associate_id, paralegal_id, rejection_reason)
VALUES ('CON-1-00151218',  12, 'CIVIL',                 'A dispute with a neighbour over damage to the boundary wall.',                                                            'ACCEPTED_BY_ASSOCIATE', '2021-04-18 10:00:00.000', 2,    5,    NULL                                                                                    ),
       ('CON-2-00199489',  13, 'CONTRACT_COMMERCIAL',   'A supplier has not delivered goods that were paid for in advance.',                                                       'REJECTED_BY_PARTNER',   '2021-06-05 10:00:00.000', NULL, NULL, 'The claim is time-barred.'                                                             ),
       ('CON-3-00247760',  14, 'CORPORATE',             'Advice is needed on restructuring the shareholding of a family company.',                                                 'ACCEPTED_BY_ASSOCIATE', '2021-07-23 10:00:00.000', 4,    5,    NULL                                                                                    ),
       ('CON-4-00296031',  15, 'EMPLOYMENT',            'The client was dismissed without notice or the agreed severance.',                                                        'ACCEPTED_BY_ASSOCIATE', '2021-09-09 10:00:00.000', 2,    5,    NULL                                                                                    ),
       ('CON-5-00344302',  16, 'FAMILY',                'The client is seeking a divorce and an agreement on child custody.',                                                      'ACCEPTED_BY_ASSOCIATE', '2021-10-27 10:00:00.000', 3,    5,    NULL                                                                                    ),
       ('CON-6-00392573',  17, 'PROPERTY',              'A buyer withdrew from a land sale after the deposit was paid.',                                                           'ACCEPTED_BY_ASSOCIATE', '2021-12-14 10:00:00.000', 4,    5,    NULL                                                                                    ),
       ('CON-7-00440844',  18, 'CRIMINAL',              'The client has been accused of cheating in a vehicle sale.',                                                              'REJECTED_BY_PARTNER',   '2022-01-31 10:00:00.000', NULL, NULL, 'A conflict of interest was found with an existing client.'                             ),
       ('CON-8-00489115',  19, 'IMMIGRATION',           'The client''s work visa renewal was refused without reasons.',                                                            'ACCEPTED_BY_ASSOCIATE', '2022-03-20 10:00:00.000', 3,    5,    NULL                                                                                    ),
       ('CON-9-00537386',  20, 'CIVIL',                 'A dispute with a neighbour over damage to the boundary wall.',                                                            'REJECTED_BY_ASSOCIATE', '2022-05-07 10:00:00.000', 4,    5,    'The matter falls outside the firm''s practice areas.'                                  ),
       ('CON-10-00585657', 21, 'CONTRACT_COMMERCIAL',   'A supplier has not delivered goods that were paid for in advance.',                                                       'ACCEPTED_BY_ASSOCIATE', '2022-06-24 10:00:00.000', 2,    5,    NULL                                                                                    ),
       ('CON-11-00633928', 22, 'CORPORATE',             'Advice is needed on restructuring the shareholding of a family company.',                                                 'ACCEPTED_BY_ASSOCIATE', '2022-08-11 10:00:00.000', 3,    5,    NULL                                                                                    ),
       ('CON-12-00682199', 12, 'CRIMINAL',              'The client has been accused of cheating in a vehicle sale.',                                                              'REJECTED_BY_ASSOCIATE', '2022-08-15 10:00:00.000', 2,    5,    'A conflict of interest was found with an existing client.'                             ),
       ('CON-13-00730470', 13, 'IMMIGRATION',           'The client''s work visa renewal was refused without reasons.',                                                            'ACCEPTED_BY_ASSOCIATE', '2022-09-15 10:00:00.000', 3,    5,    NULL                                                                                    ),
       ('CON-14-00778741', 23, 'EMPLOYMENT',            'The client was dismissed without notice or the agreed severance.',                                                        'ACCEPTED_BY_ASSOCIATE', '2022-09-28 10:00:00.000', 4,    5,    NULL                                                                                    ),
       ('CON-15-00827012', 14, 'CIVIL',                 'A dispute with a neighbour over damage to the boundary wall.',                                                            'REJECTED_BY_PARTNER',   '2022-10-17 10:00:00.000', NULL, NULL, 'The matter falls outside the firm''s practice areas.'                                  ),
       ('CON-16-00875283', 24, 'FAMILY',                'The client is seeking a divorce and an agreement on child custody.',                                                      'ACCEPTED_BY_ASSOCIATE', '2022-11-15 10:00:00.000', 2,    5,    NULL                                                                                    ),
       ('CON-17-00923554', 15, 'CONTRACT_COMMERCIAL',   'A supplier has not delivered goods that were paid for in advance.',                                                       'ACCEPTED_BY_ASSOCIATE', '2022-11-17 10:00:00.000', 2,    5,    NULL                                                                                    ),
       ('CON-18-00971825', 16, 'CORPORATE',             'Advice is needed on restructuring the shareholding of a family company.',                                                 'REJECTED_BY_PARTNER',   '2022-12-19 10:00:00.000', NULL, NULL, 'A conflict of interest was found with an existing client.'                             ),
       ('CON-19-01020096', 25, 'PROPERTY',              'A buyer withdrew from a land sale after the deposit was paid.',                                                           'REJECTED_BY_ASSOCIATE', '2023-01-02 10:00:00.000', 3,    5,    'The claim is time-barred.'                                                             ),
       ('CON-20-01068367', 17, 'EMPLOYMENT',            'The client was dismissed without notice or the agreed severance.',                                                        'REJECTED_BY_ASSOCIATE', '2023-01-19 10:00:00.000', 4,    5,    'The client did not provide the required documents.'                                    ),
       ('CON-21-01116638', 26, 'CRIMINAL',              'The client has been accused of cheating in a vehicle sale.',                                                              'REJECTED_BY_ASSOCIATE', '2023-02-19 10:00:00.000', 4,    5,    'A conflict of interest was found with an existing client.'                             ),
       ('CON-22-01164909', 18, 'FAMILY',                'The client is seeking a divorce and an agreement on child custody.',                                                      'REJECTED_BY_PARTNER',   '2023-02-20 10:00:00.000', NULL, NULL, 'The matter falls outside the firm''s practice areas.'                                  ),
       ('CON-23-01213180', 19, 'PROPERTY',              'A buyer withdrew from a land sale after the deposit was paid.',                                                           'ACCEPTED_BY_ASSOCIATE', '2023-03-23 10:00:00.000', 3,    5,    NULL                                                                                    ),
       ('CON-24-01261451', 27, 'IMMIGRATION',           'The client''s work visa renewal was refused without reasons.',                                                            'ACCEPTED_BY_ASSOCIATE', '2023-04-08 10:00:00.000', 2,    5,    NULL                                                                                    ),
       ('CON-25-01309722', 20, 'CRIMINAL',              'The client has been accused of cheating in a vehicle sale.',                                                              'REJECTED_BY_PARTNER',   '2023-04-24 10:00:00.000', NULL, NULL, 'A conflict of interest was found with an existing client.'                             ),
       ('CON-26-01357993', 21, 'IMMIGRATION',           'The client''s work visa renewal was refused without reasons.',                                                            'ACCEPTED_BY_ASSOCIATE', '2023-05-25 10:00:00.000', 2,    5,    NULL                                                                                    ),
       ('CON-27-01406264', 28, 'CIVIL',                 'A dispute with a neighbour over damage to the boundary wall.',                                                            'REJECTED_BY_ASSOCIATE', '2023-05-26 10:00:00.000', 3,    5,    'The matter falls outside the firm''s practice areas.'                                  ),
       ('CON-28-01454535', 22, 'CIVIL',                 'A dispute with a neighbour over damage to the boundary wall.',                                                            'ACCEPTED_BY_ASSOCIATE', '2023-06-26 10:00:00.000', 3,    5,    NULL                                                                                    ),
       ('CON-29-01502806', 29, 'CONTRACT_COMMERCIAL',   'A supplier has not delivered goods that were paid for in advance.',                                                       'ACCEPTED_BY_ASSOCIATE', '2023-07-13 10:00:00.000', 4,    5,    NULL                                                                                    ),
       ('CON-30-01551077', 23, 'CONTRACT_COMMERCIAL',   'A supplier has not delivered goods that were paid for in advance.',                                                       'ACCEPTED_BY_ASSOCIATE', '2023-07-27 10:00:00.000', 4,    5,    NULL                                                                                    ),
       ('CON-31-01599348', 24, 'CORPORATE',             'Advice is needed on restructuring the shareholding of a family company.',                                                 'REJECTED_BY_PARTNER',   '2023-08-28 10:00:00.000', NULL, NULL, 'A conflict of interest was found with an existing client.'                             ),
       ('CON-32-01647619', 30, 'CORPORATE',             'Advice is needed on restructuring the shareholding of a family company.',                                                 'ACCEPTED_BY_ASSOCIATE', '2023-08-30 10:00:00.000', 2,    5,    NULL                                                                                    ),
       ('CON-33-01695890', 25, 'EMPLOYMENT',            'The client was dismissed without notice or the agreed severance.',                                                        'ACCEPTED_BY_ASSOCIATE', '2023-09-28 10:00:00.000', 3,    5,    NULL                                                                                    ),
       ('CON-34-01744161', 31, 'EMPLOYMENT',            'The client was dismissed without notice or the agreed severance.',                                                        'ACCEPTED_BY_ASSOCIATE', '2023-10-17 10:00:00.000', 3,    5,    NULL                                                                                    ),
       ('CON-35-01792432', 26, 'FAMILY',                'The client is seeking a divorce and an agreement on child custody.',                                                      'REJECTED_BY_PARTNER',   '2023-10-30 10:00:00.000', NULL, NULL, 'The matter falls outside the firm''s practice areas.'                                  ),
       ('CON-36-01840703', 27, 'PROPERTY',              'A buyer withdrew from a land sale after the deposit was paid.',                                                           'ACCEPTED_BY_ASSOCIATE', '2023-11-30 10:00:00.000', 2,    5,    NULL                                                                                    ),
       ('CON-37-01888974', 32, 'FAMILY',                'The client is seeking a divorce and an agreement on child custody.',                                                      'REJECTED_BY_PARTNER',   '2023-12-04 10:00:00.000', NULL, NULL, 'The matter falls outside the firm''s practice areas.'                                  ),
       ('CON-38-01937245', 28, 'CRIMINAL',              'The client has been accused of cheating in a vehicle sale.',                                                              'ACCEPTED_BY_ASSOCIATE', '2024-01-01 10:00:00.000', 3,    5,    NULL                                                                                    ),
       ('CON-39-01985516', 33, 'PROPERTY',              'A buyer withdrew from a land sale after the deposit was paid.',                                                           'ACCEPTED_BY_ASSOCIATE', '2024-01-21 10:00:00.000', 2,    5,    NULL                                                                                    ),
       ('CON-40-02033787', 29, 'IMMIGRATION',           'The client''s work visa renewal was refused without reasons.',                                                            'ACCEPTED_BY_ASSOCIATE', '2024-02-01 10:00:00.000', 4,    5,    NULL                                                                                    ),
       ('CON-41-02082058', 30, 'CIVIL',                 'A dispute with a neighbour over damage to the boundary wall.',                                                            'REJECTED_BY_PARTNER',   '2024-03-04 10:00:00.000', NULL, NULL, 'The matter falls outside the firm''s practice areas.'                                  ),
       ('CON-42-02130329', 34, 'CRIMINAL',              'The client has been accused of cheating in a vehicle sale.',                                                              'ACCEPTED_BY_ASSOCIATE', '2024-03-09 10:00:00.000', 3,    5,    NULL                                                                                    ),
       ('CON-43-02178600', 31, 'CONTRACT_COMMERCIAL',   'A supplier has not delivered goods that were paid for in advance.',                                                       'REJECTED_BY_PARTNER',   '2024-04-04 10:00:00.000', NULL, NULL, 'The claim is time-barred.'                                                             ),
       ('CON-44-02226871', 35, 'IMMIGRATION',           'The client''s work visa renewal was refused without reasons.',                                                            'ACCEPTED_BY_ASSOCIATE', '2024-04-26 10:00:00.000', 4,    5,    NULL                                                                                    ),
       ('CON-45-02275142', 32, 'CORPORATE',             'Advice is needed on restructuring the shareholding of a family company.',                                                 'REJECTED_BY_ASSOCIATE', '2024-05-06 10:00:00.000', 4,    5,    'A conflict of interest was found with an existing client.'                             ),
       ('CON-46-02323413', 33, 'EMPLOYMENT',            'The client was dismissed without notice or the agreed severance.',                                                        'REJECTED_BY_ASSOCIATE', '2024-06-06 10:00:00.000', 2,    5,    'The client did not provide the required documents.'                                    ),
       ('CON-47-02371684', 36, 'CIVIL',                 'A dispute with a neighbour over damage to the boundary wall.',                                                            'ACCEPTED_BY_ASSOCIATE', '2024-06-13 10:00:00.000', 2,    5,    NULL                                                                                    ),
       ('CON-48-02419955', 34, 'FAMILY',                'The client is seeking a divorce and an agreement on child custody.',                                                      'ACCEPTED_BY_ASSOCIATE', '2024-07-08 10:00:00.000', 3,    5,    NULL                                                                                    ),
       ('CON-49-02468226', 37, 'CONTRACT_COMMERCIAL',   'A supplier has not delivered goods that were paid for in advance.',                                                       'ACCEPTED_BY_ASSOCIATE', '2024-07-31 10:00:00.000', 3,    5,    NULL                                                                                    ),
       ('CON-50-02516497', 35, 'PROPERTY',              'A buyer withdrew from a land sale after the deposit was paid.',                                                           'ACCEPTED_BY_ASSOCIATE', '2024-08-08 10:00:00.000', 4,    5,    NULL                                                                                    ),
       ('CON-51-02564768', 36, 'CRIMINAL',              'The client has been accused of cheating in a vehicle sale.',                                                              'REJECTED_BY_PARTNER',   '2024-09-09 10:00:00.000', NULL, NULL, 'A conflict of interest was found with an existing client.'                             ),
       ('CON-52-02613039', 38, 'CORPORATE',             'Advice is needed on restructuring the shareholding of a family company.',                                                 'REJECTED_BY_PARTNER',   '2024-09-17 10:00:00.000', NULL, NULL, 'A conflict of interest was found with an existing client.'                             ),
       ('CON-53-02661310', 37, 'IMMIGRATION',           'The client''s work visa renewal was refused without reasons.',                                                            'ACCEPTED_BY_ASSOCIATE', '2024-10-10 10:00:00.000', 3,    5,    NULL                                                                                    ),
       ('CON-54-02709581', 39, 'EMPLOYMENT',            'The client was dismissed without notice or the agreed severance.',                                                        'ACCEPTED_BY_ASSOCIATE', '2024-11-04 10:00:00.000', 2,    5,    NULL                                                                                    ),
       ('CON-55-02757852', 38, 'CIVIL',                 'A dispute with a neighbour over damage to the boundary wall.',                                                            'REJECTED_BY_PARTNER',   '2024-11-11 10:00:00.000', NULL, NULL, 'The matter falls outside the firm''s practice areas.'                                  ),
       ('CON-56-02806123', 39, 'CONTRACT_COMMERCIAL',   'A supplier has not delivered goods that were paid for in advance.',                                                       'ACCEPTED_BY_ASSOCIATE', '2024-12-12 10:00:00.000', 2,    5,    NULL                                                                                    ),
       ('CON-57-02854394', 40, 'FAMILY',                'The client is seeking a divorce and an agreement on child custody.',                                                      'ACCEPTED_BY_ASSOCIATE', '2024-12-22 10:00:00.000', 3,    5,    NULL                                                                                    ),
       ('CON-58-02902665', 40, 'CORPORATE',             'Advice is needed on restructuring the shareholding of a family company.',                                                 'ACCEPTED_BY_ASSOCIATE', '2025-01-13 10:00:00.000', 3,    6,    NULL                                                                                    ),
       ('CON-59-02950936', 56, 'PROPERTY',              'A buyer withdrew from a land sale after the deposit was paid.',                                                           'ACCEPTED_BY_ASSOCIATE', '2025-02-08 10:00:00.000', 4,    5,    NULL                                                                                    ),
       ('CON-60-02999207', 56, 'EMPLOYMENT',            'The client was dismissed without notice or the agreed severance.',                                                        'ACCEPTED_BY_ASSOCIATE', '2025-02-13 10:00:00.000', 4,    7,    NULL                                                                                    ),
       ('CON-61-93580258', 72, 'FAMILY',                'Seeking representation for a contested divorce and custody arrangement.',                                                 'ACCEPTED_BY_ASSOCIATE', '2025-04-15 10:00:00.000', 51,   62,   NULL                                                                                    ),
       ('CON-62-74814825', 73, 'CIVIL',                 'Dispute with a neighbour over a boundary wall.',                                                                          'REJECTED_BY_PARTNER',   '2025-04-16 10:00:00.000', NULL, NULL, 'The matter is a minor boundary issue better settled through the local mediation board.'),
       ('CON-63-56049392', 74, 'CRIMINAL',              'Requesting defence representation for a traffic offence.',                                                                'REJECTED_BY_PARTNER',   '2025-04-17 10:00:00.000', NULL, NULL, 'Conflict of interest with an existing client.'                                         ),
       ('CON-64-37283959', 75, 'CORPORATE',             'Advice needed on a company merger.',                                                                                      'ACCEPTED_BY_ASSOCIATE', '2025-04-18 10:00:00.000', 52,   63,   NULL                                                                                    ),
       ('CON-65-18518526', 76, 'EMPLOYMENT',            'Wrongful termination claim against former employer.',                                                                     'ACCEPTED_BY_ASSOCIATE', '2025-04-19 10:00:00.000', 53,   64,   NULL                                                                                    ),
       ('CON-66-89753093', 77, 'PROPERTY',              'Title dispute over inherited land.',                                                                                      'ACCEPTED_BY_ASSOCIATE', '2025-04-20 10:00:00.000', 54,   65,   NULL                                                                                    ),
       ('CON-67-70987660', 78, 'INTELLECTUAL_PROPERTY', 'Trademark infringement claim.',                                                                                           'REJECTED_BY_ASSOCIATE', '2025-04-21 10:00:00.000', 55,   66,   'Insufficient evidence to proceed with the claim.'                                      ),
       ('CON-68-52222227', 79, 'CONTRACT_COMMERCIAL',   'Breach of supply contract by vendor.',                                                                                    'ACCEPTED_BY_ASSOCIATE', '2025-04-22 10:00:00.000', 57,   67,   NULL                                                                                    ),
       ('CON-69-33456794', 80, 'IMMIGRATION',           'Work visa renewal assistance.',                                                                                           'ACCEPTED_BY_ASSOCIATE', '2025-04-23 10:00:00.000', 58,   68,   NULL                                                                                    ),
       ('CON-70-14691361', 81, 'OTHER',                 'General legal consultation regarding a lease agreement.',                                                                 'ACCEPTED_BY_ASSOCIATE', '2025-04-24 10:00:00.000', 59,   69,   NULL                                                                                    ),
       ('CON-71-85925928', 72, 'CONTRACT_COMMERCIAL',   'Dispute over a supplier contract for Mr. Yapa''s business.',                                                              'ACCEPTED_BY_ASSOCIATE', '2025-07-01 10:00:00.000', 51,   62,   NULL                                                                                    ),
       ('CON-72-67160495', 72, 'CRIMINAL',              'Requesting representation for a minor traffic dispute.',                                                                  'REJECTED_BY_PARTNER',   '2025-07-05 10:00:00.000', NULL, NULL, 'Outside the firm''s current practice areas.'                                           ),
       ('CON-73-41827365', 72, 'CIVIL',                 'Dispute with a contractor over defective renovation work at his residence.',                                              'REJECTED_BY_PARTNER',   '2025-08-01 10:00:00.000', NULL, NULL, 'The client did not provide the renovation contract needed to assess the claim.'        ),
       ('CON-74-60293817', 72, 'EMPLOYMENT',            'Advice needed on terminating an underperforming employee at his business without triggering a wrongful dismissal claim.', 'ACCEPTED_BY_PARTNER',   '2025-08-02 10:00:00.000', 51,   62,   NULL                                                                                    ),
       ('CON-75-78315942', 72, 'CRIMINAL',              'Requesting defence representation for a public nuisance charge.',                                                         'REJECTED_BY_PARTNER',   '2025-08-03 10:00:00.000', NULL, NULL, 'Conflict of interest with an existing client.'                                         ),
       ('CON-76-29471683', 72, 'PROPERTY',              'Boundary encroachment dispute concerning his commercial property in Kandy.',                                              'SCHEDULED',             '2025-08-04 10:00:00.000', 51,   62,   NULL                                                                                    ),
       ('CON-77-53968217', 72, 'CORPORATE',             'Advice required on restructuring his business into a private limited company.',                                           'ACCEPTED_BY_ASSOCIATE', '2025-08-05 10:00:00.000', 51,   62,   NULL                                                                                    ),
       ('CON-78-84620173', 72, 'FAMILY',                'Requesting assistance with a prenuptial agreement ahead of his upcoming marriage.',                                       'REJECTED_BY_ASSOCIATE', '2025-08-06 10:00:00.000', 51,   62,   'Matter falls outside the firm''s consultation scope for this practice area.'           ),
       ('CON-79-25802464', 73, 'PROPERTY',              'Dispute over tenancy rights in a rented commercial property.',                                                            'ACCEPTED_BY_ASSOCIATE', '2025-08-10 10:00:00.000', 60,   70,   NULL                                                                                    ),
       ('CON-80-97037031', 74, 'EMPLOYMENT',            'Claim for unpaid overtime and end-of-service benefits.',                                                                  'ACCEPTED_BY_ASSOCIATE', '2025-08-12 10:00:00.000', 61,   71,   NULL                                                                                    ),
       ('CON-81-78271598', 72, 'PROPERTY',              'His tenant has refused to leave his Nugegoda house after the lease ended.',                                               'ACCEPTED_BY_ASSOCIATE', '2026-09-24 09:00:00.000', 51,   62,   NULL                                                                                    ),
       ('CON-82-59506165', 72, 'CIVIL',                 'A contractor abandoned the extension of his house after taking an advance.',                                              'ACCEPTED_BY_ASSOCIATE', '2026-09-25 09:00:00.000', 51,   62,   NULL                                                                                    ),
       ('CON-83-40740732', 72, 'CONTRACT_COMMERCIAL',   'A catering client has not paid six months of invoices for his business.',                                                 'ACCEPTED_BY_ASSOCIATE', '2026-09-26 09:00:00.000', 51,   62,   NULL                                                                                    ),
       ('CON-84-21975299', 72, 'CIVIL',                 'Water leaking from the apartment above has damaged his ceiling and furniture.',                                           'SCHEDULED',             '2026-09-27 09:00:00.000', 51,   62,   NULL                                                                                    ),
       ('CON-85-93209866', 72, 'CORPORATE',             'A minority shareholder is blocking the sale of his company''s warehouse.',                                                'SCHEDULED',             '2026-09-28 09:00:00.000', 51,   62,   NULL                                                                                    ),
       ('CON-86-74444433', 72, 'CIVIL',                 'A former supplier has been posting false claims about his business online.',                                              'SCHEDULED',             '2026-09-29 09:00:00.000', 51,   62,   NULL                                                                                    ),
       ('CON-87-55679000', 72, 'PROPERTY',              'A buyer withdrew from the sale of his Galle property after the deposit was paid.',                                        'ACCEPTED_BY_PARTNER',   '2026-09-30 09:00:00.000', 51,   62,   NULL                                                                                    ),
       ('CON-88-36913567', 72, 'CONTRACT_COMMERCIAL',   'A software vendor delivered an inventory system that does not work as agreed.',                                           'ACCEPTED_BY_PARTNER',   '2026-10-01 09:00:00.000', 51,   62,   NULL                                                                                    ),
       ('CON-89-18148134', 72, 'CIVIL',                 'His insurer rejected the claim for his delivery van after an accident.',                                                  'ACCEPTED_BY_PARTNER',   '2026-10-02 09:00:00.000', 51,   62,   NULL                                                                                    ),
       ('CON-90-89382701', 73, 'CIVIL',                 'His neighbour has built a wall that blocks the only access road to his land.',                                            'PENDING',               '2026-10-03 09:00:00.000', NULL, NULL, NULL                                                                                    ),
       ('CON-91-70617268', 77, 'CIVIL',                 'Tree roots from the neighbouring property are cracking her boundary wall and driveway.',                                  'PENDING',               '2026-10-04 09:00:00.000', NULL, NULL, NULL                                                                                    ),
       ('CON-92-51851835', 80, 'CONTRACT_COMMERCIAL',   'A tenant has stopped paying rent on her shop space for four months.',                                                     'PENDING',               '2026-10-04 15:00:00.000', NULL, NULL, NULL                                                                                    ),
       ('CON-93-33086402', 72, 'EMPLOYMENT',            'A former manager is using his client list to solicit his customers after resigning.',                                     'PENDING',               '2026-10-05 09:00:00.000', NULL, NULL, NULL                                                                                    );

-- Consultation Opposing Parties Data
INSERT INTO consultation_opposing_parties (consultation_id, opposing_party)
VALUES (1,  'Lanka Steel (Pvt) Ltd'                   ),
       (2,  'Mr. Ruchira Jayatilleke'                 ),
       (3,  'Ms. Hemali Kumarasinghe'                 ),
       (4,  'Seaview Constructions'                   ),
       (5,  'Mr. Damith Weerakoon'                    ),
       (6,  'Ms. Anoma Goonetilleke'                  ),
       (7,  'Island Freight (Pvt) Ltd'                ),
       (8,  'Mr. Kelum Ellawala'                      ),
       (9,  'Ms. Renuka Rajakaruna'                   ),
       (10, 'Mr. Saliya Senadheera'                   ),
       (11, 'Hillside Developers'                     ),
       (12, 'Island Freight (Pvt) Ltd'                ),
       (13, 'Mr. Kelum Ellawala'                      ),
       (14, 'Mr. Asitha Warnakulasuriya'              ),
       (15, 'Ms. Renuka Rajakaruna'                   ),
       (16, 'Lanka Steel (Pvt) Ltd'                   ),
       (17, 'Mr. Saliya Senadheera'                   ),
       (18, 'Hillside Developers'                     ),
       (19, 'Mr. Ruchira Jayatilleke'                 ),
       (20, 'Mr. Asitha Warnakulasuriya'              ),
       (21, 'Ms. Hemali Kumarasinghe'                 ),
       (22, 'Lanka Steel (Pvt) Ltd'                   ),
       (23, 'Mr. Ruchira Jayatilleke'                 ),
       (24, 'Seaview Constructions'                   ),
       (25, 'Ms. Hemali Kumarasinghe'                 ),
       (26, 'Seaview Constructions'                   ),
       (27, 'Mr. Damith Weerakoon'                    ),
       (28, 'Mr. Damith Weerakoon'                    ),
       (29, 'Ms. Anoma Goonetilleke'                  ),
       (30, 'Ms. Anoma Goonetilleke'                  ),
       (31, 'Island Freight (Pvt) Ltd'                ),
       (32, 'Island Freight (Pvt) Ltd'                ),
       (33, 'Mr. Kelum Ellawala'                      ),
       (34, 'Mr. Kelum Ellawala'                      ),
       (35, 'Ms. Renuka Rajakaruna'                   ),
       (36, 'Mr. Saliya Senadheera'                   ),
       (37, 'Ms. Renuka Rajakaruna'                   ),
       (38, 'Hillside Developers'                     ),
       (39, 'Mr. Saliya Senadheera'                   ),
       (40, 'Mr. Asitha Warnakulasuriya'              ),
       (41, 'Lanka Steel (Pvt) Ltd'                   ),
       (42, 'Hillside Developers'                     ),
       (43, 'Mr. Ruchira Jayatilleke'                 ),
       (44, 'Mr. Asitha Warnakulasuriya'              ),
       (45, 'Ms. Hemali Kumarasinghe'                 ),
       (46, 'Seaview Constructions'                   ),
       (47, 'Lanka Steel (Pvt) Ltd'                   ),
       (48, 'Mr. Damith Weerakoon'                    ),
       (49, 'Mr. Ruchira Jayatilleke'                 ),
       (50, 'Ms. Anoma Goonetilleke'                  ),
       (51, 'Island Freight (Pvt) Ltd'                ),
       (52, 'Ms. Hemali Kumarasinghe'                 ),
       (53, 'Mr. Kelum Ellawala'                      ),
       (54, 'Seaview Constructions'                   ),
       (55, 'Ms. Renuka Rajakaruna'                   ),
       (56, 'Mr. Saliya Senadheera'                   ),
       (57, 'Mr. Damith Weerakoon'                    ),
       (58, 'Hillside Developers'                     ),
       (59, 'Ms. Anoma Goonetilleke'                  ),
       (60, 'Mr. Asitha Warnakulasuriya'              ),
       (61, 'Mrs. Chandrika Perera'                   ),
       (62, 'Mr. Sunil Rathnayake (neighbour)'        ),
       (63, 'Attorney General'                        ),
       (64, 'Meridian Holdings (Pvt) Ltd'             ),
       (65, 'Lanka Textiles (Pvt) Ltd'                ),
       (66, 'Wijesundara Estate'                      ),
       (67, 'Ceylon Apparel Co.'                      ),
       (68, 'Horizon Supplies Ltd'                    ),
       (69, 'Department of Immigration and Emigration'),
       (70, 'Mr. Ariyaratne (landlord)'               ),
       (71, 'Coastal Traders (Pvt) Ltd'               ),
       (72, 'Attorney General'                        ),
       (73, 'Mr. Nimal Costa (contractor)'            ),
       (74, 'Mr. K. Abeysekera (employee)'            ),
       (75, 'Attorney General'                        ),
       (76, 'Mr. D.M. Ranatunga (adjoining owner)'    ),
       (77, 'Registrar of Companies (advisory matter)'),
       (78, 'Ms. R. Jayawardena (fiancee)'            ),
       (79, 'Mr. Farook Hameed (landlord)'            ),
       (80, 'Southern Garments (Pvt) Ltd'             ),
       (81, 'Mr. Ajith Kumara (tenant)'               ),
       (82, 'Mr. Priyantha Jayasena (contractor)'     ),
       (83, 'Royal Events (Pvt) Ltd'                  ),
       (84, 'Mr. Lalith Gunasekara (upstairs owner)'  ),
       (85, 'Mr. Sunil Ekanayake (shareholder)'       ),
       (86, 'Mr. Dinesh Rodrigo (former supplier)'    ),
       (87, 'Mr. Roshan Silva (buyer)'                ),
       (88, 'Codeworks Lanka (Pvt) Ltd'               ),
       (89, 'Ceylinco Motor Insurance'                ),
       (90, 'Mr. Ravindu Senarath (neighbour)'        ),
       (91, 'Ms. Poornima Mendis (neighbour)'         ),
       (92, 'Mr. Nadun Perera (tenant)'               ),
       (93, 'Mr. Kasun Dias (former manager)'         );

-- Cases Data
INSERT INTO cases (case_code, consultation_id, client_id, associate_id, paralegal_id, case_status, opened_at, case_resolution_status, closed_at)
VALUES ('CAS-1-00087866',  1,  12, 2,  5,  'CLOSED', '2021-05-03 09:00:00.000', 'SETTLED',   '2021-08-01 09:00:00.000'),
       ('CAS-2-00136137',  3,  14, 4,  5,  'CLOSED', '2021-08-03 09:00:00.000', 'LOST',      '2022-01-14 09:00:00.000'),
       ('CAS-3-00184408',  4,  15, 2,  5,  'CLOSED', '2021-09-20 09:00:00.000', 'SETTLED',   '2022-04-09 09:00:00.000'),
       ('CAS-4-00232679',  5,  16, 3,  5,  'CLOSED', '2021-11-07 09:00:00.000', 'LOST',      '2022-07-03 09:00:00.000'),
       ('CAS-5-00280950',  6,  17, 4,  5,  'CLOSED', '2021-12-25 09:00:00.000', 'WON',       '2022-09-26 09:00:00.000'),
       ('CAS-6-00329221',  8,  19, 3,  5,  'CLOSED', '2022-03-31 09:00:00.000', 'WON',       '2023-03-15 09:00:00.000'),
       ('CAS-7-00377492',  10, 21, 2,  5,  'CLOSED', '2022-07-05 09:00:00.000', 'WON',       '2022-11-05 09:00:00.000'),
       ('CAS-8-00425763',  11, 22, 3,  5,  'CLOSED', '2022-08-22 09:00:00.000', 'WON',       '2023-01-29 09:00:00.000'),
       ('CAS-9-00474034',  13, 13, 3,  5,  'CLOSED', '2022-09-26 09:00:00.000', 'DISMISSED', '2023-08-29 09:00:00.000'),
       ('CAS-10-00522305', 14, 23, 4,  5,  'CLOSED', '2022-10-09 09:00:00.000', 'WON',       '2023-04-24 09:00:00.000'),
       ('CAS-11-00570576', 17, 15, 2,  5,  'CLOSED', '2022-11-28 09:00:00.000', 'WON',       '2026-01-05 16:00:00.000'),
       ('CAS-12-00618847', 16, 24, 2,  5,  'CLOSED', '2022-11-30 09:00:00.000', 'WON',       '2023-07-22 09:00:00.000'),
       ('CAS-13-00667118', 23, 19, 3,  5,  'CLOSED', '2023-04-03 09:00:00.000', 'WON',       '2026-02-07 16:00:00.000'),
       ('CAS-14-00715389', 24, 27, 2,  5,  'CLOSED', '2023-04-19 09:00:00.000', 'WON',       '2024-03-29 09:00:00.000'),
       ('CAS-15-00763660', 26, 21, 2,  5,  'CLOSED', '2023-06-05 09:00:00.000', 'SETTLED',   '2026-03-09 16:00:00.000'),
       ('CAS-16-00811931', 28, 22, 3,  5,  'CLOSED', '2023-07-07 09:00:00.000', 'DISMISSED', '2026-04-11 16:00:00.000'),
       ('CAS-17-00860202', 29, 29, 4,  5,  'CLOSED', '2023-07-24 09:00:00.000', 'LOST',      '2023-11-20 09:00:00.000'),
       ('CAS-18-00908473', 30, 23, 4,  5,  'CLOSED', '2023-08-07 09:00:00.000', 'LOST',      '2026-05-13 16:00:00.000'),
       ('CAS-19-00956744', 32, 30, 2,  5,  'CLOSED', '2023-09-14 09:00:00.000', 'WON',       '2024-02-17 09:00:00.000'),
       ('CAS-20-01005015', 33, 25, 3,  5,  'CLOSED', '2023-10-09 09:00:00.000', 'DISMISSED', '2026-06-15 16:00:00.000'),
       ('CAS-21-01053286', 34, 31, 3,  5,  'CLOSED', '2023-10-28 09:00:00.000', 'WON',       '2024-05-08 09:00:00.000'),
       ('CAS-22-01101557', 36, 27, 2,  5,  'CLOSED', '2023-12-11 09:00:00.000', 'LOST',      '2026-07-17 16:00:00.000'),
       ('CAS-23-01149828', 38, 28, 3,  5,  'CLOSED', '2024-01-12 09:00:00.000', 'SETTLED',   '2026-08-19 16:00:00.000'),
       ('CAS-24-01198099', 39, 33, 2,  5,  'CLOSED', '2024-02-01 09:00:00.000', 'DISMISSED', '2024-10-25 09:00:00.000'),
       ('CAS-25-01246370', 40, 29, 4,  5,  'CLOSED', '2024-02-12 09:00:00.000', 'SETTLED',   '2026-09-21 16:00:00.000'),
       ('CAS-26-01294641', 42, 34, 3,  5,  'CLOSED', '2024-03-20 09:00:00.000', 'WON',       '2025-01-18 09:00:00.000'),
       ('CAS-27-01342912', 44, 35, 4,  5,  'CLOSED', '2024-05-07 09:00:00.000', 'WON',       '2025-04-13 09:00:00.000'),
       ('CAS-28-01391183', 47, 36, 2,  5,  'CLOSED', '2024-06-28 09:00:00.000', 'SETTLED',   '2025-07-11 09:00:00.000'),
       ('CAS-29-01439454', 48, 34, 3,  5,  'CLOSED', '2024-07-19 09:00:00.000', 'WON',       '2026-01-23 16:00:00.000'),
       ('CAS-30-01487725', 49, 37, 3,  5,  'CLOSED', '2024-08-11 09:00:00.000', 'LOST',      '2024-12-04 09:00:00.000'),
       ('CAS-31-01535996', 50, 35, 4,  5,  'CLOSED', '2024-08-19 09:00:00.000', 'SETTLED',   '2026-02-25 16:00:00.000'),
       ('CAS-32-01584267', 53, 37, 3,  5,  'CLOSED', '2024-10-21 09:00:00.000', 'SETTLED',   '2026-03-27 16:00:00.000'),
       ('CAS-33-01632538', 54, 39, 2,  5,  'CLOSED', '2024-11-15 09:00:00.000', 'LOST',      '2025-05-23 09:00:00.000'),
       ('CAS-34-01680809', 56, 39, 2,  5,  'OPEN',   '2024-12-23 09:00:00.000', NULL,        NULL                     ),
       ('CAS-35-01729080', 57, 40, 3,  5,  'CLOSED', '2025-01-02 09:00:00.000', 'SETTLED',   '2025-08-16 09:00:00.000'),
       ('CAS-36-01777351', 58, 40, 3,  6,  'OPEN',   '2025-01-24 09:00:00.000', NULL,        NULL                     ),
       ('CAS-37-01825622', 59, 56, 4,  5,  'CLOSED', '2025-02-19 09:00:00.000', 'SETTLED',   '2025-11-09 09:00:00.000'),
       ('CAS-38-01873893', 60, 56, 4,  7,  'OPEN',   '2025-02-24 09:00:00.000', NULL,        NULL                     ),
       ('CAS-39-93580258', 61, 72, 51, 62, 'OPEN',   '2025-04-20 09:00:00.000', NULL,        NULL                     ),
       ('CAS-40-74814825', 64, 75, 52, 63, 'OPEN',   '2025-04-23 09:00:00.000', NULL,        NULL                     ),
       ('CAS-41-56049392', 65, 76, 53, 64, 'OPEN',   '2025-04-24 09:00:00.000', NULL,        NULL                     ),
       ('CAS-42-37283959', 66, 77, 54, 65, 'CLOSED', '2025-04-25 09:00:00.000', 'WON',       '2025-06-10 15:00:00.000'),
       ('CAS-43-18518526', 68, 79, 57, 67, 'OPEN',   '2025-04-27 09:00:00.000', NULL,        NULL                     ),
       ('CAS-44-89753093', 69, 80, 58, 68, 'OPEN',   '2025-04-28 09:00:00.000', NULL,        NULL                     ),
       ('CAS-45-70987660', 70, 81, 59, 69, 'OPEN',   '2025-04-29 09:00:00.000', NULL,        NULL                     ),
       ('CAS-46-52222227', 71, 72, 51, 62, 'CLOSED', '2025-07-10 09:00:00.000', 'WON',       '2025-08-20 15:00:00.000'),
       ('CAS-47-33456794', 79, 73, 60, 70, 'OPEN',   '2025-08-15 09:00:00.000', NULL,        NULL                     ),
       ('CAS-48-14691361', 80, 74, 61, 71, 'CLOSED', '2025-08-18 09:00:00.000', 'SETTLED',   '2025-09-05 15:00:00.000');

-- Contacts Data
INSERT INTO contacts (case_id, name, notes, added_at)
VALUES (1,  'Lanka Steel (Pvt) Ltd (counsel)',      NULL,                                                    '2021-05-04 09:00:00.000'),
       (2,  'Ms. Hemali Kumarasinghe (counsel)',    NULL,                                                    '2021-08-04 09:00:00.000'),
       (3,  'Seaview Constructions (counsel)',      NULL,                                                    '2021-09-21 09:00:00.000'),
       (4,  'Mr. Damith Weerakoon (counsel)',       NULL,                                                    '2021-11-08 09:00:00.000'),
       (5,  'Ms. Anoma Goonetilleke (counsel)',     NULL,                                                    '2021-12-26 09:00:00.000'),
       (6,  'Mr. Kelum Ellawala (counsel)',         NULL,                                                    '2022-04-01 09:00:00.000'),
       (7,  'Mr. Saliya Senadheera (counsel)',      NULL,                                                    '2022-07-06 09:00:00.000'),
       (8,  'Hillside Developers (counsel)',        NULL,                                                    '2022-08-23 09:00:00.000'),
       (9,  'Mr. Kelum Ellawala (counsel)',         NULL,                                                    '2022-09-27 09:00:00.000'),
       (10, 'Mr. Asitha Warnakulasuriya (counsel)', NULL,                                                    '2022-10-10 09:00:00.000'),
       (11, 'Mr. Saliya Senadheera (counsel)',      NULL,                                                    '2022-11-29 09:00:00.000'),
       (12, 'Lanka Steel (Pvt) Ltd (counsel)',      NULL,                                                    '2022-12-01 09:00:00.000'),
       (13, 'Mr. Ruchira Jayatilleke (counsel)',    NULL,                                                    '2023-04-04 09:00:00.000'),
       (14, 'Seaview Constructions (counsel)',      NULL,                                                    '2023-04-20 09:00:00.000'),
       (15, 'Seaview Constructions (counsel)',      NULL,                                                    '2023-06-06 09:00:00.000'),
       (16, 'Mr. Damith Weerakoon (counsel)',       NULL,                                                    '2023-07-08 09:00:00.000'),
       (17, 'Ms. Anoma Goonetilleke (counsel)',     NULL,                                                    '2023-07-25 09:00:00.000'),
       (18, 'Ms. Anoma Goonetilleke (counsel)',     NULL,                                                    '2023-08-08 09:00:00.000'),
       (19, 'Island Freight (Pvt) Ltd (counsel)',   NULL,                                                    '2023-09-15 09:00:00.000'),
       (20, 'Mr. Kelum Ellawala (counsel)',         NULL,                                                    '2023-10-10 09:00:00.000'),
       (21, 'Mr. Kelum Ellawala (counsel)',         NULL,                                                    '2023-10-29 09:00:00.000'),
       (22, 'Mr. Saliya Senadheera (counsel)',      NULL,                                                    '2023-12-12 09:00:00.000'),
       (23, 'Hillside Developers (counsel)',        NULL,                                                    '2024-01-13 09:00:00.000'),
       (24, 'Mr. Saliya Senadheera (counsel)',      NULL,                                                    '2024-02-02 09:00:00.000'),
       (25, 'Mr. Asitha Warnakulasuriya (counsel)', NULL,                                                    '2024-02-13 09:00:00.000'),
       (26, 'Hillside Developers (counsel)',        NULL,                                                    '2024-03-21 09:00:00.000'),
       (27, 'Mr. Asitha Warnakulasuriya (counsel)', NULL,                                                    '2024-05-08 09:00:00.000'),
       (28, 'Lanka Steel (Pvt) Ltd (counsel)',      NULL,                                                    '2024-06-29 09:00:00.000'),
       (29, 'Mr. Damith Weerakoon (counsel)',       NULL,                                                    '2024-07-20 09:00:00.000'),
       (30, 'Mr. Ruchira Jayatilleke (counsel)',    NULL,                                                    '2024-08-12 09:00:00.000'),
       (31, 'Ms. Anoma Goonetilleke (counsel)',     NULL,                                                    '2024-08-20 09:00:00.000'),
       (32, 'Mr. Kelum Ellawala (counsel)',         NULL,                                                    '2024-10-22 09:00:00.000'),
       (33, 'Seaview Constructions (counsel)',      NULL,                                                    '2024-11-16 09:00:00.000'),
       (34, 'Mr. Saliya Senadheera (counsel)',      NULL,                                                    '2024-12-24 09:00:00.000'),
       (35, 'Mr. Damith Weerakoon (counsel)',       NULL,                                                    '2025-01-03 09:00:00.000'),
       (36, 'Hillside Developers (counsel)',        NULL,                                                    '2025-01-25 09:00:00.000'),
       (37, 'Ms. Anoma Goonetilleke (counsel)',     NULL,                                                    '2025-02-20 09:00:00.000'),
       (38, 'Mr. Asitha Warnakulasuriya (counsel)', NULL,                                                    '2025-02-25 09:00:00.000'),
       (39, 'Suresh Wijeratne',                     NULL,                                                    '2025-05-01 09:00:00.000'),
       (41, 'Suresh Wijeratne',                     NULL,                                                    '2025-05-01 09:00:00.000'),
       (39, 'Malini Gunasekara',                    'Witness for the Perera custody matter.',                '2025-05-02 09:00:00.000'),
       (40, 'Rohan Fernando',                       NULL,                                                    '2025-05-03 09:00:00.000'),
       (41, 'Priya Rajaratnam',                     NULL,                                                    '2025-05-04 09:00:00.000'),
       (42, 'Ajith Kumara',                         NULL,                                                    '2025-05-05 09:00:00.000'),
       (42, 'Nilmini Perera',                       'Frequently retained expert witness.',                   '2025-05-06 09:00:00.000'),
       (43, 'Nilmini Perera',                       'Frequently retained expert witness.',                   '2025-05-06 09:00:00.000'),
       (43, 'Saman Dias',                           NULL,                                                    '2025-05-07 09:00:00.000'),
       (44, 'Kumari Silva',                         NULL,                                                    '2025-05-08 09:00:00.000'),
       (44, 'Lasantha Bandara',                     'Land surveyor, called in for boundary disputes.',       '2025-05-09 09:00:00.000'),
       (45, 'Ramya Jayasundara',                    NULL,                                                    '2025-05-10 09:00:00.000'),
       (39, 'Nirmal Costa',                         NULL,                                                    '2025-06-01 09:00:00.000'),
       (39, 'Aruni Peiris',                         NULL,                                                    '2025-06-02 09:00:00.000'),
       (39, 'Sunil Bandaranaike',                   NULL,                                                    '2025-06-03 09:00:00.000'),
       (39, 'Champa Rodrigo',                       NULL,                                                    '2025-06-04 09:00:00.000'),
       (46, 'Champa Rodrigo',                       NULL,                                                    '2025-06-04 09:00:00.000'),
       (39, 'Deepal Wickrama',                      'Long-standing expert witness for Mr. Yapa''s matters.', '2025-06-05 09:00:00.000'),
       (46, 'Deepal Wickrama',                      'Long-standing expert witness for Mr. Yapa''s matters.', '2025-06-05 09:00:00.000'),
       (47, 'Tilak Ranasinghe',                     'Represents the tenants association.',                   '2025-08-16 09:00:00.000');

-- Contact Phone Numbers Data
INSERT INTO contact_phone_numbers (contact_id, phone_number)
VALUES (1,  '0762000000'),
       (2,  '0762001826'),
       (3,  '0762002739'),
       (4,  '0762003652'),
       (5,  '0762004565'),
       (6,  '0762006391'),
       (7,  '0762008217'),
       (8,  '0762009130'),
       (9,  '0762028303'),
       (10, '0762010043'),
       (11, '0762030129'),
       (12, '0762010956'),
       (13, '0762033781'),
       (14, '0762013695'),
       (15, '0762035607'),
       (16, '0762036520'),
       (17, '0762015521'),
       (18, '0762037433'),
       (19, '0762016434'),
       (20, '0762039259'),
       (21, '0762017347'),
       (22, '0762041085'),
       (23, '0762041998'),
       (24, '0762019173'),
       (25, '0762042911'),
       (26, '0762020086'),
       (27, '0762020999'),
       (28, '0762021912'),
       (29, '0762047476'),
       (30, '0762022825'),
       (31, '0762048389'),
       (32, '0762050215'),
       (33, '0762024651'),
       (34, '0762052041'),
       (35, '0762025564'),
       (36, '0762052954'),
       (37, '0762026477'),
       (38, '0762053867'),
       (39, '0761111111'),
       (39, '0711111111'),
       (40, '0761111111'),
       (40, '0711111111'),
       (41, '0761111112'),
       (42, '0761111113'),
       (43, '0761111114'),
       (44, '0761111115'),
       (45, '0761111116'),
       (46, '0761111116'),
       (47, '0761111117'),
       (48, '0761111118'),
       (49, '0761111119'),
       (50, '0761111120'),
       (51, '0762222211'),
       (52, '0762222212'),
       (56, '0762222215'),
       (56, '0712222215'),
       (57, '0762222215'),
       (57, '0712222215');

-- Contact Emails Data
INSERT INTO contact_emails (contact_id, email)
VALUES (1,  'counsel1@example.com'         ),
       (2,  'counsel3@example.com'         ),
       (3,  'counsel4@example.com'         ),
       (4,  'counsel5@example.com'         ),
       (5,  'counsel6@example.com'         ),
       (6,  'counsel8@example.com'         ),
       (7,  'counsel10@example.com'        ),
       (8,  'counsel11@example.com'        ),
       (9,  'counsel32@example.com'        ),
       (10, 'counsel12@example.com'        ),
       (11, 'counsel34@example.com'        ),
       (12, 'counsel13@example.com'        ),
       (13, 'counsel38@example.com'        ),
       (14, 'counsel16@example.com'        ),
       (15, 'counsel40@example.com'        ),
       (16, 'counsel41@example.com'        ),
       (17, 'counsel18@example.com'        ),
       (18, 'counsel42@example.com'        ),
       (19, 'counsel19@example.com'        ),
       (20, 'counsel44@example.com'        ),
       (21, 'counsel20@example.com'        ),
       (22, 'counsel46@example.com'        ),
       (23, 'counsel47@example.com'        ),
       (24, 'counsel22@example.com'        ),
       (25, 'counsel48@example.com'        ),
       (26, 'counsel23@example.com'        ),
       (27, 'counsel24@example.com'        ),
       (28, 'counsel25@example.com'        ),
       (29, 'counsel53@example.com'        ),
       (30, 'counsel26@example.com'        ),
       (31, 'counsel54@example.com'        ),
       (32, 'counsel56@example.com'        ),
       (33, 'counsel28@example.com'        ),
       (34, 'counsel58@example.com'        ),
       (35, 'counsel29@example.com'        ),
       (36, 'counsel59@example.com'        ),
       (37, 'counsel30@example.com'        ),
       (38, 'counsel60@example.com'        ),
       (42, 'rohan.fernando@example.com'   ),
       (45, 'nilmini.perera@example.com'   ),
       (45, 'n.perera.expert@example.com'  ),
       (46, 'nilmini.perera@example.com'   ),
       (46, 'n.perera.expert@example.com'  ),
       (47, 'saman.dias@example.com'       ),
       (50, 'ramya.jayasundara@example.com'),
       (52, 'aruni.peiris@example.com'     ),
       (56, 'deepal.wickrama@example.com'  ),
       (57, 'deepal.wickrama@example.com'  );

-- Contact Addresses Data
INSERT INTO contact_addresses (contact_id, address)
VALUES (1,  'No. 5, Flower Road, Colombo'      ),
       (2,  'No. 7, Main Street, Colombo'      ),
       (3,  'No. 8, Hospital Road, Colombo'    ),
       (4,  'No. 9, Church Street, Colombo'    ),
       (5,  'No. 10, Market Road, Colombo'     ),
       (6,  'No. 12, Station Road, Colombo'    ),
       (7,  'No. 14, Lake Road, Colombo'       ),
       (8,  'No. 15, Flower Road, Colombo'     ),
       (9,  'No. 36, School Lane, Colombo'     ),
       (10, 'No. 16, School Lane, Colombo'     ),
       (11, 'No. 38, Hospital Road, Colombo'   ),
       (12, 'No. 17, Main Street, Colombo'     ),
       (13, 'No. 42, Station Road, Colombo'    ),
       (14, 'No. 20, Market Road, Colombo'     ),
       (15, 'No. 44, Lake Road, Colombo'       ),
       (16, 'No. 45, Flower Road, Colombo'     ),
       (17, 'No. 22, Station Road, Colombo'    ),
       (18, 'No. 46, School Lane, Colombo'     ),
       (19, 'No. 23, Park Avenue, Colombo'     ),
       (20, 'No. 48, Hospital Road, Colombo'   ),
       (21, 'No. 24, Lake Road, Colombo'       ),
       (22, 'No. 50, Market Road, Colombo'     ),
       (23, 'No. 51, Temple Road, Colombo'     ),
       (24, 'No. 26, School Lane, Colombo'     ),
       (25, 'No. 52, Station Road, Colombo'    ),
       (26, 'No. 27, Main Street, Colombo'     ),
       (27, 'No. 28, Hospital Road, Colombo'   ),
       (28, 'No. 29, Church Street, Colombo'   ),
       (29, 'No. 57, Main Street, Colombo'     ),
       (30, 'No. 30, Market Road, Colombo'     ),
       (31, 'No. 58, Hospital Road, Colombo'   ),
       (32, 'No. 60, Market Road, Colombo'     ),
       (33, 'No. 32, Station Road, Colombo'    ),
       (34, 'No. 62, Station Road, Colombo'    ),
       (35, 'No. 33, Park Avenue, Colombo'     ),
       (36, 'No. 63, Park Avenue, Colombo'     ),
       (37, 'No. 34, Lake Road, Colombo'       ),
       (38, 'No. 64, Lake Road, Colombo'       ),
       (42, 'No. 34, Baseline Road, Colombo'   ),
       (43, 'No. 70, Galle Road, Colombo'      ),
       (45, 'No. 15, Kandy Road, Colombo'      ),
       (46, 'No. 15, Kandy Road, Colombo'      ),
       (47, 'No. 21, Duplication Road, Colombo'),
       (50, 'No. 88, Lake Drive, Colombo'      ),
       (54, 'No. 92, Galle Road, Colombo'      ),
       (55, 'No. 92, Galle Road, Colombo'      ),
       (56, 'No. 5, Lake Drive, Colombo'       ),
       (57, 'No. 5, Lake Drive, Colombo'       );

-- Invoices Data
INSERT INTO invoices (invoice_code, case_id, total_amount, invoice_status, compiled_at, paid_at)
VALUES ('INV-1-00111623',  1,  51000.00, 'VOIDED', '2021-06-30 09:00:00.000', NULL                     ),
       ('INV-2-00159894',  1,  51000.00, 'PAID',   '2021-07-20 09:00:00.000', '2021-07-30 09:00:00.000'),
       ('INV-3-00208165',  2,  34000.00, 'PAID',   '2022-01-02 09:00:00.000', '2022-01-12 09:00:00.000'),
       ('INV-4-00256436',  3,  65000.00, 'PAID',   '2022-03-28 09:00:00.000', '2022-04-07 09:00:00.000'),
       ('INV-5-00304707',  4,  16000.00, 'PAID',   '2022-06-21 09:00:00.000', '2022-07-01 09:00:00.000'),
       ('INV-6-00352978',  5,  58000.00, 'VOIDED', '2022-08-25 09:00:00.000', NULL                     ),
       ('INV-7-00401249',  5,  58000.00, 'PAID',   '2022-09-14 09:00:00.000', '2022-09-24 09:00:00.000'),
       ('INV-8-00449520',  7,  33000.00, 'PAID',   '2022-10-24 09:00:00.000', '2022-11-03 09:00:00.000'),
       ('INV-9-00497791',  8,  35000.00, 'VOIDED', '2022-12-28 09:00:00.000', NULL                     ),
       ('INV-10-00546062', 8,  35000.00, 'PAID',   '2023-01-17 09:00:00.000', '2023-01-27 09:00:00.000'),
       ('INV-11-00594333', 6,  51000.00, 'PAID',   '2023-03-03 09:00:00.000', '2023-03-13 09:00:00.000'),
       ('INV-12-00642604', 10, 25000.00, 'PAID',   '2023-04-12 09:00:00.000', '2023-04-22 09:00:00.000'),
       ('INV-13-00690875', 12, 55000.00, 'PAID',   '2023-07-10 09:00:00.000', '2023-07-20 09:00:00.000'),
       ('INV-14-00739146', 9,  69000.00, 'PAID',   '2023-08-17 09:00:00.000', '2023-08-27 09:00:00.000'),
       ('INV-15-00787417', 17, 53000.00, 'PAID',   '2023-11-08 09:00:00.000', '2023-11-18 09:00:00.000'),
       ('INV-16-00835688', 19, 32000.00, 'PAID',   '2024-02-05 09:00:00.000', '2024-02-15 09:00:00.000'),
       ('INV-17-00883959', 14, 36000.00, 'VOIDED', '2024-02-26 09:00:00.000', NULL                     ),
       ('INV-18-00932230', 14, 36000.00, 'PAID',   '2024-03-17 09:00:00.000', '2024-03-27 09:00:00.000'),
       ('INV-19-00980501', 21, 25000.00, 'PAID',   '2024-04-26 09:00:00.000', '2024-05-06 09:00:00.000'),
       ('INV-20-01028772', 24, 39000.00, 'PAID',   '2024-10-13 09:00:00.000', '2024-10-23 09:00:00.000'),
       ('INV-21-01077043', 30, 69000.00, 'VOIDED', '2024-11-02 09:00:00.000', NULL                     ),
       ('INV-22-01125314', 30, 69000.00, 'PAID',   '2024-11-22 09:00:00.000', '2024-12-02 09:00:00.000'),
       ('INV-23-01173585', 26, 66000.00, 'PAID',   '2025-01-06 09:00:00.000', '2025-01-16 09:00:00.000'),
       ('INV-24-01221856', 27, 82000.00, 'PAID',   '2025-04-01 09:00:00.000', '2025-04-11 09:00:00.000'),
       ('INV-25-01270127', 33, 58000.00, 'PAID',   '2025-05-11 09:00:00.000', '2025-05-21 09:00:00.000'),
       ('INV-26-93580258', 39, 15000.00, 'UNPAID', '2025-06-01 09:00:00.000', NULL                     ),
       ('INV-27-74814825', 40, 33000.00, 'UNPAID', '2025-06-02 09:00:00.000', NULL                     ),
       ('INV-28-56049392', 41, 12000.00, 'PAID',   '2025-06-03 09:00:00.000', '2025-06-15 10:00:00.000'),
       ('INV-29-37283959', 42, 30000.00, 'PAID',   '2025-06-04 09:00:00.000', '2025-06-16 10:00:00.000'),
       ('INV-30-18518526', 44, 9500.00,  'VOIDED', '2025-06-05 09:00:00.000', NULL                     ),
       ('INV-31-01559753', 28, 49000.00, 'PAID',   '2025-06-29 09:00:00.000', '2025-07-09 09:00:00.000'),
       ('INV-32-89753093', 39, 20000.00, 'PAID',   '2025-08-01 09:00:00.000', '2025-08-05 10:00:00.000'),
       ('INV-33-70987660', 39, 10000.00, 'VOIDED', '2025-08-02 09:00:00.000', NULL                     ),
       ('INV-34-52222227', 39, 12000.00, 'VOIDED', '2025-08-03 09:00:00.000', '2025-08-06 10:00:00.000'),
       ('INV-35-01752837', 35, 51000.00, 'PAID',   '2025-08-04 09:00:00.000', '2025-08-14 09:00:00.000'),
       ('INV-36-33456794', 46, 45000.00, 'PAID',   '2025-08-10 09:00:00.000', '2025-08-15 10:00:00.000'),
       ('INV-37-14691361', 43, 14000.00, 'UNPAID', '2025-08-20 09:00:00.000', NULL                     ),
       ('INV-38-01897650', 37, 52000.00, 'PAID',   '2025-10-28 09:00:00.000', '2025-11-07 09:00:00.000'),
       ('INV-39-01945921', 11, 64000.00, 'PAID',   '2025-12-24 16:00:00.000', '2026-01-03 16:00:00.000'),
       ('INV-40-01994192', 29, 45000.00, 'PAID',   '2026-01-11 16:00:00.000', '2026-01-21 16:00:00.000'),
       ('INV-41-02042463', 13, 36000.00, 'PAID',   '2026-01-26 16:00:00.000', '2026-02-05 16:00:00.000'),
       ('INV-42-02090734', 31, 57000.00, 'PAID',   '2026-02-13 16:00:00.000', '2026-02-23 16:00:00.000'),
       ('INV-43-02139005', 32, 54000.00, 'VOIDED', '2026-02-23 16:00:00.000', NULL                     ),
       ('INV-44-02187276', 15, 58000.00, 'PAID',   '2026-02-25 16:00:00.000', '2026-03-07 16:00:00.000'),
       ('INV-45-02235547', 16, 14000.00, 'VOIDED', '2026-03-10 16:00:00.000', NULL                     ),
       ('INV-46-02283818', 32, 54000.00, 'PAID',   '2026-03-15 16:00:00.000', '2026-03-25 16:00:00.000'),
       ('INV-47-02332089', 16, 14000.00, 'PAID',   '2026-03-30 16:00:00.000', '2026-04-09 16:00:00.000'),
       ('INV-48-02380360', 18, 65000.00, 'PAID',   '2026-05-01 16:00:00.000', '2026-05-11 16:00:00.000'),
       ('INV-49-02428631', 20, 82000.00, 'PAID',   '2026-06-03 16:00:00.000', '2026-06-13 16:00:00.000'),
       ('INV-50-02476902', 22, 37000.00, 'VOIDED', '2026-06-15 16:00:00.000', NULL                     ),
       ('INV-51-02525173', 22, 37000.00, 'PAID',   '2026-07-05 16:00:00.000', '2026-07-15 16:00:00.000'),
       ('INV-52-02573444', 23, 20000.00, 'PAID',   '2026-08-07 16:00:00.000', '2026-08-17 16:00:00.000'),
       ('INV-53-02621715', 25, 42000.00, 'PAID',   '2026-09-09 16:00:00.000', '2026-09-19 16:00:00.000');

-- Transactions Data
INSERT INTO transactions (transaction_code, client_id, amount, transaction_type, invoice_id, occurred_at)
VALUES ('TXN-1-00143299',  12, 51000.00, 'CASH_TOP_UP', NULL, '2021-07-29 09:00:00.000'),
       ('TXN-2-00191570',  12, 51000.00, 'CHARGE',      2,    '2021-07-30 09:00:00.000'),
       ('TXN-3-00239841',  14, 39000.00, 'CASH_TOP_UP', NULL, '2022-01-11 09:00:00.000'),
       ('TXN-4-00288112',  14, 34000.00, 'CHARGE',      3,    '2022-01-12 09:00:00.000'),
       ('TXN-5-00336383',  15, 65000.00, 'CARD_TOP_UP', NULL, '2022-04-06 09:00:00.000'),
       ('TXN-6-00384654',  15, 65000.00, 'CHARGE',      4,    '2022-04-07 09:00:00.000'),
       ('TXN-7-00432925',  16, 18500.00, 'CASH_TOP_UP', NULL, '2022-06-30 09:00:00.000'),
       ('TXN-8-00481196',  16, 16000.00, 'CHARGE',      5,    '2022-07-01 09:00:00.000'),
       ('TXN-9-00529467',  17, 63000.00, 'CARD_TOP_UP', NULL, '2022-09-23 09:00:00.000'),
       ('TXN-10-00577738', 17, 58000.00, 'CHARGE',      7,    '2022-09-24 09:00:00.000'),
       ('TXN-11-00626009', 21, 33000.00, 'CARD_TOP_UP', NULL, '2022-11-02 09:00:00.000'),
       ('TXN-12-00674280', 21, 33000.00, 'CHARGE',      8,    '2022-11-03 09:00:00.000'),
       ('TXN-13-00722551', 22, 37500.00, 'CASH_TOP_UP', NULL, '2023-01-26 09:00:00.000'),
       ('TXN-14-00770822', 22, 35000.00, 'CHARGE',      10,   '2023-01-27 09:00:00.000'),
       ('TXN-15-00819093', 19, 53500.00, 'CARD_TOP_UP', NULL, '2023-03-12 09:00:00.000'),
       ('TXN-16-00867364', 19, 51000.00, 'CHARGE',      11,   '2023-03-13 09:00:00.000'),
       ('TXN-17-00915635', 23, 30000.00, 'CARD_TOP_UP', NULL, '2023-04-21 09:00:00.000'),
       ('TXN-18-00963906', 23, 25000.00, 'CHARGE',      12,   '2023-04-22 09:00:00.000'),
       ('TXN-19-01012177', 24, 55000.00, 'CASH_TOP_UP', NULL, '2023-07-19 09:00:00.000'),
       ('TXN-20-01060448', 24, 55000.00, 'CHARGE',      13,   '2023-07-20 09:00:00.000'),
       ('TXN-21-01108719', 13, 71500.00, 'CARD_TOP_UP', NULL, '2023-08-26 09:00:00.000'),
       ('TXN-22-01156990', 13, 69000.00, 'CHARGE',      14,   '2023-08-27 09:00:00.000'),
       ('TXN-23-01205261', 29, 58000.00, 'CARD_TOP_UP', NULL, '2023-11-17 09:00:00.000'),
       ('TXN-24-01253532', 29, 53000.00, 'CHARGE',      15,   '2023-11-18 09:00:00.000'),
       ('TXN-25-01301803', 30, 32000.00, 'CASH_TOP_UP', NULL, '2024-02-14 09:00:00.000'),
       ('TXN-26-01350074', 30, 32000.00, 'CHARGE',      16,   '2024-02-15 09:00:00.000'),
       ('TXN-27-01398345', 27, 36000.00, 'CARD_TOP_UP', NULL, '2024-03-26 09:00:00.000'),
       ('TXN-28-01446616', 27, 36000.00, 'CHARGE',      18,   '2024-03-27 09:00:00.000'),
       ('TXN-29-01494887', 31, 27500.00, 'CARD_TOP_UP', NULL, '2024-05-05 09:00:00.000'),
       ('TXN-30-01543158', 31, 25000.00, 'CHARGE',      19,   '2024-05-06 09:00:00.000'),
       ('TXN-31-01591429', 33, 39000.00, 'CARD_TOP_UP', NULL, '2024-10-22 09:00:00.000'),
       ('TXN-32-01639700', 33, 39000.00, 'CHARGE',      20,   '2024-10-23 09:00:00.000'),
       ('TXN-33-01687971', 37, 71500.00, 'CARD_TOP_UP', NULL, '2024-12-01 09:00:00.000'),
       ('TXN-34-01736242', 37, 69000.00, 'CHARGE',      22,   '2024-12-02 09:00:00.000'),
       ('TXN-35-01784513', 34, 68500.00, 'CASH_TOP_UP', NULL, '2025-01-15 09:00:00.000'),
       ('TXN-36-01832784', 34, 66000.00, 'CHARGE',      23,   '2025-01-16 09:00:00.000'),
       ('TXN-37-01881055', 35, 87000.00, 'CARD_TOP_UP', NULL, '2025-04-10 09:00:00.000'),
       ('TXN-38-01929326', 35, 82000.00, 'CHARGE',      24,   '2025-04-11 09:00:00.000'),
       ('TXN-39-93580258', 72, 50000.00, 'CARD_TOP_UP', NULL, '2025-05-01 09:00:00.000'),
       ('TXN-40-56049392', 73, 20000.00, 'CARD_TOP_UP', NULL, '2025-05-02 09:00:00.000'),
       ('TXN-41-37283959', 75, 40000.00, 'CARD_TOP_UP', NULL, '2025-05-03 09:00:00.000'),
       ('TXN-42-18518526', 76, 15000.00, 'CARD_TOP_UP', NULL, '2025-05-04 09:00:00.000'),
       ('TXN-43-70987660', 77, 35000.00, 'CARD_TOP_UP', NULL, '2025-05-05 09:00:00.000'),
       ('TXN-44-33456794', 79, 25000.00, 'CASH_TOP_UP', NULL, '2025-05-06 09:00:00.000'),
       ('TXN-45-14691361', 80, 18000.00, 'CARD_TOP_UP', NULL, '2025-05-07 09:00:00.000'),
       ('TXN-46-74814825', 72, 10000.00, 'CASH_TOP_UP', NULL, '2025-05-15 09:00:00.000'),
       ('TXN-47-02363765', 39, 58000.00, 'CARD_TOP_UP', NULL, '2025-05-20 09:00:00.000'),
       ('TXN-48-02412036', 39, 58000.00, 'CHARGE',      25,   '2025-05-21 09:00:00.000'),
       ('TXN-49-85925928', 72, 5000.00,  'CARD_TOP_UP', NULL, '2025-06-01 09:00:00.000'),
       ('TXN-50-67160495', 72, 8000.00,  'CASH_TOP_UP', NULL, '2025-06-08 09:00:00.000'),
       ('TXN-51-48395062', 72, 12000.00, 'CARD_TOP_UP', NULL, '2025-06-15 09:00:00.000'),
       ('TXN-52-89753093', 76, 12000.00, 'CHARGE',      28,   '2025-06-15 10:00:00.000'),
       ('TXN-53-52222227', 77, 30000.00, 'CHARGE',      29,   '2025-06-16 10:00:00.000'),
       ('TXN-54-29629629', 72, 3000.00,  'CASH_TOP_UP', NULL, '2025-06-22 09:00:00.000'),
       ('TXN-55-10864196', 72, 7000.00,  'CARD_TOP_UP', NULL, '2025-06-29 09:00:00.000'),
       ('TXN-56-82098763', 72, 15000.00, 'CASH_TOP_UP', NULL, '2025-07-06 09:00:00.000'),
       ('TXN-57-02846475', 36, 49000.00, 'CASH_TOP_UP', NULL, '2025-07-08 09:00:00.000'),
       ('TXN-58-02894746', 36, 49000.00, 'CHARGE',      31,   '2025-07-09 09:00:00.000'),
       ('TXN-59-44567897', 72, 12000.00, 'CHARGE',      34,   '2025-08-04 10:00:00.000'),
       ('TXN-60-63333330', 72, 20000.00, 'CHARGE',      32,   '2025-08-05 10:00:00.000'),
       ('TXN-61-25802464', 72, 12000.00, 'REFUND',      34,   '2025-08-06 10:00:00.000'),
       ('TXN-62-03087830', 40, 53500.00, 'CASH_TOP_UP', NULL, '2025-08-13 09:00:00.000'),
       ('TXN-63-03136101', 40, 51000.00, 'CHARGE',      35,   '2025-08-14 09:00:00.000'),
       ('TXN-64-97037031', 72, 45000.00, 'CHARGE',      36,   '2025-08-15 10:00:00.000'),
       ('TXN-65-03232643', 56, 57000.00, 'CARD_TOP_UP', NULL, '2025-11-06 09:00:00.000'),
       ('TXN-66-03280914', 56, 52000.00, 'CHARGE',      38,   '2025-11-07 09:00:00.000'),
       ('TXN-67-03329185', 15, 64000.00, 'CARD_TOP_UP', NULL, '2026-01-02 16:00:00.000'),
       ('TXN-68-03377456', 15, 64000.00, 'CHARGE',      39,   '2026-01-03 16:00:00.000'),
       ('TXN-69-03425727', 34, 47500.00, 'CASH_TOP_UP', NULL, '2026-01-20 16:00:00.000'),
       ('TXN-70-03473998', 34, 45000.00, 'CHARGE',      40,   '2026-01-21 16:00:00.000'),
       ('TXN-71-03522269', 19, 38500.00, 'CARD_TOP_UP', NULL, '2026-02-04 16:00:00.000'),
       ('TXN-72-03570540', 19, 36000.00, 'CHARGE',      41,   '2026-02-05 16:00:00.000'),
       ('TXN-73-03618811', 35, 62000.00, 'CARD_TOP_UP', NULL, '2026-02-22 16:00:00.000'),
       ('TXN-74-03667082', 35, 57000.00, 'CHARGE',      42,   '2026-02-23 16:00:00.000'),
       ('TXN-75-03715353', 21, 58000.00, 'CARD_TOP_UP', NULL, '2026-03-06 16:00:00.000'),
       ('TXN-76-03763624', 21, 58000.00, 'CHARGE',      44,   '2026-03-07 16:00:00.000'),
       ('TXN-77-03811895', 37, 56500.00, 'CARD_TOP_UP', NULL, '2026-03-24 16:00:00.000'),
       ('TXN-78-03860166', 37, 54000.00, 'CHARGE',      46,   '2026-03-25 16:00:00.000'),
       ('TXN-79-03908437', 22, 16500.00, 'CASH_TOP_UP', NULL, '2026-04-08 16:00:00.000'),
       ('TXN-80-03956708', 22, 14000.00, 'CHARGE',      47,   '2026-04-09 16:00:00.000'),
       ('TXN-81-04004979', 23, 70000.00, 'CARD_TOP_UP', NULL, '2026-05-10 16:00:00.000'),
       ('TXN-82-04053250', 23, 65000.00, 'CHARGE',      48,   '2026-05-11 16:00:00.000'),
       ('TXN-83-04101521', 25, 84500.00, 'CARD_TOP_UP', NULL, '2026-06-12 16:00:00.000'),
       ('TXN-84-04149792', 25, 82000.00, 'CHARGE',      49,   '2026-06-13 16:00:00.000'),
       ('TXN-85-04198063', 27, 37000.00, 'CARD_TOP_UP', NULL, '2026-07-14 16:00:00.000'),
       ('TXN-86-04246334', 27, 37000.00, 'CHARGE',      51,   '2026-07-15 16:00:00.000'),
       ('TXN-87-04294605', 28, 22500.00, 'CASH_TOP_UP', NULL, '2026-08-16 16:00:00.000'),
       ('TXN-88-04342876', 28, 20000.00, 'CHARGE',      52,   '2026-08-17 16:00:00.000'),
       ('TXN-89-04391147', 29, 47000.00, 'CARD_TOP_UP', NULL, '2026-09-18 16:00:00.000'),
       ('TXN-90-04439418', 29, 42000.00, 'CHARGE',      53,   '2026-09-19 16:00:00.000');

-- Tasks Data
INSERT INTO tasks (case_id, title, documenting_task, scheduling_task, billing_task, billable_amount, billed_invoice_id, logged_at)
VALUES (1,  'Prepare the case file',                    1, 0, 0, NULL,     NULL, '2021-05-05 09:00:00.000'),
       (1,  'Court hearing',                            0, 1, 0, NULL,     NULL, '2021-05-06 09:00:00.000'),
       (1,  'Legal representation fee',                 0, 0, 1, 33000.00, 2,    '2021-06-21 09:00:00.000'),
       (1,  'Drafting and filing fee',                  0, 0, 1, 18000.00, 2,    '2021-06-22 09:00:00.000'),
       (2,  'Court hearing',                            0, 1, 0, NULL,     NULL, '2021-08-06 09:00:00.000'),
       (3,  'Prepare the case file',                    1, 0, 0, NULL,     NULL, '2021-09-22 09:00:00.000'),
       (3,  'Court hearing',                            0, 1, 0, NULL,     NULL, '2021-09-23 09:00:00.000'),
       (4,  'Court hearing',                            0, 1, 0, NULL,     NULL, '2021-11-10 09:00:00.000'),
       (2,  'Legal representation fee',                 0, 0, 1, 12000.00, 3,    '2021-12-04 09:00:00.000'),
       (2,  'Drafting and filing fee',                  0, 0, 1, 22000.00, 3,    '2021-12-05 09:00:00.000'),
       (5,  'Court hearing',                            0, 1, 0, NULL,     NULL, '2021-12-28 09:00:00.000'),
       (3,  'Legal representation fee',                 0, 0, 1, 35000.00, 4,    '2022-02-27 09:00:00.000'),
       (3,  'Drafting and filing fee',                  0, 0, 1, 30000.00, 4,    '2022-02-28 09:00:00.000'),
       (6,  'Court hearing',                            0, 1, 0, NULL,     NULL, '2022-04-03 09:00:00.000'),
       (4,  'Legal representation fee',                 0, 0, 1, 11000.00, 5,    '2022-05-23 09:00:00.000'),
       (4,  'Drafting and filing fee',                  0, 0, 1, 5000.00,  5,    '2022-05-24 09:00:00.000'),
       (7,  'Prepare the case file',                    1, 0, 0, NULL,     NULL, '2022-07-07 09:00:00.000'),
       (7,  'Court hearing',                            0, 1, 0, NULL,     NULL, '2022-07-08 09:00:00.000'),
       (5,  'Legal representation fee',                 0, 0, 1, 49000.00, 7,    '2022-08-16 09:00:00.000'),
       (5,  'Drafting and filing fee',                  0, 0, 1, 9000.00,  7,    '2022-08-17 09:00:00.000'),
       (8,  'Court hearing',                            0, 1, 0, NULL,     NULL, '2022-08-25 09:00:00.000'),
       (7,  'Legal representation fee',                 0, 0, 1, 13000.00, 8,    '2022-09-25 09:00:00.000'),
       (7,  'Drafting and filing fee',                  0, 0, 1, 20000.00, 8,    '2022-09-26 09:00:00.000'),
       (9,  'Court hearing',                            0, 1, 0, NULL,     NULL, '2022-09-29 09:00:00.000'),
       (10, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2022-10-12 09:00:00.000'),
       (11, 'Prepare the case file',                    1, 0, 0, NULL,     NULL, '2022-11-30 09:00:00.000'),
       (11, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2022-12-01 09:00:00.000'),
       (12, 'Prepare the case file',                    1, 0, 0, NULL,     NULL, '2022-12-02 09:00:00.000'),
       (12, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2022-12-03 09:00:00.000'),
       (8,  'Legal representation fee',                 0, 0, 1, 11000.00, 10,   '2022-12-19 09:00:00.000'),
       (8,  'Drafting and filing fee',                  0, 0, 1, 24000.00, 10,   '2022-12-20 09:00:00.000'),
       (6,  'Legal representation fee',                 0, 0, 1, 29000.00, 11,   '2023-02-02 09:00:00.000'),
       (6,  'Drafting and filing fee',                  0, 0, 1, 22000.00, 11,   '2023-02-03 09:00:00.000'),
       (10, 'Legal representation fee',                 0, 0, 1, 14000.00, 12,   '2023-03-14 09:00:00.000'),
       (10, 'Drafting and filing fee',                  0, 0, 1, 11000.00, 12,   '2023-03-15 09:00:00.000'),
       (13, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2023-04-06 09:00:00.000'),
       (14, 'Prepare the case file',                    1, 0, 0, NULL,     NULL, '2023-04-21 09:00:00.000'),
       (14, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2023-04-22 09:00:00.000'),
       (15, 'Prepare the case file',                    1, 0, 0, NULL,     NULL, '2023-06-07 09:00:00.000'),
       (15, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2023-06-08 09:00:00.000'),
       (12, 'Legal representation fee',                 0, 0, 1, 25000.00, 13,   '2023-06-11 09:00:00.000'),
       (12, 'Drafting and filing fee',                  0, 0, 1, 30000.00, 13,   '2023-06-12 09:00:00.000'),
       (16, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2023-07-10 09:00:00.000'),
       (9,  'Legal representation fee',                 0, 0, 1, 40000.00, 14,   '2023-07-19 09:00:00.000'),
       (9,  'Drafting and filing fee',                  0, 0, 1, 29000.00, 14,   '2023-07-20 09:00:00.000'),
       (17, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2023-07-27 09:00:00.000'),
       (18, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2023-08-10 09:00:00.000'),
       (19, 'Prepare the case file',                    1, 0, 0, NULL,     NULL, '2023-09-16 09:00:00.000'),
       (19, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2023-09-17 09:00:00.000'),
       (17, 'Legal representation fee',                 0, 0, 1, 40000.00, 15,   '2023-10-10 09:00:00.000'),
       (17, 'Drafting and filing fee',                  0, 0, 1, 13000.00, 15,   '2023-10-11 09:00:00.000'),
       (20, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2023-10-12 09:00:00.000'),
       (21, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2023-10-31 09:00:00.000'),
       (22, 'Prepare the case file',                    1, 0, 0, NULL,     NULL, '2023-12-13 09:00:00.000'),
       (22, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2023-12-14 09:00:00.000'),
       (19, 'Legal representation fee',                 0, 0, 1, 26000.00, 16,   '2024-01-07 09:00:00.000'),
       (19, 'Drafting and filing fee',                  0, 0, 1, 6000.00,  16,   '2024-01-08 09:00:00.000'),
       (23, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2024-01-15 09:00:00.000'),
       (24, 'Prepare the case file',                    1, 0, 0, NULL,     NULL, '2024-02-03 09:00:00.000'),
       (24, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2024-02-04 09:00:00.000'),
       (25, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2024-02-15 09:00:00.000'),
       (14, 'Legal representation fee',                 0, 0, 1, 31000.00, 18,   '2024-02-17 09:00:00.000'),
       (14, 'Drafting and filing fee',                  0, 0, 1, 5000.00,  18,   '2024-02-18 09:00:00.000'),
       (26, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2024-03-23 09:00:00.000'),
       (21, 'Legal representation fee',                 0, 0, 1, 17000.00, 19,   '2024-03-28 09:00:00.000'),
       (21, 'Drafting and filing fee',                  0, 0, 1, 8000.00,  19,   '2024-03-29 09:00:00.000'),
       (27, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2024-05-10 09:00:00.000'),
       (28, 'Prepare the case file',                    1, 0, 0, NULL,     NULL, '2024-06-30 09:00:00.000'),
       (28, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2024-07-01 09:00:00.000'),
       (29, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2024-07-22 09:00:00.000'),
       (30, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2024-08-14 09:00:00.000'),
       (31, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2024-08-22 09:00:00.000'),
       (24, 'Legal representation fee',                 0, 0, 1, 11000.00, 20,   '2024-09-14 09:00:00.000'),
       (24, 'Drafting and filing fee',                  0, 0, 1, 28000.00, 20,   '2024-09-15 09:00:00.000'),
       (30, 'Legal representation fee',                 0, 0, 1, 48000.00, 22,   '2024-10-24 09:00:00.000'),
       (32, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2024-10-24 09:00:00.000'),
       (30, 'Drafting and filing fee',                  0, 0, 1, 21000.00, 22,   '2024-10-25 09:00:00.000'),
       (33, 'Prepare the case file',                    1, 0, 0, NULL,     NULL, '2024-11-17 09:00:00.000'),
       (33, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2024-11-18 09:00:00.000'),
       (26, 'Legal representation fee',                 0, 0, 1, 57000.00, 23,   '2024-12-08 09:00:00.000'),
       (26, 'Drafting and filing fee',                  0, 0, 1, 9000.00,  23,   '2024-12-09 09:00:00.000'),
       (34, 'Prepare the case file',                    1, 0, 0, NULL,     NULL, '2024-12-25 09:00:00.000'),
       (34, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2024-12-26 09:00:00.000'),
       (35, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2025-01-05 09:00:00.000'),
       (36, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2025-01-27 09:00:00.000'),
       (37, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2025-02-22 09:00:00.000'),
       (38, 'Court hearing',                            0, 1, 0, NULL,     NULL, '2025-02-27 09:00:00.000'),
       (27, 'Legal representation fee',                 0, 0, 1, 57000.00, 24,   '2025-03-03 09:00:00.000'),
       (27, 'Drafting and filing fee',                  0, 0, 1, 25000.00, 24,   '2025-03-04 09:00:00.000'),
       (33, 'Legal representation fee',                 0, 0, 1, 28000.00, 25,   '2025-04-12 09:00:00.000'),
       (33, 'Drafting and filing fee',                  0, 0, 1, 30000.00, 25,   '2025-04-13 09:00:00.000'),
       (39, 'Client strategy meeting',                  0, 1, 0, NULL,     NULL, '2025-05-01 09:00:00.000'),
       (43, 'Vendor contract review',                   0, 1, 0, NULL,     NULL, '2025-05-02 09:00:00.000'),
       (39, 'Draft settlement agreement',               1, 0, 1, 15000.00, 26,   '2025-05-10 09:00:00.000'),
       (41, 'Tribunal preparation meeting',             0, 1, 0, NULL,     NULL, '2025-05-14 09:00:00.000'),
       (39, 'Document review meeting',                  0, 1, 0, NULL,     NULL, '2025-05-15 09:00:00.000'),
       (39, 'File response to opposing counsel',        1, 0, 0, NULL,     NULL, '2025-05-18 09:00:00.000'),
       (41, 'Employment tribunal filing',               1, 0, 1, 12000.00, 28,   '2025-05-19 09:00:00.000'),
       (40, 'Prepare merger documents',                 1, 0, 1, 25000.00, 27,   '2025-05-20 09:00:00.000'),
       (40, 'Merger negotiation session',               0, 1, 0, NULL,     NULL, '2025-05-20 09:00:00.000'),
       (40, 'Contract review consultation fee',         0, 0, 1, 8000.00,  27,   '2025-05-22 09:00:00.000'),
       (42, 'Final settlement documentation',           1, 0, 1, 30000.00, 29,   '2025-05-25 09:00:00.000'),
       (43, 'IP registration filing',                   1, 0, 0, NULL,     NULL, '2025-05-28 09:00:00.000'),
       (44, 'Immigration form review',                  0, 0, 1, 9500.00,  NULL, '2025-05-29 09:00:00.000'),
       (45, 'General case notes',                       1, 0, 0, NULL,     NULL, '2025-05-30 09:00:00.000'),
       (28, 'Legal representation fee',                 0, 0, 1, 19000.00, 31,   '2025-05-31 09:00:00.000'),
       (28, 'Drafting and filing fee',                  0, 0, 1, 30000.00, 31,   '2025-06-01 09:00:00.000'),
       (39, 'Update client contact information',        0, 0, 0, NULL,     NULL, '2025-06-01 09:00:00.000'),
       (39, 'Organize case file',                       0, 0, 0, NULL,     NULL, '2025-06-02 09:00:00.000'),
       (39, 'Internal case status review',              0, 0, 0, NULL,     NULL, '2025-06-03 09:00:00.000'),
       (39, 'Upload medical records',                   1, 0, 0, NULL,     NULL, '2025-06-04 09:00:00.000'),
       (39, 'Scan signed affidavit',                    1, 0, 0, NULL,     NULL, '2025-06-05 09:00:00.000'),
       (39, 'File court submission copy',               1, 0, 0, NULL,     NULL, '2025-06-06 09:00:00.000'),
       (39, 'Follow-up call with client',               0, 1, 0, NULL,     NULL, '2025-06-06 09:00:00.000'),
       (39, 'Archive correspondence',                   1, 0, 0, NULL,     NULL, '2025-06-07 09:00:00.000'),
       (39, 'Site visit for property inspection',       0, 1, 0, NULL,     NULL, '2025-06-10 09:00:00.000'),
       (39, 'Phone consultation fee',                   0, 0, 1, 3000.00,  NULL, '2025-06-11 09:00:00.000'),
       (39, 'Document notarization fee',                0, 0, 1, 4500.00,  NULL, '2025-06-12 09:00:00.000'),
       (39, 'Legal research fee',                       0, 0, 1, 6000.00,  NULL, '2025-06-13 09:00:00.000'),
       (39, 'Case filing fee',                          0, 0, 1, 4000.00,  NULL, '2025-06-14 09:00:00.000'),
       (39, 'Discovery meeting with document exchange', 1, 1, 0, NULL,     NULL, '2025-06-15 09:00:00.000'),
       (39, 'Settlement negotiation session',           1, 1, 0, NULL,     NULL, '2025-06-16 09:00:00.000'),
       (39, 'Contract drafting fee',                    1, 0, 1, 8000.00,  32,   '2025-06-17 09:00:00.000'),
       (39, 'Deposition preparation fee',               1, 0, 1, 7000.00,  32,   '2025-06-18 09:00:00.000'),
       (39, 'Correspondence review fee',                1, 0, 1, 2000.00,  NULL, '2025-06-19 09:00:00.000'),
       (39, 'Mediation session fee',                    0, 1, 1, 5000.00,  32,   '2025-06-20 09:00:00.000'),
       (39, 'Arbitration hearing prep fee',             0, 1, 1, 6000.00,  NULL, '2025-06-21 09:00:00.000'),
       (39, 'Court hearing representation',             1, 1, 1, 7000.00,  NULL, '2025-06-25 09:00:00.000'),
       (39, 'Settlement conference representation',     1, 1, 1, 5000.00,  NULL, '2025-06-26 09:00:00.000'),
       (39, 'Trial preparation session',                1, 1, 1, 3500.00,  NULL, '2025-06-27 09:00:00.000'),
       (35, 'Legal representation fee',                 0, 0, 1, 34000.00, 35,   '2025-07-06 09:00:00.000'),
       (35, 'Drafting and filing fee',                  0, 0, 1, 17000.00, 35,   '2025-07-07 09:00:00.000'),
       (46, 'Prepare closing documents',                1, 0, 1, 10000.00, 36,   '2025-07-15 09:00:00.000'),
       (46, 'Final case review fee',                    0, 0, 1, 8000.00,  36,   '2025-07-16 09:00:00.000'),
       (46, 'Settlement documentation fee',             1, 0, 1, 6000.00,  36,   '2025-07-17 09:00:00.000'),
       (46, 'Court appearance fee',                     0, 0, 1, 12000.00, 36,   '2025-07-18 09:00:00.000'),
       (46, 'Case closure administrative fee',          0, 0, 1, 9000.00,  36,   '2025-07-19 09:00:00.000'),
       (47, 'Draft tenancy dispute notice',             1, 0, 0, NULL,     NULL, '2025-08-16 09:00:00.000'),
       (43, 'Vendor dispute negotiation fee',           0, 0, 1, 14000.00, 37,   '2025-08-18 09:00:00.000'),
       (37, 'Legal representation fee',                 0, 0, 1, 31000.00, 38,   '2025-09-29 09:00:00.000'),
       (37, 'Drafting and filing fee',                  0, 0, 1, 21000.00, 38,   '2025-09-30 09:00:00.000'),
       (11, 'Legal representation fee',                 0, 0, 1, 59000.00, 39,   '2025-11-25 16:00:00.000'),
       (11, 'Drafting and filing fee',                  0, 0, 1, 5000.00,  39,   '2025-11-26 16:00:00.000'),
       (29, 'Legal representation fee',                 0, 0, 1, 26000.00, 40,   '2025-12-13 16:00:00.000'),
       (29, 'Drafting and filing fee',                  0, 0, 1, 19000.00, 40,   '2025-12-14 16:00:00.000'),
       (13, 'Legal representation fee',                 0, 0, 1, 26000.00, 41,   '2025-12-28 16:00:00.000'),
       (13, 'Drafting and filing fee',                  0, 0, 1, 10000.00, 41,   '2025-12-29 16:00:00.000'),
       (31, 'Legal representation fee',                 0, 0, 1, 33000.00, 42,   '2026-01-15 16:00:00.000'),
       (31, 'Drafting and filing fee',                  0, 0, 1, 24000.00, 42,   '2026-01-16 16:00:00.000'),
       (15, 'Legal representation fee',                 0, 0, 1, 34000.00, 44,   '2026-01-27 16:00:00.000'),
       (15, 'Drafting and filing fee',                  0, 0, 1, 24000.00, 44,   '2026-01-28 16:00:00.000'),
       (32, 'Legal representation fee',                 0, 0, 1, 44000.00, 46,   '2026-02-14 16:00:00.000'),
       (32, 'Drafting and filing fee',                  0, 0, 1, 10000.00, 46,   '2026-02-15 16:00:00.000'),
       (16, 'Legal representation fee',                 0, 0, 1, 8000.00,  47,   '2026-03-01 16:00:00.000'),
       (16, 'Drafting and filing fee',                  0, 0, 1, 6000.00,  47,   '2026-03-02 16:00:00.000'),
       (18, 'Legal representation fee',                 0, 0, 1, 55000.00, 48,   '2026-04-02 16:00:00.000'),
       (18, 'Drafting and filing fee',                  0, 0, 1, 10000.00, 48,   '2026-04-03 16:00:00.000'),
       (20, 'Legal representation fee',                 0, 0, 1, 60000.00, 49,   '2026-05-05 16:00:00.000'),
       (20, 'Drafting and filing fee',                  0, 0, 1, 22000.00, 49,   '2026-05-06 16:00:00.000'),
       (22, 'Legal representation fee',                 0, 0, 1, 25000.00, 51,   '2026-06-06 16:00:00.000'),
       (22, 'Drafting and filing fee',                  0, 0, 1, 12000.00, 51,   '2026-06-07 16:00:00.000'),
       (23, 'Legal representation fee',                 0, 0, 1, 12000.00, 52,   '2026-07-09 16:00:00.000'),
       (23, 'Drafting and filing fee',                  0, 0, 1, 8000.00,  52,   '2026-07-10 16:00:00.000'),
       (25, 'Legal representation fee',                 0, 0, 1, 33000.00, 53,   '2026-08-11 16:00:00.000'),
       (25, 'Drafting and filing fee',                  0, 0, 1, 9000.00,  53,   '2026-08-12 16:00:00.000'),
       (39, 'Appeal preparation fee',                   0, 0, 1, 60000.00, NULL, '2026-10-05 09:00:00.000');

-- Events Data
INSERT INTO events (event_type, subject, date, start_time, end_time, location, notes, event_status, scheduled_at, completed_at, canceled_at, consultation_id, case_id, task_id)
VALUES ('CONSULTATION_EVENT', 'Initial consultation meeting',             '2021-04-23', '10:00:00', '10:30:00', 'LexFlow Office, Colombo',    NULL,                                       'CANCELED',  '2021-04-19 10:00:00.000', NULL,                      '2021-04-20 10:00:00.000', 1,    NULL, NULL),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2021-04-27', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2021-04-20 10:00:00.000', '2021-04-27 14:45:00.000', NULL,                      1,    NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2021-06-17', '09:30:00', '11:30:00', 'District Court, Kandy',      NULL,                                       'COMPLETED', '2021-05-06 09:00:00.000', '2021-06-17 11:30:00.000', NULL,                      NULL, 1,    2   ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2021-07-28', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2021-07-25 10:00:00.000', '2021-07-28 14:45:00.000', NULL,                      3,    NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2021-10-24', '09:30:00', '11:30:00', 'District Court, Kurunegala', NULL,                                       'COMPLETED', '2021-08-06 09:00:00.000', '2021-10-24 11:30:00.000', NULL,                      NULL, 2,    5   ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2021-09-14', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2021-09-11 10:00:00.000', '2021-09-14 14:45:00.000', NULL,                      4,    NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2021-12-29', '09:30:00', '11:30:00', 'District Court, Galle',      NULL,                                       'COMPLETED', '2021-09-23 09:00:00.000', '2021-12-29 11:30:00.000', NULL,                      NULL, 3,    7   ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2021-11-01', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2021-10-29 10:00:00.000', '2021-11-01 14:45:00.000', NULL,                      5,    NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2022-03-06', '09:30:00', '11:30:00', 'District Court, Matara',     NULL,                                       'COMPLETED', '2021-11-10 09:00:00.000', '2022-03-06 11:30:00.000', NULL,                      NULL, 4,    8   ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2021-12-19', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2021-12-16 10:00:00.000', '2021-12-19 14:45:00.000', NULL,                      6,    NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2022-05-11', '09:30:00', '11:30:00', 'District Court, Negombo',    NULL,                                       'COMPLETED', '2021-12-28 09:00:00.000', '2022-05-11 11:30:00.000', NULL,                      NULL, 5,    11  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2022-03-25', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2022-03-22 10:00:00.000', '2022-03-25 14:45:00.000', NULL,                      8,    NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2022-09-21', '09:30:00', '11:30:00', 'District Court, Ratnapura',  NULL,                                       'COMPLETED', '2022-04-03 09:00:00.000', '2022-09-21 11:30:00.000', NULL,                      NULL, 6,    14  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2022-05-12', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2022-05-09 10:00:00.000', '2022-05-12 14:45:00.000', NULL,                      9,    NULL, NULL),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2022-06-29', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2022-06-26 10:00:00.000', '2022-06-29 14:45:00.000', NULL,                      10,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2022-09-04', '09:30:00', '11:30:00', 'District Court, Kegalle',    NULL,                                       'COMPLETED', '2022-07-08 09:00:00.000', '2022-09-04 11:30:00.000', NULL,                      NULL, 7,    18  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2022-08-16', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2022-08-13 10:00:00.000', '2022-08-16 14:45:00.000', NULL,                      11,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Initial consultation meeting',             '2022-08-20', '10:00:00', '10:30:00', 'LexFlow Office, Colombo',    NULL,                                       'CANCELED',  '2022-08-16 10:00:00.000', NULL,                      '2022-08-17 10:00:00.000', 12,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2022-08-24', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2022-08-17 10:00:00.000', '2022-08-24 14:45:00.000', NULL,                      12,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2022-11-10', '09:30:00', '11:30:00', 'District Court, Kandy',      NULL,                                       'COMPLETED', '2022-08-25 09:00:00.000', '2022-11-10 11:30:00.000', NULL,                      NULL, 8,    21  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2022-09-20', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2022-09-17 10:00:00.000', '2022-09-20 14:45:00.000', NULL,                      13,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2023-03-13', '09:30:00', '11:30:00', 'District Court, Gampaha',    NULL,                                       'COMPLETED', '2022-09-29 09:00:00.000', '2023-03-13 11:30:00.000', NULL,                      NULL, 9,    24  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2022-10-03', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2022-09-30 10:00:00.000', '2022-10-03 14:45:00.000', NULL,                      14,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2023-01-15', '09:30:00', '11:30:00', 'District Court, Gampaha',    NULL,                                       'COMPLETED', '2022-10-12 09:00:00.000', '2023-01-15 11:30:00.000', NULL,                      NULL, 10,   25  ),
       ('CONSULTATION_EVENT', 'Initial consultation meeting',             '2022-11-20', '10:00:00', '10:30:00', 'LexFlow Office, Colombo',    NULL,                                       'CANCELED',  '2022-11-16 10:00:00.000', NULL,                      '2022-11-17 10:00:00.000', 16,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2022-11-24', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2022-11-17 10:00:00.000', '2022-11-24 14:45:00.000', NULL,                      16,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2022-11-22', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2022-11-19 10:00:00.000', '2022-11-22 14:45:00.000', NULL,                      17,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2024-06-17', '09:30:00', '11:30:00', 'District Court, Galle',      NULL,                                       'COMPLETED', '2022-12-01 09:00:00.000', '2024-06-17 11:30:00.000', NULL,                      NULL, 11,   27  ),
       ('CASE_EVENT',         'Court hearing',                            '2023-03-27', '09:30:00', '11:30:00', 'District Court, Kurunegala', NULL,                                       'COMPLETED', '2022-12-03 09:00:00.000', '2023-03-27 11:30:00.000', NULL,                      NULL, 12,   29  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2023-01-07', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2023-01-04 10:00:00.000', '2023-01-07 14:45:00.000', NULL,                      19,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2023-01-24', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2023-01-21 10:00:00.000', '2023-01-24 14:45:00.000', NULL,                      20,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2023-02-24', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2023-02-21 10:00:00.000', '2023-02-24 14:45:00.000', NULL,                      21,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2023-03-28', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2023-03-25 10:00:00.000', '2023-03-28 14:45:00.000', NULL,                      23,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2024-09-05', '09:30:00', '11:30:00', 'District Court, Ratnapura',  NULL,                                       'COMPLETED', '2023-04-06 09:00:00.000', '2024-09-05 11:30:00.000', NULL,                      NULL, 13,   36  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2023-04-13', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2023-04-10 10:00:00.000', '2023-04-13 14:45:00.000', NULL,                      24,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2023-10-08', '09:30:00', '11:30:00', 'District Court, Negombo',    NULL,                                       'COMPLETED', '2023-04-22 09:00:00.000', '2023-10-08 11:30:00.000', NULL,                      NULL, 14,   38  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2023-05-30', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2023-05-27 10:00:00.000', '2023-05-30 14:45:00.000', NULL,                      26,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2023-05-31', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2023-05-28 10:00:00.000', '2023-05-31 14:45:00.000', NULL,                      27,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2024-10-21', '09:30:00', '11:30:00', 'District Court, Kegalle',    NULL,                                       'COMPLETED', '2023-06-08 09:00:00.000', '2024-10-21 11:30:00.000', NULL,                      NULL, 15,   40  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2023-07-01', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2023-06-28 10:00:00.000', '2023-07-01 14:45:00.000', NULL,                      28,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2024-11-23', '09:30:00', '11:30:00', 'District Court, Kandy',      NULL,                                       'COMPLETED', '2023-07-10 09:00:00.000', '2024-11-23 11:30:00.000', NULL,                      NULL, 16,   43  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2023-07-18', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2023-07-15 10:00:00.000', '2023-07-18 14:45:00.000', NULL,                      29,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2023-09-21', '09:30:00', '11:30:00', 'District Court, Ratnapura',  NULL,                                       'COMPLETED', '2023-07-27 09:00:00.000', '2023-09-21 11:30:00.000', NULL,                      NULL, 17,   46  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2023-08-01', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2023-07-29 10:00:00.000', '2023-08-01 14:45:00.000', NULL,                      30,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2024-12-24', '09:30:00', '11:30:00', 'District Court, Gampaha',    NULL,                                       'COMPLETED', '2023-08-10 09:00:00.000', '2024-12-24 11:30:00.000', NULL,                      NULL, 18,   47  ),
       ('CONSULTATION_EVENT', 'Initial consultation meeting',             '2023-09-04', '10:00:00', '10:30:00', 'LexFlow Office, Colombo',    NULL,                                       'CANCELED',  '2023-08-31 10:00:00.000', NULL,                      '2023-09-01 10:00:00.000', 32,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2023-09-08', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2023-09-01 10:00:00.000', '2023-09-08 14:45:00.000', NULL,                      32,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2023-12-01', '09:30:00', '11:30:00', 'District Court, Colombo',    NULL,                                       'COMPLETED', '2023-09-17 09:00:00.000', '2023-12-01 11:30:00.000', NULL,                      NULL, 19,   49  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2023-10-03', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2023-09-30 10:00:00.000', '2023-10-03 14:45:00.000', NULL,                      33,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2025-02-10', '09:30:00', '11:30:00', 'District Court, Galle',      NULL,                                       'COMPLETED', '2023-10-12 09:00:00.000', '2025-02-10 11:30:00.000', NULL,                      NULL, 20,   52  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2023-10-22', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2023-10-19 10:00:00.000', '2023-10-22 14:45:00.000', NULL,                      34,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2024-02-01', '09:30:00', '11:30:00', 'District Court, Kegalle',    NULL,                                       'COMPLETED', '2023-10-31 09:00:00.000', '2024-02-01 11:30:00.000', NULL,                      NULL, 21,   53  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2023-12-05', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2023-12-02 10:00:00.000', '2023-12-05 14:45:00.000', NULL,                      36,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2025-03-30', '09:30:00', '11:30:00', 'District Court, Negombo',    NULL,                                       'COMPLETED', '2023-12-14 09:00:00.000', '2025-03-30 11:30:00.000', NULL,                      NULL, 22,   55  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2024-01-06', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2024-01-03 10:00:00.000', '2024-01-06 14:45:00.000', NULL,                      38,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2025-05-01', '09:30:00', '11:30:00', 'District Court, Kalutara',   NULL,                                       'COMPLETED', '2024-01-15 09:00:00.000', '2025-05-01 11:30:00.000', NULL,                      NULL, 23,   58  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2024-01-26', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2024-01-23 10:00:00.000', '2024-01-26 14:45:00.000', NULL,                      39,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2024-02-06', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2024-02-03 10:00:00.000', '2024-02-06 14:45:00.000', NULL,                      40,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2024-06-13', '09:30:00', '11:30:00', 'District Court, Gampaha',    NULL,                                       'COMPLETED', '2024-02-04 09:00:00.000', '2024-06-13 11:30:00.000', NULL,                      NULL, 24,   60  ),
       ('CASE_EVENT',         'Court hearing',                            '2025-06-02', '09:30:00', '11:30:00', 'District Court, Ratnapura',  NULL,                                       'COMPLETED', '2024-02-15 09:00:00.000', '2025-06-02 11:30:00.000', NULL,                      NULL, 25,   61  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2024-03-14', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2024-03-11 10:00:00.000', '2024-03-14 14:45:00.000', NULL,                      42,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2024-08-19', '09:30:00', '11:30:00', 'District Court, Kurunegala', NULL,                                       'COMPLETED', '2024-03-23 09:00:00.000', '2024-08-19 11:30:00.000', NULL,                      NULL, 26,   64  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2024-05-01', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2024-04-28 10:00:00.000', '2024-05-01 14:45:00.000', NULL,                      44,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2024-05-11', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2024-05-08 10:00:00.000', '2024-05-11 14:45:00.000', NULL,                      45,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2024-10-24', '09:30:00', '11:30:00', 'District Court, Galle',      NULL,                                       'COMPLETED', '2024-05-10 09:00:00.000', '2024-10-24 11:30:00.000', NULL,                      NULL, 27,   67  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2024-06-11', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2024-06-08 10:00:00.000', '2024-06-11 14:45:00.000', NULL,                      46,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Initial consultation meeting',             '2024-06-18', '10:00:00', '10:30:00', 'LexFlow Office, Colombo',    NULL,                                       'CANCELED',  '2024-06-14 10:00:00.000', NULL,                      '2024-06-15 10:00:00.000', 47,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2024-06-22', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2024-06-15 10:00:00.000', '2024-06-22 14:45:00.000', NULL,                      47,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2025-01-03', '09:30:00', '11:30:00', 'District Court, Matara',     NULL,                                       'COMPLETED', '2024-07-01 09:00:00.000', '2025-01-03 11:30:00.000', NULL,                      NULL, 28,   69  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2024-07-13', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2024-07-10 10:00:00.000', '2024-07-13 14:45:00.000', NULL,                      48,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2025-04-22', '09:30:00', '11:30:00', 'District Court, Kurunegala', NULL,                                       'COMPLETED', '2024-07-22 09:00:00.000', '2025-04-22 11:30:00.000', NULL,                      NULL, 29,   70  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2024-08-05', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2024-08-02 10:00:00.000', '2024-08-05 14:45:00.000', NULL,                      49,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2024-08-13', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2024-08-10 10:00:00.000', '2024-08-13 14:45:00.000', NULL,                      50,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2024-10-07', '09:30:00', '11:30:00', 'District Court, Negombo',    NULL,                                       'COMPLETED', '2024-08-14 09:00:00.000', '2024-10-07 11:30:00.000', NULL,                      NULL, 30,   71  ),
       ('CASE_EVENT',         'Court hearing',                            '2025-05-24', '09:30:00', '11:30:00', 'District Court, Galle',      NULL,                                       'COMPLETED', '2024-08-22 09:00:00.000', '2025-05-24 11:30:00.000', NULL,                      NULL, 31,   72  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2024-10-15', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2024-10-12 10:00:00.000', '2024-10-15 14:45:00.000', NULL,                      53,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2025-07-09', '09:30:00', '11:30:00', 'District Court, Negombo',    NULL,                                       'COMPLETED', '2024-10-24 09:00:00.000', '2025-07-09 11:30:00.000', NULL,                      NULL, 32,   76  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2024-11-09', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2024-11-06 10:00:00.000', '2024-11-09 14:45:00.000', NULL,                      54,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2025-02-17', '09:30:00', '11:30:00', 'District Court, Ratnapura',  NULL,                                       'COMPLETED', '2024-11-18 09:00:00.000', '2025-02-17 11:30:00.000', NULL,                      NULL, 33,   79  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2024-12-17', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2024-12-14 10:00:00.000', '2024-12-17 14:45:00.000', NULL,                      56,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2024-12-27', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2024-12-24 10:00:00.000', '2024-12-27 14:45:00.000', NULL,                      57,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2026-10-08', '08:00:00', '08:45:00', 'District Court, Ratnapura',  NULL,                                       'SCHEDULED', '2024-12-26 09:00:00.000', NULL,                      NULL,                      NULL, 34,   83  ),
       ('CASE_EVENT',         'Court hearing',                            '2025-04-25', '09:30:00', '11:30:00', 'District Court, Colombo',    NULL,                                       'COMPLETED', '2025-01-05 09:00:00.000', '2025-04-25 11:30:00.000', NULL,                      NULL, 35,   84  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2025-01-18', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2025-01-15 10:00:00.000', '2025-01-18 14:45:00.000', NULL,                      58,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2026-10-10', '09:00:00', '09:45:00', 'District Court, Colombo',    NULL,                                       'SCHEDULED', '2025-01-27 09:00:00.000', NULL,                      NULL,                      NULL, 36,   85  ),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2025-02-13', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2025-02-10 10:00:00.000', '2025-02-13 14:45:00.000', NULL,                      59,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Consultation meeting',                     '2025-02-18', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2025-02-15 10:00:00.000', '2025-02-18 14:45:00.000', NULL,                      60,   NULL, NULL),
       ('CASE_EVENT',         'Court hearing',                            '2025-06-30', '09:30:00', '11:30:00', 'District Court, Kegalle',    NULL,                                       'COMPLETED', '2025-02-22 09:00:00.000', '2025-06-30 11:30:00.000', NULL,                      NULL, 37,   86  ),
       ('CASE_EVENT',         'Court hearing',                            '2026-10-12', '10:00:00', '10:45:00', 'District Court, Kegalle',    NULL,                                       'SCHEDULED', '2025-02-27 09:00:00.000', NULL,                      NULL,                      NULL, 38,   87  ),
       ('CONSULTATION_EVENT', 'Divorce and custody consultation',         '2025-04-16', '10:00:00', '10:30:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2025-04-15 11:00:00.000', '2025-04-16 10:30:00.000', NULL,                      61,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Merger advisory consultation',             '2025-04-19', '14:00:00', '14:45:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2025-04-18 11:00:00.000', '2025-04-19 14:45:00.000', NULL,                      64,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Wrongful termination consultation',        '2025-04-20', '09:30:00', '10:00:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2025-04-19 11:00:00.000', '2025-04-20 10:00:00.000', NULL,                      65,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Property title dispute consultation',      '2025-04-21', '11:00:00', '11:30:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2025-04-20 11:00:00.000', '2025-04-21 11:30:00.000', NULL,                      66,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Supply contract breach consultation',      '2026-12-05', '10:00:00', '10:30:00', 'LexFlow Office, Colombo',    NULL,                                       'SCHEDULED', '2025-04-22 11:00:00.000', NULL,                      NULL,                      68,   NULL, NULL),
       ('CASE_EVENT',         'Client strategy meeting',                  '2026-12-08', '09:00:00', '10:00:00', 'LexFlow Office, Colombo',    NULL,                                       'SCHEDULED', '2025-05-01 09:00:00.000', NULL,                      NULL,                      NULL, 39,   92  ),
       ('CASE_EVENT',         'Vendor contract review',                   '2026-12-10', '11:00:00', '12:00:00', 'LexFlow Office, Colombo',    NULL,                                       'SCHEDULED', '2025-05-02 09:00:00.000', NULL,                      NULL,                      NULL, 43,   93  ),
       ('CASE_EVENT',         'Tribunal preparation meeting',             '2025-05-18', '15:00:00', '16:00:00', 'LexFlow Office, Colombo',    'Client unavailable, rescheduling needed.', 'CANCELED',  '2025-05-14 09:00:00.000', NULL,                      '2025-05-16 09:00:00.000', NULL, 41,   95  ),
       ('CASE_EVENT',         'Document review meeting',                  '2025-05-20', '13:00:00', '14:00:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2025-05-15 09:00:00.000', '2025-05-20 14:00:00.000', NULL,                      NULL, 39,   96  ),
       ('CASE_EVENT',         'Merger negotiation session',               '2025-05-25', '10:00:00', '12:00:00', 'Client Site, Colombo',       NULL,                                       'COMPLETED', '2025-05-20 09:00:00.000', '2025-05-25 12:00:00.000', NULL,                      NULL, 40,   100 ),
       ('CASE_EVENT',         'Follow-up call with client',               '2026-12-12', '09:00:00', '09:30:00', 'LexFlow Office, Colombo',    NULL,                                       'SCHEDULED', '2025-06-06 09:00:00.000', NULL,                      NULL,                      NULL, 39,   114 ),
       ('CASE_EVENT',         'Site visit for property inspection',       '2025-06-20', '10:00:00', '11:30:00', 'Client Property, Colombo',   NULL,                                       'COMPLETED', '2025-06-10 09:00:00.000', '2025-06-20 11:30:00.000', NULL,                      NULL, 39,   116 ),
       ('CASE_EVENT',         'Discovery meeting with document exchange', '2025-06-25', '13:00:00', '15:00:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2025-06-15 09:00:00.000', '2025-06-25 15:00:00.000', NULL,                      NULL, 39,   121 ),
       ('CASE_EVENT',         'Settlement negotiation session',           '2026-12-15', '10:00:00', '12:00:00', 'LexFlow Office, Colombo',    NULL,                                       'SCHEDULED', '2025-06-16 09:00:00.000', NULL,                      NULL,                      NULL, 39,   122 ),
       ('CASE_EVENT',         'Mediation session fee',                    '2025-07-01', '09:00:00', '10:30:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2025-06-20 09:00:00.000', '2025-07-01 10:30:00.000', NULL,                      NULL, 39,   126 ),
       ('CASE_EVENT',         'Arbitration hearing prep fee',             '2026-12-18', '14:00:00', '15:30:00', 'LexFlow Office, Colombo',    NULL,                                       'SCHEDULED', '2025-06-21 09:00:00.000', NULL,                      NULL,                      NULL, 39,   127 ),
       ('CASE_EVENT',         'Court hearing representation',             '2025-07-10', '09:00:00', '12:00:00', 'District Court, Colombo',    NULL,                                       'COMPLETED', '2025-06-25 09:00:00.000', '2025-07-10 12:00:00.000', NULL,                      NULL, 39,   128 ),
       ('CASE_EVENT',         'Settlement conference representation',     '2025-07-15', '13:00:00', '15:00:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2025-06-26 09:00:00.000', '2025-07-15 15:00:00.000', NULL,                      NULL, 39,   129 ),
       ('CASE_EVENT',         'Trial preparation session',                '2026-12-20', '09:00:00', '11:00:00', 'LexFlow Office, Colombo',    NULL,                                       'SCHEDULED', '2025-06-27 09:00:00.000', NULL,                      NULL,                      NULL, 39,   130 ),
       ('CONSULTATION_EVENT', 'Boundary dispute consultation',            '2026-12-22', '10:00:00', '10:30:00', 'LexFlow Office, Colombo',    NULL,                                       'SCHEDULED', '2025-08-04 11:00:00.000', NULL,                      NULL,                      76,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Tenant eviction consultation',             '2026-09-28', '10:00:00', '10:30:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2026-09-24 11:00:00.000', '2026-09-28 10:30:00.000', NULL,                      81,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Abandoned construction consultation',      '2026-09-29', '10:00:00', '10:30:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2026-09-25 11:00:00.000', '2026-09-29 10:30:00.000', NULL,                      82,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Unpaid invoices consultation',             '2026-09-30', '10:00:00', '10:30:00', 'LexFlow Office, Colombo',    NULL,                                       'COMPLETED', '2026-09-26 11:00:00.000', '2026-09-30 10:30:00.000', NULL,                      83,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Water damage consultation',                '2026-10-05', '11:00:00', '11:30:00', 'LexFlow Office, Colombo',    NULL,                                       'SCHEDULED', '2026-09-30 11:00:00.000', NULL,                      NULL,                      84,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Shareholder dispute consultation',         '2026-10-05', '14:00:00', '14:30:00', 'LexFlow Office, Colombo',    NULL,                                       'SCHEDULED', '2026-10-01 11:00:00.000', NULL,                      NULL,                      85,   NULL, NULL),
       ('CONSULTATION_EVENT', 'Online defamation consultation',           '2026-10-05', '15:30:00', '16:00:00', 'LexFlow Office, Colombo',    NULL,                                       'SCHEDULED', '2026-10-02 11:00:00.000', NULL,                      NULL,                      86,   NULL, NULL);

-- Documents Data
INSERT INTO documents (document_type, file_path, original_filename, uploaded_at, user_id, consultation_id, case_id, in_staging, task_id, invoice_id)
VALUES ('NIC_DOCUMENT',          'uploads/1-NIC_DOCUMENT-d01957-nic-document.pdf',                      'nic-document.pdf',            '2025-01-10 09:05:00.000', 41,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/1-BAR_DOCUMENT-191f06-bar-document.pdf',                      'bar-document.pdf',            '2025-01-10 09:06:00.000', 41,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/2-NIC_DOCUMENT-3739ee-nic-document.pdf',                      'nic-document.pdf',            '2025-01-11 09:05:00.000', 42,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/2-BAR_DOCUMENT-e30866-bar-document.pdf',                      'bar-document.pdf',            '2025-01-11 09:06:00.000', 42,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/3-NIC_DOCUMENT-07ec70-nic-document.pdf',                      'nic-document.pdf',            '2025-01-12 09:05:00.000', 43,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/3-BAR_DOCUMENT-35e949-bar-document.pdf',                      'bar-document.pdf',            '2025-01-12 09:06:00.000', 43,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/4-NIC_DOCUMENT-45fb33-nic-document.pdf',                      'nic-document.pdf',            '2025-01-13 09:05:00.000', 44,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/4-BAR_DOCUMENT-e1e6dc-bar-document.pdf',                      'bar-document.pdf',            '2025-01-13 09:06:00.000', 44,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/5-NIC_DOCUMENT-147320-nic-document.pdf',                      'nic-document.pdf',            '2025-01-14 09:05:00.000', 45,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/5-BAR_DOCUMENT-c2f490-bar-document.pdf',                      'bar-document.pdf',            '2025-01-14 09:06:00.000', 45,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/6-NIC_DOCUMENT-5a14a9-nic-document.pdf',                      'nic-document.pdf',            '2025-01-15 09:05:00.000', 46,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/6-BAR_DOCUMENT-d44423-bar-document.pdf',                      'bar-document.pdf',            '2025-01-15 09:06:00.000', 46,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/7-NIC_DOCUMENT-db8281-nic-document.pdf',                      'nic-document.pdf',            '2025-01-16 09:05:00.000', 47,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/7-BAR_DOCUMENT-031859-bar-document.pdf',                      'bar-document.pdf',            '2025-01-16 09:06:00.000', 47,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/8-NIC_DOCUMENT-e652ad-nic-document.pdf',                      'nic-document.pdf',            '2025-01-17 09:05:00.000', 48,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/8-BAR_DOCUMENT-f6d28a-bar-document.pdf',                      'bar-document.pdf',            '2025-01-17 09:06:00.000', 48,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/9-NIC_DOCUMENT-844ff2-nic-document.pdf',                      'nic-document.pdf',            '2025-01-18 09:05:00.000', 49,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/9-BAR_DOCUMENT-87ca60-bar-document.pdf',                      'bar-document.pdf',            '2025-01-18 09:06:00.000', 49,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/10-NIC_DOCUMENT-f605e2-nic-document.pdf',                     'nic-document.pdf',            '2025-01-19 09:05:00.000', 50,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/10-BAR_DOCUMENT-8c5084-bar-document.pdf',                     'bar-document.pdf',            '2025-01-19 09:06:00.000', 50,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/11-NIC_DOCUMENT-9d1c67-nic-document.pdf',                     'nic-document.pdf',            '2025-02-01 09:05:00.000', 51,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/11-BAR_DOCUMENT-3188da-bar-document.pdf',                     'bar-document.pdf',            '2025-02-01 09:06:00.000', 51,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/12-NIC_DOCUMENT-deb871-nic-document.pdf',                     'nic-document.pdf',            '2025-02-02 09:05:00.000', 52,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/12-BAR_DOCUMENT-f04b36-bar-document.pdf',                     'bar-document.pdf',            '2025-02-02 09:06:00.000', 52,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/13-NIC_DOCUMENT-431a59-nic-document.pdf',                     'nic-document.pdf',            '2025-02-03 09:05:00.000', 53,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/13-BAR_DOCUMENT-7cb4e3-bar-document.pdf',                     'bar-document.pdf',            '2025-02-03 09:06:00.000', 53,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/14-NIC_DOCUMENT-59ef3d-nic-document.pdf',                     'nic-document.pdf',            '2025-02-04 09:05:00.000', 54,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/14-BAR_DOCUMENT-cabad4-bar-document.pdf',                     'bar-document.pdf',            '2025-02-04 09:06:00.000', 54,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/15-NIC_DOCUMENT-147991-nic-document.pdf',                     'nic-document.pdf',            '2025-02-05 09:05:00.000', 55,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/15-BAR_DOCUMENT-7d59c6-bar-document.pdf',                     'bar-document.pdf',            '2025-02-05 09:06:00.000', 55,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/16-NIC_DOCUMENT-356814-nic-document.pdf',                     'nic-document.pdf',            '2025-02-06 09:05:00.000', 57,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/16-BAR_DOCUMENT-a158f1-bar-document.pdf',                     'bar-document.pdf',            '2025-02-06 09:06:00.000', 57,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/17-NIC_DOCUMENT-dbfe69-nic-document.pdf',                     'nic-document.pdf',            '2025-02-07 09:05:00.000', 58,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/17-BAR_DOCUMENT-bec899-bar-document.pdf',                     'bar-document.pdf',            '2025-02-07 09:06:00.000', 58,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/18-NIC_DOCUMENT-dada46-nic-document.pdf',                     'nic-document.pdf',            '2025-02-08 09:05:00.000', 59,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/18-BAR_DOCUMENT-97b8fa-bar-document.pdf',                     'bar-document.pdf',            '2025-02-08 09:06:00.000', 59,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/19-NIC_DOCUMENT-2274cf-nic-document.pdf',                     'nic-document.pdf',            '2025-02-09 09:05:00.000', 60,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/19-BAR_DOCUMENT-ecabea-bar-document.pdf',                     'bar-document.pdf',            '2025-02-09 09:06:00.000', 60,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/20-NIC_DOCUMENT-dea51b-nic-document.pdf',                     'nic-document.pdf',            '2025-02-10 09:05:00.000', 61,   NULL, NULL, NULL, NULL, NULL),
       ('BAR_DOCUMENT',          'uploads/20-BAR_DOCUMENT-9a64b6-bar-document.pdf',                     'bar-document.pdf',            '2025-02-10 09:06:00.000', 61,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/21-NIC_DOCUMENT-3a5499-nic-document.pdf',                     'nic-document.pdf',            '2025-02-15 09:05:00.000', 62,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/22-NIC_DOCUMENT-1ef0d1-nic-document.pdf',                     'nic-document.pdf',            '2025-02-16 09:05:00.000', 63,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/23-NIC_DOCUMENT-000022-nic-document.pdf',                     'nic-document.pdf',            '2025-02-17 09:05:00.000', 64,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/24-NIC_DOCUMENT-31dde0-nic-document.pdf',                     'nic-document.pdf',            '2025-02-18 09:05:00.000', 65,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/25-NIC_DOCUMENT-5ed40f-nic-document.pdf',                     'nic-document.pdf',            '2025-02-19 09:05:00.000', 66,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/26-NIC_DOCUMENT-358e0e-nic-document.pdf',                     'nic-document.pdf',            '2025-02-20 09:05:00.000', 67,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/27-NIC_DOCUMENT-3a66d4-nic-document.pdf',                     'nic-document.pdf',            '2025-02-21 09:05:00.000', 68,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/28-NIC_DOCUMENT-62ca9f-nic-document.pdf',                     'nic-document.pdf',            '2025-02-22 09:05:00.000', 69,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/29-NIC_DOCUMENT-b20e2f-nic-document.pdf',                     'nic-document.pdf',            '2025-02-23 09:05:00.000', 70,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/30-NIC_DOCUMENT-9fc0ec-nic-document.pdf',                     'nic-document.pdf',            '2025-02-24 09:05:00.000', 71,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/31-NIC_DOCUMENT-83584a-nic-document.pdf',                     'nic-document.pdf',            '2025-03-01 09:05:00.000', 72,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/32-NIC_DOCUMENT-a1c135-nic-document.pdf',                     'nic-document.pdf',            '2025-03-02 09:05:00.000', 73,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/33-NIC_DOCUMENT-7d1b97-nic-document.pdf',                     'nic-document.pdf',            '2025-03-03 09:05:00.000', 74,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/34-NIC_DOCUMENT-171562-nic-document.pdf',                     'nic-document.pdf',            '2025-03-04 09:05:00.000', 75,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/35-NIC_DOCUMENT-ae2dd8-nic-document.pdf',                     'nic-document.pdf',            '2025-03-05 09:05:00.000', 76,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/36-NIC_DOCUMENT-d3722e-nic-document.pdf',                     'nic-document.pdf',            '2025-03-06 09:05:00.000', 77,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/37-NIC_DOCUMENT-c5f65b-nic-document.pdf',                     'nic-document.pdf',            '2025-03-07 09:05:00.000', 78,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/38-NIC_DOCUMENT-19f255-nic-document.pdf',                     'nic-document.pdf',            '2025-03-08 09:05:00.000', 79,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/39-NIC_DOCUMENT-d3d849-nic-document.pdf',                     'nic-document.pdf',            '2025-03-09 09:05:00.000', 80,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/40-NIC_DOCUMENT-e9fb43-nic-document.pdf',                     'nic-document.pdf',            '2025-03-10 09:05:00.000', 81,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/41-NIC_DOCUMENT-3a2552-nic-document.pdf',                     'nic-document.pdf',            '2025-03-15 09:05:00.000', 82,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/42-NIC_DOCUMENT-48f2e8-nic-document.pdf',                     'nic-document.pdf',            '2025-03-16 09:05:00.000', 83,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/43-NIC_DOCUMENT-ffd19d-nic-document.pdf',                     'nic-document.pdf',            '2025-03-17 09:05:00.000', 84,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/44-NIC_DOCUMENT-40b48c-nic-document.pdf',                     'nic-document.pdf',            '2025-03-18 09:05:00.000', 85,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/45-NIC_DOCUMENT-017fe2-nic-document.pdf',                     'nic-document.pdf',            '2025-03-19 09:05:00.000', 86,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/46-NIC_DOCUMENT-8dae57-nic-document.pdf',                     'nic-document.pdf',            '2025-03-20 09:05:00.000', 87,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/47-NIC_DOCUMENT-203f20-nic-document.pdf',                     'nic-document.pdf',            '2025-03-21 09:05:00.000', 88,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/48-NIC_DOCUMENT-5ff8b7-nic-document.pdf',                     'nic-document.pdf',            '2025-03-22 09:05:00.000', 89,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/49-NIC_DOCUMENT-1c8eaa-nic-document.pdf',                     'nic-document.pdf',            '2025-03-23 09:05:00.000', 90,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/50-NIC_DOCUMENT-899928-nic-document.pdf',                     'nic-document.pdf',            '2025-03-24 09:05:00.000', 91,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/51-NIC_DOCUMENT-86cdfc-nic-document.pdf',                     'nic-document.pdf',            '2025-04-01 09:05:00.000', 92,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/52-NIC_DOCUMENT-3c679d-nic-document.pdf',                     'nic-document.pdf',            '2025-04-02 09:05:00.000', 93,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/53-NIC_DOCUMENT-7a3311-nic-document.pdf',                     'nic-document.pdf',            '2025-04-03 09:05:00.000', 94,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/54-NIC_DOCUMENT-b98ce3-nic-document.pdf',                     'nic-document.pdf',            '2025-04-04 09:05:00.000', 95,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/55-NIC_DOCUMENT-39a9a5-nic-document.pdf',                     'nic-document.pdf',            '2025-04-05 09:05:00.000', 96,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/56-NIC_DOCUMENT-d4fec4-nic-document.pdf',                     'nic-document.pdf',            '2025-04-06 09:05:00.000', 97,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/57-NIC_DOCUMENT-d4f7c9-nic-document.pdf',                     'nic-document.pdf',            '2025-04-07 09:05:00.000', 98,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/58-NIC_DOCUMENT-0088ce-nic-document.pdf',                     'nic-document.pdf',            '2025-04-08 09:05:00.000', 99,   NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/59-NIC_DOCUMENT-c937bb-nic-document.pdf',                     'nic-document.pdf',            '2025-04-09 09:05:00.000', 100,  NULL, NULL, NULL, NULL, NULL),
       ('NIC_DOCUMENT',          'uploads/60-NIC_DOCUMENT-fdd8bd-nic-document.pdf',                     'nic-document.pdf',            '2025-04-10 09:05:00.000', 101,  NULL, NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/1-CONSULTATION_DOCUMENT-57f871-consultation-document-1.pdf',  'consultation-document-1.pdf', '2025-04-15 10:01:00.000', NULL, 61,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/1-CONSULTATION_DOCUMENT-149d93-consultation-document-2.pdf',  'consultation-document-2.pdf', '2025-04-15 10:02:00.000', NULL, 61,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/1-CONSULTATION_DOCUMENT-770925-consultation-document-3.pdf',  'consultation-document-3.pdf', '2025-04-15 10:03:00.000', NULL, 61,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/2-CONSULTATION_DOCUMENT-7cc172-consultation-document-1.pdf',  'consultation-document-1.pdf', '2025-04-16 10:01:00.000', NULL, 62,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/2-CONSULTATION_DOCUMENT-9b08f2-consultation-document-2.pdf',  'consultation-document-2.pdf', '2025-04-16 10:02:00.000', NULL, 62,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/2-CONSULTATION_DOCUMENT-31d957-consultation-document-3.pdf',  'consultation-document-3.pdf', '2025-04-16 10:03:00.000', NULL, 62,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/3-CONSULTATION_DOCUMENT-462f6c-consultation-document-1.pdf',  'consultation-document-1.pdf', '2025-04-17 10:01:00.000', NULL, 63,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/3-CONSULTATION_DOCUMENT-b151c3-consultation-document-2.pdf',  'consultation-document-2.pdf', '2025-04-17 10:02:00.000', NULL, 63,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/3-CONSULTATION_DOCUMENT-aa1d6b-consultation-document-3.pdf',  'consultation-document-3.pdf', '2025-04-17 10:03:00.000', NULL, 63,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/4-CONSULTATION_DOCUMENT-bf7d39-consultation-document-1.pdf',  'consultation-document-1.pdf', '2025-04-18 10:01:00.000', NULL, 64,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/4-CONSULTATION_DOCUMENT-715124-consultation-document-2.pdf',  'consultation-document-2.pdf', '2025-04-18 10:02:00.000', NULL, 64,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/4-CONSULTATION_DOCUMENT-044a5d-consultation-document-3.pdf',  'consultation-document-3.pdf', '2025-04-18 10:03:00.000', NULL, 64,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/5-CONSULTATION_DOCUMENT-449331-consultation-document-1.pdf',  'consultation-document-1.pdf', '2025-04-19 10:01:00.000', NULL, 65,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/5-CONSULTATION_DOCUMENT-d680be-consultation-document-2.pdf',  'consultation-document-2.pdf', '2025-04-19 10:02:00.000', NULL, 65,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/5-CONSULTATION_DOCUMENT-85c493-consultation-document-3.pdf',  'consultation-document-3.pdf', '2025-04-19 10:03:00.000', NULL, 65,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/6-CONSULTATION_DOCUMENT-334d5f-consultation-document-1.pdf',  'consultation-document-1.pdf', '2025-04-20 10:01:00.000', NULL, 66,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/6-CONSULTATION_DOCUMENT-1979d1-consultation-document-2.pdf',  'consultation-document-2.pdf', '2025-04-20 10:02:00.000', NULL, 66,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/6-CONSULTATION_DOCUMENT-88c7d6-consultation-document-3.pdf',  'consultation-document-3.pdf', '2025-04-20 10:03:00.000', NULL, 66,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/7-CONSULTATION_DOCUMENT-92d5e2-consultation-document-1.pdf',  'consultation-document-1.pdf', '2025-04-21 10:01:00.000', NULL, 67,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/7-CONSULTATION_DOCUMENT-38a446-consultation-document-2.pdf',  'consultation-document-2.pdf', '2025-04-21 10:02:00.000', NULL, 67,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/7-CONSULTATION_DOCUMENT-1c6b49-consultation-document-3.pdf',  'consultation-document-3.pdf', '2025-04-21 10:03:00.000', NULL, 67,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/8-CONSULTATION_DOCUMENT-d70ba6-consultation-document-1.pdf',  'consultation-document-1.pdf', '2025-04-22 10:01:00.000', NULL, 68,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/8-CONSULTATION_DOCUMENT-9eeb00-consultation-document-2.pdf',  'consultation-document-2.pdf', '2025-04-22 10:02:00.000', NULL, 68,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/8-CONSULTATION_DOCUMENT-582cf3-consultation-document-3.pdf',  'consultation-document-3.pdf', '2025-04-22 10:03:00.000', NULL, 68,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/9-CONSULTATION_DOCUMENT-f35a54-consultation-document-1.pdf',  'consultation-document-1.pdf', '2025-04-23 10:01:00.000', NULL, 69,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/9-CONSULTATION_DOCUMENT-230c0d-consultation-document-2.pdf',  'consultation-document-2.pdf', '2025-04-23 10:02:00.000', NULL, 69,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/9-CONSULTATION_DOCUMENT-db2de8-consultation-document-3.pdf',  'consultation-document-3.pdf', '2025-04-23 10:03:00.000', NULL, 69,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/10-CONSULTATION_DOCUMENT-31a310-consultation-document-1.pdf', 'consultation-document-1.pdf', '2025-04-24 10:01:00.000', NULL, 70,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/10-CONSULTATION_DOCUMENT-dd48f2-consultation-document-2.pdf', 'consultation-document-2.pdf', '2025-04-24 10:02:00.000', NULL, 70,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/10-CONSULTATION_DOCUMENT-33e893-consultation-document-3.pdf', 'consultation-document-3.pdf', '2025-04-24 10:03:00.000', NULL, 70,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/11-CONSULTATION_DOCUMENT-8441d1-consultation-document-1.pdf', 'consultation-document-1.pdf', '2025-07-01 10:01:00.000', NULL, 71,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/11-CONSULTATION_DOCUMENT-d70caf-consultation-document-2.pdf', 'consultation-document-2.pdf', '2025-07-01 10:02:00.000', NULL, 71,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/11-CONSULTATION_DOCUMENT-44b046-consultation-document-3.pdf', 'consultation-document-3.pdf', '2025-07-01 10:03:00.000', NULL, 71,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/12-CONSULTATION_DOCUMENT-87ac5c-consultation-document-1.pdf', 'consultation-document-1.pdf', '2025-07-05 10:01:00.000', NULL, 72,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/12-CONSULTATION_DOCUMENT-3660cc-consultation-document-2.pdf', 'consultation-document-2.pdf', '2025-07-05 10:02:00.000', NULL, 72,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/12-CONSULTATION_DOCUMENT-224bfe-consultation-document-3.pdf', 'consultation-document-3.pdf', '2025-07-05 10:03:00.000', NULL, 72,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/13-CONSULTATION_DOCUMENT-e226f9-consultation-document-1.pdf', 'consultation-document-1.pdf', '2025-08-01 10:01:00.000', NULL, 73,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/13-CONSULTATION_DOCUMENT-492956-consultation-document-2.pdf', 'consultation-document-2.pdf', '2025-08-01 10:02:00.000', NULL, 73,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/13-CONSULTATION_DOCUMENT-c244a2-consultation-document-3.pdf', 'consultation-document-3.pdf', '2025-08-01 10:03:00.000', NULL, 73,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/14-CONSULTATION_DOCUMENT-ef82de-consultation-document-1.pdf', 'consultation-document-1.pdf', '2025-08-02 10:01:00.000', NULL, 74,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/14-CONSULTATION_DOCUMENT-e34ce6-consultation-document-2.pdf', 'consultation-document-2.pdf', '2025-08-02 10:02:00.000', NULL, 74,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/14-CONSULTATION_DOCUMENT-95e755-consultation-document-3.pdf', 'consultation-document-3.pdf', '2025-08-02 10:03:00.000', NULL, 74,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/15-CONSULTATION_DOCUMENT-a14f22-consultation-document-1.pdf', 'consultation-document-1.pdf', '2025-08-03 10:01:00.000', NULL, 75,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/15-CONSULTATION_DOCUMENT-2d10fc-consultation-document-2.pdf', 'consultation-document-2.pdf', '2025-08-03 10:02:00.000', NULL, 75,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/15-CONSULTATION_DOCUMENT-05a627-consultation-document-3.pdf', 'consultation-document-3.pdf', '2025-08-03 10:03:00.000', NULL, 75,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/16-CONSULTATION_DOCUMENT-7b8f90-consultation-document-1.pdf', 'consultation-document-1.pdf', '2025-08-04 10:01:00.000', NULL, 76,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/16-CONSULTATION_DOCUMENT-41f4b3-consultation-document-2.pdf', 'consultation-document-2.pdf', '2025-08-04 10:02:00.000', NULL, 76,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/16-CONSULTATION_DOCUMENT-26d640-consultation-document-3.pdf', 'consultation-document-3.pdf', '2025-08-04 10:03:00.000', NULL, 76,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/17-CONSULTATION_DOCUMENT-b675bd-consultation-document-1.pdf', 'consultation-document-1.pdf', '2025-08-05 10:01:00.000', NULL, 77,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/17-CONSULTATION_DOCUMENT-801b4d-consultation-document-2.pdf', 'consultation-document-2.pdf', '2025-08-05 10:02:00.000', NULL, 77,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/17-CONSULTATION_DOCUMENT-4c6b06-consultation-document-3.pdf', 'consultation-document-3.pdf', '2025-08-05 10:03:00.000', NULL, 77,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/18-CONSULTATION_DOCUMENT-48d146-consultation-document-1.pdf', 'consultation-document-1.pdf', '2025-08-06 10:01:00.000', NULL, 78,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/18-CONSULTATION_DOCUMENT-e84a90-consultation-document-2.pdf', 'consultation-document-2.pdf', '2025-08-06 10:02:00.000', NULL, 78,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/18-CONSULTATION_DOCUMENT-97b75a-consultation-document-3.pdf', 'consultation-document-3.pdf', '2025-08-06 10:03:00.000', NULL, 78,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/19-CONSULTATION_DOCUMENT-7e9935-consultation-document-1.pdf', 'consultation-document-1.pdf', '2025-08-10 10:01:00.000', NULL, 79,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/19-CONSULTATION_DOCUMENT-53e10f-consultation-document-2.pdf', 'consultation-document-2.pdf', '2025-08-10 10:02:00.000', NULL, 79,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/19-CONSULTATION_DOCUMENT-a26947-consultation-document-3.pdf', 'consultation-document-3.pdf', '2025-08-10 10:03:00.000', NULL, 79,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/20-CONSULTATION_DOCUMENT-e03672-consultation-document-1.pdf', 'consultation-document-1.pdf', '2025-08-12 10:01:00.000', NULL, 80,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/20-CONSULTATION_DOCUMENT-997f76-consultation-document-2.pdf', 'consultation-document-2.pdf', '2025-08-12 10:02:00.000', NULL, 80,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/20-CONSULTATION_DOCUMENT-e4e808-consultation-document-3.pdf', 'consultation-document-3.pdf', '2025-08-12 10:03:00.000', NULL, 80,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/21-CONSULTATION_DOCUMENT-852b0b-consultation-document-1.pdf', 'consultation-document-1.pdf', '2026-09-24 09:01:00.000', NULL, 81,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/21-CONSULTATION_DOCUMENT-7265f5-consultation-document-2.pdf', 'consultation-document-2.pdf', '2026-09-24 09:02:00.000', NULL, 81,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/21-CONSULTATION_DOCUMENT-483911-consultation-document-3.pdf', 'consultation-document-3.pdf', '2026-09-24 09:03:00.000', NULL, 81,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/22-CONSULTATION_DOCUMENT-ca6518-consultation-document-1.pdf', 'consultation-document-1.pdf', '2026-09-25 09:01:00.000', NULL, 82,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/22-CONSULTATION_DOCUMENT-1114f9-consultation-document-2.pdf', 'consultation-document-2.pdf', '2026-09-25 09:02:00.000', NULL, 82,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/22-CONSULTATION_DOCUMENT-dfe0a8-consultation-document-3.pdf', 'consultation-document-3.pdf', '2026-09-25 09:03:00.000', NULL, 82,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/23-CONSULTATION_DOCUMENT-bebe47-consultation-document-1.pdf', 'consultation-document-1.pdf', '2026-09-26 09:01:00.000', NULL, 83,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/23-CONSULTATION_DOCUMENT-660131-consultation-document-2.pdf', 'consultation-document-2.pdf', '2026-09-26 09:02:00.000', NULL, 83,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/23-CONSULTATION_DOCUMENT-09022f-consultation-document-3.pdf', 'consultation-document-3.pdf', '2026-09-26 09:03:00.000', NULL, 83,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/24-CONSULTATION_DOCUMENT-7aed93-consultation-document-1.pdf', 'consultation-document-1.pdf', '2026-09-27 09:01:00.000', NULL, 84,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/24-CONSULTATION_DOCUMENT-7e13d2-consultation-document-2.pdf', 'consultation-document-2.pdf', '2026-09-27 09:02:00.000', NULL, 84,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/24-CONSULTATION_DOCUMENT-4e48c1-consultation-document-3.pdf', 'consultation-document-3.pdf', '2026-09-27 09:03:00.000', NULL, 84,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/25-CONSULTATION_DOCUMENT-d9bd18-consultation-document-1.pdf', 'consultation-document-1.pdf', '2026-09-28 09:01:00.000', NULL, 85,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/25-CONSULTATION_DOCUMENT-83d991-consultation-document-2.pdf', 'consultation-document-2.pdf', '2026-09-28 09:02:00.000', NULL, 85,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/25-CONSULTATION_DOCUMENT-cdcf9d-consultation-document-3.pdf', 'consultation-document-3.pdf', '2026-09-28 09:03:00.000', NULL, 85,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/26-CONSULTATION_DOCUMENT-65ebe8-consultation-document-1.pdf', 'consultation-document-1.pdf', '2026-09-29 09:01:00.000', NULL, 86,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/26-CONSULTATION_DOCUMENT-1d4f45-consultation-document-2.pdf', 'consultation-document-2.pdf', '2026-09-29 09:02:00.000', NULL, 86,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/26-CONSULTATION_DOCUMENT-5a13e9-consultation-document-3.pdf', 'consultation-document-3.pdf', '2026-09-29 09:03:00.000', NULL, 86,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/27-CONSULTATION_DOCUMENT-8af0fc-consultation-document-1.pdf', 'consultation-document-1.pdf', '2026-09-30 09:01:00.000', NULL, 87,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/27-CONSULTATION_DOCUMENT-f3b096-consultation-document-2.pdf', 'consultation-document-2.pdf', '2026-09-30 09:02:00.000', NULL, 87,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/27-CONSULTATION_DOCUMENT-bf05d3-consultation-document-3.pdf', 'consultation-document-3.pdf', '2026-09-30 09:03:00.000', NULL, 87,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/28-CONSULTATION_DOCUMENT-2b3fd5-consultation-document-1.pdf', 'consultation-document-1.pdf', '2026-10-01 09:01:00.000', NULL, 88,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/28-CONSULTATION_DOCUMENT-fe625f-consultation-document-2.pdf', 'consultation-document-2.pdf', '2026-10-01 09:02:00.000', NULL, 88,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/28-CONSULTATION_DOCUMENT-f09133-consultation-document-3.pdf', 'consultation-document-3.pdf', '2026-10-01 09:03:00.000', NULL, 88,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/29-CONSULTATION_DOCUMENT-7ffc4a-consultation-document-1.pdf', 'consultation-document-1.pdf', '2026-10-02 09:01:00.000', NULL, 89,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/29-CONSULTATION_DOCUMENT-d183cd-consultation-document-2.pdf', 'consultation-document-2.pdf', '2026-10-02 09:02:00.000', NULL, 89,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/29-CONSULTATION_DOCUMENT-b4a39b-consultation-document-3.pdf', 'consultation-document-3.pdf', '2026-10-02 09:03:00.000', NULL, 89,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/30-CONSULTATION_DOCUMENT-f8dede-consultation-document-1.pdf', 'consultation-document-1.pdf', '2026-10-03 09:01:00.000', NULL, 90,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/30-CONSULTATION_DOCUMENT-27b0db-consultation-document-2.pdf', 'consultation-document-2.pdf', '2026-10-03 09:02:00.000', NULL, 90,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/30-CONSULTATION_DOCUMENT-04fce5-consultation-document-3.pdf', 'consultation-document-3.pdf', '2026-10-03 09:03:00.000', NULL, 90,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/31-CONSULTATION_DOCUMENT-bb8cd0-consultation-document-1.pdf', 'consultation-document-1.pdf', '2026-10-04 09:01:00.000', NULL, 91,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/31-CONSULTATION_DOCUMENT-1bc757-consultation-document-2.pdf', 'consultation-document-2.pdf', '2026-10-04 09:02:00.000', NULL, 91,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/31-CONSULTATION_DOCUMENT-927e8b-consultation-document-3.pdf', 'consultation-document-3.pdf', '2026-10-04 09:03:00.000', NULL, 91,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/32-CONSULTATION_DOCUMENT-070f77-consultation-document-1.pdf', 'consultation-document-1.pdf', '2026-10-04 15:01:00.000', NULL, 92,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/32-CONSULTATION_DOCUMENT-a7ae1a-consultation-document-2.pdf', 'consultation-document-2.pdf', '2026-10-04 15:02:00.000', NULL, 92,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/32-CONSULTATION_DOCUMENT-73a6c6-consultation-document-3.pdf', 'consultation-document-3.pdf', '2026-10-04 15:03:00.000', NULL, 92,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/33-CONSULTATION_DOCUMENT-ce0e6d-consultation-document-1.pdf', 'consultation-document-1.pdf', '2026-10-05 09:01:00.000', NULL, 93,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/33-CONSULTATION_DOCUMENT-938a7f-consultation-document-2.pdf', 'consultation-document-2.pdf', '2026-10-05 09:02:00.000', NULL, 93,   NULL, NULL, NULL, NULL),
       ('CONSULTATION_DOCUMENT', 'uploads/33-CONSULTATION_DOCUMENT-306be9-consultation-document-3.pdf', 'consultation-document-3.pdf', '2026-10-05 09:03:00.000', NULL, 93,   NULL, NULL, NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-e8a6b5-case-document-1.pdf',                  'case-document-1.pdf',         '2025-05-10 09:00:00.000', NULL, NULL, 39,   0,    94,   NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-29bf3c-case-document-2.pdf',                  'case-document-2.pdf',         '2025-05-18 09:00:00.000', NULL, NULL, 39,   0,    97,   NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-2a4807-case-document-3.pdf',                  'case-document-3.pdf',         '2025-06-04 09:00:00.000', NULL, NULL, 39,   0,    111,  NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-192b47-case-document-4.pdf',                  'case-document-4.pdf',         '2025-06-05 09:00:00.000', NULL, NULL, 39,   0,    112,  NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-e1ff31-case-document-5.pdf',                  'case-document-5.pdf',         '2025-06-06 09:00:00.000', NULL, NULL, 39,   0,    113,  NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-ca004a-case-document-6.pdf',                  'case-document-6.pdf',         '2025-06-07 09:00:00.000', NULL, NULL, 39,   0,    115,  NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-a3e99b-case-document-7.pdf',                  'case-document-7.pdf',         '2025-06-15 09:00:00.000', NULL, NULL, 39,   0,    121,  NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-f9a44c-case-document-8.pdf',                  'case-document-8.pdf',         '2025-06-16 09:00:00.000', NULL, NULL, 39,   0,    122,  NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-60c055-case-document-9.pdf',                  'case-document-9.pdf',         '2025-06-17 09:00:00.000', NULL, NULL, 39,   0,    123,  NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-fd08b3-case-document-10.pdf',                 'case-document-10.pdf',        '2025-06-18 09:00:00.000', NULL, NULL, 39,   0,    124,  NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-42c941-case-document-11.pdf',                 'case-document-11.pdf',        '2025-06-19 09:00:00.000', NULL, NULL, 39,   0,    125,  NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-cf6d8a-case-document-12.pdf',                 'case-document-12.pdf',        '2025-06-25 09:00:00.000', NULL, NULL, 39,   0,    128,  NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-fe7223-case-document-13.pdf',                 'case-document-13.pdf',        '2025-06-26 09:00:00.000', NULL, NULL, 39,   0,    129,  NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-4e06b3-case-document-14.pdf',                 'case-document-14.pdf',        '2025-06-27 09:00:00.000', NULL, NULL, 39,   0,    130,  NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-ca2e43-case-document-15.pdf',                 'case-document-15.pdf',        '2025-04-20 09:11:00.000', NULL, NULL, 39,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-508daf-case-document-16.pdf',                 'case-document-16.pdf',        '2025-04-20 09:12:00.000', NULL, NULL, 39,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-a2870c-case-document-17.pdf',                 'case-document-17.pdf',        '2025-04-20 09:13:00.000', NULL, NULL, 39,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-4fe9f3-case-document-18.pdf',                 'case-document-18.pdf',        '2025-04-21 09:01:00.000', NULL, NULL, 39,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-74bef9-case-document-19.pdf',                 'case-document-19.pdf',        '2025-04-21 09:02:00.000', NULL, NULL, 39,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/1-CASE_DOCUMENT-df0493-case-document-20.pdf',                 'case-document-20.pdf',        '2025-04-21 09:03:00.000', NULL, NULL, 39,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/2-CASE_DOCUMENT-ff4c3d-case-document-1.pdf',                  'case-document-1.pdf',         '2025-05-20 09:00:00.000', NULL, NULL, 40,   0,    99,   NULL),
       ('CASE_DOCUMENT',         'uploads/2-CASE_DOCUMENT-d0a368-case-document-2.pdf',                  'case-document-2.pdf',         '2025-04-23 09:11:00.000', NULL, NULL, 40,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/2-CASE_DOCUMENT-8a9044-case-document-3.pdf',                  'case-document-3.pdf',         '2025-04-23 09:12:00.000', NULL, NULL, 40,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/2-CASE_DOCUMENT-8e462c-case-document-4.pdf',                  'case-document-4.pdf',         '2025-04-23 09:13:00.000', NULL, NULL, 40,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/2-CASE_DOCUMENT-98297b-case-document-5.pdf',                  'case-document-5.pdf',         '2025-04-24 09:01:00.000', NULL, NULL, 40,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/2-CASE_DOCUMENT-b65c40-case-document-6.pdf',                  'case-document-6.pdf',         '2025-04-24 09:02:00.000', NULL, NULL, 40,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/2-CASE_DOCUMENT-b332b4-case-document-7.pdf',                  'case-document-7.pdf',         '2025-04-24 09:03:00.000', NULL, NULL, 40,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/3-CASE_DOCUMENT-c84419-case-document-1.pdf',                  'case-document-1.pdf',         '2025-05-19 09:00:00.000', NULL, NULL, 41,   0,    98,   NULL),
       ('CASE_DOCUMENT',         'uploads/3-CASE_DOCUMENT-16b24a-case-document-2.pdf',                  'case-document-2.pdf',         '2025-04-24 09:11:00.000', NULL, NULL, 41,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/3-CASE_DOCUMENT-73922d-case-document-3.pdf',                  'case-document-3.pdf',         '2025-04-24 09:12:00.000', NULL, NULL, 41,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/3-CASE_DOCUMENT-e87ace-case-document-4.pdf',                  'case-document-4.pdf',         '2025-04-24 09:13:00.000', NULL, NULL, 41,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/3-CASE_DOCUMENT-286cf1-case-document-5.pdf',                  'case-document-5.pdf',         '2025-04-25 09:01:00.000', NULL, NULL, 41,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/3-CASE_DOCUMENT-6e9f17-case-document-6.pdf',                  'case-document-6.pdf',         '2025-04-25 09:02:00.000', NULL, NULL, 41,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/3-CASE_DOCUMENT-a5a55c-case-document-7.pdf',                  'case-document-7.pdf',         '2025-04-25 09:03:00.000', NULL, NULL, 41,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/4-CASE_DOCUMENT-49e97c-case-document-1.pdf',                  'case-document-1.pdf',         '2025-05-25 09:00:00.000', NULL, NULL, 42,   0,    102,  NULL),
       ('CASE_DOCUMENT',         'uploads/4-CASE_DOCUMENT-057691-case-document-2.pdf',                  'case-document-2.pdf',         '2025-04-25 09:11:00.000', NULL, NULL, 42,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/4-CASE_DOCUMENT-d7b448-case-document-3.pdf',                  'case-document-3.pdf',         '2025-04-25 09:12:00.000', NULL, NULL, 42,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/4-CASE_DOCUMENT-8451ab-case-document-4.pdf',                  'case-document-4.pdf',         '2025-04-25 09:13:00.000', NULL, NULL, 42,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/4-CASE_DOCUMENT-e94f1d-case-document-5.pdf',                  'case-document-5.pdf',         '2025-04-26 09:01:00.000', NULL, NULL, 42,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/4-CASE_DOCUMENT-956440-case-document-6.pdf',                  'case-document-6.pdf',         '2025-04-26 09:02:00.000', NULL, NULL, 42,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/4-CASE_DOCUMENT-33414b-case-document-7.pdf',                  'case-document-7.pdf',         '2025-04-26 09:03:00.000', NULL, NULL, 42,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/5-CASE_DOCUMENT-4da030-case-document-1.pdf',                  'case-document-1.pdf',         '2025-05-28 09:00:00.000', NULL, NULL, 43,   0,    103,  NULL),
       ('CASE_DOCUMENT',         'uploads/5-CASE_DOCUMENT-2df0a9-case-document-2.pdf',                  'case-document-2.pdf',         '2025-04-27 09:11:00.000', NULL, NULL, 43,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/5-CASE_DOCUMENT-bc5d94-case-document-3.pdf',                  'case-document-3.pdf',         '2025-04-27 09:12:00.000', NULL, NULL, 43,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/5-CASE_DOCUMENT-37ca0e-case-document-4.pdf',                  'case-document-4.pdf',         '2025-04-27 09:13:00.000', NULL, NULL, 43,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/5-CASE_DOCUMENT-a8e433-case-document-5.pdf',                  'case-document-5.pdf',         '2025-04-28 09:01:00.000', NULL, NULL, 43,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/5-CASE_DOCUMENT-f409b8-case-document-6.pdf',                  'case-document-6.pdf',         '2025-04-28 09:02:00.000', NULL, NULL, 43,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/5-CASE_DOCUMENT-1512aa-case-document-7.pdf',                  'case-document-7.pdf',         '2025-04-28 09:03:00.000', NULL, NULL, 43,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/6-CASE_DOCUMENT-05aec7-case-document-1.pdf',                  'case-document-1.pdf',         '2025-04-28 09:11:00.000', NULL, NULL, 44,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/6-CASE_DOCUMENT-a799e8-case-document-2.pdf',                  'case-document-2.pdf',         '2025-04-28 09:12:00.000', NULL, NULL, 44,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/6-CASE_DOCUMENT-6cefaa-case-document-3.pdf',                  'case-document-3.pdf',         '2025-04-28 09:13:00.000', NULL, NULL, 44,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/6-CASE_DOCUMENT-2cfbc1-case-document-4.pdf',                  'case-document-4.pdf',         '2025-04-29 09:01:00.000', NULL, NULL, 44,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/6-CASE_DOCUMENT-98a9d5-case-document-5.pdf',                  'case-document-5.pdf',         '2025-04-29 09:02:00.000', NULL, NULL, 44,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/6-CASE_DOCUMENT-d0fa8f-case-document-6.pdf',                  'case-document-6.pdf',         '2025-04-29 09:03:00.000', NULL, NULL, 44,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/7-CASE_DOCUMENT-798136-case-document-1.pdf',                  'case-document-1.pdf',         '2025-05-30 09:00:00.000', NULL, NULL, 45,   0,    105,  NULL),
       ('CASE_DOCUMENT',         'uploads/7-CASE_DOCUMENT-b55c6a-case-document-2.pdf',                  'case-document-2.pdf',         '2025-04-29 09:11:00.000', NULL, NULL, 45,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/7-CASE_DOCUMENT-7b8c7c-case-document-3.pdf',                  'case-document-3.pdf',         '2025-04-29 09:12:00.000', NULL, NULL, 45,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/7-CASE_DOCUMENT-0e5fb2-case-document-4.pdf',                  'case-document-4.pdf',         '2025-04-29 09:13:00.000', NULL, NULL, 45,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/7-CASE_DOCUMENT-2b4fb0-case-document-5.pdf',                  'case-document-5.pdf',         '2025-04-30 09:01:00.000', NULL, NULL, 45,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/7-CASE_DOCUMENT-cd5b00-case-document-6.pdf',                  'case-document-6.pdf',         '2025-04-30 09:02:00.000', NULL, NULL, 45,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/7-CASE_DOCUMENT-2a1c49-case-document-7.pdf',                  'case-document-7.pdf',         '2025-04-30 09:03:00.000', NULL, NULL, 45,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/8-CASE_DOCUMENT-ccaded-case-document-1.pdf',                  'case-document-1.pdf',         '2025-07-15 09:00:00.000', NULL, NULL, 46,   0,    133,  NULL),
       ('CASE_DOCUMENT',         'uploads/8-CASE_DOCUMENT-d0c808-case-document-2.pdf',                  'case-document-2.pdf',         '2025-07-17 09:00:00.000', NULL, NULL, 46,   0,    135,  NULL),
       ('CASE_DOCUMENT',         'uploads/8-CASE_DOCUMENT-c033f2-case-document-3.pdf',                  'case-document-3.pdf',         '2025-07-10 09:11:00.000', NULL, NULL, 46,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/8-CASE_DOCUMENT-8c11b8-case-document-4.pdf',                  'case-document-4.pdf',         '2025-07-10 09:12:00.000', NULL, NULL, 46,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/8-CASE_DOCUMENT-bafb20-case-document-5.pdf',                  'case-document-5.pdf',         '2025-07-10 09:13:00.000', NULL, NULL, 46,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/8-CASE_DOCUMENT-87bcfa-case-document-6.pdf',                  'case-document-6.pdf',         '2025-07-11 09:01:00.000', NULL, NULL, 46,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/8-CASE_DOCUMENT-a53234-case-document-7.pdf',                  'case-document-7.pdf',         '2025-07-11 09:02:00.000', NULL, NULL, 46,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/8-CASE_DOCUMENT-2a21ca-case-document-8.pdf',                  'case-document-8.pdf',         '2025-07-11 09:03:00.000', NULL, NULL, 46,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/9-CASE_DOCUMENT-b85185-case-document-1.pdf',                  'case-document-1.pdf',         '2025-08-16 09:00:00.000', NULL, NULL, 47,   0,    138,  NULL),
       ('CASE_DOCUMENT',         'uploads/9-CASE_DOCUMENT-fe3da0-case-document-2.pdf',                  'case-document-2.pdf',         '2025-08-15 09:11:00.000', NULL, NULL, 47,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/9-CASE_DOCUMENT-d5803a-case-document-3.pdf',                  'case-document-3.pdf',         '2025-08-15 09:12:00.000', NULL, NULL, 47,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/9-CASE_DOCUMENT-03cb6d-case-document-4.pdf',                  'case-document-4.pdf',         '2025-08-15 09:13:00.000', NULL, NULL, 47,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/9-CASE_DOCUMENT-537981-case-document-5.pdf',                  'case-document-5.pdf',         '2025-08-16 09:01:00.000', NULL, NULL, 47,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/9-CASE_DOCUMENT-03decb-case-document-6.pdf',                  'case-document-6.pdf',         '2025-08-16 09:02:00.000', NULL, NULL, 47,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/9-CASE_DOCUMENT-ca4842-case-document-7.pdf',                  'case-document-7.pdf',         '2025-08-16 09:03:00.000', NULL, NULL, 47,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/10-CASE_DOCUMENT-1afd14-case-document-1.pdf',                 'case-document-1.pdf',         '2025-08-18 09:11:00.000', NULL, NULL, 48,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/10-CASE_DOCUMENT-506c93-case-document-2.pdf',                 'case-document-2.pdf',         '2025-08-18 09:12:00.000', NULL, NULL, 48,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/10-CASE_DOCUMENT-54fdaf-case-document-3.pdf',                 'case-document-3.pdf',         '2025-08-18 09:13:00.000', NULL, NULL, 48,   0,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/10-CASE_DOCUMENT-db57c8-case-document-4.pdf',                 'case-document-4.pdf',         '2025-08-19 09:01:00.000', NULL, NULL, 48,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/10-CASE_DOCUMENT-73adfd-case-document-5.pdf',                 'case-document-5.pdf',         '2025-08-19 09:02:00.000', NULL, NULL, 48,   1,    NULL, NULL),
       ('CASE_DOCUMENT',         'uploads/10-CASE_DOCUMENT-dfb7b0-case-document-6.pdf',                 'case-document-6.pdf',         '2025-08-19 09:03:00.000', NULL, NULL, 48,   1,    NULL, NULL),
       ('INVOICE_DOCUMENT',      'uploads/1-INVOICE_DOCUMENT-707ddc-INV-1-93580258.pdf',                'INV-1-93580258.pdf',          '2025-06-01 09:00:00.000', NULL, NULL, NULL, NULL, NULL, 26  ),
       ('INVOICE_DOCUMENT',      'uploads/2-INVOICE_DOCUMENT-401d88-INV-2-74814825.pdf',                'INV-2-74814825.pdf',          '2025-06-02 09:00:00.000', NULL, NULL, NULL, NULL, NULL, 27  ),
       ('INVOICE_DOCUMENT',      'uploads/3-INVOICE_DOCUMENT-0a1c2c-INV-3-56049392.pdf',                'INV-3-56049392.pdf',          '2025-06-15 10:00:00.000', NULL, NULL, NULL, NULL, NULL, 28  ),
       ('INVOICE_DOCUMENT',      'uploads/4-INVOICE_DOCUMENT-da6527-INV-4-37283959.pdf',                'INV-4-37283959.pdf',          '2025-06-16 10:00:00.000', NULL, NULL, NULL, NULL, NULL, 29  ),
       ('INVOICE_DOCUMENT',      'uploads/6-INVOICE_DOCUMENT-7d3b8f-INV-5-18518526.pdf',                'INV-5-18518526.pdf',          '2025-06-05 09:00:00.000', NULL, NULL, NULL, NULL, NULL, 30  ),
       ('INVOICE_DOCUMENT',      'uploads/1-INVOICE_DOCUMENT-9fb2db-INV-6-89753093.pdf',                'INV-6-89753093.pdf',          '2025-08-05 10:00:00.000', NULL, NULL, NULL, NULL, NULL, 32  ),
       ('INVOICE_DOCUMENT',      'uploads/1-INVOICE_DOCUMENT-51b99e-INV-7-70987660.pdf',                'INV-7-70987660.pdf',          '2025-08-02 09:00:00.000', NULL, NULL, NULL, NULL, NULL, 33  ),
       ('INVOICE_DOCUMENT',      'uploads/1-INVOICE_DOCUMENT-2dda1a-INV-8-52222227.pdf',                'INV-8-52222227.pdf',          '2025-08-06 10:00:00.000', NULL, NULL, NULL, NULL, NULL, 34  ),
       ('INVOICE_DOCUMENT',      'uploads/8-INVOICE_DOCUMENT-a262c9-INV-9-33456794.pdf',                'INV-9-33456794.pdf',          '2025-08-15 10:00:00.000', NULL, NULL, NULL, NULL, NULL, 36  ),
       ('INVOICE_DOCUMENT',      'uploads/5-INVOICE_DOCUMENT-b84f34-INV-10-14691361.pdf',               'INV-10-14691361.pdf',         '2025-08-20 09:00:00.000', NULL, NULL, NULL, NULL, NULL, 37  );

-- Recreate Functions
-- Session Setting for Functions
SET
    QUOTED_IDENTIFIER ON;
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
    SET
        NOCOUNT ON;
    SET
        XACT_ABORT ON;

    -- Reset Outputs
    SET
        @transaction_id = NULL;
    SET
        @transaction_code = NULL;
    SET
        @balance = NULL;

    -- Check Amount
    IF
        @amount IS NULL OR @amount <= 0.00
        BEGIN
            SET
                @status_code = 1;
            RETURN;
        END;

    BEGIN TRY
        -- Start Transaction
        BEGIN
            TRANSACTION;

        -- Find and Lock Client
        DECLARE
            @current_balance DECIMAL(38, 2);

        SELECT @current_balance = wallet_balance
        FROM users
        WITH (UPDLOCK, ROWLOCK)
        WHERE user_id = @client_id
          AND role = 'CLIENT'
          AND account_status = 'ACTIVE';

        IF
            @current_balance IS NULL
            BEGIN
                SET
                    @status_code = 2;

                ROLLBACK TRANSACTION;

                RETURN;

            END;

        -- Work Out Balance Change
        DECLARE
            @delta DECIMAL(38, 2);

        IF
            @transaction_type IN ('CHARGE', 'WITHDRAWAL')
            SET @delta = -@amount;
        ELSE
            SET @delta = @amount;

        -- Check Sufficient Funds
        IF
            @delta < 0.00 AND (@current_balance + @delta) < 0.00
            BEGIN
                SET
                    @status_code = 3;
                SET
                    @balance = @current_balance;

                ROLLBACK TRANSACTION;

                RETURN;

            END;

        -- Update Balance
        SET
            @balance = @current_balance + @delta;

        UPDATE users
        SET wallet_balance = @balance
        WHERE user_id = @client_id;

-- Insert Ledger Row
        INSERT INTO transactions (client_id, amount, transaction_type, invoice_id, occurred_at)
        VALUES (@client_id, @amount, @transaction_type, @invoice_id, @occurred_at);

-- Set Transaction Code
        SET
            @transaction_id = CAST(SCOPE_IDENTITY() AS BIGINT);
        SET
            @transaction_code = dbo.ufn_generate_transaction_code(@transaction_id);

        UPDATE transactions
        SET transaction_code = @transaction_code
        WHERE transaction_id = @transaction_id;

-- Commit and Report Success
        COMMIT TRANSACTION;

        SET
            @status_code = 0;
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

-- Recreate Trigger
-- Session Setting for Triggers
SET QUOTED_IDENTIFIER ON;
GO

-- Prevent Deletion of Client With Outstanding Wallet Balance Trigger
CREATE TRIGGER trg_users_au_prevent_deletion_of_client_with_outstanding_wallet_balance
    ON users
    AFTER
        UPDATE
    AS
BEGIN
    -- Trigger Options
    SET
        NOCOUNT ON;

    -- Find Deleted Clients With Outstanding Wallet Balance
    IF
        EXISTS (SELECT 1
                FROM inserted i
                INNER JOIN deleted d
                    ON d.user_id = i.user_id
                WHERE i.role = 'CLIENT'
                  AND d.account_status = 'ACTIVE'
                  AND i.account_status = 'DELETED'
                  AND i.wallet_balance > 0.00)
        BEGIN
            -- Reject the Update
            THROW
                50001, 'A client with an outstanding wallet balance cannot be deleted.', 1;
        END;
END;
GO

-- Prevent Deletion of Client, Associate, or Paralegal With an Active Consultation Trigger
CREATE TRIGGER trg_users_au_prevent_deletion_of_user_with_active_consultation
    ON users
    AFTER
        UPDATE
    AS
BEGIN
    -- Trigger Options
    SET
        NOCOUNT ON;

    -- Find Deleted Users Assigned to an Active Consultation
    IF
        EXISTS (SELECT 1
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
            THROW
                50002, 'A user assigned to an active consultation cannot be deleted.', 1;
        END;
END;
GO

-- Prevent Deletion of Client, Associate, or Paralegal With an Open Case Trigger
CREATE TRIGGER trg_users_au_prevent_deletion_of_user_with_open_case
    ON users
    AFTER
        UPDATE
    AS
BEGIN
    -- Trigger Options
    SET
        NOCOUNT ON;

    -- Find Deleted Users Assigned to an Open Case
    IF
        EXISTS (SELECT 1
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
            THROW
                50003, 'A user assigned to an open case cannot be deleted.', 1;
        END;
END;
GO

-- Prevent Deletion of Client With an Unpaid Invoice Trigger
CREATE TRIGGER trg_users_au_prevent_deletion_of_client_with_unpaid_invoice
    ON users
    AFTER
        UPDATE
    AS
BEGIN
    -- Trigger Options
    SET
        NOCOUNT ON;

    -- Find Deleted Clients With an Unpaid Invoice
    IF
        EXISTS (SELECT 1
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
            THROW
                50004, 'A client with an unpaid invoice cannot be deleted.', 1;
        END;
END;
GO
