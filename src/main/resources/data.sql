-- Crear usuario administrador inicial (password: admin123)
INSERT INTO users (email, password, role) VALUES
('admin@scbox.com', '$2a$10$X.Rio15RFNmalRN9/XKmKOIGiH2FZD2bcyBroGKaItaUPZzOHwy6W', 'ADMIN');

-- Reset secuencia de users
ALTER SEQUENCE users_id_seq RESTART WITH 2;

-- Insertar clientes de ejemplo
INSERT INTO clients (first_name, last_name, email, phone, is_active, start_date, notes, created_at, updated_at) VALUES
('Juan', 'Pérez', 'juan@example.com', '11-1234-5678', true, CURRENT_DATE - INTERVAL '3 months', 'Cliente regular', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ana', 'García', 'ana@example.com', '11-5678-1234', true, CURRENT_DATE - INTERVAL '6 months', 'Plan premium', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Carlos', 'López', 'carlos@example.com', '11-9012-3456', true, CURRENT_DATE - INTERVAL '1 month', 'Nuevo cliente', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('María', 'Rodríguez', 'maria@example.com', '11-3456-7890', false, CURRENT_DATE - INTERVAL '8 months', 'Inactivo', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Reset secuencia de clients
ALTER SEQUENCE clients_id_seq RESTART WITH 5;

-- Insertar pagos de ejemplo
INSERT INTO payments (client_id, amount, method, month, year, payment_date, payment_state, voided) VALUES
(1, 5000.00, 'CASH', 9, 2025, CURRENT_DATE - INTERVAL '5 days', 'UP_TO_DATE', false),
(1, 5000.00, 'TRANSFER', 8, 2025, CURRENT_DATE - INTERVAL '35 days', 'UP_TO_DATE', false),
(2, 6000.00, 'CREDIT', 9, 2025, CURRENT_DATE - INTERVAL '3 days', 'UP_TO_DATE', false),
(2, 6000.00, 'DEBIT', 8, 2025, CURRENT_DATE - INTERVAL '33 days', 'UP_TO_DATE', false),
(3, 5000.00, 'CASH', 9, 2025, CURRENT_DATE, 'PENDING', false),
(4, 5000.00, 'TRANSFER', 7, 2025, CURRENT_DATE - INTERVAL '60 days', 'EXPIRED', false);

-- Reset secuencia de payments
ALTER SEQUENCE payments_id_seq RESTART WITH 7;

-- Insertar algunos registros de auditoría
INSERT INTO audit_logs (action, user_id, entity, entity_id, old_values, new_values, created_at) VALUES
('CREATE_CLIENT', 1, 'Client', 1, null, 'Cliente Juan Pérez creado', CURRENT_TIMESTAMP),
('CREATE_PAYMENT', 1, 'Payment', 1, null, 'Pago registrado por $5000.00', CURRENT_TIMESTAMP),
('UPDATE_CLIENT', 1, 'Client', 4, 'isActive=true', 'isActive=false', CURRENT_TIMESTAMP);

-- Reset secuencia de audit_logs
ALTER SEQUENCE audit_logs_id_seq RESTART WITH 4;
