INSERT INTO users (username, email, first_name, last_name, password, role)
VALUES (
    'Rowdey8',
    'admin@example.com',
    'Admin',
    'User',
    '$2a$10$MddPhOIvvl1CqearA620T.msVmfNNYnoMpb8z3NT9ATKZc3a8rdV6',
    'ADMIN'
)
ON CONFLICT (id) DO NOTHING;