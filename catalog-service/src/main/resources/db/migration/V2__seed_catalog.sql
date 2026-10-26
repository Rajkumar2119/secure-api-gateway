INSERT INTO categories (name, slug) VALUES
    ('Electronics', 'electronics'),
    ('Home and Kitchen', 'home-and-kitchen'),
    ('Books', 'books');

INSERT INTO products (sku, name, description, price, stock_quantity, category_id) VALUES
    ('ELEC-0001', 'Wireless Headphones', 'Over-ear, noise cancelling', 129.99, 50, 1),
    ('ELEC-0002', 'USB-C Charger 65W', 'GaN charger with two ports', 34.50, 200, 1),
    ('HOME-0001', 'Cast Iron Skillet', '28 cm, pre-seasoned', 39.00, 75, 2),
    ('HOME-0002', 'Coffee Grinder', 'Burr grinder, 15 settings', 59.95, 30, 2),
    ('BOOK-0001', 'Designing Data-Intensive Applications', 'Paperback', 42.00, 20, 3);
