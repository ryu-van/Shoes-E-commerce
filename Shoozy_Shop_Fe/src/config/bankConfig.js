// Cấu hình thông tin ngân hàng cho VietQR
export const BANK_CONFIG = {
  // MB Bank configuration
  mbbank: {
    acqId: '970422', // Mã BIN của MB Bank
    accountNo: '0365684005',
    accountName: 'NGUYEN MINH VU',
    bankName: 'MB Bank',
    bankCode: 'mbbank'
  },
  
  // Có thể thêm các ngân hàng khác
  // vietinbank: {
  //   acqId: '970415',
  //   accountNo: '1234567890',
  //   accountName: 'TEN CHU TAI KHOAN',
  //   bankName: 'VietinBank',
  //   bankCode: 'vietinbank'
  // },
  
  // vietcombank: {
  //   acqId: '970436',
  //   accountNo: '1234567890',
  //   accountName: 'TEN CHU TAI KHOAN',
  //   bankName: 'Vietcombank',
  //   bankCode: 'vietcombank'
  // }
};

// Cấu hình mặc định
export const DEFAULT_BANK = 'mbbank';

// Cấu hình VietQR API
export const VIETQR_CONFIG = {
  API_BASE_URL: 'https://api.vietqr.io/v1',
  API_KEY: 'YOUR_API_KEY', // Thay thế bằng API key thực tế
  DEFAULT_FORMAT: 'vietqr_net',
  TIMEOUT: 10000, // 10 seconds
  FALLBACK_ENABLED: true // Cho phép sử dụng fallback URL
};

// Cấu hình nội dung chuyển khoản
export const TRANSFER_CONFIG = {
  DEFAULT_ADD_INFO: 'Thanh toan don hang',
  MAX_ADD_INFO_LENGTH: 200,
  MAX_AMOUNT: 500000000, // 500 triệu VND
  MIN_AMOUNT: 1000 // 1,000 VND
};

export default {
  BANK_CONFIG,
  DEFAULT_BANK,
  VIETQR_CONFIG,
  TRANSFER_CONFIG
};
