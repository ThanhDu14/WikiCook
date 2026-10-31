-- Seed for demo PA3
INSERT INTO aisle_categories (id, code, name_vi, icon_name, position) OVERRIDING SYSTEM VALUE
VALUES
    (1, 'PRODUCE', 'Rau củ & Trái cây', 'produce', 1),
    (2, 'MEAT_SEAFOOD', 'Thịt & Thủy hải sản', 'meat_seafood', 2),
    (3, 'DAIRY_EGGS', 'Trứng & Sữa', 'dairy_eggs', 3),
    (4, 'PANTRY_SPICES', 'Gia vị & Đồ khô', 'pantry_spices', 4),
    (5, 'FROZEN', 'Đồ đông lạnh', 'frozen', 5),
    (6, 'GENERAL', 'Đồ dùng khác', 'general', 6)
ON CONFLICT (id) DO NOTHING;

INSERT INTO units (id, code, name_vi, unit_type, requires_quantity, is_active) OVERRIDING SYSTEM VALUE
VALUES
    (1, 'g', 'g', 'MASS', true, true),
    (2, 'kg', 'kg', 'MASS', true, true),
    (3, 'ml', 'ml', 'VOLUME', true, true),
    (4, 'l', 'l', 'VOLUME', true, true),
    (5, 'qua', 'quả', 'COUNT', true, true),
    (6, 'cu', 'củ', 'COUNT', true, true),
    (7, 'cay', 'cây', 'COUNT', true, true)
ON CONFLICT (id) DO NOTHING;
