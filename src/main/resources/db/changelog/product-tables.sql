-- Create product table
CREATE TABLE IF NOT EXISTS product (
   id BIGSERIAL PRIMARY KEY,
   description VARCHAR(255) NOT NULL
);

-- Create join table between order and product
CREATE TABLE IF NOT EXISTS order_product (
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    CONSTRAINT pk_order_product PRIMARY KEY (order_id, product_id),
    CONSTRAINT fk_order_product_order FOREIGN KEY (order_id) REFERENCES "order" (id),
    CONSTRAINT fk_order_product_product FOREIGN KEY (product_id) REFERENCES product (id)
);