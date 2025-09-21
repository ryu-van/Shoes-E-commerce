CREATE DATABASE Shoozy_Shop
Go
Use Shoozy_Shop

-- Roles
CREATE TABLE roles (
                       id BIGINT IDENTITY(1,1) PRIMARY KEY,
                       name VARCHAR(255),
                       description TEXT,
                       status BIT DEFAULT 1
);

-- Users
CREATE TABLE users (
                       id BIGINT IDENTITY(1,1) PRIMARY KEY,
                       role_id BIGINT,
                       fullname VARCHAR(255),
                       gender BIT,
                       email VARCHAR(255) UNIQUE,
                       phone_number VARCHAR(50),
                       address TEXT,
                       password VARCHAR(255),
                       created_at DATE,
                       updated_at DATE,
                       date_of_birth DATE,
                       is_active BIT DEFAULT 1,
                       FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Tokens
CREATE TABLE tokens (
                        id BIGINT IDENTITY(1,1) PRIMARY KEY,
                        user_id BIGINT,
                        token TEXT,
                        token_type VARCHAR(50),
                        expiration_date DATE,
                        revoked BIT DEFAULT 1,
                        expired BIT DEFAULT 1,
                        FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Brands
CREATE TABLE brands (
                        id BIGINT IDENTITY(1,1) PRIMARY KEY,
                        name VARCHAR(255),
                        description TEXT,
                        country VARCHAR(100)
);

-- Categories
CREATE TABLE categories (
                            id BIGINT IDENTITY(1,1) PRIMARY KEY,
                            name VARCHAR(255),
                            description TEXT,
                            status BIT DEFAULT 1
);

-- Products
CREATE TABLE products (
                          id BIGINT IDENTITY(1,1) PRIMARY KEY,
                          brand_id BIGINT,
                          category_id BIGINT,
                          sku AS ('SP' + RIGHT('00000' + CAST(id AS VARCHAR(5)), 5)) PERSISTED,
                          name VARCHAR(255),
                          price DECIMAL(15,2),
                          thumbnail TEXT,
                          description TEXT,
                          created_at DATE,
                          updated_at DATE,
                          status BIT DEFAULT 1,
                          FOREIGN KEY (brand_id) REFERENCES brands(id),
                          FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- Product Images
CREATE TABLE product_images (
                                id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                product_id BIGINT,
                                image_url TEXT,
                                FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Sizes
CREATE TABLE sizes (
                       id BIGINT IDENTITY(1,1) PRIMARY KEY,
                       name VARCHAR(50),
                       status BIT DEFAULT 1
);

-- Colors
CREATE TABLE colors (
                        id BIGINT IDENTITY(1,1) PRIMARY KEY,
                        name VARCHAR(50),
                        hex_code VARCHAR(7),
                        status BIT DEFAULT 1
);

-- Materials
CREATE TABLE materials (
                           id BIGINT IDENTITY(1,1) PRIMARY KEY,
                           name VARCHAR(50),
                           description TEXT,
                           status BIT DEFAULT 1
);

-- Product Variants
CREATE TABLE product_variants (
                                  id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                  product_id BIGINT,
                                  size_id BIGINT,
                                  color_id BIGINT,
                                  material_id BIGINT,
                                  quantity INT CHECK (quantity >= 0),
                                  FOREIGN KEY (product_id) REFERENCES products(id),
                                  FOREIGN KEY (size_id) REFERENCES sizes(id),
                                  FOREIGN KEY (color_id) REFERENCES colors(id),
                                  FOREIGN KEY (material_id) REFERENCES materials(id)
);

-- Promotions
CREATE TABLE promotions (
                            id BIGINT IDENTITY(1,1) PRIMARY KEY,
                            name VARCHAR(255),
                            start_date DATE,
                            expiration_date DATE,
                            value DECIMAL(10,2),
                            created_at DATE,
                            updated_at DATE,
                            status BIT
);

-- Product Promotions
CREATE TABLE product_promotions (
                                    id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                    product_id BIGINT,
                                    promotion_id BIGINT,
                                    apply_order INT,
                                    stackable BIT,
                                    FOREIGN KEY (product_id) REFERENCES products(id),
                                    FOREIGN KEY (promotion_id) REFERENCES promotions(id)
);

-- Reviews
CREATE TABLE reviews (
                         id BIGINT IDENTITY(1,1) PRIMARY KEY,
                         user_id BIGINT,
                         product_id BIGINT,
                         content TEXT,
                         rating INT CHECK (rating BETWEEN 1 AND 5),
                         is_hidden BIT,
                         created_at DATE,
                         updated_at DATE,
                         FOREIGN KEY (user_id) REFERENCES users(id),
                         FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Carts
CREATE TABLE carts (
                       id BIGINT IDENTITY(1,1) PRIMARY KEY,
                       user_id BIGINT,
                       created_at DATE,
                       updated_at DATE,
                       FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Cart Items
CREATE TABLE cart_items (
                            id BIGINT IDENTITY(1,1) PRIMARY KEY,
                            cart_id BIGINT,
                            product_variant_id BIGINT,
                            quantity INT CHECK (quantity > 0),
                            FOREIGN KEY (cart_id) REFERENCES carts(id),
                            FOREIGN KEY (product_variant_id) REFERENCES product_variants(id)
);

-- Payment Methods
CREATE TABLE payment_methods (
                                 id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                 name VARCHAR(255),
                                 type VARCHAR(100),
                                 description TEXT,
                                 status BIT DEFAULT 1
);

-- Coupons
CREATE TABLE coupons (
                         id BIGINT IDENTITY(1,1) PRIMARY KEY,
                         name VARCHAR(100),
                         start_date DATE,
                         expiration_date DATE,
                         value DECIMAL(10,2),
                         condition TEXT,
                         created_at DATE,
                         updated_at DATE,
                         status BIT DEFAULT 1
);

-- Orders
CREATE TABLE orders (
                        id BIGINT IDENTITY(1,1) PRIMARY KEY,
                        user_id BIGINT,
                        payment_method_id BIGINT,
                        fullname VARCHAR(255),
                        phone_number VARCHAR(50),
                        address TEXT,
                        note TEXT,
                        order_date DATE,
                        coupon_id BIGINT,
                        status VARCHAR(50) CHECK (status IN ('pending', 'processing', 'shipped', 'delivered', 'cancelled')),
                        total_money DECIMAL(15,2),
                        shipping_method VARCHAR(255),
                        shipping_address TEXT,
                        shipping_date DATE,
                        active BIT DEFAULT 1,
                        FOREIGN KEY (user_id) REFERENCES users(id),
                        FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id),
                        FOREIGN KEY (coupon_id) REFERENCES coupons(id)
);

-- Order Details
CREATE TABLE order_details (
                               id BIGINT IDENTITY(1,1) PRIMARY KEY,
                               order_id BIGINT,
                               product_variant_id BIGINT,
                               price DECIMAL(15,2),
                               quantity INT CHECK (quantity > 0),
                               total_money DECIMAL(15,2),
                               status BIT DEFAULT 1,
                               FOREIGN KEY (order_id) REFERENCES orders(id),
                               FOREIGN KEY (product_variant_id) REFERENCES product_variants(id)
);


-- Insert into roles
INSERT INTO coupons (name, start_date, expiration_date, value, condition, created_at, updated_at) VALUES
                                                                                                      ('NEWCUSTOMER', '2024-01-01', '2024-12-31', 200000.00, 'Áp dụng cho khách hàng mới, đơn hàng từ 1 triệu', '2024-01-01', '2024-01-01'),
                                                                                                      ('SUMMER2024', '2024-06-01', '2024-08-31', 500000.00, 'Giảm 500k cho đơn hàng từ 3 triệu', '2024-05-15', '2024-05-15'),
                                                                                                      ('VIP100', '2024-01-01', '2024-12-31', 100000.00, 'Dành cho khách VIP, không điều kiện', '2024-01-01', '2024-01-01'),
                                                                                                      ('FREESHIP50', '2024-03-01', '2024-12-31', 50000.00, 'Miễn phí ship cho đơn từ 500k', '2024-02-28', '2024-02-28'),
                                                                                                      ('STUDENT15', '2024-08-01', '2024-12-31', 15.00, 'Giảm 15% cho sinh viên', '2024-07-25', '2024-07-25'),
                                                                                                      ('RUNNER20', '2024-01-01', '2024-12-31', 20.00, 'Giảm 20% cho giày chạy bộ', '2024-01-01', '2024-01-01');
--
INSERT INTO roles (name, description) VALUES
                                          ('Admin', N'Quản trị viên hệ thống với quyền truy cập đầy đủ'),
                                          ('Staff', N'Nhân viên bán hàng'),
                                          ('Customer', N'Khách hàng')

-- Insert into users
    INSERT INTO users (role_id, fullname, gender, email, phone_number, address, password, created_at, updated_at, date_of_birth) VALUES
    (1, 'Nguyễn Văn Admin', '0', 'admin@shoesstore.com', '0901234567', '123 Lê Lợi, Q1, TP.HCM', 'hashed_password_1', '2024-01-15', '2024-01-15', '1985-03-20'),
    (2, 'Trần Thị Manager', '1', 'manager@shoesstore.com', '0902345678', '456 Nguyễn Huệ, Q1, TP.HCM', 'hashed_password_2', '2024-01-16', '2024-01-16', '1988-07-12'),
    (2, 'Lê Văn Khách', '0', 'customer1@gmail.com', '0903456789', '789 Hai Bà Trưng, Q3, TP.HCM', 'hashed_password_3', '2024-01-17', '2024-01-17', '1992-11-05'),
    (2, 'Phạm Thị Hoa', '1', 'customer2@gmail.com', '0904567890', '321 Điện Biên Phủ, Q1, TP.HCM', 'hashed_password_4', '2024-01-18', '2024-01-18', '1995-02-28'),
    (2, 'Hoàng Văn VIP', '1', 'vip@gmail.com', '0905678901', '654 Võ Văn Tần, Q3, TP.HCM', 'hashed_password_5', '2024-01-19', '2024-01-19', '1980-09-15');

-- Insert into tokens
INSERT INTO tokens (user_id, token, token_type, expiration_date) VALUES
                                                                     (1, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...admin_token', 'ACCESS_TOKEN', '2024-07-15'),
                                                                     (2, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...manager_token', 'ACCESS_TOKEN', '2024-07-16'),
                                                                     (3, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...customer1_token', 'ACCESS_TOKEN', '2024-07-17'),
                                                                     (4, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...customer2_token', 'REFRESH_TOKEN', '2024-12-18'),
                                                                     (5, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...vip_token', 'ACCESS_TOKEN', '2024-07-19');

-- Insert into brands
INSERT INTO brands (name, description, country) VALUES
                                                    ('Nike', 'Just Do It - Thương hiệu giày thể thao hàng đầu thế giới', 'USA'),
                                                    ('Adidas', 'Impossible is Nothing - Thương hiệu giày thể thao Đức', 'Germany'),
                                                    ('Puma', 'Forever Faster - Thương hiệu giày thể thao nổi tiếng', 'Germany'),
                                                    ('Converse', 'Thương hiệu giày classic Mỹ', 'USA'),
                                                    ('Vans', 'Off The Wall - Thương hiệu giày skateboard', 'USA'),
                                                    ('New Balance', 'Thương hiệu giày chạy bộ cao cấp', 'USA'),
                                                    ('ASICS', 'Anima Sana In Corpore Sano - Giày thể thao Nhật Bản', 'Japan'),
                                                    ('Reebok', 'Be More Human - Thương hiệu giày thể thao', 'USA');

-- Insert into categories
INSERT INTO categories (name, description) VALUES
                                               ('Giày chạy bộ', 'Giày thiết kế nhẹ, êm và hỗ trợ chạy đường dài hoặc luyện tập hàng ngày.'),
                                               ('Giày bóng đá', 'Giày chuyên dụng có đinh giúp bám sân cỏ khi chơi bóng đá.'),
                                               ('Giày bóng rổ', 'Giày có cổ cao, hỗ trợ cổ chân và đệm tốt cho việc nhảy và di chuyển linh hoạt.'),
                                               ('Giày lifestyle', 'Giày thời trang, thường mang tính casual, phù hợp với phong cách hàng ngày.'),
                                               ('Giày skateboard', 'Giày thiết kế bền chắc, đế phẳng để hỗ trợ trượt ván và thao tác kỹ thuật.'),
                                               ('Giày tennis', 'Giày hỗ trợ các chuyển động ngang, độ bám tốt trên sân cứng hoặc sân đất nện.'),
                                               ('Giày tập gym', 'Giày đa năng hỗ trợ tập luyện thể hình, cardio và các hoạt động trong phòng gym.'),
                                               ('Giày casual', 'Giày phong cách thường ngày, dễ phối đồ và mang lại sự thoải mái.');

-- Insert into products
INSERT INTO products (brand_id, category_id, name, price, thumbnail, description, created_at, updated_at) VALUES
                                                                                                              (1, 1, 'Nike Air Zoom Pegasus 40', 3200000.00, 'nike_pegasus_40_thumb.jpg', 'Giày chạy bộ Nike Air Zoom Pegasus 40 với công nghệ đệm khí Zoom Air', '2024-02-01', '2024-02-01'),
                                                                                                              (2, 1, 'Adidas Ultraboost 23', 4800000.00, 'adidas_ultraboost_23_thumb.jpg', 'Giày chạy bộ Adidas Ultraboost 23 với công nghệ đệm Boost', '2024-02-02', '2024-02-02'),
                                                                                                              (1, 3, 'Nike Air Jordan 1 Retro High', 4500000.00, 'jordan_1_retro_thumb.jpg', 'Giày bóng rổ huyền thoại Nike Air Jordan 1 Retro High', '2024-02-03', '2024-02-03'),
                                                                                                              (4, 4, 'Converse Chuck Taylor All Star', 1800000.00, 'converse_chuck_taylor_thumb.jpg', 'Giày lifestyle kinh điển Converse Chuck Taylor All Star', '2024-02-04', '2024-02-04'),
                                                                                                              (5, 5, 'Vans Old Skool', 2100000.00, 'vans_old_skool_thumb.jpg', 'Giày skateboard kinh điển Vans Old Skool', '2024-02-05', '2024-02-05'),
                                                                                                              (6, 1, 'New Balance Fresh Foam X 1080v12', 3800000.00, 'nb_1080v12_thumb.jpg', 'Giày chạy bộ New Balance với đệm Fresh Foam X', '2024-02-06', '2024-02-06'),
                                                                                                              (7, 6, 'ASICS Gel-Resolution 9', 3500000.00, 'asics_gel_resolution_thumb.jpg', 'Giày tennis ASICS với công nghệ GEL', '2024-02-07', '2024-02-07'),
                                                                                                              (3, 2, 'Puma Future 7 Ultimate', 5200000.00, 'puma_future_7_thumb.jpg', 'Giày bóng đá Puma Future 7 Ultimate chuyên nghiệp', '2024-02-08', '2024-02-08');

-- Insert into product_images
INSERT INTO product_images (product_id, image_url) VALUES
                                                       (1, 'nike_pegasus_40_1.jpg'),
                                                       (1, 'nike_pegasus_40_2.jpg'),
                                                       (1, 'nike_pegasus_40_3.jpg'),
                                                       (2, 'adidas_ultraboost_23_1.jpg'),
                                                       (2, 'adidas_ultraboost_23_2.jpg'),
                                                       (3, 'jordan_1_retro_1.jpg'),
                                                       (3, 'jordan_1_retro_2.jpg'),
                                                       (4, 'converse_chuck_taylor_1.jpg'),
                                                       (5, 'vans_old_skool_1.jpg'),
                                                       (6, 'nb_1080v12_1.jpg'),
                                                       (7, 'asics_gel_resolution_1.jpg'),
                                                       (8, 'puma_future_7_1.jpg');

-- Insert into sizes (sizes cho giày)
INSERT INTO sizes (name) VALUES
                             ('36'),
                             ('37'),
                             ('38'),
                             ('39'),
                             ('40'),
                             ('41'),
                             ('42'),
                             ('43'),
                             ('44'),
                             ('45');

-- Insert into colors
INSERT INTO colors (name, hex_code) VALUES
                                        ('Đen', '#000000'),
                                        ('Trắng', '#FFFFFF'),
                                        ('Xanh navy', '#000080'),
                                        ('Đỏ', '#FF0000'),
                                        ('Xám', '#808080'),
                                        ('Xanh dương', '#0066CC'),
                                        ('Vàng', '#FFD700'),
                                        ('Cam', '#FF6600'),
                                        ('Hồng', '#FF69B4'),
                                        ('Xanh lá', '#00CC66');

-- Insert into materials
INSERT INTO materials (name, description) VALUES
                                              ('Mesh', 'Vật liệu lưới thoáng khí cho phần upper'),
                                              ('Leather', 'Da thật cao cấp, bền bỉ'),
                                              ('Synthetic Leather', 'Da tổng hợp nhẹ, dễ vệ sinh'),
                                              ('Canvas', 'Vải canvas dày dặn, phong cách casual'),
                                              ('Flyknit', 'Công nghệ dệt Flyknit của Nike'),
                                              ('Primeknit', 'Công nghệ dệt Primeknit của Adidas'),
                                              ('Rubber', 'Cao su tự nhiên cho đế giày'),
                                              ('EVA', 'Vật liệu đệm giữa EVA nhẹ');

-- Insert into product_variants (bỏ cột price)
INSERT INTO product_variants (product_id, size_id, color_id, quantity, material_id) VALUES
-- Nike Air Zoom Pegasus 40
(1, 4, 1, 8, 1), -- Size 39, Đen
(1, 5, 2, 12, 1), -- Size 40, Trắng
(1, 6, 6, 10, 1), -- Size 41, Xanh dương
-- Adidas Ultraboost 23
(2, 4, 2, 6, 6), -- Size 39, Trắng
(2, 5, 1, 8, 6), -- Size 40, Đen
(2, 6, 5, 5, 6), -- Size 41, Xám
-- Nike Air Jordan 1 Retro High
(3, 5, 4, 7, 2), -- Size 40, Đỏ
(3, 6, 1, 9, 2), -- Size 41, Đen
-- Converse Chuck Taylor All Star
(4, 3, 2, 15, 4), -- Size 38, Trắng
(4, 4, 1, 18, 4), -- Size 39, Đen
(4, 5, 4, 12, 4), -- Size 40, Đỏ
-- Vans Old Skool
(5, 4, 1, 10, 4), -- Size 39, Đen
(5, 5, 2, 8, 4), -- Size 40, Trắng
-- New Balance Fresh Foam X 1080v12
(6, 5, 5, 7, 1), -- Size 40, Xám
(6, 6, 2, 6, 1), -- Size 41, Trắng
-- ASICS Gel-Resolution 9
(7, 5, 2, 5, 3), -- Size 40, Trắng
(7, 6, 3, 4, 3), -- Size 41, Xanh navy
-- Puma Future 7 Ultimate
(8, 5, 7, 3, 3), -- Size 40, Vàng
(8, 6, 8, 2, 3); -- Size 41, Cam

-- Insert into promotions
INSERT INTO promotions (name, start_date, expiration_date, value, created_at, updated_at) VALUES
                                                                                              ('Giảm giá mùa hè', '2024-06-01', '2024-08-31', 15.00, '2024-05-20', '2024-05-20'),
                                                                                              ('Black Friday Sale', '2024-11-24', '2024-11-30', 30.00, '2024-11-01', '2024-11-01'),
                                                                                              ('Khuyến mãi Back to School', '2024-08-01', '2024-09-15', 20.00, '2024-07-15', '2024-07-15'),
                                                                                              ('Giảm giá cuối năm', '2024-12-15', '2024-12-31', 25.00, '2024-12-01', '2024-12-01'),
                                                                                              ('Flash Sale Weekend', '2024-05-15', '2024-05-16', 40.00, '2024-05-14', '2024-05-14'),
                                                                                              ('Ưu đãi Nike Month', '2024-07-01', '2024-07-31', 12.00, '2024-06-25', '2024-06-25');

-- Insert into product_promotions
INSERT INTO product_promotions (product_id, promotion_id, apply_order, stackable) VALUES
                                                                                      (1, 1, 1, 1), -- Nike Pegasus - Giảm giá mùa hè
                                                                                      (1, 6, 2, 1), -- Nike Pegasus - Ưu đãi Nike Month
                                                                                      (2, 2, 1, 0), -- Adidas Ultraboost - Black Friday
                                                                                      (3, 6, 1, 1), -- Jordan 1 - Ưu đãi Nike Month
                                                                                      (4, 3, 1, 1), -- Converse - Back to School
                                                                                      (5, 5, 1, 0), -- Vans - Flash Sale
                                                                                      (6, 1, 1, 1), -- New Balance - Giảm giá mùa hè
                                                                                      (7, 4, 1, 1), -- ASICS - Giảm giá cuối năm
                                                                                      (8, 2, 1, 0); -- Puma - Black Friday

-- Insert into reviews
INSERT INTO reviews (user_id, product_id, content, rating, is_hidden, created_at, updated_at) VALUES
                                                                                                  (3, 1, 'Giày chạy bộ tuyệt vời! Êm chân, nhẹ và thoáng khí. Rất phù hợp cho việc chạy đường dài.', 5, 0, '2024-03-01', '2024-03-01'),
                                                                                                  (4, 2, 'Adidas Ultraboost quả thật đáng đồng tiền. Đệm Boost rất êm, thiết kế đẹp.', 5, 0, '2024-03-02', '2024-03-02'),
                                                                                                  (5, 3, 'Jordan 1 chất lượng tuyệt vời, da thật rất đẹp. Đúng là huyền thoại!', 5, 0, '2024-03-03', '2024-03-03'),
                                                                                                  (3, 4, 'Converse classic không bao giờ lỗi mốt. Giá cả hợp lý, chất lượng tốt.', 4, 0, '2024-03-04', '2024-03-04'),
                                                                                                  (4, 5, 'Vans Old Skool phong cách, bền bỉ. Rất phù hợp với style streetwear.', 4, 0, '2024-03-05', '2024-03-05'),
                                                                                                  (5, 6, 'New Balance 1080v12 êm như mây. Rất tốt cho người chạy bộ lâu năm.', 5, 0, '2024-03-10', '2024-03-10'),
                                                                                                  (3, 7, 'ASICS tennis shoes chất lượng cao, grip rất tốt trên sân.', 4, 0, '2024-03-12', '2024-03-12'),
                                                                                                  (4, 8, 'Puma Future 7 tuyệt vời cho bóng đá, cảm giác bóng rất tốt.', 5, 0, '2024-03-15', '2024-03-15');

-- Insert into carts
INSERT INTO carts (user_id, created_at, updated_at) VALUES
                                                        (3, '2024-04-01', '2024-04-01'),
                                                        (4, '2024-04-02', '2024-04-02'),
                                                        (5, '2024-04-03', '2024-04-03'),
                                                        (3, '2024-04-04', '2024-04-04'),
                                                        (4, '2024-04-05', '2024-04-05');

-- Insert into cart_items
INSERT INTO cart_items (cart_id, product_variant_id, quantity) VALUES
                                                                   (1, 1, 1), -- Nike Pegasus size 39 đen
                                                                   (1, 9, 1), -- Converse size 38 trắng
                                                                   (2, 4, 1), -- Adidas Ultraboost size 39 trắng
                                                                   (3, 7, 1), -- Jordan 1 size 40 đỏ
                                                                   (4, 12, 1), -- Vans size 39 đen
                                                                   (5, 14, 1); -- New Balance size 40 xám

-- Insert into payment_methods
INSERT INTO payment_methods (name, type, description) VALUES
                                                          ('Thanh toán khi nhận hàng', 'COD', 'Thanh toán bằng tiền mặt khi nhận hàng'),
                                                          ('Chuyển khoản ngân hàng', 'BANK_TRANSFER', 'Chuyển khoản qua ngân hàng'),
                                                          ('Thẻ tín dụng/Ghi nợ', 'CREDIT_CARD', 'Thanh toán bằng thẻ Visa/Mastercard'),
                                                          ('Ví điện tử MoMo', 'E_WALLET', 'Thanh toán qua ví MoMo'),
                                                          ('Ví điện tử ZaloPay', 'E_WALLET', 'Thanh toán qua ví ZaloPay'),
                                                          ('Ví điện tử ShopeePay', 'E_WALLET', 'Thanh toán qua ví ShopeePay');


-- Insert into orders
INSERT INTO orders (user_id, payment_method_id, fullname, phone_number, address, note, order_date, coupon_id, status, total_money, shipping_method, shipping_address, shipping_date) VALUES
                                                                                                                                                                                         (3, 1, 'Lê Văn Khách', '0903456789', '789 Hai Bà Trưng, Q3, TP.HCM', 'Giao hàng giờ hành chính', '2024-04-10', 1, 'pending', 3000000.00, 'Standard', '789 Hai Bà Trưng, Q3, TP.HCM', '2024-04-12'),
                                                                                                                                                                                         (4, 2, 'Phạm Thị Hoa', '0904567890', '321 Điện Biên Phủ, Q1, TP.HCM', 'Gọi trước khi giao', '2024-04-11', 2, 'processing', 4300000.00, 'Express', '321 Điện Biên Phủ, Q1, TP.HCM', '2024-04-13'),
                                                                                                                                                                                         (5, 3, 'Hoàng Văn VIP', '0905678901', '654 Võ Văn Tần, Q3, TP.HCM', 'Kiểm tra kỹ trước khi giao', '2024-04-12', 3, 'pending', 4400000.00, 'Standard', '654 Võ Văn Tần, Q3, TP.HCM', '2024-04-15'),
                                                                                                                                                                                         (3, 4, 'Lê Văn Khách', '0903456789', '789 Hai Bà Trưng, Q3, TP.HCM', 'Hàng dễ vỡ, xin cẩn thận', '2024-04-13', 4, 'delivered', 1750000.00, 'Standard', '789 Hai Bà Trưng, Q3, TP.HCM', '2024-04-16'),
                                                                                                                                                                                         (4, 5, 'Phạm Thị Hoa', '0904567890', '321 Điện Biên Phủ, Q1, TP.HCM', 'Giao nhanh trong ngày', '2024-04-14', 5, 'shipped', 2000000.00, 'Express', '321 Điện Biên Phủ, Q1, TP.HCM', '2024-04-15'),
                                                                                                                                                                                         (5, 6, 'Hoàng Văn VIP', '0905678901', '654 Võ Văn Tần, Q3, TP.HCM', 'Khách VIP, ưu tiên giao hàng', '2024-04-15', 3, 'cancelled', 3700000.00, 'Express', '654 Võ Văn Tần, Q3, TP.HCM', '2024-04-16');

-- Insert into order_details
INSERT INTO order_details (order_id, product_variant_id, price, quantity, total_money) VALUES
                                                                                           (1, 1, 3200000.00, 1, 3200000.00), -- Nike Pegasus
                                                                                           (2, 4, 4800000.00, 1, 4800000.00), -- Adidas Ultraboost
                                                                                           (3, 7, 4500000.00, 1, 4500000.00), -- Jordan 1
                                                                                           (4, 9, 1800000.00, 1, 1800000.00), -- Converse Chuck Taylor
                                                                                           (5, 12, 2100000.00, 1, 2100000.00), -- Vans Old Skool
                                                                                           (6, 14, 3800000.00, 1, 3800000.00); -- New Balance 1080v12

select * from product_images
select * from brands
select * from categories
select * from products
select * from product_variants
select * from colors
select * from sizes
select * from materials

SELECT
    pv.product_id,
    SUM(pv.quantity) AS total_quantity
FROM
    product_variants pv
GROUP BY
    pv.product_id;