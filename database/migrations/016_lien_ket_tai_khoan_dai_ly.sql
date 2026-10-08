USE oms;

ALTER TABLE customers
    ADD COLUMN user_id BIGINT NULL AFTER sales_rep_id,
    ADD UNIQUE KEY uk_customers_user_id (user_id),
    ADD CONSTRAINT fk_customers_user
        FOREIGN KEY (user_id) REFERENCES users (id);
