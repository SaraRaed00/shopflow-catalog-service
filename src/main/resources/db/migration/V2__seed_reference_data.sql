
INSERT INTO warehouse (code, name, country) VALUES
('KWT-MAIN', 'Kuwait Main Warehouse', 'KW'),
('UAE-DXB', 'Dubai Distribution Center', 'AE'),
('SAU-RUH', 'Riyadh Hub', 'SA');

INSERT INTO category (name, slug, parent_id) VALUES
('Electronics', 'electronics', NULL);

INSERT INTO category (name, slug, parent_id)
SELECT 'Phones', 'phones', id FROM category WHERE slug = 'electronics'
UNION ALL
SELECT 'Laptops', 'laptops', id FROM category WHERE slug = 'electronics';

