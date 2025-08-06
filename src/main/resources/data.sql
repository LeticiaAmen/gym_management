INSERT INTO users(id, email, password, role) VALUES
    (1, 'admin@gym.com',
     '$2a$10$X.Rio15RFNmalRN9/XKmKOIGiH2FZD2bcyBroGKaItaUPZzOHwy6W', 'USER');

INSERT INTO users (id, email, password, role) VALUES
    (2,  'juan@example.com',      '$2a$10$iMawbPXsPFX7pnxpHJTidO8jtFhCpLWnJ45jmSTi1JPXXSvPxs6BW', 'CLIENT'),
    (3,  'ana@example.com',       '$2a$10$iMawbPXsPFX7pnxpHJTidO8jtFhCpLWnJ45jmSTi1JPXXSvPxs6BW', 'CLIENT'),
    (4,  'marcos@example.com',    '$2a$10$iMawbPXsPFX7pnxpHJTidO8jtFhCpLWnJ45jmSTi1JPXXSvPxs6BW', 'CLIENT'),
    (5,  'lucia@example.com',     '$2a$10$iMawbPXsPFX7pnxpHJTidO8jtFhCpLWnJ45jmSTi1JPXXSvPxs6BW', 'CLIENT'),
    (6,  'carlos@example.com',    '$2a$10$iMawbPXsPFX7pnxpHJTidO8jtFhCpLWnJ45jmSTi1JPXXSvPxs6BW', 'CLIENT'),
    (7,  'sofia@example.com',     '$2a$10$iMawbPXsPFX7pnxpHJTidO8jtFhCpLWnJ45jmSTi1JPXXSvPxs6BW', 'CLIENT'),
    (8,  'diego@example.com',     '$2a$10$iMawbPXsPFX7pnxpHJTidO8jtFhCpLWnJ45jmSTi1JPXXSvPxs6BWq', 'CLIENT'),
    (9,  'valeria@example.com',   '$2a$10$iMawbPXsPFX7pnxpHJTidO8jtFhCpLWnJ45jmSTi1JPXXSvPxs6BW', 'CLIENT'),
    (10, 'tomas@example.com',     '$2a$10$iMawbPXsPFX7pnxpHJTidO8jtFhCpLWnJ45jmSTi1JPXXSvPxs6BW', 'CLIENT'),
    (11, 'flor@example.com',      '$2a$10$iMawbPXsPFX7pnxpHJTidO8jtFhCpLWnJ45jmSTi1JPXXSvPxs6BW', 'CLIENT');

INSERT INTO clients(user_id, first_name, last_name, telephone, is_active) VALUES
    (2,  'Juan',    'Pérez',     '111-222-333',  true),
    (3,  'Ana',     'García',    '444-555-666',  true),
    (4,  'Marcos',  'Silva',     '777-888-999',  true),
    (5,  'Lucía',   'Fernández', '222-333-444',  true),
    (6,  'Carlos',  'Rodríguez', '333-444-555',  true),
    (7,  'Sofía',   'López',     '555-666-777',  true),
    (8,  'Diego',   'Martínez',  '666-777-888',  true),
    (9,  'Valeria', 'Sosa',      '888-999-111',  true),
    (10, 'Tomás',   'Gómez',     '999-111-222',  true),
    (11, 'Flor',    'Ramos',     '123-456-789',  true);

-- sincroniza la secuencia con el mayor id + 1
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
