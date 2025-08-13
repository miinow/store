-- On order.customer_id
CREATE INDEX IF NOT EXISTS idx_order_customer_id ON "order" (customer_id);

-- On customer.name
CREATE INDEX IF NOT EXISTS idx_customer_name ON customer (name);

-- On join table order_product columns
CREATE INDEX IF NOT EXISTS idx_order_product_order_id ON order_product (order_id);
CREATE INDEX IF NOT EXISTS idx_order_product_product_id ON order_product (product_id);