-- Session Setting for Filtered Unique Indexes
SET QUOTED_IDENTIFIER ON;

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
