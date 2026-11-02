-- ============================================================================
-- SEED SCRIPT: seed_sample_customers.sql
-- PURPOSE: Inserts ~10 diverse sample customers with a mix of INDIVIDUAL and
--          BUSINESS accounts across ACTIVE, INACTIVE, and BLACKLISTED statuses.
--          Fully re-runnable with MERGE / idempotency checks.
-- RUN VIA SQL*PLUS:
--   sqlplus isp_app/"ISP_Secure#2025"@//localhost:1521/FREEPDB1 @database/seeds/seed_sample_customers.sql
-- ============================================================================

SET FEEDBACK ON;
SET SERVEROUTPUT ON;

PROMPT [INFO] Seeding diverse sample customers into Oracle 23ai...

DECLARE
    v_area_1 NUMBER;
    v_area_2 NUMBER;
    v_area_3 NUMBER;

    PROCEDURE upsert_customer(
        p_type        VARCHAR2,
        p_first       VARCHAR2,
        p_last        VARCHAR2,
        p_comp        VARCHAR2,
        p_email       VARCHAR2,
        p_phone       VARCHAR2,
        p_alt         VARCHAR2,
        p_area        NUMBER,
        p_id_type     VARCHAR2,
        p_id_num      VARCHAR2,
        p_gst         VARCHAR2,
        p_status      VARCHAR2
    ) IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count FROM customer WHERE LOWER(email) = LOWER(p_email);
        IF v_count = 0 THEN
            INSERT INTO customer (
                customer_type, first_name, last_name, company_name, email, phone,
                alternate_phone, area_id, status, id_proof_type, id_proof_number, gst_number
            ) VALUES (
                p_type, p_first, p_last, p_comp, p_email, p_phone,
                p_alt, p_area, p_status, p_id_type, p_id_num, p_gst
            );
        ELSE
            UPDATE customer
               SET customer_type   = p_type,
                   first_name      = p_first,
                   last_name       = p_last,
                   company_name    = p_comp,
                   phone           = p_phone,
                   alternate_phone = p_alt,
                   area_id         = p_area,
                   status          = p_status,
                   id_proof_type   = p_id_type,
                   id_proof_number = p_id_num,
                   gst_number      = p_gst
             WHERE LOWER(email)    = LOWER(p_email);
        END IF;
    END;

BEGIN
    -- Resolve valid area IDs
    SELECT NVL(MIN(area_id), 1) INTO v_area_1 FROM service_area;
    SELECT NVL(MAX(area_id), v_area_1) INTO v_area_2 FROM service_area;
    v_area_3 := v_area_1;

    -- 1. Individual - Active
    upsert_customer('INDIVIDUAL', 'Aditya', 'Verma', NULL,
                    'aditya.verma@example.com', '9811122334', '9811122335',
                    v_area_1, 'AADHAAR', '1234-5678-9012', NULL, 'ACTIVE');

    -- 2. Individual - Active
    upsert_customer('INDIVIDUAL', 'Sunita', 'Rao', NULL,
                    'sunita.rao@example.com', '9822233445', NULL,
                    v_area_2, 'PAN', 'ABCDE1234F', NULL, 'ACTIVE');

    -- 3. Individual - Inactive (Account cancelled)
    upsert_customer('INDIVIDUAL', 'Kunal', 'Mehta', NULL,
                    'kunal.mehta@example.com', '9833344556', '9833344557',
                    v_area_1, 'PASSPORT', 'P9876543', NULL, 'INACTIVE');

    -- 4. Individual - Blacklisted (Defaulted on payments)
    upsert_customer('INDIVIDUAL', 'Rohan', 'Bansal', NULL,
                    'rohan.bansal@example.com', '9844455667', NULL,
                    v_area_2, 'VOTER_ID', 'VOT88776655', NULL, 'BLACKLISTED');

    -- 5. Business - Active (Tech Startup)
    upsert_customer('BUSINESS', 'Ananya', 'Deshmukh', 'Apex Cloud Solutions Pvt Ltd',
                    'accounts@apexcloud.io', '9855566778', '9855566779',
                    v_area_1, 'PAN', 'AAACA1234H', '27AAACA1234H1Z5', 'ACTIVE');

    -- 6. Business - Active (Logistics Firm)
    upsert_customer('BUSINESS', 'Vikram', 'Singhania', 'SpeedCargo Freight LLP',
                    'billing@speedcargo.in', '9866677889', NULL,
                    v_area_2, 'PAN', 'AABCS9988G', '27AABCS9988G1ZQ', 'ACTIVE');

    -- 7. Business - Inactive (Migrated / Dormant)
    upsert_customer('BUSINESS', 'Nandini', 'Kapoor', 'Metro Retailers India',
                    'contact@metroretail.com', '9877788990', '9877788991',
                    v_area_1, 'PAN', 'AABCM7766D', '27AABCM7766D1Z2', 'INACTIVE');

    -- 8. Business - Blacklisted (Fraudulent chargebacks)
    upsert_customer('BUSINESS', 'Deepak', 'Chawla', 'Global Finconsult Trading',
                    'finance@globalfin.biz', '9888899001', NULL,
                    v_area_2, 'PAN', 'AABCG5544K', '27AABCG5544K1ZM', 'BLACKLISTED');

    -- 9. Individual - Active (High tier residential)
    upsert_customer('INDIVIDUAL', 'Pooja', 'Nair', NULL,
                    'pooja.nair@example.com', '9899900112', '9899900113',
                    v_area_1, 'AADHAAR', '5566-7788-9900', NULL, 'ACTIVE');

    -- 10. Individual - Active (FTTH customer)
    upsert_customer('INDIVIDUAL', 'Manish', 'Tiwari', NULL,
                    'manish.tiwari@example.com', '9800011223', NULL,
                    v_area_2, 'PAN', 'AKLPT9081J', NULL, 'ACTIVE');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('[SUCCESS] 10 diverse sample customers seeded successfully.');
END;
/
