import axios from 'axios';
import { BANK_CONFIG, DEFAULT_BANK, VIETQR_CONFIG, TRANSFER_CONFIG } from '@/config/bankConfig.js';

/**
 * Tạo QR code cho chuyển khoản bằng VietQR API
 * @param {Object} params - Thông tin chuyển khoản
 * @param {string} params.amount - Số tiền chuyển khoản
 * @param {string} params.addInfo - Nội dung chuyển khoản
 * @param {string} params.bankCode - Mã ngân hàng (mặc định: mbbank)
 * @param {string} params.format - Định dạng QR (mặc định: vietqr_net)
 * @returns {Promise<Object>} - Kết quả từ API
 */
export const generateVietQR = async (params) => {
  try {
    const {
      amount,
      addInfo = TRANSFER_CONFIG.DEFAULT_ADD_INFO,
      bankCode = DEFAULT_BANK,
      format = VIETQR_CONFIG.DEFAULT_FORMAT
    } = params;

    // Lấy thông tin ngân hàng
    const bankInfo = BANK_CONFIG[bankCode];
    if (!bankInfo) {
      throw new Error(`Không hỗ trợ ngân hàng: ${bankCode}`);
    }

    // Chuẩn bị dữ liệu gửi lên API
    const requestData = {
      accountNo: bankInfo.accountNo,
      accountName: bankInfo.accountName,
      acqId: bankInfo.acqId,
      addInfo: addInfo,
      amount: amount.toString(),
      format: format
    };

    console.log('VietQR API Request:', requestData);

    // Gọi API VietQR
    const response = await axios.post(`${VIETQR_CONFIG.API_BASE_URL}/generate`, requestData, {
      headers: {
        'x-api-key': VIETQR_CONFIG.API_KEY,
        'Content-Type': 'application/json'
      },
      timeout: VIETQR_CONFIG.TIMEOUT
    });

    console.log('VietQR API Response:', response.data);

    if (response.data && response.data.code === '00') {
      return {
        success: true,
        qrDataURL: response.data.data.qrDataURL,
        qrCode: response.data.data.qrCode,
        bankInfo: bankInfo
      };
    } else {
      throw new Error(response.data?.desc || 'Lỗi khi tạo QR code');
    }

  } catch (error) {
    console.error('VietQR API Error:', error);
    
    // Trả về lỗi chi tiết
    if (error.response) {
      // Server trả về lỗi
      const errorData = error.response.data;
      throw new Error(errorData?.desc || errorData?.message || `Lỗi server: ${error.response.status}`);
    } else if (error.request) {
      // Không nhận được response
      throw new Error('Không thể kết nối đến VietQR API');
    } else {
      // Lỗi khác
      throw new Error(error.message || 'Lỗi không xác định');
    }
  }
};

/**
 * Tạo URL QR code đơn giản (không cần API key)
 * @param {Object} params - Thông tin chuyển khoản
 * @param {string} params.amount - Số tiền chuyển khoản
 * @param {string} params.addInfo - Nội dung chuyển khoản
 * @param {string} params.bankCode - Mã ngân hàng (mặc định: mbbank)
 * @returns {string} - URL của QR code
 */
export const generateSimpleVietQR = (params) => {
  const {
    amount,
    addInfo = TRANSFER_CONFIG.DEFAULT_ADD_INFO,
    bankCode = DEFAULT_BANK
  } = params;

  const bankInfo = BANK_CONFIG[bankCode];
  if (!bankInfo) {
    throw new Error(`Không hỗ trợ ngân hàng: ${bankCode}`);
  }

  // Tạo URL QR code đơn giản
  const qrParams = new URLSearchParams({
    accountNo: bankInfo.accountNo,
    accountName: bankInfo.accountName,
    acqId: bankInfo.acqId,
    addInfo: addInfo,
    amount: amount.toString(),
    format: 'vietqr_net'
  });

  return `https://img.vietqr.io/image/${bankCode}-${bankInfo.accountNo}-compact.jpg?${qrParams.toString()}`;
};

/**
 * Lấy danh sách ngân hàng được hỗ trợ
 * @returns {Array} - Danh sách ngân hàng
 */
export const getSupportedBanks = () => {
  return Object.keys(BANK_CONFIG).map(code => ({
    code,
    name: code === 'mbbank' ? 'MB Bank' : code,
    ...BANK_CONFIG[code]
  }));
};

/**
 * Validate thông tin chuyển khoản
 * @param {Object} params - Thông tin chuyển khoản
 * @returns {Object} - Kết quả validation
 */
export const validatePaymentInfo = (params) => {
  const { amount, addInfo } = params;
  const errors = [];

  // Validate số tiền
  if (!amount || amount <= 0) {
    errors.push('Số tiền phải lớn hơn 0');
  }

  if (amount < TRANSFER_CONFIG.MIN_AMOUNT) {
    errors.push(`Số tiền tối thiểu là ${TRANSFER_CONFIG.MIN_AMOUNT.toLocaleString('vi-VN')} VND`);
  }

  if (amount > TRANSFER_CONFIG.MAX_AMOUNT) {
    errors.push(`Số tiền không được vượt quá ${TRANSFER_CONFIG.MAX_AMOUNT.toLocaleString('vi-VN')} VND`);
  }

  // Validate nội dung chuyển khoản
  if (addInfo && addInfo.length > TRANSFER_CONFIG.MAX_ADD_INFO_LENGTH) {
    errors.push(`Nội dung chuyển khoản không được vượt quá ${TRANSFER_CONFIG.MAX_ADD_INFO_LENGTH} ký tự`);
  }

  return {
    isValid: errors.length === 0,
    errors
  };
};

export default {
  generateVietQR,
  generateSimpleVietQR,
  getSupportedBanks,
  validatePaymentInfo
};
