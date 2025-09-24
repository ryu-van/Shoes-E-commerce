CREATE DATABASE Shoozy_Shop
Go
Use Shoozy_Shop

-- Roles
CREATE TABLE roles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255),
    description NVARCHAR(MAX),
    status BIT DEFAULT 1
);
-- Users
CREATE TABLE users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    role_id BIGINT,
    avatar NVARCHAR(MAX),
    fullname NVARCHAR(255),
    gender BIT,
    email VARCHAR(255) UNIQUE,
    phone_number VARCHAR(50),
    address NVARCHAR(MAX),
    password VARCHAR(255),
    created_at DATETIME,
    updated_at DATETIME,
    date_of_birth DATETIME,
    is_active BIT DEFAULT 1,
    FOREIGN KEY (role_id) REFERENCES roles(id)
);
--address
Create table addresses(
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT,
    province_code int,
    province_name NVARCHAR(255),
    district_code int,
    district_name NVARCHAR(255),
    ward_code VARCHAR(20),
    ward_name NVARCHAR(255),
    address_detail NVARCHAR(MAX),
    is_selected bit,
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (user_id) references users(id)

)

-- Tokens
CREATE TABLE tokens (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT,
    token NVARCHAR(MAX),
    token_type VARCHAR(50),
    expiration_date DATETIME,
    revoked BIT DEFAULT 1,
    expired BIT DEFAULT 1,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Brands
CREATE TABLE brands (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255),
    description NVARCHAR(MAX),
    country VARCHAR(100),
    status BIT DEFAULT 1
);

-- Categories
CREATE TABLE categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255),
    description NVARCHAR(MAX),
    status BIT DEFAULT 1
);

-- Sizes
CREATE TABLE sizes (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    value VARCHAR(50),
    status BIT DEFAULT 1
);

-- Colors
CREATE TABLE colors (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50),
    hex_code VARCHAR(7),
    status BIT DEFAULT 1
);

-- Materials
CREATE TABLE materials (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50),
    description NVARCHAR(MAX),
    status BIT DEFAULT 1
);

-- Products
CREATE TABLE products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    brand_id BIGINT,
    category_id BIGINT,
    sku NVARCHAR(10),
    name NVARCHAR(255),
    thumbnail NVARCHAR(MAX),
    material_id BIGINT,
    description NVARCHAR(MAX),
	weight DECIMAL(10,2),
    created_at DATETIME,
    updated_at DATETIME,
    status BIT DEFAULT 1,
    product_gender NVARCHAR(20) DEFAULT 'Unisex' CHECK (product_gender IN ('Male', 'Female', 'Unisex', 'Kids')),
    FOREIGN KEY (brand_id) REFERENCES brands(id),
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (material_id) REFERENCES materials(id)
);

CREATE TABLE product_variants (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT,
    size_id BIGINT,
    color_id BIGINT,
    thumbnail NVARCHAR(MAX),
    cost_price DECIMAL(15,2) CHECK (cost_price >= 0),   -- Giá vốn
    sell_price DECIMAL(15,2) CHECK (sell_price >= 0),   -- Giá bán
    quantity INT CHECK (quantity >= 0),
    status BIT DEFAULT 1,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (size_id) REFERENCES sizes(id),
    FOREIGN KEY (color_id) REFERENCES colors(id)
);

-- Product Images
CREATE TABLE product_images (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    image_url NVARCHAR(MAX)
);

CREATE TABLE product_variant_images (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_variant_id BIGINT,
    product_image_id BIGINT,
    FOREIGN KEY (product_variant_id) REFERENCES product_variants(id),
    FOREIGN KEY (product_image_id) REFERENCES product_images(id)
);

-- Promotions
CREATE TABLE promotions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(255),
    code VARCHAR(30),
    start_date DATETIME,
    expiration_date DATETIME,
    value DECIMAL(10,2),
    description NVARCHAR(500),
    created_at DATETIME,
    updated_at DATETIME,
	status TINYINT
);

-- Product Promotions
CREATE TABLE product_promotions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_variant_id BIGINT NULL,
    promotion_id BIGINT NOT NULL,
    custom_value DECIMAL(10,2),
    FOREIGN KEY (product_variant_id) REFERENCES product_variants(id),
    FOREIGN KEY (promotion_id) REFERENCES promotions(id)
);


-- Carts
CREATE TABLE carts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT,
    created_at DATETIME,
    updated_at DATETIME,
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

-- Coupons
CREATE TABLE coupons (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(100),
    code VARCHAR(30),
    type BIT,
    start_date DATETIME,
    expiration_date DATETIME,
    value DECIMAL(10,2),
    condition FLOAT,
    created_at DATETIME,
    updated_at DATETIME,
    quantity INT,
    value_limit DECIMAL(10,2) DEFAULT 0,
    description NVARCHAR(500),
    status TINYINT
);

-- Payment Methods
CREATE TABLE payment_methods (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255),
    type NVARCHAR(100),
    description NVARCHAR(MAX),
    status BIT DEFAULT 1
);

-- Orders
CREATE TABLE orders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
	order_code VARCHAR(50) UNIQUE,
    user_id BIGINT,
    payment_method_id BIGINT,
    fullname NVARCHAR(255),
    phone_number VARCHAR(50),
    address NVARCHAR(MAX),
    note NVARCHAR(MAX),
    created_at DATETIME,
    updated_at DATETIME,
    coupon_id BIGINT,
    coupon_code VARCHAR(30),
    coupon_name NVARCHAR(255),
    coupon_type BIT,              -- PERCENT hoặc FIXED
    coupon_value DECIMAL(15,2),
    coupon_value_limit DECIMAL(15,2),
    coupon_discount_amount DECIMAL(15,2),
    type BIT, -- 0 off 1 onl
    status VARCHAR(50) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPING', 'DELIVERED', 'COMPLETED', 'CANCELLED')),
    total_money DECIMAL(15,2),            -- Tổng tiền trước giảm
    final_price DECIMAL(15,2),            -- Giá cuối cùng sau giảm
    shipping_date DATETIME,
	shipping_fee DECIMAL(15,2),
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
    promotion_code VARCHAR(50),
    promotion_name NVARCHAR(255),
    promotion_value DECIMAL(15,2),
    promotion_discount_amount DECIMAL(15,2),
    final_price DECIMAL(15,2),              
    status BIT DEFAULT 1,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_variant_id) REFERENCES product_variants(id)
);

ALTER TABLE order_details
ADD refund_status NVARCHAR(50) DEFAULT 'NONE' NOT NULL;

-- Reviews
CREATE TABLE reviews (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT,
    product_id BIGINT,
    order_detail_id BIGINT, -- thêm dòng này
    parent_id BIGINT NULL,
    content NVARCHAR(MAX),
    rating INT CHECK (rating BETWEEN 1 AND 5),
    is_hidden BIT,
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (order_detail_id) REFERENCES order_details(id), -- thêm dòng này
    FOREIGN KEY (parent_id) REFERENCES reviews(id)
);

-- Order Timeline
CREATE TABLE order_timeline (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT,
    type VARCHAR(50),
    description NVARCHAR(500),
    create_date DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Transactions
CREATE TABLE transactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL,
    transaction_code VARCHAR(50) UNIQUE,
    amount DECIMAL(18,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED')),
    vnp_txn_ref VARCHAR(100),
    transaction_date DATETIME DEFAULT GETDATE(),
    completed_date DATETIME NULL,
    note NVARCHAR(500),
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- VNPay Transaction Details
CREATE TABLE vnpay_transaction_details (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    vnp_txn_ref VARCHAR(100) NOT NULL,
    vnp_transaction_no VARCHAR(100),
    vnp_bank_code VARCHAR(20),
    vnp_card_type VARCHAR(20),
    vnp_pay_date VARCHAR(14),
    vnp_order_info NVARCHAR(255),
    vnp_response_code VARCHAR(2),
    vnp_transaction_status VARCHAR(2),
    request_url NVARCHAR(MAX),
    ipn_data NVARCHAR(MAX),
    return_data NVARCHAR(MAX),
    secure_hash VARCHAR(256),
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);

-- Password Reset Tokens
CREATE TABLE password_reset_tokens (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token NVARCHAR(255) NOT NULL UNIQUE,
  expiration_date DATETIME NOT NULL,
  used BIT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT GETDATE(),
  CONSTRAINT FK_PRT_USER FOREIGN KEY (user_id) REFERENCES users(id)
);


-- return_requests
CREATE TABLE return_requests (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    reason NVARCHAR(500),
    note NVARCHAR(MAX),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN (
            'PENDING','APPROVED','REJECTED','WAIT_FOR_PICKUP',
            'RETURNED','REFUNDED','CANCELLED','COMPLETED'
        )),
    refund_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_rr_user  FOREIGN KEY (user_id)  REFERENCES users(id),
    CONSTRAINT fk_rr_order FOREIGN KEY (order_id) REFERENCES orders(id)
);
CREATE INDEX idx_rr_user      ON return_requests(user_id);
CREATE INDEX idx_rr_order     ON return_requests(order_id);
CREATE INDEX idx_rr_status    ON return_requests(status);
CREATE INDEX idx_rr_created   ON return_requests(created_at);

-- refund_transactions (1–1 return_request, CASCADE OK)
CREATE TABLE refund_transactions (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    return_request_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    method VARCHAR(40) NOT NULL,
    reference_code VARCHAR(100),
    note NVARCHAR(500),
    created_by NVARCHAR(100),
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT uk_refund_return_request UNIQUE (return_request_id),
    CONSTRAINT fk_refund_return_request FOREIGN KEY (return_request_id)
        REFERENCES return_requests(id) ON DELETE CASCADE
);
CREATE INDEX idx_refund_created ON refund_transactions(created_at);

-- refund_infos: thông tin tài khoản KH để nhận tiền (1–1 return_request)
CREATE TABLE refund_infos (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  return_request_id BIGINT NOT NULL UNIQUE,
  method VARCHAR(40) NOT NULL,           -- CASH / BANK_TRANSFER / EWALLET
  bank_name NVARCHAR(200) NULL,
  account_number NVARCHAR(100) NULL,
  account_holder NVARCHAR(200) NULL,
  wallet_provider NVARCHAR(100) NULL,    -- Momo, ZaloPay...
  wallet_account NVARCHAR(100) NULL,     -- SĐT / email ví
  created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT fk_refundinfo_rr FOREIGN KEY (return_request_id)
    REFERENCES return_requests(id) ON DELETE CASCADE
);
CREATE INDEX idx_refundinfo_created ON refund_infos(created_at);

-- return_items (CASCADE về request)
CREATE TABLE return_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    return_request_id BIGINT NOT NULL,
    order_detail_id BIGINT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    -- GỢI Ý 1: bỏ trạng thái item nếu không dùng
    -- return_status VARCHAR(50) NULL,
    note NVARCHAR(MAX),
    CONSTRAINT fk_ri_rr   FOREIGN KEY (return_request_id)
        REFERENCES return_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_ri_od   FOREIGN KEY (order_detail_id)
        REFERENCES order_details(id)
);
CREATE INDEX idx_ri_rr   ON return_items(return_request_id);
CREATE INDEX idx_ri_od   ON return_items(order_detail_id);

-- return_item_images (CASCADE về item)
CREATE TABLE return_item_images (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    return_item_id BIGINT NOT NULL,
    image_url NVARCHAR(1000) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_rimg_ri FOREIGN KEY (return_item_id)
        REFERENCES return_items(id) ON DELETE CASCADE
);
CREATE INDEX idx_rimg_ri ON return_item_images(return_item_id);

-- trigger auto-update updated_at
GO
CREATE TRIGGER trg_rr_set_updated_at
ON return_requests
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE rr SET updated_at = SYSDATETIME()
    FROM return_requests rr
    INNER JOIN inserted i ON rr.id = i.id;
END
GO


INSERT INTO coupons (name, code, type, start_date, expiration_date, value, condition, created_at, updated_at, quantity, description, status) VALUES
('NEWCUSTOMER', 'cp000', 0, '2024-01-01', '2024-12-31', 200000.00, 60000, '2024-01-01 08:30:15', '2024-01-01 08:30:15', 100, N'Dành cho khách mới', 2),
('SUMMER2024', 'cp001', 0, '2024-06-01', '2024-08-31', 500000.00, 70000, '2024-05-15 14:22:08', '2024-05-15 14:22:08', 50, N'Khuyến mãi hè', 2),
('VIP100', 'cp002', 1, '2024-01-01', '2024-12-31', 100000.00, 80000, '2024-01-01 09:45:33', '2024-01-01 09:45:33', 20, N'VIP cho khách hàng thân thiết', 2),
('FREESHIP50', 'cp009', 0, '2024-03-01', '2024-12-31', 50000.00, 9000000, '2024-02-28 16:18:42', '2024-02-28 16:18:42', 999, N'Miễn phí ship đơn từ 9tr', 2),
('STUDENT15', 'cp007', 1, '2024-08-01', '2024-12-31', 15.00, 100000, '2024-07-25 11:55:27', '2024-07-25 11:55:27', 500, N'Giảm giá cho sinh viên', 2),
('RUNNER20', 'cp004', 1, '2024-01-01', '2024-12-31', 20.00, 999999, '2024-01-01 13:12:09', '2024-01-01 13:12:09', 77, N'Chương trình chạy bộ', 2);

INSERT INTO roles (name, description) VALUES
('Admin', N'Quản trị viên hệ thống với quyền truy cập đầy đủ'),
('Staff', N'Nhân viên bán hàng'),
('Customer', N'Khách hàng')

-- Insert into users
INSERT INTO users (role_id, avatar, fullname, gender, email, phone_number, address, password, created_at, updated_at, date_of_birth)
VALUES
(1, NULL, N'Nguyễn Văn A', 1, 'nguyenvana1@gmail.com', '0901000001', N'12 Lê Lợi, Hà Nội', 'passwordA1', '2024-01-01', '2024-01-01', '1990-01-01'),
(2, NULL, N'Lê Thị B', 0, 'lethib2@gmail.com', '0901000002', N'45 Phan Đình Phùng, TP.HCM', 'passwordB2', '2024-01-02', '2024-01-02', '1993-02-02'),
(3, NULL, N'Phạm Văn C', 1, 'phamvanc3@gmail.com', '0901000003', N'98 Trần Hưng Đạo, Đà Nẵng', 'passwordC3', '2024-01-03', '2024-01-03', '1992-03-03'),
(1, NULL, N'Hồ Thị D', 0, 'hothid4@gmail.com', '0901000004', N'23 Lê Duẩn, Huế', 'passwordD4', '2024-01-04', '2024-01-04', '1995-04-04'),
(2, NULL, N'Trần Văn E', 1, 'tranvane5@gmail.com', '0901000005', N'67 Nguyễn Trãi, Hải Phòng', 'passwordE5', '2024-01-05', '2024-01-05', '1991-05-05'),
(3, NULL, N'Vũ Thị F', 0, 'vuthif6@gmail.com', '0901000006', N'101 Nguyễn Huệ, Cần Thơ', 'passwordF6', '2024-01-06', '2024-01-06', '1990-06-06'),
(1, NULL, N'Đỗ Văn G', 1, 'dovang7@gmail.com', '0901000007', N'11 Quang Trung, Bắc Ninh', 'passwordG7', '2024-01-07', '2024-01-07', '1989-07-07'),
(2, NULL, N'Ngô Thị H', 0, 'ngothih8@gmail.com', '0901000008', N'321 Lý Thường Kiệt, Nha Trang', 'passwordH8', '2024-01-08', '2024-01-08', '1996-08-08'),
(3, NULL, N'Bùi Văn I', 1, 'buivani9@gmail.com', '0901000009', N'50 Hoàng Văn Thụ, Biên Hòa', 'passwordI9', '2024-01-09', '2024-01-09', '1993-09-09'),
(1, NULL, N'Mai Thị K', 0, 'maithik10@gmail.com', '0901000010', N'120 Đinh Tiên Hoàng, Vũng Tàu', 'passwordK10', '2024-01-10', '2024-01-10', '1992-10-10'),
(2, NULL, N'Phan Văn L', 1, 'phanvanl11@gmail.com', '0901000011', N'87 Nguyễn Đình Chiểu, Quy Nhơn', 'passwordL11', '2024-01-11', '2024-01-11', '1990-11-11'),
(3, NULL, N'Dương Thị M', 0, 'duongthim12@gmail.com', '0901000012', N'31 Đống Đa, Quảng Ninh', 'passwordM12', '2024-01-12', '2024-01-12', '1997-12-12'),
(1, NULL, N'Đặng Văn N', 1, 'dangvann13@gmail.com', '0901000013', N'76 Phạm Hùng, Thanh Hóa', 'passwordN13', '2024-01-13', '2024-01-13', '1994-01-13'),
(2, NULL, N'Tô Thị O', 0, 'tothio14@gmail.com', '0901000014', N'67 Cầu Giấy, Hà Nội', 'passwordO14', '2024-01-14', '2024-01-14', '1996-02-14'),
(3, NULL, N'Lý Văn P', 1, 'lyvanp15@gmail.com', '0901000015', N'210 Lạc Long Quân, TP.HCM', 'passwordP15', '2024-01-15', '2024-01-15', '1991-03-15'),
(1, NULL, N'Kiều Thị Q', 0, 'kieuthiq16@gmail.com', '0901000016', N'12 Nguyễn Sơn, Gia Lai', 'passwordQ16', '2024-01-16', '2024-01-16', '1995-04-16'),
(2, NULL, N'Chu Văn R', 1, 'chuvanr17@gmail.com', '0901000017', N'23 Trần Phú, Sóc Trăng', 'passwordR17', '2024-01-17', '2024-01-17', '1990-05-17'),
(3, NULL, N'Giáp Thị S', 0, 'giapthis18@gmail.com', '0901000018', N'89 Phan Bội Châu, Đồng Tháp', 'passwordS18', '2024-01-18', '2024-01-18', '1992-06-18'),
(1, NULL, N'Tống Văn T', 1, 'tongvant19@gmail.com', '0901000019', N'77 Võ Nguyên Giáp, Tây Ninh', 'passwordT19', '2024-01-19', '2024-01-19', '1993-07-19'),
(2, NULL, N'Hà Thị U', 0, 'hathiu20@gmail.com', '0901000020', N'16 Trần Cao Vân, Lâm Đồng', 'passwordU20', '2024-01-20', '2024-01-20', '1996-08-20');


-- Insert into tokens
INSERT INTO tokens (user_id, token, token_type, expiration_date) VALUES
(1, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...admin_token', 'ACCESS_TOKEN', '2024-07-15'),
(2, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...manager_token', 'ACCESS_TOKEN', '2024-07-16'),
(3, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...customer1_token', 'ACCESS_TOKEN', '2024-07-17'),
(4, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...customer2_token', 'REFRESH_TOKEN', '2024-12-18'),
(5, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...vip_token', 'ACCESS_TOKEN', '2024-07-19');

-- Insert into sizes
INSERT INTO sizes (value) VALUES
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
(N'Đen', N'#000000'),
(N'Trắng', N'#FFFFFF'),
(N'Xanh navy', N'#000080'),
(N'Đỏ', N'#FF0000'),
(N'Xám', N'#808080'),
(N'Xanh dương', N'#0066CC'),
(N'Vàng', N'#FFD700'),
(N'Cam', N'#FF6600'),
(N'Hồng', N'#FF69B4'),
(N'Xanh lá', N'#00CC66'),
(N'Tím', N'#800080'),
(N'Nâu', N'#8B4513'),
(N'Xanh da trời', N'#87CEEB'),
(N'Kem', N'#FFFDD0'), 
(N'Xanh rêu', N'#3B5323');

-- Insert into materials (với dấu tiếng Việt)
INSERT INTO materials (name, description) VALUES
(N'Mesh', N'Vật liệu lưới thoáng khí cho phần upper'),
(N'Leather', N'Da thật cao cấp, bền bỉ'),
(N'Synthetic Leather', N'Da tổng hợp nhẹ, dễ vệ sinh'),
(N'Canvas', N'Vải canvas dày dặn, phong cách casual'),
(N'Flyknit', N'Công nghệ dệt Flyknit của Nike'),
(N'Primeknit', N'Công nghệ dệt Primeknit của Adidas'),
(N'Rubber', N'Cao su tự nhiên cho đế giày'),
(N'EVA', N'Vật liệu đệm giữa EVA nhẹ');

-- Insert into brands (với dấu tiếng Việt)
INSERT INTO brands (name, description, country) VALUES
(N'Nike', N'Just Do It - Thương hiệu giày thể thao hàng đầu thế giới', N'USA'),
(N'Adidas', N'Impossible is Nothing - Thương hiệu giày thể thao Đức', N'Germany'),
(N'Puma', N'Forever Faster - Thương hiệu giày thể thao nổi tiếng', N'Germany'),
(N'Converse', N'Thương hiệu giày classic Mỹ', N'USA'),
(N'Vans', N'Off The Wall - Thương hiệu giày skateboard', N'USA'),
(N'New Balance', N'Thương hiệu giày chạy bộ cao cấp', N'USA'),
(N'ASICS', N'Anima Sana In Corpore Sano - Giày thể thao Nhật Bản', N'Japan'),
(N'Reebok', N'Be More Human - Thương hiệu giày thể thao', N'USA');

-- Insert into categories (với dấu tiếng Việt)
-- Insert into categories (với dấu tiếng Việt)
INSERT INTO categories (name, description) VALUES
(N'Giày chạy bộ', N'Giày thiết kế nhẹ, êm và hỗ trợ chạy đường dài hoặc luyện tập hàng ngày.'),
(N'Giày bóng đá', N'Giày chuyên dụng có đinh giúp bám sân cỏ khi chơi bóng đá.'),
(N'Giày bóng rổ', N'Giày có cổ cao, hỗ trợ cổ chân và đệm tốt cho việc nhảy và di chuyển linh hoạt.'),
(N'Giày lifestyle', N'Giày thời trang, thường mang tính casual, phù hợp với phong cách hàng ngày.'),
(N'Giày skateboard', N'Giày thiết kế bền chắc, đế phẳng để hỗ trợ trượt ván và thao tác kỹ thuật.'),
(N'Giày tennis', N'Giày hỗ trợ các chuyển động ngang, độ bám tốt trên sân cứng hoặc sân đất nện.'),
(N'Giày tập gym', N'Giày đa năng hỗ trợ tập luyện thể hình, cardio và các hoạt động trong phòng gym.'),
(N'Giày casual', N'Giày phong cách thường ngày, dễ phối đồ và mang lại sự thoải mái.');

SET IDENTITY_INSERT products ON;
INSERT INTO products (id, brand_id, category_id, sku, name, thumbnail, material_id, description, created_at, updated_at, status, product_gender, weight) VALUES
-- Giày chạy bộ (category_id = 1) - 8 sản phẩm
(48,1,1,N'SP100038',N'Giày Thể Thao Nike Unisex Beatspec-L',N'thumb38.jpg',6,N'Giày Thể Thao Nike unisex chạy bộ','2024-03-10','2024-03-10',1,N'Unisex',0.48),
(50,2,1,N'SP100040',N'Giày Thể Thao Adidas Nam Keiran',N'thumb40.jpg',8,N'Giày Thể Thao Adidas nam chạy bộ','2024-03-12','2024-03-12',1,N'Male',0.54),
(52,3,1,N'SP100042',N'Giày Thể Thao Puma Unisex Quicklane',N'thumb42.jpg',2,N'Giày Thể Thao Puma unisex tốc độ','2024-03-14','2024-03-14',1,N'Unisex',0.50),
(53,1,1,N'SP100043',N'Giày Thể Thao Nike Nữ Chicsneaker',N'thumb43.jpg',3,N'Giày Thể Thao Nike nữ thể thao','2024-03-15','2024-03-15',1,N'Female',0.38),
(54,7,1,N'SP100044',N'Giày Thể Thao ASICS Nam Scottiee',N'thumb44.jpg',4,N'Giày Thể Thao ASICS nam chuyên nghiệp','2024-03-16','2024-03-16',1,N'Male',0.55),
(55,2,1,N'SP100045',N'Giày Thể Thao Adidas Nữ Chicsneaker2',N'thumb45.jpg',5,N'Giày Thể Thao Adidas nữ năng động','2024-03-17','2024-03-17',1,N'Female',0.39),
(56,8,1,N'SP100046',N'Giày Thể Thao Reebok Unisex Wavespec',N'thumb46.jpg',6,N'Giày Thể Thao Reebok unisex năng động','2024-03-18','2024-03-18',1,N'Unisex',0.47),
(57,3,1,N'SP100047',N'Giày Thể Thao Puma Nữ Deevale',N'thumb47.jpg',7,N'Giày Thể Thao Puma nữ phong cách','2024-03-19','2024-03-19',1,N'Female',0.36),

-- Giày bóng đá (category_id = 2) - 8 sản phẩm
(58,4,2,N'SP100048',N'Giày Thể Thao Converse Nữ Dilathielle',N'thumb48.jpg',8,N'Giày Thể Thao Converse nữ cá tính','2024-03-20','2024-03-20',1,N'Female',0.34),
(59,5,2,N'SP100049',N'Giày Thể Thao Vans Unisex Elabrintar',N'thumb49.jpg',1,N'Giày Thể Thao Vans unisex năng động','2024-03-21','2024-03-21',1,N'Unisex',0.45),
(60,8,2,N'SP100050',N'Giày Thể Thao Reebok Nữ Greenspan',N'thumb50.jpg',2,N'Giày Thể Thao Reebok nữ thời trang','2024-03-22','2024-03-22',1,N'Female',0.40),
(61,7,2,N'SP100051',N'Giày Thể Thao ASICS Nữ Pillowsnkr-L',N'thumb51.jpg',3,N'Giày Thể Thao ASICS nữ thoải mái','2024-03-23','2024-03-23',1,N'Female',0.43),
(62,6,2,N'SP100052',N'Giày Thể Thao New Balance Unisex Saxony',N'thumb52.jpg',4,N'Giày Thể Thao New Balance unisex cao cấp','2024-03-24','2024-03-24',1,N'Unisex',0.56),
(1,1,2,N'SP100001',N'Giày Lười Nike Nam Softcourt',N'thumb1.jpg',1,N'Giày lười Nike nam phong cách thời trang','2024-02-01','2024-02-01',1,N'Male',0.45),
(2,1,2,N'SP100002',N'Giày Nike Nữ Clublux',N'thumb2.jpg',2,N'Giày Nike nữ thời trang','2024-02-02','2024-02-02',1,N'Female',0.35),
(4,2,2,N'SP100003',N'Giày Adidas Unisex Clubmtl',N'thumb3.jpg',3,N'Giày Adidas unisex năng động','2024-02-03','2024-02-03',1,N'Unisex',0.50),

-- Giày bóng rổ (category_id = 3) - 8 sản phẩm
(5,3,3,N'SP100004',N'Giày Puma Nam Marty',N'thumb4.jpg',4,N'Giày Puma nam chất lượng','2024-02-04','2024-02-04',1,N'Male',0.48),
(6,2,3,N'SP100005',N'Giày Adidas Nữ Clublux1',N'thumb5.jpg',5,N'Giày Adidas nữ phong cách','2024-02-05','2024-02-05',1,N'Female',0.38),
(8,5,3,N'SP100006',N'Giày Slips On Vans Unisex Leejay',N'thumb6.jpg',6,N'Giày Slips On Vans unisex dễ mang','2024-02-06','2024-02-06',1,N'Unisex',0.32),
(9,3,3,N'SP100007',N'Giày Puma Nữ Sweetthing',N'thumb7.jpg',7,N'Giày Puma nữ dễ thương','2024-02-07','2024-02-07',1,N'Female',0.36),
(10,1,3,N'SP100008',N'Giày Sneaker Nike Nam Derryk',N'thumb8.jpg',8,N'Giày Sneaker Nike nam cho mọi phong cách','2024-02-08','2024-02-08',1,N'Male',0.52),
(12,2,3,N'SP100009',N'Giày Sneaker Adidas Nam Kylian',N'thumb9.jpg',1,N'Giày Sneaker Adidas nam phối màu đẹp','2024-02-09','2024-02-09',1,N'Male',0.55),
(13,5,3,N'SP100010',N'Giày Sneaker Vans Nữ Motionxx',N'thumb10.jpg',2,N'Giày Sneaker Vans nữ năng động','2024-02-10','2024-02-10',1,N'Female',0.42),
(14,3,3,N'SP100011',N'Giày Sneaker Puma Unisex Motionx',N'thumb11.jpg',3,N'Giày Sneaker Puma unisex chuyên nghiệp','2024-02-11','2024-02-11',1,N'Unisex',0.51),

-- Giày lifestyle (category_id = 4) - 8 sản phẩm
(16,7,4,N'SP100012',N'Giày Sneaker ASICS Nam Scottie',N'thumb12.jpg',4,N'Giày Sneaker ASICS nam chất lượng','2024-02-12','2024-02-12',1,N'Male',0.49),
(17,1,4,N'SP100013',N'Giày Sneakers Nike Nữ Caroteriel',N'thumb13.jpg',5,N'Giày Sneakers Nike nữ thanh lịch','2024-02-13','2024-02-13',1,N'Female',0.39),
(18,6,4,N'SP100014',N'Giày Sneaker New Balance Unisex Zethan',N'thumb14.jpg',6,N'Giày Sneaker New Balance unisex đẳng cấp','2024-02-14','2024-02-14',1,N'Unisex',0.56),
(20,1,4,N'SP100015',N'Giày Sneakers Nike Nam Clubkicks',N'thumb15.jpg',7,N'Giày Sneakers Nike nam thể thao','2024-02-15','2024-02-15',1,N'Male',0.53),
(21,2,4,N'SP100016',N'Giày Sneakers Adidas Nữ Caroteriel 1',N'thumb16.jpg',8,N'Giày Sneakers Adidas nữ hiện đại','2024-02-16','2024-02-16',1,N'Female',0.40),
(22,2,4,N'SP100017',N'Giày Sneakers Adidas Nam Coelin',N'thumb17.jpg',1,N'Giày Sneakers Adidas nam hiện đại','2024-02-17','2024-02-17',1,N'Male',0.54),
(24,3,4,N'SP100018',N'Giày Sneakers Puma Nam Jensen',N'thumb18.jpg',2,N'Giày Sneakers Puma nam cá tính','2024-02-18','2024-02-18',1,N'Male',0.47),
(25,3,4,N'SP100019',N'Giày Sneakers Puma Nữ Chicsneaker',N'thumb19.jpg',3,N'Giày Sneakers Puma nữ sành điệu','2024-02-19','2024-02-19',1,N'Female',0.37),

-- Giày skateboard (category_id = 5) - 8 sản phẩm
(26,1,5,N'SP100020',N'Giày Sneakers Nike Air Max 97 Unisex - Đỏ',N'thumb20.jpg',4,N'Giày Sneakers Nike Air Max 97 unisex màu đỏ nổi bật','2024-02-20','2024-02-20',1,N'Unisex',0.58),
(28,4,5,N'SP100021',N'Giày Sneakers Converse Nữ Clubchic',N'thumb21.jpg',5,N'Giày Sneakers Converse nữ thời trang','2024-02-21','2024-02-21',1,N'Female',0.34),
(29,1,5,N'SP100022',N'Giày Sneakers Nike Blazer Low ''77 Vintage Unisex - Trắng',N'thumb22.jpg',6,N'Giày Sneakers Nike Blazer Low vintage unisex trắng','2024-02-22','2024-02-22',1,N'Unisex',0.49),
(30,5,5,N'SP100023',N'Giày Sneakers Vans Nữ Clubstyle',N'thumb23.jpg',7,N'Giày Sneakers Vans nữ phong cách','2024-02-23','2024-02-23',1,N'Female',0.41),
(32,1,5,N'SP100024',N'Giày Sneakers Nike Court Legacy Next Natural Unisex - Đen',N'thumb24.jpg',8,N'Giày Sneakers Nike Court Legacy unisex màu đen','2024-02-24','2024-02-24',1,N'Unisex',0.50),
(33,7,5,N'SP100025',N'Giày Sneakers ASICS Nữ Easyinwedge',N'thumb25.jpg',1,N'Giày Sneakers ASICS nữ đa năng','2024-02-25','2024-02-25',1,N'Female',0.43),
(34,1,5,N'SP100026',N'Giày Sneakers Nike Court Vision Mid Next Natural Nam - Them Anh',N'thumb26.jpg',2,N'Giày Sneakers Nike Court Vision Mid nam','2024-02-26','2024-02-26',1,N'Male',0.57),
(36,6,5,N'SP100027',N'Giày Sneakers New Balance Nữ Enzie',N'thumb27.jpg',3,N'Giày Sneakers New Balance nữ tinh tế','2024-02-27','2024-02-27',1,N'Female',0.44),

-- Giày tennis (category_id = 6) - 8 sản phẩm
(37,5,6,N'SP100028',N'Giày Sneakers Vans Unisex Omono',N'thumb28.jpg',4,N'Giày Sneakers Vans unisex phong cách','2024-02-28','2024-02-28',1,N'Unisex',0.46),
(38,8,6,N'SP100029',N'Giày Sneakers Reebok Nữ Foremore',N'thumb29.jpg',5,N'Giày Sneakers Reebok nữ năng động','2024-03-01','2024-03-01',1,N'Female',0.38),
(40,4,6,N'SP100030',N'Giày Sneakers Converse Unisex Poker',N'thumb30.jpg',6,N'Giày Sneakers Converse unisex cổ điển','2024-03-02','2024-03-02',1,N'Unisex',0.35),
(41,1,6,N'SP100031',N'Giày Sneakers Nike Nữ Garelia',N'thumb31.jpg',7,N'Giày Sneakers Nike nữ thanh lịch','2024-03-03','2024-03-03',1,N'Female',0.40),
(42,8,6,N'SP100032',N'Giày Sneakers Reebok Nam Uptown',N'thumb32.jpg',8,N'Giày Sneakers Reebok nam thời trang','2024-03-04','2024-03-04',1,N'Male',0.51),
(43,2,6,N'SP100033',N'Giày Sneakers Adidas Nữ Griedia',N'thumb33.jpg',1,N'Giày Sneakers Adidas nữ trẻ trung','2024-03-05','2024-03-05',1,N'Female',0.39),
(44,7,6,N'SP100034',N'Giày Sneakers ASICS Unisex Wexley',N'thumb34.jpg',2,N'Giày Sneakers ASICS unisex chất lượng','2024-03-06','2024-03-06',1,N'Unisex',0.52),
(45,3,6,N'SP100035',N'Giày Sneakers Puma Nữ Halerenna',N'thumb35.jpg',3,N'Giày Sneakers Puma nữ cá tính','2024-03-07','2024-03-07',1,N'Female',0.37),

-- Giày tập gym (category_id = 7) - 8 sản phẩm
(46,6,7,N'SP100036',N'Giày Sneakers New Balance Nam Willio',N'thumb36.jpg',4,N'Giày Sneakers New Balance nam bền bỉ','2024-03-08','2024-03-08',1,N'Male',0.59),
(47,4,7,N'SP100037',N'Giày Sneakers Converse Nữ Palazzi',N'thumb37.jpg',5,N'Giày Sneakers Converse nữ đáng yêu','2024-03-09','2024-03-09',1,N'Female',0.33),
(49,5,7,N'SP100039',N'Giày Sneakers Vans Nữ Rongan',N'thumb39.jpg',7,N'Giày Sneakers Vans nữ cá tính','2024-03-11','2024-03-11',1,N'Female',0.41),
(51,7,7,N'SP100041',N'Giày Sneakers ASICS Nữ Stepcount',N'thumb41.jpg',1,N'Giày Sneakers ASICS nữ đa năng','2024-03-13','2024-03-13',1,N'Female',0.42),
(3,2,7,N'SP100053',N'Giày Sneaker Trẻ Em Adidas Marvel''s Iron Man Racer - Đỏ',N'thumb53.jpg',5,N'Giày Sneaker Adidas trẻ em màu đỏ Iron Man','2024-03-25','2024-03-25',1,N'Kids',0.25),
(7,1,7,N'SP100054',N'Giày Sneaker Trẻ Em Nike Flex Runner 3 (PS)',N'thumb54.jpg',6,N'Giày Sneaker Nike trẻ em linh hoạt','2024-03-26','2024-03-26',1,N'Kids',0.28),
(11,1,7,N'SP100055',N'Giày Sneaker Trẻ Em Nike Team Hustle D 11 (PS) - Trắng',N'thumb55.jpg',7,N'Giày Sneaker Nike trẻ em màu trắng','2024-03-27','2024-03-27',1,N'Kids',0.26),
(15,3,7,N'SP100056',N'Giày Sneaker Trẻ Em Puma RS-X Playstation - Xám',N'thumb56.jpg',8,N'Giày Sneaker Puma trẻ em màu xám Playstation','2024-03-28','2024-03-28',1,N'Kids',0.30),

-- Giày casual (category_id = 8) - 6 sản phẩm
(19,3,8,N'SP100057',N'Giày Sneaker Trẻ Em Puma Trolls 2.0 Caven - Trắng',N'thumb57.jpg',1,N'Giày Sneaker Puma trẻ em màu trắng Trolls','2024-03-29','2024-03-29',1,N'Kids',0.24),
(23,1,8,N'SP100058',N'Giày Thời Trang Bé Gái Nike Revolution 7 (GS)',N'thumb58.jpg',2,N'Giày thời trang Nike bé gái Revolution 7','2024-03-30','2024-03-30',1,N'Kids',0.27),
(27,4,8,N'SP100059',N'Giày Thời Trang Trẻ Em Converse Chuck Taylor All Star',N'thumb59.jpg',3,N'Giày thời trang Converse trẻ em Chuck Taylor','2024-03-31','2024-03-31',1,N'Kids',0.29),
(31,1,8,N'SP100060',N'Giày Thời Trang Trẻ Em Nike Revolution 7 (PSV)',N'thumb60.jpg',4,N'Giày thời trang Nike trẻ em Revolution 7','2024-04-01','2024-04-01',1,N'Kids',0.26),
(35,3,8,N'SP100061',N'Giày Sneaker Trẻ Em Puma Palermo Leather Lace Up',N'thumb61.jpg',5,N'Giày Sneaker Puma trẻ em da Palermo','2024-04-02','2024-04-02',1,N'Kids',0.31),
(39,3,8,N'SP100062',N'Giày Sneaker Trẻ Em Puma Palermo Leather Lace Up Jr',N'thumb62.jpg',6,N'Giày Sneaker Puma trẻ em da Palermo Jr','2024-04-03','2024-04-03',1,N'Kids',0.32);
SET IDENTITY_INSERT products OFF;

SET IDENTITY_INSERT product_variants ON;
INSERT INTO product_variants (id, product_id, size_id, color_id, thumbnail, cost_price, sell_price, quantity, status) VALUES
-- GIÀY LƯỜI NAM SOFTCOURT (ID: 1) - Màu Xanh dương
(1, 1, 2, 6, NULL, 960000, 1200000, 25, 1),
(2, 1, 3, 6, NULL, 960000, 1200000, 18, 1),
(3, 1, 4, 6, NULL, 960000, 1200000, 32, 1),
(4, 1, 5, 6, NULL, 960000, 1200000, 15, 1),

-- GIÀY NIKE NỮ CLUBLUX (ID: 2) - Màu Đen, Trắng
(5, 2, 1, 1, NULL, 920000, 1150000, 20, 1),
(6, 2, 2, 1, NULL, 920000, 1150000, 30, 1),
(7, 2, 3, 1, NULL, 920000, 1150000, 25, 1),
(8, 2, 1, 2, NULL, 880000, 1100000, 22, 1),
(9, 2, 2, 2, NULL, 880000, 1100000, 28, 1),
(10, 2, 3, 2, NULL, 880000, 1100000, 35, 1),

-- GIÀY SNEAKER TRẺ EM ADIDAS MARVEL'S IRON MAN RACER (ID: 3) - Màu Đỏ, Xanh
(11, 3, 1, 4, NULL, 640000, 800000, 15, 1),
(12, 3, 2, 4, NULL, 640000, 800000, 12, 1),
(13, 3, 3, 4, NULL, 640000, 800000, 18, 1),
(14, 3, 1, 6, NULL, 600000, 750000, 20, 1),
(15, 3, 2, 6, NULL, 600000, 750000, 16, 1),

-- GIÀY ADIDAS NAM CLUBMTL (ID: 4) - Màu Trắng, Trắng_navy, Xanh-dương, Xanh-lá
(16, 4, 4, 2, NULL, 1040000, 1300000, 22, 1),
(17, 4, 5, 2, NULL, 1040000, 1300000, 28, 1),
(18, 4, 6, 2, NULL, 1040000, 1300000, 35, 1),
(19, 4, 4, 3, NULL, 1080000, 1350000, 18, 1),
(20, 4, 5, 3, NULL, 1080000, 1350000, 25, 1),
(21, 4, 6, 3, NULL, 1080000, 1350000, 30, 1),
(22, 4, 4, 6, NULL, 1056000, 1320000, 20, 1),
(23, 4, 5, 6, NULL, 1056000, 1320000, 26, 1),
(24, 4, 4, 10, NULL, 1104000, 1380000, 15, 1),
(25, 4, 5, 10, NULL, 1104000, 1380000, 22, 1),
(26, 4, 6, 10, NULL, 1104000, 1380000, 28, 1),

-- GIÀY PUMA NAM MARTY (ID: 5) - Màu Đen, Kem
(27, 5, 4, 1, NULL, 1000000, 1250000, 25, 1),
(28, 5, 5, 1, NULL, 1000000, 1250000, 32, 1),
(29, 5, 6, 1, NULL, 1000000, 1250000, 28, 1),
(30, 5, 4, 14, NULL, 960000, 1200000, 20, 1),
(31, 5, 5, 14, NULL, 960000, 1200000, 24, 1),
(32, 5, 6, 14, NULL, 960000, 1200000, 30, 1),

-- GIÀY ADIDAS NỮ CLUBLUX1 (ID: 6) - Màu Đen, Trắng
(33, 6, 1, 1, NULL, 944000, 1180000, 18, 1),
(34, 6, 2, 1, NULL, 944000, 1180000, 25, 1),
(35, 6, 3, 1, NULL, 944000, 1180000, 22, 1),
(36, 6, 1, 2, NULL, 920000, 1150000, 20, 1),
(37, 6, 2, 2, NULL, 920000, 1150000, 28, 1),
(38, 6, 3, 2, NULL, 920000, 1150000, 32, 1),

-- GIÀY SNEAKER TRẺ EM NIKE FLEX RUNNER 3 (ID: 7) - Màu Đen
(39, 7, 1, 1, NULL, 600000, 750000, 15, 1),
(40, 7, 2, 1, NULL, 600000, 750000, 20, 1),
(41, 7, 3, 1, NULL, 600000, 750000, 18, 1),
(42, 7, 4, 1, NULL, 600000, 750000, 22, 1),

-- GIÀY SLIPS ON VANS NAM LEEJAY (ID: 8) - Màu Đen, Xanh_navy
(43, 8, 4, 1, NULL, 880000, 1100000, 25, 1),
(44, 8, 5, 1, NULL, 880000, 1100000, 30, 1),
(45, 8, 6, 1, NULL, 880000, 1100000, 28, 1),
(46, 8, 4, 3, NULL, 896000, 1120000, 20, 1),
(47, 8, 5, 3, NULL, 896000, 1120000, 25, 1),

-- GIÀY PUMA NỮ SWEETTHING (ID: 9) - Màu Kem
(48, 9, 1, 14, NULL, 840000, 1050000, 22, 1),
(49, 9, 2, 14, NULL, 840000, 1050000, 28, 1),
(50, 9, 3, 14, NULL, 840000, 1050000, 25, 1),
(51, 9, 4, 14, NULL, 840000, 1050000, 30, 1),

-- GIÀY SNEAKER NIKE NAM DERRYK (ID: 10) - Màu Trắng
(52, 10, 4, 2, NULL, 1080000, 1350000, 20, 1),
(53, 10, 5, 2, NULL, 1080000, 1350000, 25, 1),
(54, 10, 6, 2, NULL, 1080000, 1350000, 30, 1),
(55, 10, 7, 2, NULL, 1080000, 1350000, 18, 1),

-- GIÀY SNEAKER TRẺ EM NIKE TEAM HUSTLE D 11 (ID: 11) - Màu Xám
(56, 11, 1, 5, NULL, 624000, 780000, 15, 1),
(57, 11, 2, 5, NULL, 624000, 780000, 20, 1),
(58, 11, 3, 5, NULL, 624000, 780000, 18, 1),

-- GIÀY SNEAKER ADIDAS NAM KYLIAN (ID: 12) - Màu Đen
(59, 12, 4, 1, NULL, 1120000, 1400000, 22, 1),
(60, 12, 5, 1, NULL, 1120000, 1400000, 28, 1),
(61, 12, 6, 1, NULL, 1120000, 1400000, 25, 1),
(62, 12, 7, 1, NULL, 1120000, 1400000, 30, 1),

-- GIÀY SNEAKER VANS NỮ MOTIONXX (ID: 13) - Màu Trắng
(63, 13, 1, 2, NULL, 960000, 1200000, 20, 1),
(64, 13, 2, 2, NULL, 960000, 1200000, 25, 1),
(65, 13, 3, 2, NULL, 960000, 1200000, 22, 1),
(66, 13, 4, 2, NULL, 960000, 1200000, 28, 1),

-- GIÀY SNEAKER PUMA NAM MOTIONX (ID: 14) - Màu Trắng
(67, 14, 4, 2, NULL, 1024000, 1280000, 25, 1),
(68, 14, 5, 2, NULL, 1024000, 1280000, 30, 1),
(69, 14, 6, 2, NULL, 1024000, 1280000, 28, 1),
(70, 14, 7, 2, NULL, 1024000, 1280000, 32, 1),

-- GIÀY SNEAKER TRẺ EM PUMA RS-X PLAYSTATION (ID: 15) - Màu Xám
(71, 15, 1, 5, NULL, 680000, 850000, 12, 1),
(72, 15, 2, 5, NULL, 680000, 850000, 15, 1),
(73, 15, 3, 5, NULL, 680000, 850000, 18, 1),
(74, 15, 4, 5, NULL, 680000, 850000, 20, 1),

-- GIÀY SNEAKER ASICS NAM SCOTTIE (ID: 16) - Màu Nâu, Trắng
(75, 16, 4, 12, NULL, 1080000, 1350000, 20, 1),
(76, 16, 5, 12, NULL, 1080000, 1350000, 25, 1),
(77, 16, 6, 12, NULL, 1080000, 1350000, 28, 1),
(78, 16, 4, 2, NULL, 1040000, 1300000, 22, 1),
(79, 16, 5, 2, NULL, 1040000, 1300000, 30, 1),
(80, 16, 6, 2, NULL, 1040000, 1300000, 25, 1),

-- GIÀY SNEAKERS NIKE NỮ CAROTERIEL (ID: 17) - Màu Kem, Trắng
(81, 17, 1, 14, NULL, 944000, 1180000, 18, 1),
(82, 17, 2, 14, NULL, 944000, 1180000, 22, 1),
(83, 17, 3, 14, NULL, 944000, 1180000, 25, 1),
(84, 17, 1, 2, NULL, 920000, 1150000, 20, 1),
(85, 17, 2, 2, NULL, 920000, 1150000, 28, 1),
(86, 17, 3, 2, NULL, 920000, 1150000, 30, 1),

-- GIÀY SNEAKER NEW BALANCE NAM ZETHAN (ID: 18) - Màu Trắng
(87, 18, 4, 2, NULL, 1160000, 1450000, 20, 1),
(88, 18, 5, 2, NULL, 1160000, 1450000, 25, 1),
(89, 18, 6, 2, NULL, 1160000, 1450000, 28, 1),
(90, 18, 7, 2, NULL, 1160000, 1450000, 22, 1),

-- GIÀY SNEAKER TRẺ EM PUMA TROLLS 2.0 CAVEN (ID: 19) - Màu Trắng
(91, 19, 1, 2, NULL, 576000, 720000, 15, 1),
(92, 19, 2, 2, NULL, 576000, 720000, 18, 1),
(93, 19, 3, 2, NULL, 576000, 720000, 20, 1),
(94, 19, 4, 2, NULL, 576000, 720000, 16, 1),

-- GIÀY SNEAKERS NIKE NAM CLUBKICKS (ID: 20) - Màu Đen, Kem
(95, 20, 4, 1, NULL, 1056000, 1320000, 22, 1),
(96, 20, 5, 1, NULL, 1056000, 1320000, 28, 1),
(97, 20, 6, 1, NULL, 1056000, 1320000, 25, 1),
(98, 20, 4, 14, NULL, 1024000, 1280000, 20, 1),
(99, 20, 5, 14, NULL, 1024000, 1280000, 24, 1),
(100, 20, 6, 14, NULL, 1024000, 1280000, 30, 1),

-- GIÀY SNEAKERS ADIDAS NỮ CAROTERIEL 1 (ID: 21) - Màu Kem, Trắng
(101, 21, 1, 14, NULL, 960000, 1200000, 18, 1),
(102, 21, 2, 14, NULL, 960000, 1200000, 22, 1),
(103, 21, 3, 14, NULL, 960000, 1200000, 25, 1),
(104, 21, 1, 2, NULL, 944000, 1180000, 20, 1),
(105, 21, 2, 2, NULL, 944000, 1180000, 28, 1),
(106, 21, 3, 2, NULL, 944000, 1180000, 30, 1),

-- GIÀY SNEAKERS ADIDAS NAM COELIN (ID: 22) - Màu Nâu, Trắng
(107, 22, 4, 12, NULL, 1000000, 1250000, 20, 1),
(108, 22, 5, 12, NULL, 1000000, 1250000, 25, 1),
(109, 22, 6, 12, NULL, 1000000, 1250000, 28, 1),
(110, 22, 4, 2, NULL, 976000, 1220000, 22, 1),
(111, 22, 5, 2, NULL, 976000, 1220000, 30, 1),
(112, 22, 6, 2, NULL, 976000, 1220000, 25, 1),

-- GIÀY THỜI TRANG BÉ GÁI NIKE REVOLUTION 7 (ID: 23) - Màu Cam, Tím, Xám
(113, 23, 1, 8, NULL, 544000, 680000, 15, 1),
(114, 23, 2, 8, NULL, 544000, 680000, 18, 1),
(115, 23, 3, 8, NULL, 544000, 680000, 20, 1),
(116, 23, 1, 11, NULL, 560000, 700000, 12, 1),
(117, 23, 2, 11, NULL, 560000, 700000, 16, 1),
(118, 23, 1, 5, NULL, 520000, 650000, 18, 1),
(119, 23, 2, 5, NULL, 520000, 650000, 22, 1),

-- GIÀY SNEAKERS PUMA NAM JENSEN (ID: 24) - Màu Kem, Trắng
(120, 24, 4, 14, NULL, 944000, 1180000, 20, 1),
(121, 24, 5, 14, NULL, 944000, 1180000, 25, 1),
(122, 24, 6, 14, NULL, 944000, 1180000, 28, 1),
(123, 24, 4, 2, NULL, 920000, 1150000, 22, 1),
(124, 24, 5, 2, NULL, 920000, 1150000, 30, 1),
(125, 24, 6, 2, NULL, 920000, 1150000, 25, 1),

-- GIÀY SNEAKERS PUMA NỮ CHICSNEAKER (ID: 25) - Màu Tím
(126, 25, 1, 11, NULL, 896000, 1120000, 18, 1),
(127, 25, 2, 11, NULL, 896000, 1120000, 22, 1),
(128, 25, 3, 11, NULL, 896000, 1120000, 25, 1),
(129, 25, 4, 11, NULL, 896000, 1120000, 20, 1),

-- GIÀY SNEAKERS NIKE AIR MAX 97 (ID: 26) - Màu Đỏ
(130, 26, 4, 4, NULL, 1440000, 1800000, 15, 1),
(131, 26, 5, 4, NULL, 1440000, 1800000, 18, 1),
(132, 26, 6, 4, NULL, 1440000, 1800000, 20, 1),
(133, 26, 7, 4, NULL, 1440000, 1800000, 22, 1),

-- GIÀY THỜI TRANG TRẺ EM CONVERSE CHUCK TAYLOR ALL STAR (ID: 27) - Màu Đen
(134, 27, 1, 1, NULL, 520000, 650000, 15, 1),
(135, 27, 2, 1, NULL, 520000, 650000, 18, 1),
(136, 27, 3, 1, NULL, 520000, 650000, 20, 1),
(137, 27, 4, 1, NULL, 520000, 650000, 16, 1),

-- GIÀY SNEAKERS CONVERSE NỮ CLUBCHIC (ID: 28) - Màu Kem, Trắng
(138, 28, 1, 14, NULL, 784000, 980000, 20, 1),
(139, 28, 2, 14, NULL, 784000, 980000, 25, 1),
(140, 28, 3, 14, NULL, 784000, 980000, 22, 1),
(141, 28, 1, 2, NULL, 760000, 950000, 18, 1),
(142, 28, 2, 2, NULL, 760000, 950000, 28, 1),
(143, 28, 3, 2, NULL, 760000, 950000, 30, 1),

-- GIÀY SNEAKERS NIKE BLAZER LOW '77 VINTAGE (ID: 29) - Màu Cam, Đen
(144, 29, 4, 8, NULL, 1320000, 1650000, 15, 1),
(145, 29, 5, 8, NULL, 1320000, 1650000, 18, 1),
(146, 29, 6, 8, NULL, 1320000, 1650000, 20, 1),
(147, 29, 4, 1, NULL, 1296000, 1620000, 22, 1),
(148, 29, 5, 1, NULL, 1296000, 1620000, 25, 1),
(149, 29, 6, 1, NULL, 1296000, 1620000, 28, 1),

-- GIÀY SNEAKERS VANS NỮ CLUBSTYLE (ID: 30) - Màu Tím
(150, 30, 1, 11, NULL, 920000, 1150000, 18, 1),
(151, 30, 2, 11, NULL, 920000, 1150000, 22, 1),
(152, 30, 3, 11, NULL, 920000, 1150000, 25, 1),
(153, 30, 4, 11, NULL, 920000, 1150000, 20, 1),

-- GIÀY THỜI TRANG TRẺ EM NIKE REVOLUTION 7 (ID: 31) - Màu Đen, Hồng, Xám
(154, 31, 1, 1, NULL, 560000, 700000, 15, 1),
(155, 31, 2, 1, NULL, 560000, 700000, 18, 1),
(156, 31, 3, 1, NULL, 560000, 700000, 20, 1),
(157, 31, 1, 9, NULL, 576000, 720000, 12, 1),
(158, 31, 2, 9, NULL, 576000, 720000, 16, 1),
(159, 31, 1, 5, NULL, 544000, 680000, 18, 1),
(160, 31, 2, 5, NULL, 544000, 680000, 22, 1),

-- GIÀY SNEAKERS NIKE COURT LEGACY NEXT NATURAL (ID: 32) - Màu Đen, Trắng
(161, 32, 4, 1, NULL, 1024000, 1280000, 20, 1),
(162, 32, 5, 1, NULL, 1024000, 1280000, 25, 1),
(163, 32, 6, 1, NULL, 1024000, 1280000, 28, 1),
(164, 32, 4, 2, NULL, 1000000, 1250000, 22, 1),
(165, 32, 5, 2, NULL, 1000000, 1250000, 30, 1),
(166, 32, 6, 2, NULL, 1000000, 1250000, 25, 1),

-- GIÀY SNEAKERS ASICS NỮ EASYINWEDGE (ID: 33) - Màu Đen, Trắng
(167, 33, 1, 1, NULL, 1104000, 1380000, 18, 1),
(168, 33, 2, 1, NULL, 1104000, 1380000, 22, 1),
(169, 33, 3, 1, NULL, 1104000, 1380000, 25, 1),
(170, 33, 1, 2, NULL, 1080000, 1350000, 20, 1),
(171, 33, 2, 2, NULL, 1080000, 1350000, 28, 1),
(172, 33, 3, 2, NULL, 1080000, 1350000, 30, 1),

-- GIÀY SNEAKERS NIKE COURT VISION MID NEXT NATURAL (ID: 34) - Màu Đen
(173, 34, 4, 1, NULL, 1136000, 1420000, 20, 1),
(174, 34, 5, 1, NULL, 1136000, 1420000, 25, 1),
(175, 34, 6, 1, NULL, 1136000, 1420000, 28, 1),
(176, 34, 7, 1, NULL, 1136000, 1420000, 22, 1),

-- GIÀY SNEAKER TRẺ EM PUMA PALERMO LEATHER LACE UP (ID: 35) - Màu Hồng
(177, 35, 1, 9, NULL, 624000, 780000, 15, 1),
(178, 35, 2, 9, NULL, 624000, 780000, 18, 1),
(179, 35, 3, 9, NULL, 624000, 780000, 20, 1),
(180, 35, 4, 9, NULL, 624000, 780000, 16, 1),

-- GIÀY SNEAKERS NEW BALANCE NỮ ENZIE (ID: 36) - Màu Trắng
(181, 36, 1, 2, NULL, 1160000, 1450000, 18, 1),
(182, 36, 2, 2, NULL, 1160000, 1450000, 22, 1),
(183, 36, 3, 2, NULL, 1160000, 1450000, 25, 1),
(184, 36, 4, 2, NULL, 1160000, 1450000, 20, 1),

-- GIÀY SNEAKERS VANS NAM OMONO (ID: 37) - Màu Đen, Nâu
(185, 37, 4, 1, NULL, 960000, 1200000, 20, 1),
(186, 37, 5, 1, NULL, 960000, 1200000, 25, 1),
(187, 37, 6, 1, NULL, 960000, 1200000, 28, 1),
(188, 37, 4, 12, NULL, 976000, 1220000, 22, 1),
(189, 37, 5, 12, NULL, 976000, 1220000, 30, 1),
(190, 37, 6, 12, NULL, 976000, 1220000, 25, 1),

-- GIÀY SNEAKERS REEBOK NỮ FOREMORE (ID: 38) - Màu Xanh lá
(191, 38, 1, 10, NULL, 864000, 1080000, 18, 1),
(192, 38, 2, 10, NULL, 864000, 1080000, 22, 1),
(193, 38, 3, 10, NULL, 864000, 1080000, 25, 1),
(194, 38, 4, 10, NULL, 864000, 1080000, 20, 1),

-- GIÀY SNEAKER TRẺ EM PUMA PALERMO LEATHER LACE UP JR (ID: 39) - Màu Xám
(195, 39, 1, 5, NULL, 656000, 820000, 15, 1),
(196, 39, 2, 5, NULL, 656000, 820000, 18, 1),
(197, 39, 3, 5, NULL, 656000, 820000, 20, 1),
(198, 39, 4, 5, NULL, 656000, 820000, 16, 1),

-- GIÀY SNEAKERS CONVERSE NAM POKER (ID: 40) - Màu Đen, Trắng
(199, 40, 4, 1, NULL, 840000, 1050000, 20, 1),
(200, 40, 5, 1, NULL, 840000, 1050000, 25, 1),
(201, 40, 6, 1, NULL, 840000, 1050000, 28, 1),
(202, 40, 4, 2, NULL, 816000, 1020000, 22, 1),
(203, 40, 5, 2, NULL, 816000, 1020000, 30, 1),
(204, 40, 6, 2, NULL, 816000, 1020000, 25, 1),

-- GIÀY SNEAKERS NIKE NỮ GARELIA (ID: 41) - Màu Kem
(205, 41, 1, 14, NULL, 1056000, 1320000, 18, 1),
(206, 41, 2, 14, NULL, 1056000, 1320000, 22, 1),
(207, 41, 3, 14, NULL, 1056000, 1320000, 25, 1),
(208, 41, 4, 14, NULL, 1056000, 1320000, 20, 1),

-- GIÀY SNEAKERS REEBOK NAM UPTOWN (ID: 42) - Màu Trắng
(209, 42, 4, 2, NULL, 944000, 1180000, 20, 1),
(210, 42, 5, 2, NULL, 944000, 1180000, 25, 1),
(211, 42, 6, 2, NULL, 944000, 1180000, 28, 1),
(212, 42, 7, 2, NULL, 944000, 1180000, 22, 1),

-- GIÀY SNEAKERS ADIDAS NỮ GRIEDIA (ID: 43) - Màu Kem, Xanh rêu
(213, 43, 1, 14, NULL, 1000000, 1250000, 18, 1),
(214, 43, 2, 14, NULL, 1000000, 1250000, 22, 1),
(215, 43, 3, 14, NULL, 1000000, 1250000, 25, 1),
(216, 43, 1, 15, NULL, 1024000, 1280000, 20, 1),
(217, 43, 2, 15, NULL, 1024000, 1280000, 28, 1),
(218, 43, 3, 15, NULL, 1024000, 1280000, 30, 1),

-- GIÀY SNEAKERS ASICS NAM WEXLEY (ID: 44) - Màu Nâu, Xám
(219, 44, 4, 12, NULL, 1136000, 1420000, 20, 1),
(220, 44, 5, 12, NULL, 1136000, 1420000, 25, 1),
(221, 44, 6, 12, NULL, 1136000, 1420000, 28, 1),
(222, 44, 4, 5, NULL, 1104000, 1380000, 22, 1),
(223, 44, 5, 5, NULL, 1104000, 1380000, 30, 1),
(224, 44, 6, 5, NULL, 1104000, 1380000, 25, 1),

-- GIÀY SNEAKERS PUMA NỮ HALERENNA (ID: 45) - Màu Trắng
(225, 45, 1, 2, NULL, 920000, 1150000, 18, 1),
(226, 45, 2, 2, NULL, 920000, 1150000, 22, 1),
(227, 45, 3, 2, NULL, 920000, 1150000, 25, 1),
(228, 45, 4, 2, NULL, 920000, 1150000, 20, 1),

-- GIÀY SNEAKERS NEW BALANCE NAM WILLIO (ID: 46) - Màu Trắng
(229, 46, 4, 2, NULL, 1216000, 1520000, 20, 1),
(230, 46, 5, 2, NULL, 1216000, 1520000, 25, 1),
(231, 46, 6, 2, NULL, 1216000, 1520000, 28, 1),
(232, 46, 7, 2, NULL, 1216000, 1520000, 22, 1),

-- GIÀY SNEAKERS CONVERSE NỮ PALAZZI (ID: 47) - Màu Hồng, Kem, Trắng, Vàng
(233, 47, 1, 9, NULL, 864000, 1080000, 15, 1),
(234, 47, 2, 9, NULL, 864000, 1080000, 18, 1),
(235, 47, 3, 9, NULL, 864000, 1080000, 20, 1),
(236, 47, 1, 14, NULL, 840000, 1050000, 16, 1),
(237, 47, 2, 14, NULL, 840000, 1050000, 22, 1),
(238, 47, 3, 14, NULL, 840000, 1050000, 25, 1),
(239, 47, 1, 2, NULL, 816000, 1020000, 18, 1),
(240, 47, 2, 2, NULL, 816000, 1020000, 5, 1),
(241, 47, 2, 2, NULL, 816000, 1020000, 24, 1),
(242, 47, 3, 2, NULL, 816000, 1020000, 28, 1),
(243, 47, 1, 7, NULL, 880000, 1100000, 12, 1),
(244, 47, 2, 7, NULL, 880000, 1100000, 16, 1),
(245, 47, 3, 7, NULL, 880000, 1100000, 20, 1),

-- GIÀY THỂ THAO NIKE NAM BEATSPEC-L (ID: 48) - Màu Đen
(246, 48, 4, 1, NULL, 1120000, 1400000, 22, 1),
(247, 48, 5, 1, NULL, 1120000, 1400000, 28, 1),
(248, 48, 6, 1, NULL, 1120000, 1400000, 25, 1),
(249, 48, 7, 1, NULL, 1120000, 1400000, 30, 1),

-- GIÀY SNEAKERS VANS NỮ RONGAN (ID: 49) - Màu Kem, Trắng
(250, 49, 1, 14, NULL, 920000, 1150000, 18, 1),
(251, 49, 2, 14, NULL, 920000, 1150000, 22, 1),
(252, 49, 3, 14, NULL, 920000, 1150000, 25, 1),
(253, 49, 1, 2, NULL, 896000, 1120000, 20, 1),
(254, 49, 2, 2, NULL, 896000, 1120000, 28, 1),
(255, 49, 3, 2, NULL, 896000, 1120000, 30, 1),

-- GIÀY THỂ THAO ADIDAS NAM KEIRAN (ID: 50) - Màu Đen, Nâu đậm
(256, 50, 4, 1, NULL, 1080000, 1350000, 20, 1),
(257, 50, 5, 1, NULL, 1080000, 1350000, 25, 1),
(258, 50, 6, 1, NULL, 1080000, 1350000, 28, 1),
(259, 50, 4, 12, NULL, 1104000, 1380000, 22, 1),
(260, 50, 5, 12, NULL, 1104000, 1380000, 30, 1),

-- GIÀY SNEAKERS ASICS NỮ STEPCOUNT (ID: 51) - Màu Bạc, Hồng, Xanh dương
(261, 51, 1, 2, NULL, 1040000, 1300000, 18, 1), -- Dùng trắng thay bạc
(262, 51, 2, 2, NULL, 1040000, 1300000, 22, 1),
(263, 51, 3, 2, NULL, 1040000, 1300000, 25, 1),
(264, 51, 1, 9, NULL, 1056000, 1320000, 20, 1),
(265, 51, 2, 9, NULL, 1056000, 1320000, 28, 1),
(266, 51, 1, 6, NULL, 1072000, 1340000, 15, 1),
(267, 51, 2, 6, NULL, 1072000, 1340000, 18, 1),

-- GIÀY THỂ THAO PUMA NAM QUICKLANE (ID: 52) - Màu Xanh
(268, 52, 4, 6, NULL, 1024000, 1280000, 20, 1),
(269, 52, 5, 6, NULL, 1024000, 1280000, 25, 1),
(270, 52, 6, 6, NULL, 1024000, 1280000, 28, 1),
(271, 52, 7, 6, NULL, 1024000, 1280000, 22, 1),

-- GIÀY THỂ THAO NIKE NỮ CHICSNEAKER (ID: 53) - Màu Hồng, Tím, Xanh
(272, 53, 1, 9, NULL, 1000000, 1250000, 15, 1),
(273, 53, 2, 9, NULL, 1000000, 1250000, 18, 1),
(274, 53, 3, 9, NULL, 1000000, 1250000, 20, 1),
(275, 53, 1, 11, NULL, 1016000, 1270000, 16, 1),
(276, 53, 2, 11, NULL, 1016000, 1270000, 22, 1),
(277, 53, 1, 6, NULL, 1032000, 1290000, 14, 1),
(278, 53, 2, 6, NULL, 1032000, 1290000, 18, 1),

-- GIÀY THỂ THAO ASICS NAM SCOTTIEE (ID: 54) - Màu Đen, Nâu
(279, 54, 4, 1, NULL, 1104000, 1380000, 20, 1),
(280, 54, 5, 1, NULL, 1104000, 1380000, 25, 1),
(281, 54, 6, 1, NULL, 1104000, 1380000, 28, 1),
(282, 54, 4, 12, NULL, 1120000, 1400000, 22, 1),
(283, 54, 5, 12, NULL, 1120000, 1400000, 30, 1),

-- GIÀY THỂ THAO ADIDAS NỮ CHICSNEAKER2 (ID: 55) - Màu Trắng
(284, 55, 1, 2, NULL, 960000, 1200000, 18, 1),
(285, 55, 2, 2, NULL, 960000, 1200000, 22, 1),
(286, 55, 3, 2, NULL, 960000, 1200000, 25, 1),
(287, 55, 4, 2, NULL, 960000, 1200000, 20, 1),

-- GIÀY THỂ THAO REEBOK NAM WAVESPEC (ID: 56) - Màu Đen
(288, 56, 4, 1, NULL, 920000, 1150000, 20, 1),
(289, 56, 5, 1, NULL, 920000, 1150000, 25, 1),
(290, 56, 6, 1, NULL, 920000, 1150000, 28, 1),
(291, 56, 7, 1, NULL, 920000, 1150000, 22, 1),

-- GIÀY THỂ THAO PUMA NỮ DEEVALE (ID: 57) - Màu Trắng
(292, 57, 1, 2, NULL, 880000, 1100000, 18, 1),
(293, 57, 2, 2, NULL, 880000, 1100000, 22, 1),
(294, 57, 3, 2, NULL, 880000, 1100000, 25, 1),
(295, 57, 4, 2, NULL, 880000, 1100000, 20, 1),

-- GIÀY THỂ THAO CONVERSE NỮ DILATHIELLE (ID: 58) - Màu Trắng
(296, 58, 1, 2, NULL, 840000, 1050000, 18, 1),
(297, 58, 2, 2, NULL, 840000, 1050000, 22, 1),
(298, 58, 3, 2, NULL, 840000, 1050000, 25, 1),
(299, 58, 4, 2, NULL, 840000, 1050000, 20, 1),

-- GIÀY THỂ THAO VANS NỮ ELABRINTAR (ID: 59) - Màu Kem, Xám
(300, 59, 1, 14, NULL, 944000, 1180000, 15, 1),
(301, 59, 2, 14, NULL, 944000, 1180000, 18, 1),
(302, 59, 3, 14, NULL, 944000, 1180000, 20, 1),
(303, 59, 1, 5, NULL, 920000, 1150000, 16, 1),
(304, 59, 2, 5, NULL, 920000, 1150000, 22, 1),

-- GIÀY THỂ THAO REEBOK NỮ GREENSPAN (ID: 60) - Màu Trắng
(305, 60, 1, 2, NULL, 864000, 1080000, 18, 1),
(306, 60, 2, 2, NULL, 864000, 1080000, 22, 1),
(307, 60, 3, 2, NULL, 864000, 1080000, 25, 1),
(308, 60, 4, 2, NULL, 864000, 1080000, 20, 1),

-- GIÀY THỂ THAO ASICS NỮ PILLOWSNKR-L (ID: 61) - Màu Bạc, Hồng
(309, 61, 1, 2, NULL, 1000000, 1250000, 15, 1), -- Dùng trắng thay bạc
(310, 61, 2, 2, NULL, 1000000, 1250000, 18, 1),
(311, 61, 3, 2, NULL, 1000000, 1250000, 20, 1),
(312, 61, 1, 9, NULL, 1016000, 1270000, 16, 1),
(313, 61, 2, 9, NULL, 1016000, 1270000, 22, 1),

-- GIÀY THỂ THAO NEW BALANCE NỮ SAXONY (ID: 62) - Màu Trắng
(314, 62, 1, 2, NULL, 1184000, 1480000, 18, 1),
(315, 62, 2, 2, NULL, 1184000, 1480000, 22, 1),
(316, 62, 3, 2, NULL, 1184000, 1480000, 25, 1),
(317, 62, 4, 2, NULL, 1184000, 1480000, 20, 1);
SET IDENTITY_INSERT product_variants OFF;
GO

-- Insert into promotions
INSERT INTO promotions (name, start_date, expiration_date, value, created_at, updated_at, code, status) VALUES
('Giảm giá mùa hè', '2024-06-01', '2024-08-31', 15.00, '2024-05-20', '2024-05-20', 'SUMMER2024', 2),
('Black Friday Sale', '2024-11-24', '2024-11-30', 30.00, '2024-11-01', '2024-11-01', 'BF2024', 2),
('Khuyến mãi Back to School', '2024-08-01', '2024-09-15', 20.00, '2024-07-15', '2024-07-15', 'BTS2024', 2),
('Giảm giá cuối năm', '2024-12-15', '2024-12-31', 25.00, '2024-12-01', '2024-12-01', 'YEAREND24', 2),
('Flash Sale Weekend', '2024-05-15', '2024-05-16', 40.00, '2024-05-14', '2024-05-14', 'FLASHWKD', 2),
('Ưu đãi Nike Month', '2024-07-01', '2024-07-31', 12.00, '2024-06-25', '2024-06-25', 'NIKEMONTH', 2);

INSERT INTO product_promotions (product_variant_id, promotion_id)
VALUES
(1, 1),   -- Nike Pegasus Turbo - Giảm giá mùa hè
(2, 2),   -- Adidas Ultraboost DNA - Black Friday Sale
(3, 1),   -- Puma King Pro - Giảm giá mùa hè
(5, 3),   -- Vans Old Skool - Khuyến mãi Back to School
(6, 1),   -- New Balance 574
(8, 2),   -- Reebok Classic Leather
(10, 2),  -- Adidas Superstar
(12, 3),  -- Converse One Star
(15, 4),  -- ASICS GT-1000 - Giảm giá cuối năm
(18, 5),  -- Adidas Predator - Flash Sale Weekend
(19, 5),  -- Puma Suede Classic
(23, 6),  -- ASICS Noosa Tri - Ưu đãi Nike Month
(25, 1),  -- Nike Air Max 270
(30, 4),  -- New Balance 327 - Giảm giá cuối năm
(32, 2),  -- Reebok Club C - Black Friday Sale
(35, 6),  -- Puma Future Rider - Ưu đãi Nike Month
(40, 1),  -- Reebok Royal Glide
(45, 2),  -- Vans Sk8-Hi
(47, 3),  -- ASICS GEL-Lyte III
(52, 5),  -- Converse Weapon - Flash Sale Weekend
(54, 4),  -- New Balance FuelCell
(57, 6),  -- Nike Air Force 1
(59, 6);  -- Puma Roma

-- Insert into payment_methods
INSERT INTO payment_methods (name, type, description) VALUES
(N'Tiền mặt', 'CASH', N'Thanh toán bằng tiền mặt '),
(N'Chuyển khoản online', 'ONLINE_PAYMENT', N'Chuyển khoản online');

-- ORDERS TABLE (sửa lại status và giá cả hợp lý)
INSERT INTO orders (user_id, order_code, payment_method_id, fullname, phone_number, address, note, created_at, updated_at, coupon_id, status, total_money, shipping_date)
VALUES
(1,'HD234567',1,N'Nguyễn Văn A','0901000001',N'12 Lê Lợi, Hà Nội',N'Giao giờ hành chính','2023-01-10','2023-01-12',1,'COMPLETED',1200000,'2023-01-15'),
(2,'HD345678',2,N'Lê Thị B','0901000002',N'45 Phan Đình Phùng, TP.HCM',N'Liên hệ trước khi giao','2023-02-15','2023-02-17',2,'PROCESSING',2700000,'2023-02-20'),
(3,'HD456789',1,N'Phạm Văn C','0901000003',N'98 Trần Hưng Đạo, Đà Nẵng',N'Giao nhanh','2023-03-12','2023-03-13',NULL,'SHIPPING',750000,'2023-03-17'),
(4,'HD567890',2,N'Hồ Thị D','0901000004',N'23 Lê Duẩn, Huế',N'Không giao cuối tuần','2023-04-01','2023-04-03',4,'CONFIRMED',2760000,'2023-04-08'),
(5,'HD678901',1,N'Trần Văn E','0901000005',N'67 Nguyễn Trãi, Hải Phòng',NULL,'2023-05-09','2023-05-10',NULL,'COMPLETED',1100000,'2023-05-14'),
(6,'HD789012',2,N'Vũ Thị F','0901000006',N'101 Nguyễn Huệ, Cần Thơ',N'Giao buổi sáng','2023-06-18','2023-06-19',2,'DELIVERED',2450000,'2023-06-21'),
(7,'HD890123',1,N'Đỗ Văn G','0901000007',N'11 Quang Trung, Bắc Ninh',N'Nhớ gọi trước','2023-07-02','2023-07-03',NULL,'PENDING',1250000,'2023-07-06'),
(8,'HD901234',2,N'Ngô Thị H','0901000008',N'321 Lý Thường Kiệt, Nha Trang',N'Không giao ngày lễ','2023-08-17','2023-08-18',NULL,'CANCELLED',2550000,'2023-08-20'),
(9,'HD012345',1,N'Bùi Văn I','0901000009',N'50 Hoàng Văn Thụ, Biên Hòa',NULL,'2023-09-12','2023-09-13',5,'COMPLETED',1500000,'2023-09-16'),
(10,'HD123456',2,N'Mai Thị K','0901000010',N'120 Đinh Tiên Hoàng, Vũng Tàu',NULL,'2023-10-01','2023-10-02',NULL,'PROCESSING',1350000,'2023-10-04'),
(11,'HD234678',1,N'Phan Văn L','0901000011',N'87 Nguyễn Đình Chiểu, Quy Nhơn',N'Giao trong tuần','2023-10-25','2023-10-27',1,'COMPLETED',2300000,'2023-10-29'),
(12,'HD345789',2,N'Dương Thị M','0901000012',N'31 Đống Đa, Quảng Ninh',NULL,'2023-11-13','2023-11-14',NULL,'CONFIRMED',2580000,'2023-11-17'),
(13,'HD456790',1,N'Đặng Văn N','0901000013',N'76 Phạm Hùng, Thanh Hóa',N'Giao ngoài giờ hành chính','2023-12-15','2023-12-16',5,'DELIVERED',2350000,'2023-12-18'),
(14,'HD567801',2,N'Tô Thị O','0901000014',N'67 Cầu Giấy, Hà Nội',NULL,'2024-01-08','2024-01-10',NULL,'SHIPPING',1400000,'2024-01-13'),
(15,'HD678912',1,N'Lý Văn P','0901000015',N'210 Lạc Long Quân, TP.HCM',N'Giao sớm','2024-02-05','2024-02-07',4,'COMPLETED',2530000,'2024-02-10'),
(16,'HD789023',2,N'Kiều Thị Q','0901000016',N'12 Nguyễn Sơn, Gia Lai',NULL,'2024-02-24','2024-02-25',NULL,'PROCESSING',1350000,'2024-02-28'),
(17,'HD890134',1,N'Chu Văn R','0901000017',N'23 Trần Phú, Sóc Trăng',N'Chỉ giao buổi chiều','2024-03-04','2024-03-05',NULL,'PENDING',2800000,'2024-03-08'),
(18,'HD901245',2,N'Giáp Thị S','0901000018',N'89 Phan Bội Châu, Đồng Tháp',NULL,'2024-03-15','2024-03-17',2,'DELIVERED',1400000,'2024-03-20'),
(19,'HD012356',1,N'Tống Văn T','0901000019',N'77 Võ Nguyên Giáp, Tây Ninh',N'Giao ngày cuối tuần','2024-04-07','2024-04-09',NULL,'CANCELLED',1970000,'2024-04-12'),
(20,'HD123467',2,N'Hà Thị U','0901000020',N'16 Trần Cao Vân, Lâm Đồng',N'Giao ban ngày','2024-04-25','2024-04-27',NULL,'COMPLETED',850000,'2024-04-30'),
(1,'HD234578',1,N'Nguyễn Văn A','0901000001',N'12 Lê Lợi, Hà Nội',N'Nhận hàng tại văn phòng','2024-05-10','2024-05-11',3,'DELIVERED',1700000,'2024-05-13'),
(2,'HD345689',2,N'Lê Thị B','0901000002',N'45 Phan Đình Phùng, TP.HCM',NULL,'2024-05-22','2024-05-23',NULL,'COMPLETED',1300000,'2024-05-27'),
(3,'HD456791',1,N'Phạm Văn C','0901000003',N'98 Trần Hưng Đạo, Đà Nẵng',N'Không giao cuối tuần','2024-06-02','2024-06-03',NULL,'PROCESSING',2380000,'2024-06-07'),
(4,'HD567802',2,N'Hồ Thị D','0901000004',N'23 Lê Duẩn, Huế',NULL,'2024-06-18','2024-06-20',3,'CANCELLED',1200000,'2024-06-24'),
(5,'HD678913',1,N'Trần Văn E','0901000005',N'67 Nguyễn Trãi, Hải Phòng',N'Giao trưa','2024-07-03','2024-07-04',NULL,'COMPLETED',1100000,'2024-07-08'),
(6,'HD789024',2,N'Vũ Thị F','0901000006',N'101 Nguyễn Huệ, Cần Thơ',N'Giao ngoài giờ hành chính','2024-07-19','2024-07-20',NULL,'DELIVERED',2450000,'2024-07-24'),
(7,'HD890135',1,N'Đỗ Văn G','0901000007',N'11 Quang Trung, Bắc Ninh',NULL,'2024-08-12','2024-08-13',1,'SHIPPING',1250000,'2024-08-15'),
(8,'HD901246',2,N'Ngô Thị H','0901000008',N'321 Lý Thường Kiệt, Nha Trang',NULL,'2024-08-28','2024-08-29',NULL,'COMPLETED',1860000,'2024-09-01'),
(9,'HD012357',1,N'Bùi Văn I','0901000009',N'50 Hoàng Văn Thụ, Biên Hòa',N'Nhận hàng sau 18h','2024-09-15','2024-09-16',NULL,'PROCESSING',1200000,'2024-09-20'),
(10,'HD123468',2,N'Mai Thị K','0901000010',N'120 Đinh Tiên Hoàng, Vũng Tàu',NULL,'2024-10-03','2024-10-04',NULL,'CONFIRMED',1350000,'2024-10-08'),
(11,'HD234579',1,N'Phan Văn L','0901000011',N'87 Nguyễn Đình Chiểu, Quy Nhơn',NULL,'2024-10-19','2024-10-20',2,'COMPLETED',2300000,'2024-10-24'),
(12,'HD345690',2,N'Dương Thị M','0901000012',N'31 Đống Đa, Quảng Ninh',N'Không giao buổi sáng','2024-11-02','2024-11-03',NULL,'PENDING',2460000,'2024-11-07'),
(13,'HD456792',1,N'Đặng Văn N','0901000013',N'76 Phạm Hùng, Thanh Hóa',NULL,'2024-11-15','2024-11-17',NULL,'DELIVERED',1350000,'2024-11-19'),
(14,'HD567803',2,N'Tô Thị O','0901000014',N'67 Cầu Giấy, Hà Nội',NULL,'2024-12-01','2024-12-02',1,'PROCESSING',1350000,'2024-12-06'),
(15,'HD678914',1,N'Lý Văn P','0901000015',N'210 Lạc Long Quân, TP.HCM',N'Chỉ giao ngày thường','2024-12-18','2024-12-19',NULL,'COMPLETED',3000000,'2024-12-23'),
(16,'HD789025',2,N'Kiều Thị Q','0901000016',N'12 Nguyễn Sơn, Gia Lai',NULL,'2025-01-07','2025-01-08',NULL,'DELIVERED',1350000,'2025-01-10'),
(17,'HD890136',1,N'Chu Văn R','0901000017',N'23 Trần Phú, Sóc Trăng',N'Giao trong tuần','2025-01-19','2025-01-20',4,'CONFIRMED',2500000,'2025-01-22'),
(18,'HD901247',2,N'Giáp Thị S','0901000018',N'89 Phan Bội Châu, Đồng Tháp',NULL,'2025-02-09','2025-02-10',NULL,'SHIPPING',1400000,'2025-02-13'),
(19,'HD012358',1,N'Tống Văn T','0901000019',N'77 Võ Nguyên Giáp, Tây Ninh',NULL,'2025-02-22','2025-02-23',NULL,'COMPLETED',1120000,'2025-02-25'),
(20,'HD123469',2,N'Hà Thị U','0901000020',N'16 Trần Cao Vân, Lâm Đồng',N'Không giao cuối tuần','2025-03-05','2025-03-06',NULL,'PROCESSING',4050000,'2025-03-10'),
(5,'HD234580',1,N'Phạm Minh V','0902000021',N'88 Lý Thường Kiệt, Hà Nội',N'Giao sớm','2023-02-20','2023-02-23',2,'CONFIRMED',1590000,'2023-02-27'),
(6,'HD345691',2,N'Nguyễn Thanh W','0902000022',N'12 Trần Phú, TP.HCM',NULL,'2023-04-19','2023-04-21',NULL,'CANCELLED',1650000,'2023-04-25'),
(7,'HD456793',1,N'Hoàng Đức X','0902000023',N'15 Đống Đa, Huế',N'Nhận hàng sau 18h','2023-06-10','2023-06-11',1,'DELIVERED',1730000,'2023-06-14'),
(8,'HD567804',2,N'Đào Quang Y','0902000024',N'60 Nguyễn Trãi, Biên Hòa',NULL,'2023-08-09','2023-08-10',NULL,'COMPLETED',1620000,'2023-08-12'),
(9,'HD678915',1,N'Trịnh Văn Z','0902000025',N'100 Nguyễn Văn Linh, Đà Nẵng',N'Liên hệ trước khi giao','2023-10-10','2023-10-12',3,'SHIPPING',1800000,'2023-10-14'),
(10,'HD789026',2,N'Nguyễn Hữu Mạnh','0902000026',N'13 Lê Duẩn, Đắk Lắk',NULL,'2024-01-17','2024-01-18',NULL,'DELIVERED',1750000,'2024-01-21'),
(11,'HD890137',1,N'Lưu Quốc Bảo','0902000027',N'27 Quang Trung, Gia Lai',N'Không giao buổi sáng','2024-03-23','2024-03-24',5,'CANCELLED',1900000,'2024-03-27'),
(12,'HD901248',2,N'Vũ Hải Đăng','0902000028',N'73 Hai Bà Trưng, Nha Trang',NULL,'2024-05-29','2024-05-31',NULL,'COMPLETED',1850000,'2024-06-02'),
(13,'HD012359',1,N'Nguyễn Văn Sơn','0902000029',N'22 Phạm Văn Đồng, Hà Nội',N'Giao giờ hành chính','2024-08-10','2024-08-11',2,'DELIVERED',1500000,'2024-08-13'),
(14,'HD123470',2,N'Hồ Thị Thu','0902000030',N'16 Nguyễn Hữu Thọ, Huế',NULL,'2024-09-20','2024-09-21',NULL,'COMPLETED',1530000,'2024-09-25');

INSERT INTO order_details (order_id, product_variant_id, price, quantity, total_money, status) VALUES
(1, 3, 1200000, 1, 1200000, 1),
(2, 11, 800000, 2, 1600000, 1),
(2, 6, 1100000, 1, 1100000, 1),
(3, 15, 750000, 1, 750000, 1),
(4, 24, 1380000, 2, 2760000, 1),
(5, 8, 1100000, 1, 1100000, 1),
(6, 21, 1350000, 1, 1350000, 1),
(6, 9, 1100000, 1, 1100000, 1),
(7, 27, 1250000, 1, 1250000, 1),
(8, 32, 1200000, 1, 1200000, 1),
(8, 19, 1350000, 1, 1350000, 1),
(9, 14, 750000, 2, 1500000, 1),
(10, 55, 1350000, 1, 1350000, 1),
(11, 38, 1150000, 2, 2300000, 1),
(12, 48, 1400000, 1, 1400000, 1),
(12, 42, 1180000, 1, 1180000, 1),
(13, 66, 1200000, 1, 1200000, 1),
(13, 45, 1150000, 1, 1150000, 1),
(14, 61, 1400000, 1, 1400000, 1),
(15, 77, 1350000, 1, 1350000, 1),
(15, 35, 1180000, 1, 1180000, 1),
(16, 52, 1350000, 1, 1350000, 1),
(17, 58, 1400000, 2, 2800000, 1),
(18, 59, 1400000, 1, 1400000, 1),
(19, 47, 1120000, 1, 1120000, 1),
(19, 73, 850000, 1, 850000, 1),
(20, 71, 850000, 1, 850000, 1),
(21, 72, 850000, 2, 1700000, 1),
(22, 80, 1300000, 1, 1300000, 1),
(23, 81, 1180000, 1, 1180000, 1),
(23, 28, 1200000, 1, 1200000, 1),
(24, 93, 720000, 2, 1440000, 1),
(25, 89, 1450000, 1, 1450000, 1),
(25, 99, 1280000, 1, 1280000, 1),
(26, 104, 1180000, 2, 2360000, 1),
(27, 107, 1250000, 1, 1250000, 1),
(28, 115, 680000, 1, 680000, 1),
(28, 83, 1180000, 1, 1180000, 1),
(29, 64, 1200000, 1, 1200000, 1),
(30, 54, 1350000, 1, 1350000, 1),
(31, 40, 750000, 2, 1500000, 1),
(32, 17, 1300000, 1, 1300000, 1),
(33, 12, 800000, 1, 800000, 1),
(34, 1, 1200000, 1, 1200000, 1),
(35, 4, 1200000, 1, 1200000, 1),
(36, 87, 1450000, 2, 2900000, 1),
(36, 106, 1180000, 1, 1180000, 1),
(37, 33, 1180000, 1, 1180000, 1),
(38, 18, 1350000, 1, 1350000, 1),
(38, 24, 1380000, 1, 1380000, 1),
(39, 53, 1350000, 1, 1350000, 1),
(40, 109, 1250000, 2, 2500000, 1),
(41, 111, 1220000, 1, 1220000, 1),
(42, 113, 680000, 1, 680000, 1),
(43, 115, 680000, 2, 1360000, 1),
(44, 8, 1100000, 1, 1100000, 1),
(45, 19, 1350000, 1, 1350000, 1),
(45, 25, 1380000, 1, 1380000, 1),
(46, 7, 1150000, 1, 1150000, 1),
(47, 10, 1100000, 2, 2200000, 1),
(47, 13, 800000, 1, 800000, 1),
(48, 39, 750000, 1, 750000, 1),
(48, 23, 1320000, 1, 1320000, 1),
(49, 22, 1320000, 1, 1320000, 1),
(50, 85, 1150000, 1, 1150000, 1);

-- Insert into reviews
INSERT INTO reviews (user_id, product_id, order_detail_id, content, rating, is_hidden, parent_id, created_at, updated_at) VALUES
(3, 1, NULL, N'Giày chạy bộ tuyệt vời! Êm chân, nhẹ và thoáng khí. Rất phù hợp cho việc chạy đường dài.', 5, 0, NULL, '2024-03-01 14:25:10', '2024-03-01 14:25:10'),
(4, 2, NULL, N'Adidas Ultraboost quả thật đáng đồng tiền. Đệm Boost rất êm, thiết kế đẹp.', 5, 0, NULL, '2024-03-02 09:30:45', '2024-03-02 09:30:45'),
(5, 3, NULL, N'Jordan 1 chất lượng tuyệt vời, da thật rất đẹp. Đúng là huyền thoại!', 5, 0, NULL, '2024-03-03 16:12:20', '2024-03-03 16:12:20'),
(3, 4, NULL, N'Converse classic không bao giờ lỗi mốt. Giá cả hợp lý, chất lượng tốt.', 4, 0, NULL, '2024-03-04 11:45:35', '2024-03-04 11:45:35'),
(4, 5, NULL, N'Vans Old Skool phong cách, bền bỉ. Rất phù hợp với style streetwear.', 4, 0, NULL, '2024-03-05 18:20:15', '2024-03-05 18:20:15'),
(5, 6, NULL, N'New Balance 1080v12 êm như mây. Rất tốt cho người chạy bộ lâu năm.', 5, 0, NULL, '2024-03-10 13:55:40', '2024-03-10 13:55:40'),
(3, 7, NULL, N'ASICS tennis shoes chất lượng cao, grip rất tốt trên sân.', 4, 0, NULL, '2024-03-12 20:10:25', '2024-03-12 20:10:25'),
(4, 8, NULL, N'Puma Future 7 tuyệt vời cho bóng đá, cảm giác bóng rất tốt.', 5, 0, NULL, '2024-03-15 07:35:50', '2024-03-15 07:35:50'),
(7, 1, NULL, N'Mặc 1 lần là giãn.', 2, 0, NULL, '2024-01-03 10:15:22', '2024-01-03 10:15:22'),
(5, 1, NULL, N'Đúng như mong đợi.', 2, 1, NULL, '2024-01-04 14:32:18', '2024-01-04 14:32:18'),
(2, 1, NULL, N'Tốt hơn mong đợi.', 4, 1, NULL, '2024-01-05 09:48:05', '2024-01-05 09:48:05'),
(1, 1, NULL, N'Mua lần 2 vẫn rất hài lòng.', 4, 0, NULL, '2024-01-06 16:23:41', '2024-01-06 16:23:41'),
(15, 2, NULL, N'Muốn đổi size, shop hỗ trợ tốt.', 2, 0, NULL, '2024-01-07 11:57:12', '2024-01-07 11:57:12'),
(13, 2, NULL, N'Sản phẩm không giống mô tả.', 5, 0, NULL, '2024-01-08 18:44:37', '2024-01-08 18:44:37'),
(3, 2, NULL, N'Giao hàng nhanh, đóng gói đẹp.', 5, 0, NULL, '2024-01-09 08:16:54', '2024-01-09 08:16:54'),
(7, 3, NULL, N'Không hài lòng chút nào.', 4, 0, NULL, '2024-01-10 15:29:33', '2024-01-10 15:29:33'),
(15, 3, NULL, N'Đóng gói cẩn thận.', 2, 1, NULL, '2024-01-11 12:41:19', '2024-01-11 12:41:19'),
(1, 3, NULL, N'Rất đẹp, đáng mua.', 3, 0, NULL, '2024-01-12 19:05:46', '2024-01-12 19:05:46'),
(9, 4, NULL, N'Shop hỗ trợ nhanh chóng.', 4, 0, NULL, '2024-01-13 07:52:31', '2024-01-13 07:52:31'),
(9, 4, NULL, N'Đóng gói cẩn thận.', 4, 0, NULL, '2024-01-14 13:18:08', '2024-01-14 13:18:08');



-- Insert transactions cho 50 order
INSERT INTO transactions (order_id, transaction_code, amount, status, vnp_txn_ref, transaction_date, completed_date, note, created_at, updated_at)
VALUES
(1, 'TRX00001', 1200000, 'SUCCESS', 'VNP00001', '2023-01-15', '2023-01-15', N'Thanh toán COD', '2023-01-15', '2023-01-15'),
(2, 'TRX00002', 2700000, 'PENDING', 'VNP00002', '2023-02-20', NULL, N'Thanh toán online', '2023-02-20', '2023-02-20'),
(3, 'TRX00003', 750000, 'PENDING', 'VNP00003', '2023-03-17', NULL, N'Thanh toán COD', '2023-03-17', '2023-03-17'),
(4, 'TRX00004', 2760000, 'PENDING', 'VNP00004', '2023-04-08', NULL, N'Thanh toán online', '2023-04-08', '2023-04-08'),
(5, 'TRX00005', 1100000, 'SUCCESS', 'VNP00005', '2023-05-14', '2023-05-14', N'Thanh toán COD', '2023-05-14', '2023-05-14'),
(6, 'TRX00006', 2450000, 'SUCCESS', 'VNP00006', '2023-06-21', '2023-06-21', N'Thanh toán online', '2023-06-21', '2023-06-21'),
(7, 'TRX00007', 1250000, 'PENDING', 'VNP00007', '2023-07-06', NULL, N'Thanh toán COD', '2023-07-06', '2023-07-06'),
(8, 'TRX00008', 2550000, 'FAILED', 'VNP00008', '2023-08-20', NULL, N'Thanh toán online thất bại', '2023-08-20', '2023-08-20'),
(9, 'TRX00009', 1500000, 'SUCCESS', 'VNP00009', '2023-09-16', '2023-09-16', N'Thanh toán COD', '2023-09-16', '2023-09-16'),
(10, 'TRX00010', 1350000, 'PENDING', 'VNP00010', '2023-10-04', NULL, N'Thanh toán online', '2023-10-04', '2023-10-04'),
(11, 'TRX00011', 2300000, 'SUCCESS', 'VNP00011', '2023-10-29', '2023-10-29', N'Thanh toán COD', '2023-10-29', '2023-10-29'),
(12, 'TRX00012', 2580000, 'PENDING', 'VNP00012', '2023-11-17', NULL, N'Thanh toán online', '2023-11-17', '2023-11-17'),
(13, 'TRX00013', 2350000, 'SUCCESS', 'VNP00013', '2023-12-18', '2023-12-18', N'Thanh toán COD', '2023-12-18', '2023-12-18'),
(14, 'TRX00014', 1400000, 'PENDING', 'VNP00014', '2024-01-13', NULL, N'Thanh toán online', '2024-01-13', '2024-01-13'),
(15, 'TRX00015', 2530000, 'SUCCESS', 'VNP00015', '2024-02-10', '2024-02-10', N'Thanh toán COD', '2024-02-10', '2024-02-10'),
(16, 'TRX00016', 1350000, 'PENDING', 'VNP00016', '2024-02-28', NULL, N'Thanh toán online', '2024-02-28', '2024-02-28'),
(17, 'TRX00017', 2800000, 'PENDING', 'VNP00017', '2024-03-08', NULL, N'Thanh toán COD', '2024-03-08', '2024-03-08'),
(18, 'TRX00018', 1400000, 'SUCCESS', 'VNP00018', '2024-03-20', '2024-03-20', N'Thanh toán online', '2024-03-20', '2024-03-20'),
(19, 'TRX00019', 1970000, 'FAILED', 'VNP00019', '2024-04-12', NULL, N'Thanh toán COD - Hủy đơn', '2024-04-12', '2024-04-12'),
(20, 'TRX00020', 850000, 'SUCCESS', 'VNP00020', '2024-04-30', '2024-04-30', N'Thanh toán online', '2024-04-30', '2024-04-30'),
(21, 'TRX00021', 1700000, 'SUCCESS', 'VNP00021', '2024-05-13', '2024-05-13', N'Thanh toán COD', '2024-05-13', '2024-05-13'),
(22, 'TRX00022', 1300000, 'SUCCESS', 'VNP00022', '2024-05-27', '2024-05-27', N'Thanh toán online', '2024-05-27', '2024-05-27'),
(23, 'TRX00023', 2380000, 'PENDING', 'VNP00023', '2024-06-07', NULL, N'Thanh toán COD', '2024-06-07', '2024-06-07'),
(24, 'TRX00024', 1200000, 'FAILED', 'VNP00024', '2024-06-24', NULL, N'Thanh toán online - Hủy đơn', '2024-06-24', '2024-06-24'),
(25, 'TRX00025', 1100000, 'SUCCESS', 'VNP00025', '2024-07-08', '2024-07-08', N'Thanh toán COD', '2024-07-08', '2024-07-08'),
(26, 'TRX00026', 2450000, 'SUCCESS', 'VNP00026', '2024-07-24', '2024-07-24', N'Thanh toán online', '2024-07-24', '2024-07-24'),
(27, 'TRX00027', 1250000, 'PENDING', 'VNP00027', '2024-08-15', NULL, N'Thanh toán COD', '2024-08-15', '2024-08-15'),
(28, 'TRX00028', 1860000, 'SUCCESS', 'VNP00028', '2024-09-01', '2024-09-01', N'Thanh toán online', '2024-09-01', '2024-09-01'),
(29, 'TRX00029', 1200000, 'PENDING', 'VNP00029', '2024-09-20', NULL, N'Thanh toán COD', '2024-09-20', '2024-09-20'),
(30, 'TRX00030', 1350000, 'PENDING', 'VNP00030', '2024-10-08', NULL, N'Thanh toán online', '2024-10-08', '2024-10-08'),
(31, 'TRX00031', 2300000, 'SUCCESS', 'VNP00031', '2024-10-24', '2024-10-24', N'Thanh toán COD', '2024-10-24', '2024-10-24'),
(32, 'TRX00032', 2460000, 'PENDING', 'VNP00032', '2024-11-07', NULL, N'Thanh toán online', '2024-11-07', '2024-11-07'),
(33, 'TRX00033', 1350000, 'SUCCESS', 'VNP00033', '2024-11-19', '2024-11-19', N'Thanh toán COD', '2024-11-19', '2024-11-19'),
(34, 'TRX00034', 1350000, 'PENDING', 'VNP00034', '2024-12-06', NULL, N'Thanh toán online', '2024-12-06', '2024-12-06'),
(35, 'TRX00035', 3000000, 'SUCCESS', 'VNP00035', '2024-12-23', '2024-12-23', N'Thanh toán COD', '2024-12-23', '2024-12-23'),
(36, 'TRX00036', 1350000, 'SUCCESS', 'VNP00036', '2025-01-10', '2025-01-10', N'Thanh toán online', '2025-01-10', '2025-01-10'),
(37, 'TRX00037', 2500000, 'PENDING', 'VNP00037', '2025-01-22', NULL, N'Thanh toán COD', '2025-01-22', '2025-01-22'),
(38, 'TRX00038', 1400000, 'PENDING', 'VNP00038', '2025-02-13', NULL, N'Thanh toán online', '2025-02-13', '2025-02-13'),
(39, 'TRX00039', 1120000, 'SUCCESS', 'VNP00039', '2025-02-25', '2025-02-25', N'Thanh toán COD', '2025-02-25', '2025-02-25'),
(40, 'TRX00040', 4050000, 'PENDING', 'VNP00040', '2025-03-10', NULL, N'Thanh toán online', '2025-03-10', '2025-03-10'),
(41, 'TRX00041', 1590000, 'PENDING', 'VNP00041', '2023-02-27', NULL, N'Thanh toán COD', '2023-02-27', '2023-02-27'),
(42, 'TRX00042', 1650000, 'FAILED', 'VNP00042', '2023-04-25', NULL, N'Thanh toán online thất bại', '2023-04-25', '2023-04-25'),
(43, 'TRX00043', 1730000, 'SUCCESS', 'VNP00043', '2023-06-14', '2023-06-14', N'Thanh toán COD', '2023-06-14', '2023-06-14'),
(44, 'TRX00044', 1620000, 'SUCCESS', 'VNP00044', '2023-08-12', '2023-08-12', N'Thanh toán online', '2023-08-12', '2023-08-12'),
(45, 'TRX00045', 1800000, 'PENDING', 'VNP00045', '2023-10-14', NULL, N'Thanh toán COD', '2023-10-14', '2023-10-14'),
(46, 'TRX00046', 1750000, 'SUCCESS', 'VNP00046', '2024-01-21', '2024-01-21', N'Thanh toán online', '2024-01-21', '2024-01-21'),
(47, 'TRX00047', 1900000, 'FAILED', 'VNP00047', '2024-03-27', NULL, N'Thanh toán COD - Hủy đơn', '2024-03-27', '2024-03-27'),
(48, 'TRX00048', 1850000, 'SUCCESS', 'VNP00048', '2024-06-02', '2024-06-02', N'Thanh toán online', '2024-06-02', '2024-06-02'),
(49, 'TRX00049', 1500000, 'SUCCESS', 'VNP00049', '2024-08-13', '2024-08-13', N'Thanh toán COD', '2024-08-13', '2024-08-13'),
(50, 'TRX00050', 1530000, 'SUCCESS', 'VNP00050', '2024-09-25', '2024-09-25', N'Thanh toán online', '2024-09-25', '2024-09-25');

-- INSERT dữ liệu cho vnpay_transaction_details chỉ cho các orders có payment_method_id = 2 (online payment)
INSERT INTO vnpay_transaction_details (
    transaction_id, vnp_txn_ref, vnp_transaction_no, vnp_bank_code, vnp_card_type, vnp_pay_date,
    vnp_order_info, vnp_response_code, vnp_transaction_status, request_url, ipn_data, return_data, secure_hash, created_at, updated_at
) VALUES
(2,  'VNP00002', '100002', 'VCB', 'ATM',  '20230220120000', N'Order #2 - Lê Thị B - 2700000 VND',  '00', '00', 'https://sandbox.vnpay.vn/2',  N'data_ipn_2',  N'data_return_2',  'hash2',  '2023-02-20', '2023-02-20'),
(4,  'VNP00004', '100004', 'TCB', 'VISA', '20230408120000', N'Order #4 - Hồ Thị D - 2760000 VND',  '00', '00', 'https://sandbox.vnpay.vn/4',  N'data_ipn_4',  N'data_return_4',  'hash4',  '2023-04-08', '2023-04-08'),
(6,  'VNP00006', '100006', 'VCB', 'ATM',  '20230621120000', N'Order #6 - Vũ Thị F - 2450000 VND',  '00', '00', 'https://sandbox.vnpay.vn/6',  N'data_ipn_6',  N'data_return_6',  'hash6',  '2023-06-21', '2023-06-21'),
(8,  'VNP00008', '100008', 'BIDV', 'VISA', '20230820120000', N'Order #8 - Ngô Thị H - 2550000 VND',  '24', '01', 'https://sandbox.vnpay.vn/8',  N'data_ipn_8',  N'data_return_8',  'hash8',  '2023-08-20', '2023-08-20'),
(10, 'VNP00010', '100010', 'VCB', 'ATM',  '20231004120000', N'Order #10 - Mai Thị K - 1350000 VND', '00', '00', 'https://sandbox.vnpay.vn/10', N'data_ipn_10', N'data_return_10', 'hash10', '2023-10-04', '2023-10-04'),
(12, 'VNP00012', '100012', 'TCB', 'ATM',  '20231117120000', N'Order #12 - Dương Thị M - 2580000 VND', '00', '00', 'https://sandbox.vnpay.vn/12', N'data_ipn_12', N'data_return_12', 'hash12', '2023-11-17', '2023-11-17'),
(14, 'VNP00014', '100014', 'VCB', 'VISA', '20240113120000', N'Order #14 - Tô Thị O - 1400000 VND',  '00', '00', 'https://sandbox.vnpay.vn/14', N'data_ipn_14', N'data_return_14', 'hash14', '2024-01-13', '2024-01-13'),
(16, 'VNP00016', '100016', 'VCB', 'ATM',  '20240228120000', N'Order #16 - Kiều Thị Q - 1350000 VND', '00', '00', 'https://sandbox.vnpay.vn/16', N'data_ipn_16', N'data_return_16', 'hash16', '2024-02-28', '2024-02-28'),
(18, 'VNP00018', '100018', 'VCB', 'ATM',  '20240320120000', N'Order #18 - Giáp Thị S - 1400000 VND', '00', '00', 'https://sandbox.vnpay.vn/18', N'data_ipn_18', N'data_return_18', 'hash18', '2024-03-20', '2024-03-20'),
(20, 'VNP00020', '100020', 'BIDV', 'VISA', '20240430120000', N'Order #20 - Hà Thị U - 850000 VND',   '00', '00', 'https://sandbox.vnpay.vn/20', N'data_ipn_20', N'data_return_20', 'hash20', '2024-04-30', '2024-04-30'),
(22, 'VNP00022', '100022', 'VCB', 'ATM',  '20240527120000', N'Order #22 - Lê Thị B - 1300000 VND',  '00', '00', 'https://sandbox.vnpay.vn/22', N'data_ipn_22', N'data_return_22', 'hash22', '2024-05-27', '2024-05-27'),
(24, 'VNP00024', '100024', 'TCB', 'VISA', '20240624120000', N'Order #24 - Hồ Thị D - 1200000 VND',  '24', '01', 'https://sandbox.vnpay.vn/24', N'data_ipn_24', N'data_return_24', 'hash24', '2024-06-24', '2024-06-24'),
(26, 'VNP00026', '100026', 'VCB', 'ATM',  '20240724120000', N'Order #26 - Vũ Thị F - 2450000 VND',  '00', '00', 'https://sandbox.vnpay.vn/26', N'data_ipn_26', N'data_return_26', 'hash26', '2024-07-24', '2024-07-24'),
(28, 'VNP00028', '100028', 'BIDV', 'VISA', '20240901120000', N'Order #28 - Ngô Thị H - 1860000 VND', '00', '00', 'https://sandbox.vnpay.vn/28', N'data_ipn_28', N'data_return_28', 'hash28', '2024-09-01', '2024-09-01'),
(30, 'VNP00030', '100030', 'VCB', 'ATM',  '20241008120000', N'Order #30 - Mai Thị K - 1350000 VND', '00', '00', 'https://sandbox.vnpay.vn/30', N'data_ipn_30', N'data_return_30', 'hash30', '2024-10-08', '2024-10-08'),
(32, 'VNP00032', '100032', 'TCB', 'ATM',  '20241107120000', N'Order #32 - Dương Thị M - 2460000 VND', '00', '00', 'https://sandbox.vnpay.vn/32', N'data_ipn_32', N'data_return_32', 'hash32', '2024-11-07', '2024-11-07'),
(34, 'VNP00034', '100034', 'VCB', 'VISA', '20241206120000', N'Order #34 - Tô Thị O - 1350000 VND',  '00', '00', 'https://sandbox.vnpay.vn/34', N'data_ipn_34', N'data_return_34', 'hash34', '2024-12-06', '2024-12-06'),
(36, 'VNP00036', '100036', 'VCB', 'ATM',  '20250110120000', N'Order #36 - Kiều Thị Q - 1350000 VND', '00', '00', 'https://sandbox.vnpay.vn/36', N'data_ipn_36', N'data_return_36', 'hash36', '2025-01-10', '2025-01-10'),
(38, 'VNP00038', '100038', 'VCB', 'ATM',  '20250213120000', N'Order #38 - Giáp Thị S - 1400000 VND', '00', '00', 'https://sandbox.vnpay.vn/38', N'data_ipn_38', N'data_return_38', 'hash38', '2025-02-13', '2025-02-13'),
(40, 'VNP00040', '100040', 'BIDV', 'VISA', '20250310120000', N'Order #40 - Hà Thị U - 4050000 VND',  '00', '00', 'https://sandbox.vnpay.vn/40', N'data_ipn_40', N'data_return_40', 'hash40', '2025-03-10', '2025-03-10'),
(42, 'VNP00042', '100042', 'VCB', 'ATM',  '20230425120000', N'Order #42 - Nguyễn Thanh W - 1650000 VND', '24', '01', 'https://sandbox.vnpay.vn/42', N'data_ipn_42', N'data_return_42', 'hash42', '2023-04-25', '2023-04-25'),
(44, 'VNP00044', '100044', 'TCB', 'VISA', '20230812120000', N'Order #44 - Đào Quang Y - 1620000 VND',  '00', '00', 'https://sandbox.vnpay.vn/44', N'data_ipn_44', N'data_return_44', 'hash44', '2023-08-12', '2023-08-12'),
(46, 'VNP00046', '100046', 'VCB', 'ATM',  '20240121120000', N'Order #46 - Nguyễn Hữu Mạnh - 1750000 VND', '00', '00', 'https://sandbox.vnpay.vn/46', N'data_ipn_46', N'data_return_46', 'hash46', '2024-01-21', '2024-01-21'),
(48, 'VNP00048', '100048', 'BIDV', 'VISA', '20240602120000', N'Order #48 - Vũ Hải Đăng - 1850000 VND',  '00', '00', 'https://sandbox.vnpay.vn/48', N'data_ipn_48', N'data_return_48', 'hash48', '2024-06-02', '2024-06-02'),
(50, 'VNP00050', '100050', 'TPB', 'VISA', '20240602120000', N'Order #50',  '00', '00', 'https://sandbox.vnpay.vn/48', N'data_ipn_50', N'data_return_50', 'hash50', '2024-06-02', '2024-06-02');

SET IDENTITY_INSERT product_images ON;
INSERT INTO product_images (id, image_url) VALUES
  (1, N'http://localhost:9000/product-variant-images/GIÀY LƯỜI NAM SOFTCOURT1.jpg'),
  (2, N'http://localhost:9000/product-variant-images/GIÀY LƯỜI NAM SOFTCOURT2.jpg'),
  (3, N'http://localhost:9000/product-variant-images/GIÀY LƯỜI NAM SOFTCOURT3.jpg'),
  (4, N'http://localhost:9000/product-variant-images/GIÀY LƯỜI NAM SOFTCOURT4.jpg'),
  (5, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX1 (3).jpg'),
  (6, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX2 (3).jpg'),
  (7, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX3 (3).jpg'),
  (8, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX4 (3).jpg'),
  (9, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX5 (2).jpg'),
  (10, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX1 (2).jpg'),
  (11, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX2 (1).jpg'),
  (12, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX2 (2).jpg'),
  (13, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX3 (2).jpg'),
  (14, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX4 (2).jpg'),
  (15, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Adidas Marvel''S Iron Man Racer - Đỏ1.jpg'),
  (16, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Adidas Marvel''S Iron Man Racer - Đỏ2.jpg'),
  (17, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Adidas Marvel''S Iron Man Racer - Đỏ3.jpg'),
  (18, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Adidas Marvel''S Iron Man Racer - Đỏ4.jpg'),
  (19, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Adidas Marvel''S Iron Man Racer - Đỏ1 (1).jpg'),
  (20, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Adidas Marvel''S Iron Man Racer - Đỏ3 (1).jpg'),
  (21, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Adidas Marvel''S Iron Man Racer - Đỏ4 (1).jpg'),
  (22, N'http://localhost:9000/product-variant-images/GIÀY NAM CLUBMTL1 (2).jpg'),
  (23, N'http://localhost:9000/product-variant-images/GIÀY NAM CLUBMTL2 (2).jpg'),
  (24, N'http://localhost:9000/product-variant-images/GIÀY NAM CLUBMTL3 (2).jpg'),
  (25, N'http://localhost:9000/product-variant-images/GIÀY NAM CLUBMTL4 (2).jpg'),
  (26, N'http://localhost:9000/product-variant-images/GIÀY NAM CLUBMTL1.jpg'),
  (27, N'http://localhost:9000/product-variant-images/GIÀY NAM CLUBMTL2.jpg'),
  (28, N'http://localhost:9000/product-variant-images/GIÀY NAM CLUBMTL3.jpg'),
  (29, N'http://localhost:9000/product-variant-images/GIÀY NAM CLUBMTL4.jpg'),
  (30, N'http://localhost:9000/product-variant-images/GIÀY NAM CLUBMTL1 (3).jpg'),
  (31, N'http://localhost:9000/product-variant-images/GIÀY NAM CLUBMTL2 (3).jpg'),
  (32, N'http://localhost:9000/product-variant-images/GIÀY NAM CLUBMTL3 (3).jpg'),
  (33, N'http://localhost:9000/product-variant-images/GIÀY NAM CLUBMTL4 (3).jpg'),
  (34, N'http://localhost:9000/product-variant-images/GIÀY NAM CLUBMTL1 (1).jpg'),
  (35, N'http://localhost:9000/product-variant-images/GIÀY NAM CLUBMTL2 (1).jpg'),
  (36, N'http://localhost:9000/product-variant-images/GIÀY NAM CLUBMTL3 (1).jpg'),
  (37, N'http://localhost:9000/product-variant-images/GIÀY NAM CLUBMTL4 (1).jpg'),
  (38, N'http://localhost:9000/product-variant-images/GIÀY NAM MARTY1.jpg'),
  (39, N'http://localhost:9000/product-variant-images/GIÀY NAM MARTY2.jpg'),
  (40, N'http://localhost:9000/product-variant-images/GIÀY NAM MARTY3.jpg'),
  (41, N'http://localhost:9000/product-variant-images/GIÀY NAM MARTY4.jpg'),
  (42, N'http://localhost:9000/product-variant-images/GIÀY NAM MARTY5.jpg'),
  (43, N'http://localhost:9000/product-variant-images/GIÀY NAM MARTY1 (1).jpg'),
  (44, N'http://localhost:9000/product-variant-images/GIÀY NAM MARTY2 (1).jpg'),
  (45, N'http://localhost:9000/product-variant-images/GIÀY NAM MARTY3 (1).jpg'),
  (46, N'http://localhost:9000/product-variant-images/GIÀY NAM MARTY4 (1).jpg'),
  (47, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX1.jpg'),
  (48, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX2.jpg'),
  (49, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX3.jpg'),
  (50, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX4.jpg'),
  (51, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX5.jpg'),
  (52, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX1 (1).jpg'),
  (53, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX3 (1).jpg'),
  (54, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX4 (1).jpg'),
  (55, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX5 (1).jpg'),
  (56, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX1 (1).jpg'),
  (57, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX3 (1).jpg'),
  (58, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX4 (1).jpg'),
  (59, N'http://localhost:9000/product-variant-images/GIÀY NỮ CLUBLUX5 (1).jpg'),
  (60, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Nike Flex Runner 3 (Ps)1 - Copy.jpg'),
  (61, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Nike Flex Runner 3 (Ps)1.jpg'),
  (62, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Nike Flex Runner 3 (Ps)2.jpg'),
  (63, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Nike Flex Runner 3 (Ps)4.jpg'),
  (64, N'http://localhost:9000/product-variant-images/GIÀY SLIPS ON NAM LEEJAY1.jpg'),
  (65, N'http://localhost:9000/product-variant-images/GIÀY SLIPS ON NAM LEEJAY2.jpg'),
  (66, N'http://localhost:9000/product-variant-images/GIÀY SLIPS ON NAM LEEJAY3.jpg'),
  (67, N'http://localhost:9000/product-variant-images/GIÀY SLIPS ON NAM LEEJAY4.jpg'),
  (68, N'http://localhost:9000/product-variant-images/GIÀY SLIPS ON NAM LEEJAY1 (1).jpg'),
  (69, N'http://localhost:9000/product-variant-images/GIÀY SLIPS ON NAM LEEJAY2 (1).jpg'),
  (70, N'http://localhost:9000/product-variant-images/GIÀY SLIPS ON NAM LEEJAY3 (1).jpg'),
  (71, N'http://localhost:9000/product-variant-images/GIÀY SLIPS ON NAM LEEJAY4 (1).jpg'),
  (72, N'http://localhost:9000/product-variant-images/GIÀY NỮ SWEETTHING1.jpg'),
  (73, N'http://localhost:9000/product-variant-images/GIÀY NỮ SWEETTHING2.jpg'),
  (74, N'http://localhost:9000/product-variant-images/GIÀY NỮ SWEETTHING3.jpg'),
  (75, N'http://localhost:9000/product-variant-images/GIÀY NỮ SWEETTHING4.jpg'),
  (76, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM DERRYK1.jpg'),
  (77, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM DERRYK2.jpg'),
  (78, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM DERRYK3.jpg'),
  (79, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM DERRYK4.jpg'),
  (80, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Nike Team Hustle D 11 (Ps) - Trắng1.jpg'),
  (81, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Nike Team Hustle D 11 (Ps) - Trắng2.jpg'),
  (82, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Nike Team Hustle D 11 (Ps) - Trắng3.jpg'),
  (83, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Nike Team Hustle D 11 (Ps) - Trắng4.jpg'),
  (84, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM KYLIAN1.jpg'),
  (85, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM KYLIAN2.jpg'),
  (86, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM KYLIAN3.jpg'),
  (87, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM KYLIAN4.jpg'),
  (88, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NỮ MOTIONXX1.jpg'),
  (89, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NỮ MOTIONXX2.jpg'),
  (90, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NỮ MOTIONXX3.jpg'),
  (91, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NỮ MOTIONXX4.jpg'),
  (92, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NỮ MOTIONXX5.jpg'),
  (93, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM MOTIONX1.jpg'),
  (94, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM MOTIONX2.jpg'),
  (95, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM MOTIONX3.jpg'),
  (96, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM MOTIONX4.jpg'),
  (97, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Puma Rs-X Playstation - Xám1.jpg'),
  (98, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Puma Rs-X Playstation - Xám2.jpg'),
  (99, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Puma Rs-X Playstation - Xám4.jpg'),
  (100, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM SCOTTIE1 (1).jpg'),
  (101, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM SCOTTIE2 (1).jpg'),
  (102, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM SCOTTIE3 (1).jpg'),
  (103, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM SCOTTIE4 (1).jpg'),
  (104, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM SCOTTIE1.jpg'),
  (105, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM SCOTTIE2.jpg'),
  (106, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM SCOTTIE3.jpg'),
  (107, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM SCOTTIE4.jpg'),
  (108, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CAROTERIEL1 (2).jpg'),
  (109, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CAROTERIEL2 (2).jpg'),
  (110, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CAROTERIEL3 (2).jpg'),
  (111, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CAROTERIEL4 (2).jpg'),
  (112, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CAROTERIEL1 (1).jpg'),
  (113, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CAROTERIEL2 (1).jpg'),
  (114, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CAROTERIEL3 (1).jpg'),
  (115, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CAROTERIEL4 (1).jpg'),
  (116, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM ZETHAN1.jpg'),
  (117, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM ZETHAN2.jpg'),
  (118, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM ZETHAN3.jpg'),
  (119, N'http://localhost:9000/product-variant-images/GIÀY SNEAKER NAM ZETHAN4.jpg'),
  (120, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Puma Trolls 2.0 Caven - Trắng1.jpg'),
  (121, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Puma Trolls 2.0 Caven - Trắng2.jpg'),
  (122, N'http://localhost:9000/product-variant-images/Giày Sneaker Trẻ Em Puma Trolls 2.0 Caven - Trắng4.jpg'),
  (123, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM CLUBKICKS1 (1).jpg'),
  (124, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM CLUBKICKS2 (1).jpg'),
  (125, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM CLUBKICKS3 (1).jpg'),
  (126, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM CLUBKICKS4 (1).jpg'),
  (127, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM CLUBKICKS1.jpg'),
  (128, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM CLUBKICKS2.jpg'),
  (129, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM CLUBKICKS3.jpg'),
  (130, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM CLUBKICKS4.jpg'),
  (131, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CAROTERIEL1.jpg'),
  (132, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CAROTERIEL2.jpg'),
  (133, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CAROTERIEL3.jpg'),
  (134, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CAROTERIEL4.jpg'),
  (135, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CAROTERIEL5.jpg'),
  (136, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CAROTERIEL1 (3).jpg'),
  (137, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CAROTERIEL2 (3).jpg'),
  (138, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CAROTERIEL3 (3).jpg'),
  (139, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CAROTERIEL4 (3).jpg'),
  (140, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM COELIN Men Sneakers1 (1).jpg'),
  (141, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM COELIN Men Sneakers2 (1).jpg'),
  (142, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM COELIN Men Sneakers3 (1).jpg'),
  (143, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM COELIN Men Sneakers4 (1).jpg'),
  (144, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM COELIN Men Sneakers1.jpg'),
  (145, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM COELIN Men Sneakers2.jpg'),
  (146, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM COELIN Men Sneakers3.jpg'),
  (147, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM COELIN Men Sneakers4.jpg'),
  (148, N'http://localhost:9000/product-variant-images/Giày Thời Trang Bé Gái Nike Revolution 7 (Gs)1 (2).jpg'),
  (149, N'http://localhost:9000/product-variant-images/Giày Thời Trang Bé Gái Nike Revolution 7 (Gs)2 (2).jpg'),
  (150, N'http://localhost:9000/product-variant-images/Giày Thời Trang Bé Gái Nike Revolution 7 (Gs)4 (2).jpg'),
  (151, N'http://localhost:9000/product-variant-images/Giày Thời Trang Bé Gái Nike Revolution 7 (Gs)1 (1).jpg'),
  (152, N'http://localhost:9000/product-variant-images/Giày Thời Trang Bé Gái Nike Revolution 7 (Gs)2 (1).jpg'),
  (153, N'http://localhost:9000/product-variant-images/Giày Thời Trang Bé Gái Nike Revolution 7 (Gs)4 (1).jpg'),
  (154, N'http://localhost:9000/product-variant-images/Giày Thời Trang Bé Gái Nike Revolution 7 (Gs)1.jpg'),
  (155, N'http://localhost:9000/product-variant-images/Giày Thời Trang Bé Gái Nike Revolution 7 (Gs)2.jpg'),
  (156, N'http://localhost:9000/product-variant-images/Giày Thời Trang Bé Gái Nike Revolution 7 (Gs)4.jpg'),
  (157, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM JENSEN1 (1).jpg'),
  (158, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM JENSEN2 (1).jpg'),
  (159, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM JENSEN3 (1).jpg'),
  (160, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM JENSEN4 (1).jpg'),
  (161, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM JENSEN1 (2).jpg'),
  (162, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM JENSEN2 (2).jpg'),
  (163, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM JENSEN3 (2).jpg'),
  (164, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM JENSEN4 (2).jpg'),
  (165, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CHICSNEAKER1.jpg'),
  (166, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CHICSNEAKER2.jpg'),
  (167, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CHICSNEAKER3.jpg'),
  (168, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CHICSNEAKER4.jpg'),
  (169, N'http://localhost:9000/product-variant-images/Giày Sneakers Nam Nike Air Max 97 - Đỏ1.jpg'),
  (170, N'http://localhost:9000/product-variant-images/Giày Sneakers Nam Nike Air Max 97 - Đỏ2.jpg'),
  (171, N'http://localhost:9000/product-variant-images/Giày Sneakers Nam Nike Air Max 97 - Đỏ3.jpg'),
  (172, N'http://localhost:9000/product-variant-images/Giày Sneakers Nam Nike Air Max 97 - Đỏ4.jpg'),
  (173, N'http://localhost:9000/product-variant-images/Giày Thời Trang Trẻ Em Converse Chuck Taylor All Star1.jpg'),
  (174, N'http://localhost:9000/product-variant-images/Giày Thời Trang Trẻ Em Converse Chuck Taylor All Star2.jpg'),
  (175, N'http://localhost:9000/product-variant-images/Giày Thời Trang Trẻ Em Converse Chuck Taylor All Star3.jpg'),
  (176, N'http://localhost:9000/product-variant-images/Giày Thời Trang Trẻ Em Converse Chuck Taylor All Star4.jpg'),
  (177, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CLUBCHIC1.jpg'),
  (178, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CLUBCHIC2.jpg'),
  (179, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CLUBCHIC3.jpg'),
  (180, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CLUBCHIC4.jpg'),
  (181, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CLUBCHIC1 (1).jpg'),
  (182, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CLUBCHIC2 (1).jpg'),
  (183, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CLUBCHIC3 (1).jpg'),
  (184, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CLUBCHIC4 (1).jpg'),
  (185, N'http://localhost:9000/product-variant-images/Giày Sneakers Nam Nike Blazer Low ''77 Vintage - Trắng2.jpg'),
  (186, N'http://localhost:9000/product-variant-images/Giày Sneakers Nam Nike Blazer Low ''77 Vintage - Trắng3.jpg'),
  (187, N'http://localhost:9000/product-variant-images/Giày Sneakers Nam Nike Blazer Low ''77 Vintage - Trắng1 (1).jpg'),
  (188, N'http://localhost:9000/product-variant-images/Giày Sneakers Nam Nike Blazer Low ''77 Vintage - Trắng3 (1).jpg'),
  (189, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CLUBSTYLE1.jpg'),
  (190, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CLUBSTYLE2.jpg'),
  (191, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CLUBSTYLE3.jpg'),
  (192, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CLUBSTYLE4.jpg'),
  (193, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ CLUBSTYLE5.jpg'),
  (194, N'http://localhost:9000/product-variant-images/Giày Thời Trang Trẻ Em Nike Revolution 7 (Psv)1 (2).jpg'),
  (195, N'http://localhost:9000/product-variant-images/Giày Thời Trang Trẻ Em Nike Revolution 7 (Psv)2 (2).jpg'),
  (196, N'http://localhost:9000/product-variant-images/Giày Thời Trang Trẻ Em Nike Revolution 7 (Psv)4 (2).jpg'),
  (197, N'http://localhost:9000/product-variant-images/Giày Thời Trang Trẻ Em Nike Revolution 7 (Psv)1 (1).jpg'),
  (198, N'http://localhost:9000/product-variant-images/Giày Thời Trang Trẻ Em Nike Revolution 7 (Psv)2 (1).jpg'),
  (199, N'http://localhost:9000/product-variant-images/Giày Thời Trang Trẻ Em Nike Revolution 7 (Psv)4 (1).jpg'),
  (200, N'http://localhost:9000/product-variant-images/Giày Thời Trang Trẻ Em Nike Revolution 7 (Psv)1.jpg'),
  (201, N'http://localhost:9000/product-variant-images/Giày Thời Trang Trẻ Em Nike Revolution 7 (Psv)2.jpg'),
  (202, N'http://localhost:9000/product-variant-images/Giày Thời Trang Trẻ Em Nike Revolution 7 (Psv)4.jpg'),
  (203, N'http://localhost:9000/product-variant-images/Giày Sneakers Nam Nike Court Legacy Next Natural - Đen2.jpg'),
  (204, N'http://localhost:9000/product-variant-images/Giày Sneakers Nam Nike Court Legacy Next Natural - Đen4.jpg'),
  (205, N'http://localhost:9000/product-variant-images/Giày Sneakers Nam Nike Court Legacy Next Natural - Đen1 (1).jpg'),
  (206, N'http://localhost:9000/product-variant-images/Giày Sneakers Nam Nike Court Legacy Next Natural - Đen3 (1).jpg'),
  (207, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ EASYINWEDGE1.jpg'),
  (208, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ EASYINWEDGE2.jpg'),
  (209, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ EASYINWEDGE3.jpg'),
  (210, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ EASYINWEDGE4.jpg'),
  (211, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ EASYINWEDGE1 (1).jpg'),
  (212, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ EASYINWEDGE2 (1).jpg'),
  (213, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ EASYINWEDGE3 (1).jpg'),
  (214, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ EASYINWEDGE4 (1).jpg'),
  (215, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ EASYINWEDGE5.jpg'),
  (216, N'http://localhost:9000/product-variant-images/Giày Sneakers Nam Nike Court Vision Mid Next Natural1.jpg'),
  (217, N'http://localhost:9000/product-variant-images/Giày Sneakers Nam Nike Court Vision Mid Next Natural4.jpg'),
  (218, N'http://localhost:9000/product-variant-images/PUMA Giày Sneaker Trẻ Em Puma Palermo Leather Lace Up Jr1 (1).jpg'),
  (219, N'http://localhost:9000/product-variant-images/PUMA Giày Sneaker Trẻ Em Puma Palermo Leather Lace Up Jr2 (1).jpg'),
  (220, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ ENZIE1.jpg'),
  (221, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ ENZIE2.jpg'),
  (222, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ ENZIE3.jpg'),
  (223, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ ENZIE4.jpg'),
  (224, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM OMONO1 (1).jpg'),
  (225, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM OMONO2 (1).jpg'),
  (226, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM OMONO3 (1).jpg'),
  (227, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM OMONO4 (1).jpg'),
  (228, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM OMONO5.jpg'),
  (229, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM OMONO1.jpg'),
  (230, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM OMONO2.jpg'),
  (231, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM OMONO3.jpg'),
  (232, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM OMONO4.jpg'),
  (233, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ FOREMORE1.jpg'),
  (234, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ FOREMORE2.jpg'),
  (235, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ FOREMORE3.jpg'),
  (236, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ FOREMORE4.jpg'),
  (237, N'http://localhost:9000/product-variant-images/PUMA Giày Sneaker Trẻ Em Puma Palermo Leather Lace Up Jr1.jpg'),
  (238, N'http://localhost:9000/product-variant-images/PUMA Giày Sneaker Trẻ Em Puma Palermo Leather Lace Up Jr2.jpg'),
  (239, N'http://localhost:9000/product-variant-images/PUMA Giày Sneaker Trẻ Em Puma Palermo Leather Lace Up Jr4 (1).jpg'),
  (240, N'http://localhost:9000/product-variant-images/PUMA Giày Sneaker Trẻ Em Puma Palermo Leather Lace Up Jr4.jpg'),
  (241, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM POKER1 (1).jpg'),
  (242, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM POKER2 (1).jpg'),
  (243, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM POKER3 (1).jpg'),
  (244, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM POKER4 (1).jpg'),
  (245, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM POKER1.jpg'),
  (246, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM POKER2.jpg'),
  (247, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM POKER3.jpg'),
  (248, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM POKER4.jpg'),
  (249, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ GARELIA Women Sneakers1.jpg'),
  (250, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ GARELIA Women Sneakers2.jpg'),
  (251, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ GARELIA Women Sneakers3.jpg'),
  (252, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ GARELIA Women Sneakers4.jpg'),
  (253, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM UPTOWN1 (1).jpg'),
  (254, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM UPTOWN2 (1).jpg'),
  (255, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM UPTOWN3 (1).jpg'),
  (256, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM UPTOWN4 (1).jpg'),
  (257, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ GRIEDIA1 (1).jpg'),
  (258, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ GRIEDIA2 (1).jpg'),
  (259, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ GRIEDIA3 (1).jpg'),
  (260, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ GRIEDIA4 (1).jpg'),
  (261, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ GRIEDIA1.jpg'),
  (262, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ GRIEDIA2.jpg'),
  (263, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ GRIEDIA3.jpg'),
  (264, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ GRIEDIA4.jpg'),
  (265, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM WEXLEY1 (1).jpg'),
  (266, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM WEXLEY2 (1).jpg'),
  (267, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM WEXLEY3 (1).jpg'),
  (268, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM WEXLEY4 (1).jpg'),
  (269, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM WEXLEY1 (2).jpg'),
  (270, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM WEXLEY2 (2).jpg'),
  (271, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM WEXLEY3 (2).jpg'),
  (272, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM WEXLEY4.jpg'),
  (273, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ HALERENNA1.jpg'),
  (274, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ HALERENNA2.jpg'),
  (275, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ HALERENNA3.jpg'),
  (276, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ HALERENNA4.jpg'),
  (277, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM WILLIO1.jpg'),
  (278, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM WILLIO2.jpg'),
  (279, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM WILLIO3.jpg'),
  (280, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NAM WILLIO4.jpg'),
  (281, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ PALAZZI1 (3).jpg'),
  (282, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ PALAZZI2 (3).jpg'),
  (283, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ PALAZZI3 (3).jpg'),
  (284, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ PALAZZI4 (3).jpg'),
  (285, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ PALAZZI1.jpg'),
  (286, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ PALAZZI2.jpg'),
  (287, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ PALAZZI3.jpg'),
  (288, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ PALAZZI4.jpg'),
  (289, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ PALAZZI1 (1).jpg'),
  (290, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ PALAZZI2 (1).jpg'),
  (291, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ PALAZZI3 (1).jpg'),
  (292, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ PALAZZI4 (1).jpg'),
  (293, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ PALAZZI1 (2).jpg'),
  (294, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ PALAZZI2 (2).jpg'),
  (295, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ PALAZZI3 (2).jpg'),
  (296, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ PALAZZI4 (2).jpg'),
  (297, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ PALAZZI5.jpg'),
  (298, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM BEATSPEC-L1.jpg'),
  (299, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM BEATSPEC-L2.jpg'),
  (300, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM BEATSPEC-L3.jpg'),
  (301, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM BEATSPEC-L4.jpg'),
  (302, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ RONGAN1 (1).jpg'),
  (303, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ RONGAN2 (1).jpg'),
  (304, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ RONGAN3 (1).jpg'),
  (305, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ RONGAN4 (1).jpg'),
  (306, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ RONGAN5.jpg'),
  (307, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ RONGAN1.jpg'),
  (308, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ RONGAN2.jpg'),
  (309, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ RONGAN3.jpg'),
  (310, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ RONGAN4.jpg'),
  (311, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM KEIRAN1 (1).jpg'),
  (312, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM KEIRAN2 (1).jpg'),
  (313, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM KEIRAN3 (1).jpg'),
  (314, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM KEIRAN4 (1).jpg'),
  (315, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM KEIRAN5 (1).jpg'),
  (316, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM KEIRAN1.jpg'),
  (317, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM KEIRAN2.jpg'),
  (318, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM KEIRAN3.jpg'),
  (319, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM KEIRAN4.jpg'),
  (320, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM KEIRAN5.jpg'),
  (321, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ STEPCOUNT1.jpg'),
  (322, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ STEPCOUNT2.jpg'),
  (323, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ STEPCOUNT3.jpg'),
  (324, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ STEPCOUNT4.jpg'),
  (325, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ STEPCOUNT1 (1).jpg'),
  (326, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ STEPCOUNT2 (1).jpg'),
  (327, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ STEPCOUNT3 (1).jpg'),
  (328, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ STEPCOUNT4 (1).jpg'),
  (329, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ STEPCOUNT1 (2).jpg'),
  (330, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ STEPCOUNT2 (2).jpg'),
  (331, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ STEPCOUNT3 (2).jpg'),
  (332, N'http://localhost:9000/product-variant-images/GIÀY SNEAKERS NỮ STEPCOUNT4 (2).jpg'),
  (333, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM QUICKLANE1.jpg'),
  (334, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM QUICKLANE2.jpg'),
  (335, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM QUICKLANE3.jpg'),
  (336, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM QUICKLANE4.jpg'),
  (337, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ CHICSNEAKER1.jpg'),
  (338, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ CHICSNEAKER2.jpg'),
  (339, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ CHICSNEAKER3.jpg'),
  (340, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ CHICSNEAKER4.jpg'),
  (341, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ CHICSNEAKER1 (1).jpg'),
  (342, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ CHICSNEAKER2 (1).jpg'),
  (343, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ CHICSNEAKER3 (1).jpg'),
  (344, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ CHICSNEAKER4 (1).jpg'),
  (345, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ CHICSNEAKER1 (2).jpg'),
  (346, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ CHICSNEAKER2 (2).jpg'),
  (347, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ CHICSNEAKER3 (2).jpg'),
  (348, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ CHICSNEAKER4 (2).jpg'),
  (349, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM SCOTTIEE1 (1).jpg'),
  (350, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM SCOTTIEE2 (1).jpg'),
  (351, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM SCOTTIEE3 (1).jpg'),
  (352, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM SCOTTIEE4 (1).jpg'),
  (353, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM SCOTTIEE1.jpg'),
  (354, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM SCOTTIEE2.jpg'),
  (355, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM SCOTTIEE3.jpg'),
  (356, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM SCOTTIEE4.jpg'),
  (357, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM SCOTTIEE5.jpg'),
  (358, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ CHICSNEAKER21.jpg'),
  (359, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ CHICSNEAKER22.jpg'),
  (360, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ CHICSNEAKER23.jpg'),
  (361, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ CHICSNEAKER24.jpg'),
  (362, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM WAVESPEC1 (1).jpg'),
  (363, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM WAVESPEC2 (1).jpg'),
  (364, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM WAVESPEC3 (1).jpg'),
  (365, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NAM WAVESPEC4 (1).jpg'),
  (366, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ DEEVALE1.jpg'),
  (367, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ DEEVALE2.jpg'),
  (368, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ DEEVALE3.jpg'),
  (369, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ DEEVALE4.jpg'),
  (370, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ DEEVALE5.jpg'),
  (371, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ DILATHIELLE1.jpg'),
  (372, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ DILATHIELLE2.jpg'),
  (373, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ DILATHIELLE3.jpg'),
  (374, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ DILATHIELLE4.jpg'),
  (375, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ DILATHIELLE1.jpg'),
  (376, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ DILATHIELLE2.jpg'),
  (377, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ DILATHIELLE3.jpg'),
  (378, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ DILATHIELLE4.jpg'),
  (379, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ ELABRINTAR1.jpg'),
  (380, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ ELABRINTAR2.jpg'),
  (381, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ ELABRINTAR3.jpg'),
  (382, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ ELABRINTAR4.jpg'),
  (383, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ ELABRINTAR1 (1).jpg'),
  (384, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ ELABRINTAR2 (1).jpg'),
  (385, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ ELABRINTAR3 (1).jpg'),
  (386, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ ELABRINTAR4 (1).jpg'),
  (387, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ GREENSPAN1.jpg'),
  (388, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ GREENSPAN2.jpg'),
  (389, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ GREENSPAN3.jpg'),
  (390, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ GREENSPAN4.jpg'),
  (391, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ PILLOWSNKR-L1.jpg'),
  (392, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ PILLOWSNKR-L2.jpg'),
  (393, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ PILLOWSNKR-L3.jpg'),
  (394, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ PILLOWSNKR-L4.jpg'),
  (395, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ PILLOWSNKR-L1 (1).jpg'),
  (396, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ PILLOWSNKR-L2 (1).jpg'),
  (397, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ PILLOWSNKR-L3 (1).jpg'),
  (398, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ PILLOWSNKR-L4 (1).jpg'),
  (399, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ SAXONY1.jpg'),
  (400, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ SAXONY2.jpg'),
  (401, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ SAXONY3.jpg'),
  (402, N'http://localhost:9000/product-variant-images/GIÀY THỂ THAO NỮ SAXONY4.jpg');
SET IDENTITY_INSERT product_images OFF;
GO

SET IDENTITY_INSERT product_variant_images ON;
INSERT INTO product_variant_images (id, product_variant_id, product_image_id) VALUES 
(1, 1, 1),(2, 1, 2),(3, 1, 3),(4, 1, 4),(5, 2, 1),(6, 2, 2),(7, 2, 3),(8, 2, 4),
(9, 3, 1),(10, 3, 2),(11, 3, 3),(12, 3, 4),(13, 4, 1),(14, 4, 2),(15, 4, 3),(16, 4, 4),
(17, 5, 5),(18, 5, 6),(19, 5, 7),(20, 5, 8),(21, 5, 9),(22, 6, 5),(23, 6, 6),(24, 6, 7),
(25, 6, 8),(26, 6, 9),(27, 7, 5),(28, 7, 6),(29, 7, 7),(30, 7, 8),(31, 7, 9),(32, 8, 10),
(33, 8, 11),(34, 8, 12),(35, 8, 13),(36, 8, 14),(37, 9, 10),(38, 9, 11),(39, 9, 12),(40, 9, 13),
(41, 9, 14),(42, 10, 10),(43, 10, 11),(44, 10, 12),(45, 10, 13),(46, 10, 14),(47, 11, 15),(48, 11, 16),
(49, 11, 17),(50, 11, 18),(51, 12, 15),(52, 12, 16),(53, 12, 17),(54, 12, 18),(55, 13, 15),(56, 13, 16),
(57, 13, 17),(58, 13, 18),(59, 14, 19),(60, 14, 20),(61, 14, 21),(62, 15, 19),(63, 15, 20),(64, 15, 21),
(65, 16, 22),(66, 16, 23),(67, 16, 24),(68, 16, 25),(69, 17, 22),(70, 17, 23),(71, 17, 24),(72, 17, 25),
(73, 18, 22),(74, 18, 23),(75, 18, 24),(76, 18, 25),(77, 19, 26),(78, 19, 27),(79, 19, 28),(80, 19, 29),
(81, 20, 26),(82, 20, 27),(83, 20, 28),(84, 20, 29),(85, 21, 26),(86, 21, 27),(87, 21, 28),(88, 21, 29),
(89, 22, 30),(90, 22, 31),(91, 22, 32),(92, 22, 33),(93, 23, 30),(94, 23, 31),(95, 23, 32),(96, 23, 33),
(97, 24, 34),(98, 24, 35),(99, 24, 36),(100, 24, 37),(101, 25, 34),(102, 25, 35),(103, 25, 36),(104, 25, 37),
(105, 26, 34),(106, 26, 35),(107, 26, 36),(108, 26, 37),(109, 27, 38),(110, 27, 39),(111, 27, 40),(112, 27, 41),
(113, 27, 42),(114, 28, 38),(115, 28, 39),(116, 28, 40),(117, 28, 41),(118, 28, 42),(119, 29, 38),(120, 29, 39),
(121, 29, 40),(122, 29, 41),(123, 29, 42),(124, 30, 43),(125, 30, 44),(126, 30, 45),(127, 30, 46),(128, 31, 43),
(129, 31, 44),(130, 31, 45),(131, 31, 46),(132, 32, 43),(133, 32, 44),(134, 32, 45),(135, 32, 46),(136, 33, 47),
(137, 33, 48),(138, 33, 49),(139, 33, 50),(140, 33, 51),(141, 34, 47),(142, 34, 48),(143, 34, 49),(144, 34, 50),
(145, 34, 51),(146, 35, 47),(147, 35, 48),(148, 35, 49),(149, 35, 50),(150, 35, 51),(151, 36, 52),(152, 36, 53),
(153, 36, 54),(154, 36, 55),(155, 38, 52),(156, 38, 53),(157, 38, 54),(158, 38, 55),(159, 39, 52),(160, 39, 53),
(161, 39, 54),(162, 39, 55),(163, 37, 56),(164, 37, 57),(165, 37, 58),(166, 37, 59),(167, 39, 60),(168, 39, 61),
(169, 39, 62),(170, 39, 63),(171, 40, 60),(172, 40, 61),(173, 40, 62),(174, 40, 63),(175, 41, 60),(176, 41, 61),
(177, 41, 62),(178, 41, 63),(179, 42, 60),(180, 42, 61),(181, 42, 62),(182, 42, 63),(183, 43, 64),(184, 43, 65),
(185, 43, 66),(186, 43, 67),(187, 44, 64),(188, 44, 65),(189, 44, 66),(190, 44, 67),(191, 45, 64),(192, 45, 65),
(193, 45, 66),(194, 45, 67),(195, 46, 68),(196, 46, 69),(197, 46, 70),(198, 46, 71),(199, 47, 68),(200, 47, 69),
(201, 47, 70),(202, 47, 71),(203, 48, 72),(204, 48, 73),(205, 48, 74),(206, 48, 75),(207, 49, 72),(208, 49, 73),
(209, 49, 74),(210, 49, 75),(211, 50, 72),(212, 50, 73),(213, 50, 74),(214, 50, 75),(215, 51, 72),(216, 51, 73),
(217, 51, 74),(218, 51, 75),(219, 52, 76),(220, 52, 77),(221, 52, 78),(222, 52, 79),(223, 53, 76),(224, 53, 77),
(225, 53, 78),(226, 53, 79),(227, 54, 76),(228, 54, 77),(229, 54, 78),(230, 54, 79),(231, 55, 76),(232, 55, 77),
(233, 55, 78),(234, 55, 79),(235, 56, 80),(236, 56, 81),(237, 56, 82),(238, 56, 83),(239, 57, 80),(240, 57, 81),
(241, 57, 82),(242, 57, 83),(243, 58, 80),(244, 58, 81),(245, 58, 82),(246, 58, 83),(247, 59, 84),(248, 59, 85),
(249, 59, 86),(250, 59, 87),(251, 60, 84),(252, 60, 85),(253, 60, 86),(254, 60, 87),(255, 61, 84),(256, 61, 85),
(257, 61, 86),(258, 61, 87),(259, 62, 84),(260, 62, 85),(261, 62, 86),(262, 62, 87),(263, 63, 88),(264, 63, 89),
(265, 63, 90),(266, 63, 91),(267, 63, 92),(268, 64, 88),(269, 64, 89),(270, 64, 90),(271, 64, 91),(272, 64, 92),
(273, 65, 88),(274, 65, 89),(275, 65, 90),(276, 65, 91),(277, 65, 92),(278, 66, 88),(279, 66, 89),(280, 66, 90),
(281, 66, 91),(282, 66, 92),(283, 67, 93),(284, 67, 94),(285, 67, 95),(286, 67, 96),(287, 68, 93),(288, 68, 94),
(289, 68, 95),(290, 68, 96),(291, 69, 93),(292, 69, 94),(293, 69, 95),(294, 69, 96),(295, 70, 93),(296, 70, 94),
(297, 70, 95),(298, 70, 96),(299, 71, 97),(300, 71, 98),(301, 71, 99),(302, 72, 97),(303, 72, 98),(304, 72, 99),
(305, 73, 97),(306, 73, 98),(307, 73, 99),(308, 74, 97),(309, 74, 98),(310, 74, 99),(311, 75, 100),(312, 75, 101),
(313, 75, 102),(314, 75, 103),(315, 76, 100),(316, 76, 101),(317, 76, 102),(318, 76, 103),(319, 77, 100),(320, 77, 101),
(321, 77, 102),(322, 77, 103),(323, 78, 104),(324, 78, 105),(325, 78, 106),(326, 78, 107),(327, 79, 104),(328, 79, 105),
(329, 79, 106),(330, 79, 107),(331, 80, 104),(332, 80, 105),(333, 80, 106),(334, 80, 107),(335, 81, 108),(336, 81, 109),
(337, 81, 110),(338, 81, 111),(339, 82, 108),(340, 82, 109),(341, 82, 110),(342, 82, 111),(343, 83, 108),(344, 83, 109),
(345, 83, 110),(346, 83, 111),(347, 84, 112),(348, 84, 113),(349, 84, 114),(350, 84, 115),(351, 85, 112),(352, 85, 113),
(353, 85, 114),(354, 85, 115),(355, 86, 112),(356, 86, 113),(357, 86, 114),(358, 86, 115),(359, 87, 116),(360, 87, 117),
(361, 87, 118),(362, 87, 119),(363, 88, 116),(364, 88, 117),(365, 88, 118),(366, 88, 119),(367, 89, 116),(368, 89, 117),
(369, 89, 118),(370, 89, 119),(371, 90, 116),(372, 90, 117),(373, 90, 118),(374, 90, 119),(375, 91, 120),(376, 91, 121),
(377, 91, 122),(378, 92, 120),(379, 92, 121),(380, 92, 122),(381, 93, 120),(382, 93, 121),(383, 93, 122),(384, 94, 120),
(385, 94, 121),(386, 94, 122),(387, 95, 123),(388, 95, 124),(389, 95, 125),(390, 95, 126),(391, 96, 123),(392, 96, 124),
(393, 96, 125),(394, 96, 126),(395, 97, 123),(396, 97, 124),(397, 97, 125),(398, 97, 126),(399, 98, 127),(400, 98, 128),
(401, 98, 129),(402, 98, 130),(403, 99, 127),(404, 99, 128),(405, 99, 129),(406, 99, 130),(407, 100, 127),(408, 100, 128),
(409, 100, 129),(410, 100, 130),(411, 101, 131),(412, 101, 132),(413, 101, 133),(414, 101, 134),(415, 101, 135),(416, 102, 131),
(417, 102, 132),(418, 102, 133),(419, 102, 134),(420, 102, 135),(421, 103, 131),(422, 103, 132),(423, 103, 133),(424, 103, 134),
(425, 103, 135),(426, 104, 136),(427, 104, 137),(428, 104, 138),(429, 104, 139),(430, 105, 136),(431, 105, 137),(432, 105, 138),
(433, 105, 139),(434, 106, 136),(435, 106, 137),(436, 106, 138),(437, 106, 139),(438, 107, 140),(439, 107, 141),(440, 107, 142),
(441, 107, 143),(442, 108, 140),(443, 108, 141),(444, 108, 142),(445, 108, 143),(446, 109, 140),(447, 109, 141),(448, 109, 142),
(449, 109, 143),(450, 110, 144),(451, 110, 145),(452, 110, 146),(453, 110, 147),(454, 111, 144),(455, 111, 145),(456, 111, 146),
(457, 111, 147),(458, 112, 144),(459, 112, 145),(460, 112, 146),(461, 112, 147),(462, 113, 148),(463, 113, 149),(464, 113, 150),
(465, 114, 148),(466, 114, 149),(467, 114, 150),(468, 115, 148),(469, 115, 149),(470, 115, 150),(471, 116, 151),(472, 116, 152),
(473, 116, 153),(474, 117, 151),(475, 117, 152),(476, 117, 153),(477, 118, 154),(478, 118, 155),(479, 118, 156),(480, 119, 154),
(481, 119, 155),(482, 119, 156),(483, 120, 157),(484, 120, 158),(485, 120, 159),(486, 120, 160),(487, 121, 157),(488, 121, 158),
(489, 121, 159),(490, 121, 160),(491, 122, 157),(492, 122, 158),(493, 122, 159),(494, 122, 160),(495, 123, 161),(496, 123, 162),
(497, 123, 163),(498, 123, 164),(499, 124, 161),(500, 124, 162),(501, 124, 163),(502, 124, 164),(503, 125, 161),(504, 125, 162),
(505, 125, 163),(506, 125, 164),(507, 126, 165),(508, 126, 166),(509, 126, 167),(510, 126, 168),(511, 127, 165),(512, 127, 166),
(513, 127, 167),(514, 127, 168),(515, 128, 165),(516, 128, 166),(517, 128, 167),(518, 128, 168),(519, 129, 165),(520, 129, 166),
(521, 129, 167),(522, 129, 168),(523, 130, 169),(524, 130, 170),(525, 130, 171),(526, 130, 172),(527, 131, 169),(528, 131, 170),
(529, 131, 171),(530, 131, 172),(531, 132, 169),(532, 132, 170),(533, 132, 171),(534, 132, 172),(535, 133, 169),(536, 133, 170),
(537, 133, 171),(538, 133, 172),(539, 134, 173),(540, 134, 174),(541, 134, 175),(542, 134, 176),(543, 135, 173),(544, 135, 174),
(545, 135, 175),(546, 135, 176),(547, 136, 173),(548, 136, 174),(549, 136, 175),(550, 136, 176),(551, 137, 173),(552, 137, 174),
(553, 137, 175),(554, 137, 176),(555, 138, 177),(556, 138, 178),(557, 138, 179),(558, 138, 180),(559, 139, 177),(560, 139, 178),
(561, 139, 179),(562, 139, 180),(563, 140, 177),(564, 140, 178),(565, 140, 179),(566, 140, 180),(567, 141, 181),(568, 141, 182),
(569, 141, 183),(570, 141, 184),(571, 142, 181),(572, 142, 182),(573, 142, 183),(574, 142, 184),(575, 143, 181),(576, 143, 182),
(577, 143, 183),(578, 143, 184),(579, 144, 185),(580, 144, 186),(581, 145, 185),(582, 145, 186),(583, 146, 185),(584, 146, 186),
(585, 147, 187),(586, 147, 188),(587, 148, 187),(588, 148, 188),(589, 149, 187),(590, 149, 188),(591, 150, 189),(592, 150, 190),
(593, 150, 191),(594, 150, 192),(595, 150, 193),(596, 151, 189),(597, 151, 190),(598, 151, 191),(599, 151, 192),(600, 151, 193),
(601, 152, 189),(602, 152, 190),(603, 152, 191),(604, 152, 192),(605, 152, 193),(606, 153, 189),(607, 153, 190),(608, 153, 191),
(609, 153, 192),(610, 153, 193),(611, 154, 194),(612, 154, 195),(613, 154, 196),(614, 155, 194),(615, 155, 195),(616, 155, 196),
(617, 156, 194),(618, 156, 195),(619, 156, 196),(620, 157, 197),(621, 157, 198),(622, 157, 199),(623, 158, 197),(624, 158, 198),
(625, 158, 199),(626, 159, 200),(627, 159, 201),(628, 159, 202),(629, 160, 200),(630, 160, 201),(631, 160, 202),(632, 161, 203),
(633, 161, 204),(634, 162, 203),(635, 162, 204),(636, 163, 203),(637, 163, 204),(638, 164, 205),(639, 164, 206),(640, 165, 205),
(641, 165, 206),(642, 166, 205),(643, 166, 206),(644, 167, 207),(645, 167, 208),(646, 167, 209),(647, 167, 210),(648, 168, 207),
(649, 168, 208),(650, 168, 209),(651, 168, 210),(652, 169, 207),(653, 169, 208),(654, 169, 209),(655, 169, 210),(656, 170, 211),
(657, 170, 212),(658, 170, 213),(659, 170, 214),(660, 170, 215),(661, 171, 211),(662, 171, 212),(663, 171, 213),(664, 171, 214),
(665, 171, 215),(666, 172, 211),(667, 172, 212),(668, 172, 213),(669, 172, 214),(670, 172, 215),(671, 173, 216),(672, 173, 217),
(673, 174, 216),(674, 174, 217),(675, 175, 216),(676, 175, 217),(677, 176, 216),(678, 176, 217),(679, 177, 218),(680, 177, 219),
(681, 178, 218),(682, 178, 219),(683, 179, 218),(684, 179, 219),(685, 180, 218),(686, 180, 219),(687, 181, 220),(688, 181, 221),
(689, 181, 222),(690, 181, 223),(691, 182, 220),(692, 182, 221),(693, 182, 222),(694, 182, 223),(695, 183, 220),(696, 183, 221),
(697, 183, 222),(698, 183, 223),(699, 184, 220),(700, 184, 221),(701, 184, 222),(702, 184, 223),(703, 185, 224),(704, 185, 225),
(705, 185, 226),(706, 185, 227),(707, 185, 228),(708, 186, 224),(709, 186, 225),(710, 186, 226),(711, 186, 227),(712, 186, 228),
(713, 187, 224),(714, 187, 225),(715, 187, 226),(716, 187, 227),(717, 187, 228),(718, 188, 229),(719, 188, 230),(720, 188, 231),
(721, 188, 232),(722, 189, 229),(723, 189, 230),(724, 189, 231),(725, 189, 232),(726, 190, 229),(727, 190, 230),(728, 190, 231),
(729, 190, 232),(730, 191, 233),(731, 191, 234),(732, 191, 235),(733, 191, 236),(734, 192, 233),(735, 192, 234),(736, 192, 235),
(737, 192, 236),(738, 193, 233),(739, 193, 234),(740, 193, 235),(741, 193, 236),(742, 194, 233),(743, 194, 234),(744, 194, 235),
(745, 194, 236),(746, 195, 237),(747, 195, 238),(748, 195, 239),(749, 195, 240),(750, 196, 237),(751, 196, 238),(752, 196, 239),
(753, 196, 240),(754, 197, 237),(755, 197, 238),(756, 197, 239),(757, 197, 240),(758, 198, 237),(759, 198, 238),(760, 198, 239),
(761, 198, 240),(762, 199, 241),(763, 199, 242),(764, 199, 243),(765, 199, 244),(766, 200, 241),(767, 200, 242),(768, 200, 243),
(769, 200, 244),(770, 201, 241),(771, 201, 242),(772, 201, 243),(773, 201, 244),(774, 202, 245),(775, 202, 246),(776, 202, 247),
(777, 202, 248),(778, 203, 245),(779, 203, 246),(780, 203, 247),(781, 203, 248),(782, 204, 245),(783, 204, 246),(784, 204, 247),
(785, 204, 248),(786, 205, 249),(787, 205, 250),(788, 205, 251),(789, 205, 252),(790, 206, 249),(791, 206, 250),(792, 206, 251),
(793, 206, 252),(794, 207, 249),(795, 207, 250),(796, 207, 251),(797, 207, 252),(798, 208, 249),(799, 208, 250),(800, 208, 251),
(801, 208, 252),(802, 209, 253),(803, 209, 254),(804, 209, 255),(805, 209, 256),(806, 210, 253),(807, 210, 254),(808, 210, 255),
(809, 210, 256),(810, 211, 253),(811, 211, 254),(812, 211, 255),(813, 211, 256),(814, 212, 253),(815, 212, 254),(816, 212, 255),
(817, 212, 256),(818, 213, 257),(819, 213, 258),(820, 213, 259),(821, 213, 260),(822, 214, 257),(823, 214, 258),(824, 214, 259),
(825, 214, 260),(826, 215, 257),(827, 215, 258),(828, 215, 259),(829, 215, 260),(830, 216, 261),(831, 216, 262),(832, 216, 263),
(833, 216, 264),(834, 217, 261),(835, 217, 262),(836, 217, 263),(837, 217, 264),(838, 218, 261),(839, 218, 262),(840, 218, 263),
(841, 218, 264),(842, 219, 265),(843, 219, 266),(844, 219, 267),(845, 219, 268),(846, 220, 265),(847, 220, 266),(848, 220, 267),
(849, 220, 268),(850, 221, 265),(851, 221, 266),(852, 221, 267),(853, 221, 268),(854, 222, 269),(855, 222, 270),(856, 222, 271),
(857, 222, 272),(858, 223, 269),(859, 223, 270),(860, 223, 271),(861, 223, 272),(862, 224, 269),(863, 224, 270),(864, 224, 271),
(865, 224, 272),(866, 225, 273),(867, 225, 274),(868, 225, 275),(869, 225, 276),(870, 226, 273),(871, 226, 274),(872, 226, 275),
(873, 226, 276),(874, 227, 273),(875, 227, 274),(876, 227, 275),(877, 227, 276),(878, 228, 273),(879, 228, 274),(880, 228, 275),
(881, 228, 276),(882, 229, 277),(883, 229, 278),(884, 229, 279),(885, 229, 280),(886, 230, 277),(887, 230, 278),(888, 230, 279),
(889, 230, 280),(890, 231, 277),(891, 231, 278),(892, 231, 279),(893, 231, 280),(894, 232, 277),(895, 232, 278),(896, 232, 279),
(897, 232, 280),(898, 233, 281),(899, 233, 282),(900, 233, 283),(901, 233, 284),(902, 234, 281),(903, 234, 282),(904, 234, 283),
(905, 234, 284),(906, 235, 281),(907, 235, 282),(908, 235, 283),(909, 235, 284),(910, 236, 285),(911, 236, 286),(912, 236, 287),
(913, 236, 288),(914, 237, 285),(915, 237, 286),(916, 237, 287),(917, 237, 288),(918, 238, 285),(919, 238, 286),(920, 238, 287),
(921, 238, 288),(922, 239, 289),(923, 239, 290),(924, 239, 291),(925, 239, 292),(926, 240, 289),(927, 240, 290),(928, 240, 291),
(929, 240, 292),(930, 241, 289),(931, 241, 290),(932, 241, 291),(933, 241, 292),(934, 242, 289),(935, 242, 290),(936, 242, 291),
(937, 242, 292),(938, 243, 293),(939, 243, 294),(940, 243, 295),(941, 243, 296),(942, 243, 297),(943, 244, 293),(944, 244, 294),
(945, 244, 295),(946, 244, 296),(947, 244, 297),(948, 245, 293),(949, 245, 294),(950, 245, 295),(951, 245, 296),(952, 245, 297),
(953, 246, 298),(954, 246, 299),(955, 246, 300),(956, 246, 301),(957, 247, 298),(958, 247, 299),(959, 247, 300),(960, 247, 301),
(961, 248, 298),(962, 248, 299),(963, 248, 300),(964, 248, 301),(965, 249, 298),(966, 249, 299),(967, 249, 300),(968, 249, 301),
(969, 250, 302),(970, 250, 303),(971, 250, 304),(972, 250, 305),(973, 250, 306),(974, 251, 302),(975, 251, 303),(976, 251, 304),
(977, 251, 305),(978, 251, 306),(979, 252, 302),(980, 252, 303),(981, 252, 304),(982, 252, 305),(983, 252, 306),(984, 253, 307),
(985, 253, 308),(986, 253, 309),(987, 253, 310),(988, 254, 307),(989, 254, 308),(990, 254, 309),(991, 254, 310),(992, 255, 307),
(993, 255, 308),(994, 255, 309),(995, 255, 310),(996, 256, 311),(997, 256, 312),(998, 256, 313),(999, 256, 314),(1000, 256, 315);
SET IDENTITY_INSERT product_variant_images OFF;
GO

SET IDENTITY_INSERT product_variant_images ON;
INSERT INTO product_variant_images (id, product_variant_id, product_image_id) VALUES 
(1001, 257, 311),(1002, 257, 312),(1003, 257, 313),(1004, 257, 314),(1005, 257, 315),(1006, 258, 311),(1007, 258, 312),(1008, 258, 313),
(1009, 258, 314),(1010, 258, 315),(1011, 259, 316),(1012, 259, 317),(1013, 259, 318),(1014, 259, 319),(1015, 259, 320),(1016, 260, 316),
(1017, 260, 317),(1018, 260, 318),(1019, 260, 319),(1020, 260, 320),(1021, 261, 321),(1022, 261, 322),(1023, 261, 323),(1024, 261, 324),
(1025, 262, 321),(1026, 262, 322),(1027, 262, 323),(1028, 262, 324),(1029, 263, 321),(1030, 263, 322),(1031, 263, 323),(1032, 263, 324),
(1033, 264, 325),(1034, 264, 326),(1035, 264, 327),(1036, 264, 328),(1037, 265, 325),(1038, 265, 326),(1039, 265, 327),(1040, 265, 328),
(1041, 266, 329),(1042, 266, 330),(1043, 266, 331),(1044, 266, 332),(1045, 267, 329),(1046, 267, 330),(1047, 267, 331),(1048, 267, 332),
(1049, 268, 333),(1050, 268, 334),(1051, 268, 335),(1052, 268, 336),(1053, 269, 333),(1054, 269, 334),(1055, 269, 335),(1056, 269, 336),
(1057, 270, 333),(1058, 270, 334),(1059, 270, 335),(1060, 270, 336),(1061, 271, 333),(1062, 271, 334),(1063, 271, 335),(1064, 271, 336),
(1065, 272, 337),(1066, 272, 338),(1067, 272, 339),(1068, 272, 340),(1069, 273, 337),(1070, 273, 338),(1071, 273, 339),(1072, 273, 340),
(1073, 274, 337),(1074, 274, 338),(1075, 274, 339),(1076, 274, 340),(1077, 275, 341),(1078, 275, 342),(1079, 275, 343),(1080, 275, 344),
(1081, 276, 341),(1082, 276, 342),(1083, 276, 343),(1084, 276, 344),(1085, 277, 345),(1086, 277, 346),(1087, 277, 347),(1088, 277, 348),
(1089, 278, 345),(1090, 278, 346),(1091, 278, 347),(1092, 278, 348),(1093, 279, 349),(1094, 279, 350),(1095, 279, 351),(1096, 279, 352),
(1097, 280, 349),(1098, 280, 350),(1099, 280, 351),(1100, 280, 352),(1101, 281, 349),(1102, 281, 350),(1103, 281, 351),(1104, 281, 352),
(1105, 282, 353),(1106, 282, 354),(1107, 282, 355),(1108, 282, 356),(1109, 282, 357),(1110, 283, 353),(1111, 283, 354),(1112, 283, 355),
(1113, 283, 356),(1114, 283, 357),(1115, 284, 358),(1116, 284, 359),(1117, 284, 360),(1118, 284, 361),(1119, 285, 358),(1120, 285, 359),
(1121, 285, 360),(1122, 285, 361),(1123, 286, 358),(1124, 286, 359),(1125, 286, 360),(1126, 286, 361),(1127, 287, 358),(1128, 287, 359),
(1129, 287, 360),(1130, 287, 361),(1131, 288, 362),(1132, 288, 363),(1133, 288, 364),(1134, 288, 365),(1135, 289, 362),(1136, 289, 363),
(1137, 289, 364),(1138, 289, 365),(1139, 290, 362),(1140, 290, 363),(1141, 290, 364),(1142, 290, 365),(1143, 291, 362),(1144, 291, 363),
(1145, 291, 364),(1146, 291, 365),(1147, 292, 366),(1148, 292, 367),(1149, 292, 368),(1150, 292, 369),(1151, 292, 370),(1152, 293, 366),
(1153, 293, 367),(1154, 293, 368),(1155, 293, 369),(1156, 293, 370),(1157, 294, 366),(1158, 294, 367),(1159, 294, 368),(1160, 294, 369),
(1161, 294, 370),(1162, 295, 366),(1163, 295, 367),(1164, 295, 368),(1165, 295, 369),(1166, 295, 370),(1167, 296, 375),(1168, 296, 376),
(1169, 296, 377),(1170, 296, 378),(1171, 297, 375),(1172, 297, 376),(1173, 297, 377),(1174, 297, 378),(1175, 298, 375),(1176, 298, 376),
(1177, 298, 377),(1178, 298, 378),(1179, 299, 375),(1180, 299, 376),(1181, 299, 377),(1182, 299, 378),(1183, 300, 379),(1184, 300, 380),
(1185, 300, 381),(1186, 300, 382),(1187, 301, 379),(1188, 301, 380),(1189, 301, 381),(1190, 301, 382),(1191, 302, 379),(1192, 302, 380),
(1193, 302, 381),(1194, 302, 382),(1195, 303, 383),(1196, 303, 384),(1197, 303, 385),(1198, 303, 386),(1199, 304, 383),(1200, 304, 384),
(1201, 304, 385),(1202, 304, 386),(1203, 305, 387),(1204, 305, 388),(1205, 305, 389),(1206, 305, 390),(1207, 306, 387),(1208, 306, 388),
(1209, 306, 389),(1210, 306, 390),(1211, 307, 387),(1212, 307, 388),(1213, 307, 389),(1214, 307, 390),(1215, 308, 387),(1216, 308, 388),
(1217, 308, 389),(1218, 308, 390),(1219, 309, 391),(1220, 309, 392),(1221, 309, 393),(1222, 309, 394),(1223, 310, 391),(1224, 310, 392),
(1225, 310, 393),(1226, 310, 394),(1227, 311, 391),(1228, 311, 392),(1229, 311, 393),(1230, 311, 394),(1231, 312, 395),(1232, 312, 396),
(1233, 312, 397),(1234, 312, 398),(1235, 313, 395),(1236, 313, 396),(1237, 313, 397),(1238, 313, 398),(1239, 314, 399),(1240, 314, 400),
(1241, 314, 401),(1242, 314, 402),(1243, 315, 399),(1244, 315, 400),(1245, 315, 401),(1246, 315, 402),(1247, 316, 399),(1248, 316, 400),
(1249, 316, 401),(1250, 316, 402),(1251, 317, 399),(1252, 317, 400),(1253, 317, 401),(1254, 317, 402);
SET IDENTITY_INSERT product_variant_images OFF;

select * from brands
select * from categories
select * from products
select * from product_variants
select * from product_variant_images
select * from product_images
select * from materials
select * from reviews
select * from tokens
select * from users
select * from payment_methods
select * from cart_items
select * from carts
select * from order_timeline 
select * from transactions where order_id = 62
select * from vnpay_transaction_details
select * from roles
select * from users
select * from order_details
select * from return_requests
select * from coupons
select * from orders where id = 54
select * from transactions where order_id = 54

update users set role_id = 1 where id = 21
update users set role_id = 2 where id = 23

-- update thumbnail cua product
UPDATE p
SET p.thumbnail = img.image_url
FROM products p
JOIN (
    SELECT 
        pv.product_id,
        img.image_url
    FROM product_variants pv
    JOIN (
        SELECT 
            pvi.product_variant_id,
            pi.image_url,
            ROW_NUMBER() OVER (PARTITION BY pv.product_id ORDER BY pvi.id) AS rn
        FROM product_variants pv
        JOIN product_variant_images pvi ON pvi.product_variant_id = pv.id
        JOIN product_images pi ON pi.id = pvi.product_image_id
    ) img ON img.product_variant_id = pv.id AND img.rn = 1
) AS img ON img.product_id = p.id;

UPDATE pv
SET pv.thumbnail = first_image.image_url
FROM product_variants pv
INNER JOIN (
    SELECT pvi.product_variant_id, pi.image_url
    FROM product_variant_images pvi
    INNER JOIN product_images pi ON pvi.product_image_id = pi.id
    WHERE pvi.id IN (
        SELECT MIN(id)
        FROM product_variant_images
        GROUP BY product_variant_id
    )
) AS first_image ON pv.id = first_image.product_variant_id;


SELECT DISTINCT p.id, p.name, p.thumbnail, pr.name AS promotion_name, pr.value, pr.start_date, pr.expiration_date
FROM products p
JOIN product_variants pv ON pv.product_id = p.id
JOIN product_promotions pp ON pp.product_variant_id = pv.id
JOIN promotions pr ON pr.id = pp.promotion_id
WHERE pr.status = 1 -- chỉ lấy promotion đang hoạt động
  AND GETDATE() BETWEEN pr.start_date AND pr.expiration_date;


  SELECT TOP 5 id, price, quantity, promotion_value, promotion_discount_amount, final_price, total_money
FROM order_details;
