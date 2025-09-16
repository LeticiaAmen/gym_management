-- Crear usuario administrador inicial (password: admin123)
INSERT INTO users (email, password, role) VALUES
('admin@scbox.com', '$2a$10$X.Rio15RFNmalRN9/XKmKOIGiH2FZD2bcyBroGKaItaUPZzOHwy6W', 'ADMIN');

-- Reset secuencia de users
ALTER SEQUENCE users_id_seq RESTART WITH 2;

-- Insertar 30 clientes (la mayoría activos)
INSERT INTO clients (first_name, last_name, email, phone, is_active, start_date, notes, created_at, updated_at) VALUES
('Juan', 'Pérez', 'juan@example.com', '11-1234-5678', true,  CURRENT_DATE - INTERVAL '9 months', 'Cliente regular', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ana', 'García', 'ana@example.com', '11-5678-1234', true,  CURRENT_DATE - INTERVAL '12 months','Plan premium', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Carlos', 'López', 'carlos@example.com', '11-9012-3456', true,  CURRENT_DATE - INTERVAL '2 months', 'Nuevo cliente', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('María', 'Rodríguez', 'maria@example.com', '11-3456-7890', false, CURRENT_DATE - INTERVAL '14 months','Inactivo', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Sofía', 'Martínez', 'sofia@example.com', '11-1111-1111', true, CURRENT_DATE - INTERVAL '5 months','Crossfit', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Pedro', 'Sánchez', 'pedro@example.com', '11-2222-2222', true, CURRENT_DATE - INTERVAL '7 months','Funcional', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Lucía', 'Fernández', 'lucia@example.com', '11-3333-3333', true, CURRENT_DATE - INTERVAL '3 months','Hiit', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Martín', 'Gómez', 'martin@example.com', '11-4444-4444', true, CURRENT_DATE - INTERVAL '10 months','Mañanas', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Valentina', 'Torres', 'valentina@example.com', '11-5555-5555', true, CURRENT_DATE - INTERVAL '6 months','Vespertino', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Diego', 'Ruiz', 'diego@example.com', '11-6666-6666', true, CURRENT_DATE - INTERVAL '8 months','Boxeo', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Camila', 'Romero', 'camila@example.com', '11-7777-7777', true, CURRENT_DATE - INTERVAL '4 months','Yoga', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Nicolás', 'Herrera', 'nicolas@example.com', '11-8888-8888', true, CURRENT_DATE - INTERVAL '11 months','Pesas', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Laura', 'Castro', 'laura@example.com', '11-9999-9999', true, CURRENT_DATE - INTERVAL '9 months','Zumba', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Javier', 'Navarro', 'javier@example.com', '11-1010-1010', true, CURRENT_DATE - INTERVAL '3 months','Cardio', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Micaela', 'Vega', 'micaela@example.com', '11-1212-1212', true, CURRENT_DATE - INTERVAL '2 months','Plan anual', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Rodrigo', 'Silva', 'rodrigo@example.com', '11-1313-1313', true, CURRENT_DATE - INTERVAL '5 months','Funcional', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Paula', 'Díaz', 'paula@example.com', '11-1414-1414', true, CURRENT_DATE - INTERVAL '6 months','Musculación', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Tomás', 'Molina', 'tomas@example.com', '11-1515-1515', true, CURRENT_DATE - INTERVAL '7 months','Tarde', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Martina', 'Ortiz', 'martina@example.com', '11-1616-1616', true, CURRENT_DATE - INTERVAL '1 months','Mañana', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Santiago', 'Cabrera', 'santiago@example.com', '11-1717-1717', true, CURRENT_DATE - INTERVAL '12 months','Fuerza', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Agustina', 'Ríos', 'agustina@example.com', '11-1818-1818', true, CURRENT_DATE - INTERVAL '10 months','Baile', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Federico', 'Soto', 'federico@example.com', '11-1919-1919', true, CURRENT_DATE - INTERVAL '8 months','TRX', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Brenda', 'Aguirre', 'brenda@example.com', '11-2020-2020', true, CURRENT_DATE - INTERVAL '4 months','Pilates', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Gonzalo', 'Paredes', 'gonzalo@example.com', '11-2121-2121', true, CURRENT_DATE - INTERVAL '3 months','Bike', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Daniela', 'Méndez', 'daniela@example.com', '11-2223-2323', true, CURRENT_DATE - INTERVAL '2 months','Libre', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Facundo', 'Ibáñez', 'facundo@example.com', '11-2323-2323', true, CURRENT_DATE - INTERVAL '5 months','Cross', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Romina', 'Duarte', 'romina@example.com', '11-2424-2424', true, CURRENT_DATE - INTERVAL '6 months','Spinning', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ezequiel', 'Godoy', 'ezequiel@example.com', '11-2525-2525', true, CURRENT_DATE - INTERVAL '7 months','Funcional', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Florencia', 'Cabrera', 'florencia@example.com', '11-2626-2626', true, CURRENT_DATE - INTERVAL '8 months','Team', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Leandro', 'Varela', 'leandro@example.com', '11-2727-2727', true, CURRENT_DATE - INTERVAL '9 months','Noches', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Reset secuencia de clients
ALTER SEQUENCE clients_id_seq RESTART WITH 31;

-- Pagos de ejemplo
-- Mensuales al día (expiration > hoy)
INSERT INTO payments (client_id, amount, method, period_month, period_year, payment_date, expiration_date, payment_state, voided)
VALUES
(1, 5000.00, 'CASH',     EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '5 days',  (CURRENT_DATE - INTERVAL '5 days')  + INTERVAL '1 month', 'UP_TO_DATE', false),
(2, 6000.00, 'CREDIT',   EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '3 days',  (CURRENT_DATE - INTERVAL '3 days')  + INTERVAL '1 month', 'UP_TO_DATE', false),
(3, 5000.00, 'TRANSFER', EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '2 days',  (CURRENT_DATE - INTERVAL '2 days')  + INTERVAL '1 month', 'UP_TO_DATE', false),
(5, 7000.00, 'DEBIT',    EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '9 days',  (CURRENT_DATE - INTERVAL '9 days')  + INTERVAL '1 month', 'UP_TO_DATE', false),
(7, 5500.00, 'CASH',     EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '6 days',  (CURRENT_DATE - INTERVAL '6 days')  + INTERVAL '1 month', 'UP_TO_DATE', false),
(9, 5200.00, 'TRANSFER', EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '7 days',  (CURRENT_DATE - INTERVAL '7 days')  + INTERVAL '1 month', 'UP_TO_DATE', false),
(11,5800.00, 'CREDIT',   EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '1 days',  (CURRENT_DATE - INTERVAL '1 days')  + INTERVAL '1 month', 'UP_TO_DATE', false),
(13,5000.00, 'DEBIT',    EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '4 days',  (CURRENT_DATE - INTERVAL '4 days')  + INTERVAL '1 month', 'UP_TO_DATE', false),
(15,6000.00, 'CASH',     EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '8 days',  (CURRENT_DATE - INTERVAL '8 days')  + INTERVAL '1 month', 'UP_TO_DATE', false),
(17,6200.00, 'TRANSFER', EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '11 days', (CURRENT_DATE - INTERVAL '11 days') + INTERVAL '1 month', 'UP_TO_DATE', false),
(19,5300.00, 'CASH',     EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '12 days', (CURRENT_DATE - INTERVAL '12 days') + INTERVAL '1 month', 'UP_TO_DATE', false),
(21,5400.00, 'DEBIT',    EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '10 days', (CURRENT_DATE - INTERVAL '10 days') + INTERVAL '1 month', 'UP_TO_DATE', false),
(23,5100.00, 'CASH',     EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '13 days', (CURRENT_DATE - INTERVAL '13 days') + INTERVAL '1 month', 'UP_TO_DATE', false),
(25,5600.00, 'CREDIT',   EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '14 days', (CURRENT_DATE - INTERVAL '14 days') + INTERVAL '1 month', 'UP_TO_DATE', false),
(27,5900.00, 'DEBIT',    EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '5 days',  (CURRENT_DATE - INTERVAL '5 days')  + INTERVAL '1 month', 'UP_TO_DATE', false),
(29,5200.00, 'TRANSFER', EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '2 days',  (CURRENT_DATE - INTERVAL '2 days')  + INTERVAL '1 month', 'UP_TO_DATE', false);

-- Pagos por días (vigentes)
INSERT INTO payments (client_id, amount, method, period_month, period_year, payment_date, expiration_date, payment_state, voided, duration_days) VALUES
(24, 3000.00, 'CASH',     EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '1 day',  (CURRENT_DATE - INTERVAL '1 day') + INTERVAL '10 days', 'UP_TO_DATE', false, 10),
(26, 3500.00, 'DEBIT',    EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '3 days', (CURRENT_DATE - INTERVAL '3 days') + INTERVAL '7 days',  'UP_TO_DATE', false, 7),
(28, 3200.00, 'CREDIT',   EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE - INTERVAL '2 days', (CURRENT_DATE - INTERVAL '2 days') + INTERVAL '15 days', 'UP_TO_DATE', false, 15);

-- Al menos 10 vencidos (expiration < hoy). Mezcla de mensuales y por días.
INSERT INTO payments (client_id, amount, method, period_month, period_year, payment_date, expiration_date, payment_state, voided) VALUES
(4,  5000.00, 'TRANSFER', EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '2 months'))::int, EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '2 months'))::int, CURRENT_DATE - INTERVAL '60 days', (CURRENT_DATE - INTERVAL '60 days') + INTERVAL '1 month', 'EXPIRED', false),
(6,  5000.00, 'CASH',     EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '2 months'))::int, EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '2 months'))::int, CURRENT_DATE - INTERVAL '50 days', (CURRENT_DATE - INTERVAL '50 days') + INTERVAL '1 month', 'EXPIRED', false),
(8,  5200.00, 'DEBIT',    EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '2 months'))::int, EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '2 months'))::int, CURRENT_DATE - INTERVAL '70 days', (CURRENT_DATE - INTERVAL '70 days') + INTERVAL '1 month', 'EXPIRED', false),
(10, 5400.00, 'CREDIT',   EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '2 months'))::int, EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '2 months'))::int, CURRENT_DATE - INTERVAL '65 days', (CURRENT_DATE - INTERVAL '65 days') + INTERVAL '1 month', 'EXPIRED', false),
(12, 5600.00, 'TRANSFER', EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '2 months'))::int, EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '2 months'))::int, CURRENT_DATE - INTERVAL '55 days', (CURRENT_DATE - INTERVAL '55 days') + INTERVAL '1 month', 'EXPIRED', false),
(14, 5000.00, 'CASH',     EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '2 months'))::int, EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '2 months'))::int, CURRENT_DATE - INTERVAL '58 days', (CURRENT_DATE - INTERVAL '58 days') + INTERVAL '1 month', 'EXPIRED', false),
(16, 6000.00, 'DEBIT',    EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '2 months'))::int, EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '2 months'))::int, CURRENT_DATE - INTERVAL '61 days', (CURRENT_DATE - INTERVAL '61 days') + INTERVAL '1 month', 'EXPIRED', false),
(18, 6200.00, 'CREDIT',   EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '2 months'))::int, EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '2 months'))::int, CURRENT_DATE - INTERVAL '63 days', (CURRENT_DATE - INTERVAL '63 days') + INTERVAL '1 month', 'EXPIRED', false),
(20, 5300.00, 'TRANSFER', EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '2 months'))::int, EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '2 months'))::int, CURRENT_DATE - INTERVAL '66 days', (CURRENT_DATE - INTERVAL '66 days') + INTERVAL '1 month', 'EXPIRED', false),
(22, 5400.00, 'CASH',     EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '2 months'))::int, EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '2 months'))::int, CURRENT_DATE - INTERVAL '62 days', (CURRENT_DATE - INTERVAL '62 days') + INTERVAL '1 month', 'EXPIRED', false);

-- Vencidos por días (con duration_days especificado)
INSERT INTO payments (client_id, amount, method, period_month, period_year, payment_date, expiration_date, payment_state, voided, duration_days) VALUES
(30, 3000.00, 'CASH',   EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '1 months'))::int, EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '1 months'))::int, CURRENT_DATE - INTERVAL '20 days', (CURRENT_DATE - INTERVAL '20 days') + INTERVAL '10 days', 'EXPIRED', false, 10),
(5,  2800.00, 'CASH',   EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '1 months'))::int, EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '1 months'))::int, CURRENT_DATE - INTERVAL '25 days', (CURRENT_DATE - INTERVAL '25 days') + INTERVAL '7 days',  'EXPIRED', false, 7);

-- Algunos pagos anulados (no deben contarse como vigentes)
INSERT INTO payments (client_id, amount, method, period_month, period_year, payment_date, expiration_date, payment_state, voided)
VALUES
(3, 5000.00, 'CASH', EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int, CURRENT_DATE, CURRENT_DATE + INTERVAL '1 month', 'VOIDED', true);

-- Reset secuencia de payments (aprox. siguiente id)
ALTER SEQUENCE payments_id_seq RESTART WITH 60;

-- Auditoría mínima
INSERT INTO audit_logs (action, user_id, entity, entity_id, old_values, new_values, created_at) VALUES
('CREATE_CLIENT', 1, 'Client', 1, null, 'Cliente Juan Pérez creado', CURRENT_TIMESTAMP),
('CREATE_PAYMENT', 1, 'Payment', 1, null, 'Pago registrado por $5000.00', CURRENT_TIMESTAMP);

-- Reset secuencia de audit_logs
ALTER SEQUENCE audit_logs_id_seq RESTART WITH 10;
