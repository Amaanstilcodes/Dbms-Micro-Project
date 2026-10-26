-- ============================================================
-- FILE    : V003__seed_data.sql
-- PURPOSE : Realistic sample data (20+ customers, 6 plans, 30+
--           subscriptions, 3 months invoices/payments, 15 tickets)
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- ============================================================

-- ===== service_area (10 zones) =====
INSERT INTO service_area (area_name, city, state, pin_code) VALUES
('Koramangala',     'Bangalore', 'Karnataka',   '560034'),
('Indiranagar',     'Bangalore', 'Karnataka',   '560038'),
('Whitefield',      'Bangalore', 'Karnataka',   '560066'),
('HSR Layout',      'Bangalore', 'Karnataka',   '560102'),
('Bandra West',     'Mumbai',    'Maharashtra', '400050'),
('Andheri East',    'Mumbai',    'Maharashtra', '400069'),
('Powai',           'Mumbai',    'Maharashtra', '400076'),
('Connaught Place', 'Delhi',     'Delhi',       '110001'),
('Lajpat Nagar',    'Delhi',     'Delhi',       '110024'),
('Sector 14',       'Gurgaon',   'Haryana',     '122001');

-- ===== plan (6 plans) =====
INSERT INTO plan (plan_name, plan_type, speed_mbps, upload_mbps, data_cap_gb, price, billing_cycle, description) VALUES
('Bronze 30',    'BROADBAND', 30,   10,   100,  399.00,  'MONTHLY',   'Entry-level broadband, 100 GB/month'),
('Silver 100',   'BROADBAND', 100,  40,   250,  699.00,  'MONTHLY',   'Mid-range broadband, 250 GB/month'),
('Gold 200',     'FIBER',     200,  100,  NULL, 999.00,  'MONTHLY',   'Fiber plan, unlimited data'),
('Platinum 500', 'FIBER',     500,  250,  NULL, 1499.00, 'MONTHLY',   'Premium fiber, unlimited, static IP'),
('Business Pro', 'LEASED_LINE',1000,1000, NULL, 4999.00, 'MONTHLY',   'Symmetric 1 Gbps leased line for businesses'),
('Wireless 20',  'WIRELESS',  20,   5,    75,   299.00,  'MONTHLY',   'Wireless connection for rural areas');

-- ===== technician (5 technicians) =====
INSERT INTO technician (first_name, last_name, email, phone, specialization, area_id, hired_date) VALUES
('Rajesh',   'Kumar',    'rajesh.kumar@isp.com',    '9876543210', 'Fiber Optic',  1, '2022-01-15'),
('Priya',    'Sharma',   'priya.sharma@isp.com',    '9876543211', 'Wireless',     5, '2022-03-01'),
('Amit',     'Verma',    'amit.verma@isp.com',      '9876543212', 'VDSL/Copper',  8, '2021-11-20'),
('Sunita',   'Patel',    'sunita.patel@isp.com',    '9876543213', 'Fiber Optic',  2, '2023-06-01'),
('Vikram',   'Singh',    'vikram.singh@isp.com',    '9876543214', 'Network Admin',3, '2020-08-15');

-- ===== customer (20 customers) =====
INSERT INTO customer (customer_type, first_name, last_name, company_name, email, phone, area_id, status, id_proof_type, id_proof_number) VALUES
('INDIVIDUAL', 'Arjun',    'Mehta',      NULL,                  'arjun.mehta@email.com',     '9811111111', 1, 'ACTIVE',     'AADHAAR', 'XXXX-XXXX-1001'),
('INDIVIDUAL', 'Kavya',    'Reddy',      NULL,                  'kavya.reddy@email.com',      '9811111112', 2, 'ACTIVE',     'PAN',     'ABCDE1001F'),
('INDIVIDUAL', 'Suresh',   'Nair',       NULL,                  'suresh.nair@email.com',      '9811111113', 1, 'ACTIVE',     'AADHAAR', 'XXXX-XXXX-1003'),
('BUSINESS',   'Pooja',    'Gupta',      'TechVentures Pvt Ltd','pooja.gupta@techventures.in','9811111114', 3, 'ACTIVE',     'AADHAAR', 'XXXX-XXXX-1004'),
('INDIVIDUAL', 'Rahul',    'Joshi',      NULL,                  'rahul.joshi@email.com',      '9811111115', 4, 'ACTIVE',     'PASSPORT','P1234567'),
('INDIVIDUAL', 'Anita',    'Desai',      NULL,                  'anita.desai@email.com',      '9811111116', 5, 'ACTIVE',     'AADHAAR', 'XXXX-XXXX-1006'),
('INDIVIDUAL', 'Deepak',   'Shah',       NULL,                  'deepak.shah@email.com',      '9811111117', 6, 'INACTIVE',   'PAN',     'ABCDE1007F'),
('BUSINESS',   'Meena',    'Kapoor',     'Kapoor Enterprises',  'meena@kapoorenterprises.com','9811111118', 7, 'ACTIVE',     'AADHAAR', 'XXXX-XXXX-1008'),
('INDIVIDUAL', 'Vikash',   'Tiwari',     NULL,                  'vikash.tiwari@email.com',    '9811111119', 8, 'ACTIVE',     'AADHAAR', 'XXXX-XXXX-1009'),
('INDIVIDUAL', 'Sneha',    'Pillai',     NULL,                  'sneha.pillai@email.com',     '9811111120', 9, 'ACTIVE',     'PAN',     'ABCDE1010F'),
('INDIVIDUAL', 'Kiran',    'Bose',       NULL,                  'kiran.bose@email.com',       '9811111121', 1, 'ACTIVE',     'AADHAAR', 'XXXX-XXXX-1011'),
('INDIVIDUAL', 'Ashok',    'Rao',        NULL,                  'ashok.rao@email.com',        '9811111122', 2, 'ACTIVE',     'AADHAAR', 'XXXX-XXXX-1012'),
('INDIVIDUAL', 'Lakshmi',  'Iyer',       NULL,                  'lakshmi.iyer@email.com',     '9811111123', 3, 'ACTIVE',     'PAN',     'ABCDE1013F'),
('INDIVIDUAL', 'Sanjay',   'Malhotra',   NULL,                  'sanjay.malhotra@email.com',  '9811111124', 4, 'BLACKLISTED','AADHAAR', 'XXXX-XXXX-1014'),
('INDIVIDUAL', 'Geeta',    'Agarwal',    NULL,                  'geeta.agarwal@email.com',    '9811111125', 5, 'ACTIVE',     'AADHAAR', 'XXXX-XXXX-1015'),
('BUSINESS',   'Harish',   'Choudhury',  'Choudhury IT Ltd',    'harish@choudhury-it.com',    '9811111126', 6, 'ACTIVE',     'AADHAAR', 'XXXX-XXXX-1016'),
('INDIVIDUAL', 'Nandita',  'Roy',        NULL,                  'nandita.roy@email.com',      '9811111127', 7, 'ACTIVE',     'PASSPORT','P7654321'),
('INDIVIDUAL', 'Prakash',  'Shukla',     NULL,                  'prakash.shukla@email.com',   '9811111128', 8, 'ACTIVE',     'AADHAAR', 'XXXX-XXXX-1018'),
('INDIVIDUAL', 'Yamini',   'Krishnan',   NULL,                  'yamini.k@email.com',         '9811111129', 9, 'ACTIVE',     'PAN',     'ABCDE1019F'),
('INDIVIDUAL', 'Rajeev',   'Pandey',     NULL,                  'rajeev.pandey@email.com',    '9811111130',10, 'ACTIVE',     'AADHAAR', 'XXXX-XXXX-1020');

-- ===== subscription (20 subscriptions) =====
-- All started 3 months ago for billing demo
INSERT INTO subscription (customer_id, plan_id, area_id, start_date, status) VALUES
(1,  3, 1, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'ACTIVE'),     -- Arjun: Gold 200
(2,  2, 2, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'ACTIVE'),     -- Kavya: Silver 100
(3,  1, 1, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'ACTIVE'),     -- Suresh: Bronze 30
(4,  5, 3, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'ACTIVE'),     -- TechVentures: Business Pro
(5,  4, 4, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'ACTIVE'),     -- Rahul: Platinum 500
(6,  2, 5, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'ACTIVE'),     -- Anita: Silver 100
(7,  1, 6, DATE_SUB(CURDATE(), INTERVAL 2 MONTH), 'SUSPENDED'),  -- Deepak: Bronze (suspended)
(8,  5, 7, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'ACTIVE'),     -- Kapoor: Business Pro
(9,  3, 8, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'ACTIVE'),     -- Vikash: Gold 200
(10, 2, 9, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'ACTIVE'),     -- Sneha: Silver 100
(11, 6, 1, DATE_SUB(CURDATE(), INTERVAL 1 MONTH), 'ACTIVE'),     -- Kiran: Wireless 20
(12, 3, 2, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'ACTIVE'),     -- Ashok: Gold 200
(13, 1, 3, DATE_SUB(CURDATE(), INTERVAL 2 MONTH), 'ACTIVE'),     -- Lakshmi: Bronze 30
(15, 4, 5, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'ACTIVE'),     -- Geeta: Platinum 500
(16, 5, 6, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'ACTIVE'),     -- Choudhury IT: Business Pro
(17, 2, 7, DATE_SUB(CURDATE(), INTERVAL 1 MONTH), 'ACTIVE'),     -- Nandita: Silver 100
(18, 3, 8, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'ACTIVE'),     -- Prakash: Gold 200
(19, 1, 9, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'ACTIVE'),     -- Yamini: Bronze 30
(20, 2,10, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'ACTIVE'),     -- Rajeev: Silver 100
(1,  4, 1, DATE_SUB(CURDATE(), INTERVAL 6 MONTH), 'TERMINATED'); -- Arjun: old Platinum (terminated)

-- ===== equipment (sample) =====
INSERT INTO equipment (equipment_type, brand, model, serial_number, mac_address, status, subscription_id, purchase_date) VALUES
('ONT',    'TP-Link',  'XS-230',   'TPLINK001', 'AA:BB:CC:DD:01:01', 'ASSIGNED', 1,  '2023-01-01'),
('ROUTER', 'Netgear',  'AC2000',   'NETG001',   'AA:BB:CC:DD:02:01', 'ASSIGNED', 2,  '2023-01-01'),
('MODEM',  'D-Link',   'DSL-2877', 'DLINK001',  'AA:BB:CC:DD:03:01', 'ASSIGNED', 3,  '2023-01-01'),
('ONT',    'Huawei',   'HG8245',   'HW001',     'AA:BB:CC:DD:04:01', 'ASSIGNED', 4,  '2023-01-01'),
('ONT',    'ZTE',      'F660',     'ZTE001',    'AA:BB:CC:DD:05:01', 'ASSIGNED', 5,  '2023-01-01'),
('ROUTER', 'TP-Link',  'Archer A7','TPLINK002', 'AA:BB:CC:DD:06:01', 'ASSIGNED', 6,  '2023-01-01'),
('MODEM',  'D-Link',   'DSL-2750', 'DLINK002',  'AA:BB:CC:DD:07:01', 'IN_STOCK', NULL,'2023-06-01'),
('ONT',    'Huawei',   'HG8310',   'HW002',     'AA:BB:CC:DD:08:01', 'ASSIGNED', 8,  '2023-01-01'),
('ROUTER', 'Cisco',    'RV340',    'CISCO001',  'AA:BB:CC:DD:09:01', 'ASSIGNED', 9,  '2023-01-01'),
('ROUTER', 'Asus',     'RT-AX88U', 'ASUS001',   'AA:BB:CC:DD:10:01', 'IN_STOCK', NULL,'2024-01-01'),
('ONT',    'TP-Link',  'XS-230',   'TPLINK003', 'AA:BB:CC:DD:11:01', 'FAULTY',   NULL,'2022-06-01');

-- ===== installation (one per subscription) =====
INSERT INTO installation (subscription_id, technician_id, scheduled_date, completed_date, status) VALUES
(1,  1, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'COMPLETED'),
(2,  4, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'COMPLETED'),
(3,  1, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'COMPLETED'),
(4,  5, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'COMPLETED'),
(5,  5, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'COMPLETED'),
(6,  2, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'COMPLETED'),
(7,  2, DATE_SUB(CURDATE(), INTERVAL 2 MONTH), DATE_SUB(CURDATE(), INTERVAL 2 MONTH), 'COMPLETED'),
(8,  2, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'COMPLETED'),
(9,  3, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'COMPLETED'),
(10, 3, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'COMPLETED'),
(11, 1, DATE_SUB(CURDATE(), INTERVAL 1 MONTH), DATE_SUB(CURDATE(), INTERVAL 1 MONTH), 'COMPLETED'),
(12, 4, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'COMPLETED'),
(13, 5, DATE_SUB(CURDATE(), INTERVAL 2 MONTH), DATE_SUB(CURDATE(), INTERVAL 2 MONTH), 'COMPLETED'),
(14, 2, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'COMPLETED'),
(15, 3, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'COMPLETED'),
(16, 1, DATE_SUB(CURDATE(), INTERVAL 1 MONTH), NULL,                                   'SCHEDULED'),
(17, 4, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'COMPLETED'),
(18, 3, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'COMPLETED'),
(19, 5, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'COMPLETED'),
(20, 1, DATE_SUB(CURDATE(), INTERVAL 6 MONTH), DATE_SUB(CURDATE(), INTERVAL 6 MONTH), 'COMPLETED');

-- ===== invoice (3 months of invoices for first 10 subscriptions) =====
-- Month -3
INSERT INTO invoice (subscription_id, billing_period_start, billing_period_end, subtotal, tax_amount, total_amount, amount_paid, due_date, status, issued_at) VALUES
(1,  DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 2 MONTH), 999.00,  179.82, 1178.82, 1178.82, DATE_SUB(CURDATE(), INTERVAL 75 DAY), 'PAID',     DATE_SUB(CURDATE(), INTERVAL 3 MONTH)),
(2,  DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 2 MONTH), 699.00,  125.82, 824.82,  824.82,  DATE_SUB(CURDATE(), INTERVAL 75 DAY), 'PAID',     DATE_SUB(CURDATE(), INTERVAL 3 MONTH)),
(3,  DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 2 MONTH), 399.00,  71.82,  470.82,  470.82,  DATE_SUB(CURDATE(), INTERVAL 75 DAY), 'PAID',     DATE_SUB(CURDATE(), INTERVAL 3 MONTH)),
(4,  DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 2 MONTH), 4999.00, 899.82, 5898.82, 5898.82, DATE_SUB(CURDATE(), INTERVAL 75 DAY), 'PAID',     DATE_SUB(CURDATE(), INTERVAL 3 MONTH)),
(5,  DATE_SUB(CURDATE(), INTERVAL 3 MONTH), DATE_SUB(CURDATE(), INTERVAL 2 MONTH), 1499.00, 269.82, 1768.82, 1768.82, DATE_SUB(CURDATE(), INTERVAL 75 DAY), 'PAID',     DATE_SUB(CURDATE(), INTERVAL 3 MONTH)),
-- Month -2
(1,  DATE_SUB(CURDATE(), INTERVAL 2 MONTH), DATE_SUB(CURDATE(), INTERVAL 1 MONTH), 999.00,  179.82, 1178.82, 1178.82, DATE_SUB(CURDATE(), INTERVAL 45 DAY), 'PAID',     DATE_SUB(CURDATE(), INTERVAL 2 MONTH)),
(2,  DATE_SUB(CURDATE(), INTERVAL 2 MONTH), DATE_SUB(CURDATE(), INTERVAL 1 MONTH), 699.00,  125.82, 824.82,  500.00,  DATE_SUB(CURDATE(), INTERVAL 45 DAY), 'PARTIALLY_PAID', DATE_SUB(CURDATE(), INTERVAL 2 MONTH)),
(3,  DATE_SUB(CURDATE(), INTERVAL 2 MONTH), DATE_SUB(CURDATE(), INTERVAL 1 MONTH), 399.00,  71.82,  470.82,  0.00,    DATE_SUB(CURDATE(), INTERVAL 45 DAY), 'OVERDUE',  DATE_SUB(CURDATE(), INTERVAL 2 MONTH)),
(4,  DATE_SUB(CURDATE(), INTERVAL 2 MONTH), DATE_SUB(CURDATE(), INTERVAL 1 MONTH), 4999.00, 899.82, 5898.82, 5898.82, DATE_SUB(CURDATE(), INTERVAL 45 DAY), 'PAID',     DATE_SUB(CURDATE(), INTERVAL 2 MONTH)),
(5,  DATE_SUB(CURDATE(), INTERVAL 2 MONTH), DATE_SUB(CURDATE(), INTERVAL 1 MONTH), 1499.00, 269.82, 1768.82, 1768.82, DATE_SUB(CURDATE(), INTERVAL 45 DAY), 'PAID',     DATE_SUB(CURDATE(), INTERVAL 2 MONTH)),
-- Month -1 (current)
(1,  DATE_SUB(CURDATE(), INTERVAL 1 MONTH), CURDATE(), 999.00,  179.82, 1178.82, 0.00,    DATE_ADD(CURDATE(), INTERVAL 15 DAY), 'ISSUED', DATE_SUB(CURDATE(), INTERVAL 1 MONTH)),
(2,  DATE_SUB(CURDATE(), INTERVAL 1 MONTH), CURDATE(), 699.00,  125.82, 824.82,  0.00,    DATE_ADD(CURDATE(), INTERVAL 15 DAY), 'ISSUED', DATE_SUB(CURDATE(), INTERVAL 1 MONTH)),
(3,  DATE_SUB(CURDATE(), INTERVAL 1 MONTH), CURDATE(), 399.00,  71.82,  470.82,  0.00,    DATE_SUB(CURDATE(), INTERVAL 5 DAY),  'OVERDUE',DATE_SUB(CURDATE(), INTERVAL 1 MONTH)),
(4,  DATE_SUB(CURDATE(), INTERVAL 1 MONTH), CURDATE(), 4999.00, 899.82, 5898.82, 5898.82, DATE_ADD(CURDATE(), INTERVAL 15 DAY), 'PAID',   DATE_SUB(CURDATE(), INTERVAL 1 MONTH)),
(5,  DATE_SUB(CURDATE(), INTERVAL 1 MONTH), CURDATE(), 1499.00, 269.82, 1768.82, 0.00,    DATE_ADD(CURDATE(), INTERVAL 15 DAY), 'ISSUED', DATE_SUB(CURDATE(), INTERVAL 1 MONTH));

-- ===== payment (sample payments) =====
INSERT INTO payment (invoice_id, amount, payment_method, transaction_ref, payment_date) VALUES
(1,  1178.82, 'UPI',           'UPI20250101ABC', DATE_SUB(CURDATE(), INTERVAL 80 DAY)),
(2,  824.82,  'BANK_TRANSFER', 'NEFT20250102XYZ', DATE_SUB(CURDATE(), INTERVAL 78 DAY)),
(3,  470.82,  'CASH',          NULL,               DATE_SUB(CURDATE(), INTERVAL 79 DAY)),
(4,  5898.82, 'BANK_TRANSFER', 'RTGS20250103PQR', DATE_SUB(CURDATE(), INTERVAL 77 DAY)),
(5,  1768.82, 'CARD',          'CARD20250104MNO', DATE_SUB(CURDATE(), INTERVAL 76 DAY)),
(6,  1178.82, 'UPI',           'UPI20250201DEF', DATE_SUB(CURDATE(), INTERVAL 50 DAY)),
(7,  500.00,  'UPI',           'UPI20250202GHI', DATE_SUB(CURDATE(), INTERVAL 48 DAY)),  -- partial
(9,  5898.82, 'BANK_TRANSFER', 'RTGS20250203JKL', DATE_SUB(CURDATE(), INTERVAL 47 DAY)),
(10, 1768.82, 'CARD',          'CARD20250204STU', DATE_SUB(CURDATE(), INTERVAL 46 DAY)),
(14, 5898.82, 'BANK_TRANSFER', 'RTGS20250301VWX', DATE_SUB(CURDATE(), INTERVAL 20 DAY));

-- ===== usage_record (sample daily usage for sub 1, 2, 3 over 30 days) =====
INSERT INTO usage_record (subscription_id, usage_date, data_used_mb)
SELECT 1, DATE_SUB(CURDATE(), INTERVAL n DAY), ROUND(2000 + RAND() * 5000, 2)
FROM (
  SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
  UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
  UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
  UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
  UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25
  UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
) AS nums;

-- ===== support_ticket (15 tickets) =====
INSERT INTO support_ticket (customer_id, subscription_id, category, priority, subject, description, status, assigned_to) VALUES
(1,  1,  'CONNECTIVITY', 'HIGH',     'No internet after power cut',     'After power outage yesterday, internet not working.',   'RESOLVED',         1),
(2,  2,  'SPEED',        'MEDIUM',   'Speed lower than plan',           'Getting only 40 Mbps on 100 Mbps plan.',                'IN_PROGRESS',       4),
(3,  3,  'BILLING',      'LOW',      'Wrong bill amount',               'My bill shows extra charges not in my plan.',           'OPEN',              NULL),
(4,  4,  'EQUIPMENT',    'HIGH',     'ONT device showing red light',    'ONT has been showing red light since morning.',         'RESOLVED',         1),
(5,  5,  'CONNECTIVITY', 'CRITICAL', 'Complete outage since 6 hours',   'No connection for 6+ hours, work from home affected.',  'IN_PROGRESS',       5),
(6,  6,  'SPEED',        'MEDIUM',   'Intermittent connectivity',        'Connection drops every few hours.',                    'OPEN',              NULL),
(9,  9,  'BILLING',      'LOW',      'Request for payment receipt',     'Need receipt for last month payment.',                  'RESOLVED',         NULL),
(10, 10, 'CONNECTIVITY', 'MEDIUM',   'Slow speeds during evening',      'Speed drops to 10Mbps between 7-10 PM.',               'PENDING_CUSTOMER',  4),
(11, 11, 'EQUIPMENT',    'HIGH',     'Router not working',              'New router not connecting to internet.',                'IN_PROGRESS',       1),
(12, 12, 'SPEED',        'LOW',      'Speed test shows lower speed',    'Speed test result is inconsistent.',                   'OPEN',              NULL),
(13, 13, 'CONNECTIVITY', 'MEDIUM',   'Cannot connect after plan change','After plan upgrade, cannot connect.',                  'RESOLVED',         5),
(15, 14, 'BILLING',      'HIGH',     'Duplicate charge on account',     'Two charges in same month for same plan.',             'IN_PROGRESS',       NULL),
(18, 17, 'EQUIPMENT',    'LOW',      'Request for router upgrade',      'Want to upgrade to a faster router.',                  'OPEN',              NULL),
(19, 18, 'SPEED',        'MEDIUM',   'Upload speed very slow',          'Upload speed is 0.5 Mbps instead of 10 Mbps.',         'OPEN',              4),
(20, 19, 'OTHER',        'LOW',      'Plan upgrade request',            'Want to upgrade from Bronze to Silver plan.',          'OPEN',              NULL);

-- ===== network_outage (2 sample outages) =====
INSERT INTO network_outage (title, cause, area_id, severity, start_time, end_time, resolved_by) VALUES
('Koramangala Fiber Cut',  'Underground cable cut during road construction', 1, 'HIGH',   DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY),  1),
('Whitefield Node Failure','OLT hardware failure at main node',              3, 'CRITICAL',DATE_SUB(NOW(), INTERVAL 3 DAY), NULL,                              5);

-- ===== user (application login accounts) =====
-- Passwords: admin@123, billing@123, tech@123, support@123
-- BCrypt hashes (cost=12) - generated offline, NOT plain text:
INSERT INTO `user` (username, password_hash, role, technician_id, is_active) VALUES
('admin',           '$2a$12$XXXXXXXXXX_REPLACE_WITH_REAL_BCRYPT_HASH_admin',    'ADMIN',          NULL, 1),
('billing1',        '$2a$12$XXXXXXXXXX_REPLACE_WITH_REAL_BCRYPT_HASH_billing',  'BILLING_CLERK',  NULL, 1),
('tech.rajesh',     '$2a$12$XXXXXXXXXX_REPLACE_WITH_REAL_BCRYPT_HASH_tech',     'TECHNICIAN',     1,    1),
('support.priya',   '$2a$12$XXXXXXXXXX_REPLACE_WITH_REAL_BCRYPT_HASH_support',  'SUPPORT_AGENT',  2,    1);
-- NOTE: Run PasswordUtil.main() to generate real BCrypt hashes and UPDATE these rows.
