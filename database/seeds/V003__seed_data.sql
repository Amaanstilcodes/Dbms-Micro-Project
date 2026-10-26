-- ============================================================
-- FILE    : V003__seed_data.sql
-- PURPOSE : Realistic sample data for all ISP tables.
--           20 customers, 6 plans, 20+ subscriptions,
--           3 months of invoices/payments/usage, 15 tickets.
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- RUN AS  : isp_app@//localhost:1521/XEPDB1
-- NOTES   : Oracle DATE literals use TO_DATE or DATE 'YYYY-MM-DD'.
--           Sequences and IDENTITY columns handle PKs automatically.
--           Script ends with explicit COMMIT.
-- ============================================================
SET SERVEROUTPUT ON SIZE UNLIMITED
SET FEEDBACK OFF

-- ===== SERVICE_AREA =====
INSERT INTO service_area (area_name, city, state_name, pin_code) VALUES ('Koramangala',     'Bangalore', 'Karnataka',   '560034');
INSERT INTO service_area (area_name, city, state_name, pin_code) VALUES ('Indiranagar',     'Bangalore', 'Karnataka',   '560038');
INSERT INTO service_area (area_name, city, state_name, pin_code) VALUES ('Whitefield',      'Bangalore', 'Karnataka',   '560066');
INSERT INTO service_area (area_name, city, state_name, pin_code) VALUES ('HSR Layout',      'Bangalore', 'Karnataka',   '560102');
INSERT INTO service_area (area_name, city, state_name, pin_code) VALUES ('Bandra West',     'Mumbai',    'Maharashtra', '400050');
INSERT INTO service_area (area_name, city, state_name, pin_code) VALUES ('Andheri East',    'Mumbai',    'Maharashtra', '400069');
INSERT INTO service_area (area_name, city, state_name, pin_code) VALUES ('Powai',           'Mumbai',    'Maharashtra', '400076');
INSERT INTO service_area (area_name, city, state_name, pin_code) VALUES ('Connaught Place', 'Delhi',     'Delhi',       '110001');
INSERT INTO service_area (area_name, city, state_name, pin_code) VALUES ('Lajpat Nagar',    'Delhi',     'Delhi',       '110024');
INSERT INTO service_area (area_name, city, state_name, pin_code) VALUES ('Sector 14',       'Gurgaon',   'Haryana',     '122001');

-- ===== PLAN (6 plans) =====
INSERT INTO plan (plan_name, plan_type, speed_mbps, upload_mbps, data_cap_gb, price, billing_cycle, description)
    VALUES ('Bronze 30',    'BROADBAND',  30,   10,   100, 399,  'MONTHLY', 'Entry-level broadband 100 GB/month');
INSERT INTO plan (plan_name, plan_type, speed_mbps, upload_mbps, data_cap_gb, price, billing_cycle, description)
    VALUES ('Silver 100',   'BROADBAND', 100,   40,   250, 699,  'MONTHLY', 'Mid-range broadband 250 GB/month');
INSERT INTO plan (plan_name, plan_type, speed_mbps, upload_mbps, data_cap_gb, price, billing_cycle, description)
    VALUES ('Gold 200',     'FIBER',     200,  100,  NULL, 999,  'MONTHLY', 'Fiber plan unlimited data');
INSERT INTO plan (plan_name, plan_type, speed_mbps, upload_mbps, data_cap_gb, price, billing_cycle, description)
    VALUES ('Platinum 500', 'FIBER',     500,  250,  NULL, 1499, 'MONTHLY', 'Premium fiber unlimited static IP');
INSERT INTO plan (plan_name, plan_type, speed_mbps, upload_mbps, data_cap_gb, price, billing_cycle, description)
    VALUES ('Business Pro', 'LEASED_LINE',1000,1000,  NULL, 4999, 'MONTHLY', 'Symmetric 1 Gbps leased line');
INSERT INTO plan (plan_name, plan_type, speed_mbps, upload_mbps, data_cap_gb, price, billing_cycle, description)
    VALUES ('Wireless 20',  'WIRELESS',   20,    5,    75, 299,  'MONTHLY', 'Wireless for rural areas');

-- ===== TECHNICIAN (5 technicians) =====
INSERT INTO technician (first_name, last_name, email, phone, specialization, area_id, hired_date)
    VALUES ('Rajesh', 'Kumar',   'rajesh.kumar@isp.com',   '9876543210', 'Fiber Optic',  1, DATE '2022-01-15');
INSERT INTO technician (first_name, last_name, email, phone, specialization, area_id, hired_date)
    VALUES ('Priya',  'Sharma',  'priya.sharma@isp.com',   '9876543211', 'Wireless',     5, DATE '2022-03-01');
INSERT INTO technician (first_name, last_name, email, phone, specialization, area_id, hired_date)
    VALUES ('Amit',   'Verma',   'amit.verma@isp.com',     '9876543212', 'VDSL/Copper',  8, DATE '2021-11-20');
INSERT INTO technician (first_name, last_name, email, phone, specialization, area_id, hired_date)
    VALUES ('Sunita', 'Patel',   'sunita.patel@isp.com',   '9876543213', 'Fiber Optic',  2, DATE '2023-06-01');
INSERT INTO technician (first_name, last_name, email, phone, specialization, area_id, hired_date)
    VALUES ('Vikram', 'Singh',   'vikram.singh@isp.com',   '9876543214', 'Network Admin',3, DATE '2020-08-15');

-- ===== CUSTOMER (20 customers) =====
INSERT INTO customer (customer_type,first_name,last_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('INDIVIDUAL','Arjun',  'Mehta',   'arjun.mehta@email.com',   '9811111111',1,'ACTIVE','AADHAAR','XXXX-XXXX-1001');
INSERT INTO customer (customer_type,first_name,last_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('INDIVIDUAL','Kavya',  'Reddy',   'kavya.reddy@email.com',   '9811111112',2,'ACTIVE','PAN',    'ABCDE1001F');
INSERT INTO customer (customer_type,first_name,last_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('INDIVIDUAL','Suresh', 'Nair',    'suresh.nair@email.com',   '9811111113',1,'ACTIVE','AADHAAR','XXXX-XXXX-1003');
INSERT INTO customer (customer_type,first_name,last_name,company_name,email,phone,area_id,status,id_proof_type,id_proof_number,gst_number)
    VALUES ('BUSINESS','Pooja','Gupta','TechVentures Pvt Ltd','pooja@techventures.in','9811111114',3,'ACTIVE','AADHAAR','XXXX-XXXX-1004','29AAACT1234C1Z5');
INSERT INTO customer (customer_type,first_name,last_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('INDIVIDUAL','Rahul',  'Joshi',   'rahul.joshi@email.com',   '9811111115',4,'ACTIVE','PASSPORT','P1234567');
INSERT INTO customer (customer_type,first_name,last_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('INDIVIDUAL','Anita',  'Desai',   'anita.desai@email.com',   '9811111116',5,'ACTIVE','AADHAAR','XXXX-XXXX-1006');
INSERT INTO customer (customer_type,first_name,last_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('INDIVIDUAL','Deepak', 'Shah',    'deepak.shah@email.com',   '9811111117',6,'INACTIVE','PAN','ABCDE1007F');
INSERT INTO customer (customer_type,first_name,last_name,company_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('BUSINESS','Meena','Kapoor','Kapoor Enterprises','meena@kapoorent.com','9811111118',7,'ACTIVE','AADHAAR','XXXX-XXXX-1008');
INSERT INTO customer (customer_type,first_name,last_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('INDIVIDUAL','Vikash', 'Tiwari',  'vikash.tiwari@email.com', '9811111119',8,'ACTIVE','AADHAAR','XXXX-XXXX-1009');
INSERT INTO customer (customer_type,first_name,last_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('INDIVIDUAL','Sneha',  'Pillai',  'sneha.pillai@email.com',  '9811111120',9,'ACTIVE','PAN','ABCDE1010F');
INSERT INTO customer (customer_type,first_name,last_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('INDIVIDUAL','Kiran',  'Bose',    'kiran.bose@email.com',    '9811111121',1,'ACTIVE','AADHAAR','XXXX-XXXX-1011');
INSERT INTO customer (customer_type,first_name,last_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('INDIVIDUAL','Ashok',  'Rao',     'ashok.rao@email.com',     '9811111122',2,'ACTIVE','AADHAAR','XXXX-XXXX-1012');
INSERT INTO customer (customer_type,first_name,last_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('INDIVIDUAL','Lakshmi','Iyer',    'lakshmi.iyer@email.com',  '9811111123',3,'ACTIVE','PAN','ABCDE1013F');
INSERT INTO customer (customer_type,first_name,last_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('INDIVIDUAL','Sanjay', 'Malhotra','sanjay.malhotra@email.com','9811111124',4,'BLACKLISTED','AADHAAR','XXXX-XXXX-1014');
INSERT INTO customer (customer_type,first_name,last_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('INDIVIDUAL','Geeta',  'Agarwal', 'geeta.agarwal@email.com', '9811111125',5,'ACTIVE','AADHAAR','XXXX-XXXX-1015');
INSERT INTO customer (customer_type,first_name,last_name,company_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('BUSINESS','Harish','Choudhury','Choudhury IT Ltd','harish@choudhury-it.com','9811111126',6,'ACTIVE','AADHAAR','XXXX-XXXX-1016');
INSERT INTO customer (customer_type,first_name,last_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('INDIVIDUAL','Nandita','Roy',     'nandita.roy@email.com',   '9811111127',7,'ACTIVE','PASSPORT','P7654321');
INSERT INTO customer (customer_type,first_name,last_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('INDIVIDUAL','Prakash','Shukla',  'prakash.shukla@email.com','9811111128',8,'ACTIVE','AADHAAR','XXXX-XXXX-1018');
INSERT INTO customer (customer_type,first_name,last_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('INDIVIDUAL','Yamini', 'Krishnan','yamini.k@email.com',      '9811111129',9,'ACTIVE','PAN','ABCDE1019F');
INSERT INTO customer (customer_type,first_name,last_name,email,phone,area_id,status,id_proof_type,id_proof_number)
    VALUES ('INDIVIDUAL','Rajeev', 'Pandey',  'rajeev.pandey@email.com', '9811111130',10,'ACTIVE','AADHAAR','XXXX-XXXX-1020');

-- ===== SUBSCRIPTION (20 subscriptions, started 3 months ago) =====
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (1, 3,1, ADD_MONTHS(TRUNC(SYSDATE),- 3),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (2, 2,2, ADD_MONTHS(TRUNC(SYSDATE),- 3),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (3, 1,1, ADD_MONTHS(TRUNC(SYSDATE),- 3),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (4, 5,3, ADD_MONTHS(TRUNC(SYSDATE),- 3),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (5, 4,4, ADD_MONTHS(TRUNC(SYSDATE),- 3),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (6, 2,5, ADD_MONTHS(TRUNC(SYSDATE),- 3),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (7, 1,6, ADD_MONTHS(TRUNC(SYSDATE),- 2),'SUSPENDED');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (8, 5,7, ADD_MONTHS(TRUNC(SYSDATE),- 3),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (9, 3,8, ADD_MONTHS(TRUNC(SYSDATE),- 3),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (10,2,9, ADD_MONTHS(TRUNC(SYSDATE),- 3),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (11,6,1, ADD_MONTHS(TRUNC(SYSDATE),- 1),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (12,3,2, ADD_MONTHS(TRUNC(SYSDATE),- 3),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (13,1,3, ADD_MONTHS(TRUNC(SYSDATE),- 2),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (15,4,5, ADD_MONTHS(TRUNC(SYSDATE),- 3),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (16,5,6, ADD_MONTHS(TRUNC(SYSDATE),- 3),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (17,2,7, ADD_MONTHS(TRUNC(SYSDATE),- 1),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (18,3,8, ADD_MONTHS(TRUNC(SYSDATE),- 3),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (19,1,9, ADD_MONTHS(TRUNC(SYSDATE),- 3),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,status) VALUES (20,2,10,ADD_MONTHS(TRUNC(SYSDATE),- 3),'ACTIVE');
INSERT INTO subscription (customer_id,plan_id,area_id,start_date,end_date,status) VALUES (1,4,1,ADD_MONTHS(TRUNC(SYSDATE),-6),ADD_MONTHS(TRUNC(SYSDATE),-3),'TERMINATED');

-- ===== EQUIPMENT =====
INSERT INTO equipment (equipment_type,brand,model,serial_number,mac_address,status,subscription_id,purchase_date)
    VALUES ('ONT','TP-Link','XS-230','TPLINK001','AA:BB:CC:DD:01:01','ASSIGNED',1,DATE '2023-01-01');
INSERT INTO equipment (equipment_type,brand,model,serial_number,mac_address,status,subscription_id,purchase_date)
    VALUES ('ROUTER','Netgear','AC2000','NETG001','AA:BB:CC:DD:02:01','ASSIGNED',2,DATE '2023-01-01');
INSERT INTO equipment (equipment_type,brand,model,serial_number,mac_address,status,subscription_id,purchase_date)
    VALUES ('MODEM','D-Link','DSL-2877','DLINK001','AA:BB:CC:DD:03:01','ASSIGNED',3,DATE '2023-01-01');
INSERT INTO equipment (equipment_type,brand,model,serial_number,mac_address,status,subscription_id,purchase_date)
    VALUES ('ONT','Huawei','HG8245','HW001','AA:BB:CC:DD:04:01','ASSIGNED',4,DATE '2023-01-01');
INSERT INTO equipment (equipment_type,brand,model,serial_number,mac_address,status,purchase_date)
    VALUES ('ROUTER','Asus','RT-AX88U','ASUS001','AA:BB:CC:DD:10:01','IN_STOCK',DATE '2024-01-01');
INSERT INTO equipment (equipment_type,brand,model,serial_number,mac_address,status,purchase_date)
    VALUES ('ONT','TP-Link','XS-230','TPLINK003','AA:BB:CC:DD:11:01','FAULTY',DATE '2022-06-01');

-- ===== INSTALLATION (one per subscription) =====
INSERT INTO installation (subscription_id,technician_id,scheduled_date,completed_date,status)
    VALUES (1,1,ADD_MONTHS(SYSDATE,-3),ADD_MONTHS(SYSDATE,-3),'COMPLETED');
INSERT INTO installation (subscription_id,technician_id,scheduled_date,completed_date,status)
    VALUES (2,4,ADD_MONTHS(SYSDATE,-3),ADD_MONTHS(SYSDATE,-3),'COMPLETED');
INSERT INTO installation (subscription_id,technician_id,scheduled_date,completed_date,status)
    VALUES (3,1,ADD_MONTHS(SYSDATE,-3),ADD_MONTHS(SYSDATE,-3),'COMPLETED');
INSERT INTO installation (subscription_id,technician_id,scheduled_date,completed_date,status)
    VALUES (4,5,ADD_MONTHS(SYSDATE,-3),ADD_MONTHS(SYSDATE,-3),'COMPLETED');
INSERT INTO installation (subscription_id,technician_id,scheduled_date,completed_date,status)
    VALUES (5,5,ADD_MONTHS(SYSDATE,-3),ADD_MONTHS(SYSDATE,-3),'COMPLETED');
INSERT INTO installation (subscription_id,technician_id,scheduled_date,completed_date,status)
    VALUES (6,2,ADD_MONTHS(SYSDATE,-3),ADD_MONTHS(SYSDATE,-3),'COMPLETED');
INSERT INTO installation (subscription_id,technician_id,scheduled_date,status)
    VALUES (7,2,ADD_MONTHS(SYSDATE,-2),'COMPLETED');
INSERT INTO installation (subscription_id,technician_id,scheduled_date,status)
    VALUES (11,1,ADD_MONTHS(SYSDATE,-1),'SCHEDULED');

-- ===== INVOICE (3 months for first 5 subscriptions) =====
-- Month -3 (all PAID)
INSERT INTO invoice (subscription_id,billing_period_start,billing_period_end,subtotal,tax_amount,total_amount,amount_paid,due_date,status,issued_at)
    VALUES (1,ADD_MONTHS(TRUNC(SYSDATE,'MM'),-3),ADD_MONTHS(TRUNC(SYSDATE,'MM'),-2)-1, 999,179.82,1178.82,1178.82,ADD_MONTHS(TRUNC(SYSDATE,'MM'),-2)+14,'PAID',ADD_MONTHS(SYSDATE,-3));
INSERT INTO invoice (subscription_id,billing_period_start,billing_period_end,subtotal,tax_amount,total_amount,amount_paid,due_date,status,issued_at)
    VALUES (2,ADD_MONTHS(TRUNC(SYSDATE,'MM'),-3),ADD_MONTHS(TRUNC(SYSDATE,'MM'),-2)-1, 699,125.82,824.82, 824.82, ADD_MONTHS(TRUNC(SYSDATE,'MM'),-2)+14,'PAID',ADD_MONTHS(SYSDATE,-3));
INSERT INTO invoice (subscription_id,billing_period_start,billing_period_end,subtotal,tax_amount,total_amount,amount_paid,due_date,status,issued_at)
    VALUES (3,ADD_MONTHS(TRUNC(SYSDATE,'MM'),-3),ADD_MONTHS(TRUNC(SYSDATE,'MM'),-2)-1, 399, 71.82, 470.82, 470.82, ADD_MONTHS(TRUNC(SYSDATE,'MM'),-2)+14,'PAID',ADD_MONTHS(SYSDATE,-3));
INSERT INTO invoice (subscription_id,billing_period_start,billing_period_end,subtotal,tax_amount,total_amount,amount_paid,due_date,status,issued_at)
    VALUES (4,ADD_MONTHS(TRUNC(SYSDATE,'MM'),-3),ADD_MONTHS(TRUNC(SYSDATE,'MM'),-2)-1,4999,899.82,5898.82,5898.82,ADD_MONTHS(TRUNC(SYSDATE,'MM'),-2)+14,'PAID',ADD_MONTHS(SYSDATE,-3));
-- Month -2 (mix of PAID and OVERDUE)
INSERT INTO invoice (subscription_id,billing_period_start,billing_period_end,subtotal,tax_amount,total_amount,amount_paid,due_date,status,issued_at)
    VALUES (1,ADD_MONTHS(TRUNC(SYSDATE,'MM'),-2),ADD_MONTHS(TRUNC(SYSDATE,'MM'),-1)-1, 999,179.82,1178.82,1178.82,ADD_MONTHS(TRUNC(SYSDATE,'MM'),-1)+14,'PAID',ADD_MONTHS(SYSDATE,-2));
INSERT INTO invoice (subscription_id,billing_period_start,billing_period_end,subtotal,tax_amount,total_amount,amount_paid,due_date,status,issued_at)
    VALUES (2,ADD_MONTHS(TRUNC(SYSDATE,'MM'),-2),ADD_MONTHS(TRUNC(SYSDATE,'MM'),-1)-1, 699,125.82,824.82, 500,    ADD_MONTHS(TRUNC(SYSDATE,'MM'),-1)+14,'PARTIALLY_PAID',ADD_MONTHS(SYSDATE,-2));
INSERT INTO invoice (subscription_id,billing_period_start,billing_period_end,subtotal,tax_amount,total_amount,amount_paid,due_date,status,issued_at)
    VALUES (3,ADD_MONTHS(TRUNC(SYSDATE,'MM'),-2),ADD_MONTHS(TRUNC(SYSDATE,'MM'),-1)-1, 399, 71.82, 470.82, 0,     ADD_MONTHS(TRUNC(SYSDATE,'MM'),-1)+14,'OVERDUE',ADD_MONTHS(SYSDATE,-2));
-- Month -1 (current cycle)
INSERT INTO invoice (subscription_id,billing_period_start,billing_period_end,subtotal,tax_amount,total_amount,amount_paid,due_date,status,issued_at)
    VALUES (1,ADD_MONTHS(TRUNC(SYSDATE,'MM'),-1),TRUNC(SYSDATE,'MM')-1, 999,179.82,1178.82, 0, TRUNC(SYSDATE,'MM')+14,'ISSUED',ADD_MONTHS(SYSDATE,-1));
INSERT INTO invoice (subscription_id,billing_period_start,billing_period_end,subtotal,tax_amount,total_amount,amount_paid,due_date,status,issued_at)
    VALUES (4,ADD_MONTHS(TRUNC(SYSDATE,'MM'),-1),TRUNC(SYSDATE,'MM')-1,4999,899.82,5898.82,5898.82,TRUNC(SYSDATE,'MM')+14,'PAID',ADD_MONTHS(SYSDATE,-1));

-- ===== PAYMENT =====
INSERT INTO payment (invoice_id,amount,payment_method,transaction_ref,payment_date)
    VALUES (1, 1178.82,'UPI',          'UPI20250101ABC', ADD_MONTHS(SYSDATE,-80/30));
INSERT INTO payment (invoice_id,amount,payment_method,transaction_ref,payment_date)
    VALUES (2, 824.82, 'BANK_TRANSFER','NEFT20250102XYZ',ADD_MONTHS(SYSDATE,-78/30));
INSERT INTO payment (invoice_id,amount,payment_method,payment_date)
    VALUES (3, 470.82, 'CASH',         ADD_MONTHS(SYSDATE,-79/30));
INSERT INTO payment (invoice_id,amount,payment_method,transaction_ref,payment_date)
    VALUES (4, 5898.82,'BANK_TRANSFER','RTGS20250103PQR',ADD_MONTHS(SYSDATE,-77/30));
INSERT INTO payment (invoice_id,amount,payment_method,transaction_ref,payment_date)
    VALUES (5, 1178.82,'UPI',          'UPI20250201DEF', ADD_MONTHS(SYSDATE,-50/30));
INSERT INTO payment (invoice_id,amount,payment_method,transaction_ref,payment_date)
    VALUES (6, 500.00, 'UPI',          'UPI20250202GHI', ADD_MONTHS(SYSDATE,-48/30));
INSERT INTO payment (invoice_id,amount,payment_method,transaction_ref,payment_date)
    VALUES (10,5898.82,'BANK_TRANSFER','RTGS20250301VWX',ADD_MONTHS(SYSDATE,-20/30));

-- ===== USAGE_RECORD (30 days for subscription 1) =====
DECLARE
    i NUMBER;
BEGIN
    FOR i IN 1..30 LOOP
        INSERT INTO usage_record (subscription_id, usage_date, data_used_mb)
        VALUES (1, TRUNC(SYSDATE) - i, ROUND(2000 + DBMS_RANDOM.VALUE * 5000, 2));
    END LOOP;
END;
/

-- ===== SUPPORT_TICKET (15 tickets) =====
INSERT INTO support_ticket (customer_id,subscription_id,category,priority,subject,description,status,assigned_to)
    VALUES (1,1,'CONNECTIVITY','HIGH','No internet after power cut','Internet not working after power outage.','RESOLVED',1);
INSERT INTO support_ticket (customer_id,subscription_id,category,priority,subject,description,status,assigned_to)
    VALUES (2,2,'SPEED','MEDIUM','Speed lower than plan','Getting only 40 Mbps on 100 Mbps plan.','IN_PROGRESS',4);
INSERT INTO support_ticket (customer_id,subscription_id,category,priority,subject,description,status)
    VALUES (3,3,'BILLING','LOW','Wrong bill amount','My bill shows extra charges.','OPEN');
INSERT INTO support_ticket (customer_id,subscription_id,category,priority,subject,description,status,assigned_to)
    VALUES (4,4,'EQUIPMENT','HIGH','ONT red light','ONT showing red light since morning.','RESOLVED',1);
INSERT INTO support_ticket (customer_id,subscription_id,category,priority,subject,description,status,assigned_to)
    VALUES (5,5,'CONNECTIVITY','CRITICAL','Complete outage 6 hours','No connection for 6+ hours.','IN_PROGRESS',5);
INSERT INTO support_ticket (customer_id,subscription_id,category,priority,subject,status)
    VALUES (6,6,'SPEED','MEDIUM','Intermittent connectivity','OPEN');
INSERT INTO support_ticket (customer_id,subscription_id,category,priority,subject,status)
    VALUES (9,9,'BILLING','LOW','Request payment receipt','RESOLVED');
INSERT INTO support_ticket (customer_id,subscription_id,category,priority,subject,status,assigned_to)
    VALUES (10,10,'CONNECTIVITY','MEDIUM','Slow speeds evening','PENDING_CUSTOMER',4);
INSERT INTO support_ticket (customer_id,subscription_id,category,priority,subject,status,assigned_to)
    VALUES (11,11,'EQUIPMENT','HIGH','Router not working','IN_PROGRESS',1);
INSERT INTO support_ticket (customer_id,subscription_id,category,priority,subject,status)
    VALUES (12,12,'SPEED','LOW','Speed test inconsistent','OPEN');
INSERT INTO support_ticket (customer_id,subscription_id,category,priority,subject,status,assigned_to)
    VALUES (13,13,'CONNECTIVITY','MEDIUM','Cannot connect after plan change','RESOLVED',5);
INSERT INTO support_ticket (customer_id,subscription_id,category,priority,subject,status)
    VALUES (15,14,'BILLING','HIGH','Duplicate charge on account','IN_PROGRESS');
INSERT INTO support_ticket (customer_id,subscription_id,category,priority,subject,status)
    VALUES (18,17,'EQUIPMENT','LOW','Request router upgrade','OPEN');
INSERT INTO support_ticket (customer_id,subscription_id,category,priority,subject,status,assigned_to)
    VALUES (19,18,'SPEED','MEDIUM','Upload speed very slow','OPEN',4);
INSERT INTO support_ticket (customer_id,subscription_id,category,priority,subject,status)
    VALUES (20,19,'OTHER','LOW','Plan upgrade request','OPEN');

-- ===== NETWORK_OUTAGE =====
INSERT INTO network_outage (title,cause,area_id,severity,start_time,end_time,resolved_by)
    VALUES ('Koramangala Fiber Cut','Underground cable cut during road construction',1,'HIGH',SYSDATE-10,SYSDATE-9,1);
INSERT INTO network_outage (title,cause,area_id,severity,start_time,resolved_by)
    VALUES ('Whitefield Node Failure','OLT hardware failure at main node',3,'CRITICAL',SYSDATE-3,5);

-- ===== APP_USER (login accounts; BCrypt hashes must be updated via PasswordUtil) =====
INSERT INTO app_user (username,password_hash,role) VALUES ('admin',  '$2a$12$REPLACE_HASH_admin',  'ADMIN');
INSERT INTO app_user (username,password_hash,role) VALUES ('billing1','$2a$12$REPLACE_HASH_billing','BILLING_CLERK');
INSERT INTO app_user (username,password_hash,role,technician_id) VALUES ('tech.rajesh','$2a$12$REPLACE_HASH_tech','TECHNICIAN',1);
INSERT INTO app_user (username,password_hash,role,technician_id) VALUES ('support.priya','$2a$12$REPLACE_HASH_supp','SUPPORT_AGENT',2);

COMMIT;
SET FEEDBACK ON
PROMPT Seed data inserted successfully.
PROMPT NOTE: Update APP_USER password_hash values by running PasswordUtil.main() from Java.
