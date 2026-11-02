-- Fix sequence high-water mark after explicit identity inserts in V402
SELECT setval(pg_get_serial_sequence('aisle_categories', 'id'), COALESCE((SELECT MAX(id) FROM aisle_categories), 1));
SELECT setval(pg_get_serial_sequence('units', 'id'), COALESCE((SELECT MAX(id) FROM units), 1));
