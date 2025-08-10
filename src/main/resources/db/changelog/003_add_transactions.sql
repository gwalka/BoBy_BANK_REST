CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    card_id BIGINT NOT NULL,
    operation_date_time TIMESTAMP NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    CONSTRAINT fk_card FOREIGN KEY (card_id) REFERENCES cards(id)
);