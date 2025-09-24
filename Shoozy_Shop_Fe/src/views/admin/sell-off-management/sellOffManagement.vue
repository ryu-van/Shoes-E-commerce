<template>
  <!-- Tiêu đề và nút tạo đơn -->
   <br>
  <div class="py-4 px-4" style="width: 100%; margin-top: -20px">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="fw-extrabold mb-0">Bán hàng</h2>
      <button class="btn btn-warning btn-sm" @click="createNewOrder" title="Tạo đơn mới" :disabled="pendingOrders.length >= 5">
        <i class="fas fa-plus me-1"></i>Tạo đơn hàng
        <span v-if="pendingOrders.length > 0" class="badge bg-light text-dark ms-1">{{ pendingOrders.length }}/5</span>
      </button>
    </div>
  </div>

  <!-- ConfirmModal -->
  <ConfirmModal
    :show="showConfirmModal"
    :title="confirmModalTitle"
    :message="confirmModalMessage"
    :type="confirmModalType"
    :confirm-text="confirmModalConfirmText"
    :cancel-text="confirmModalCancelText"
    :loading="confirmModalLoading"
    @confirm="handleConfirmAction"
    @cancel="hideConfirmModal"
    @close="hideConfirmModal"
  />

  <!-- Modal chọn số lượng -->
  <div v-if="showQuantityModal" class="modal fade show d-block" tabindex="-1" style="background:rgba(0,0,0,0.5); z-index: 1060;">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content">
        <div class="modal-header bg-primary text-white">
          <h5 class="modal-title fw-bold">🛒 Chọn số lượng</h5>
          <button type="button" class="btn-close btn-close-white" @click="closeQuantityModal"></button>
        </div>
        <div class="modal-body">
          <div v-if="selectedProductForQuantity" class="text-center">
            <!-- Thông tin sản phẩm -->
            <div class="mb-4">
              <img :src="selectedProductForQuantity.thumbnail || '/default.jpg'" 
                   class="img-fluid rounded shadow-sm mb-3" 
                   style="width: 120px; height: 120px; object-fit: cover;" 
                   alt="Ảnh sản phẩm">
              <h6 class="fw-bold mb-2">{{ selectedProductForQuantity.displayName }}</h6>
              <div class="mb-2">
                <span class="badge bg-info me-1">{{ selectedProductForQuantity.brand?.name || 'N/A' }}</span>
                <span class="badge bg-secondary">{{ selectedProductForQuantity.category?.name || 'N/A' }}</span>
              </div>
              <div class="fw-bold text-danger fs-5">
                {{ currency(getProductDiscountedPrice(selectedProductForQuantity)) }}
                <span v-if="getProductDiscountPercentage(selectedProductForQuantity) > 0" 
                      class="text-muted text-decoration-line-through small ms-2">
                  {{ currency(selectedProductForQuantity.price) }}
                </span>
              </div>
            </div>

            <!-- Chọn số lượng -->
            <div class="mb-4">
              <label class="form-label fw-bold">Số lượng:</label>
              <div class="d-flex justify-content-center align-items-center gap-3">
                <button class="btn btn-outline-secondary btn-sm" 
                        @click="decreaseQuantity" 
                        :disabled="quantityToAdd <= 1">
                  <i class="fas fa-minus"></i>
                </button>
                <input type="number" 
                       v-model="quantityToAdd" 
                       class="form-control text-center" 
                       style="width: 80px;"
                       min="1" 
                       :max="selectedProductForQuantity.quantity - calculateTotalQuantityInAllOrders(selectedProductForQuantity)"
                       @input="validateQuantity">
                <button class="btn btn-outline-secondary btn-sm" 
                        @click="increaseQuantity" 
                        :disabled="quantityToAdd >= (selectedProductForQuantity.quantity - calculateTotalQuantityInAllOrders(selectedProductForQuantity))">
                  <i class="fas fa-plus"></i>
                </button>
              </div>
              <div class="small text-muted mt-2">
                Tồn kho: {{ selectedProductForQuantity.quantity }} sản phẩm
                <br>
                <span v-if="calculateTotalQuantityInAllOrders(selectedProductForQuantity) > 0">
                  Đã có {{ calculateTotalQuantityInAllOrders(selectedProductForQuantity) }} trong tất cả đơn chờ
                  <br>
                </span>
                Có thể thêm: {{ Math.max(0, selectedProductForQuantity.quantity - calculateTotalQuantityInAllOrders(selectedProductForQuantity)) }} sản phẩm
              </div>
            </div>

          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="closeQuantityModal">Hủy</button>
          <button type="button" class="btn btn-primary" @click="addToCartWithQuantity" :disabled="isAddingToCart">
            <i v-if="isAddingToCart" class="fas fa-spinner fa-spin me-1"></i>
            <i v-else class="fas fa-shopping-cart me-1"></i>
            Thêm vào giỏ hàng
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- Giao diện ban đầu khi chưa có đơn chờ -->
  <div v-if="!hasAnyOrders" class="py-5">
    <!-- Hiển thị đơn chờ nếu có -->
    <div v-if="pendingOrders.length" class="mb-4">
      <ul class="nav nav-tabs" role="tablist">
        <li v-for="po in pendingOrders" :key="po.code" class="nav-item" role="presentation">
          <div class="nav-link d-flex align-items-center" @click="resumePendingOrder(po)" style="cursor: pointer;">
            <i class="fas fa-shopping-cart me-1"></i>{{ po.code }} - {{ currency(po.total || 0) }}
            <button class="btn btn-sm btn-link text-danger ms-2 p-0" @click.stop="removePendingOrder(po.code)" title="Xóa">
              <i class="fas fa-times"></i>
            </button>
          </div>
        </li>
      </ul>
    </div>

    <!-- Nội dung chính - No Data Found -->
    <div class="text-center">
      <div class="mb-4">
        <!-- Icon thư mục buồn với bong bóng suy nghĩ -->
        <div class="position-relative d-inline-block mb-3">
          <!-- Thư mục lớn phía sau -->
          <i class="fas fa-folder fa-4x text-primary position-relative" style="z-index: 1;"></i>
          <!-- Thư mục nhỏ phía trước với khuôn mặt buồn -->
          <i class="fas fa-folder fa-3x text-secondary position-absolute" style="top: 10px; left: 15px; z-index: 2;">
            <!-- Khuôn mặt buồn -->
            <span class="position-absolute" style="top: 8px; left: 8px; font-size: 12px;">😔</span>
          </i>
          <!-- Bong bóng suy nghĩ với ký hiệu Euro -->
          <div class="position-absolute" style="top: -10px; right: -10px; z-index: 3;">
            <div class="bg-light border rounded-circle d-flex align-items-center justify-content-center" style="width: 40px; height: 40px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
              <span class="text-primary fw-bold">€</span>
            </div>
          </div>
          <!-- Các chấm trang trí -->
          <div class="position-absolute" style="top: 20px; right: -20px; width: 6px; height: 6px; background: #007bff; border-radius: 50%;"></div>
          <div class="position-absolute" style="bottom: 10px; left: -15px; width: 4px; height: 4px; background: #007bff; border-radius: 50%;"></div>
          <div class="position-absolute" style="top: 40px; left: -25px; width: 5px; height: 5px; background: #007bff; border-radius: 50%;"></div>
        </div>
        <h4 class="text-muted mb-2">No Data Found</h4>
        <p class="text-muted">Chưa có đơn hàng nào được tạo</p>
      </div>
    </div>
  </div>

  <!-- Layout đầy đủ khi có đơn chờ -->
  <div v-else class="row g-3">
    <!-- Nửa trên: Giỏ hàng -->
    <div class="col-12">
              <!-- Tab đơn chờ -->
        <div v-if="pendingOrders.length" class="mb-3">
          <ul class="nav nav-tabs" role="tablist">
            <!-- Tab các đơn chờ -->
            <li v-for="po in pendingOrders" :key="po.code" class="nav-item" role="presentation">
              <div class="nav-link d-flex align-items-center" :class="activePendingCode === po.code ? 'active' : ''" @click="resumePendingOrder(po)" style="cursor: pointer;">
                <i class="fas fa-shopping-cart me-1"></i>{{ po.code }} - {{ currency(po.total || 0) }}
                <button class="btn btn-sm btn-link text-danger ms-2 p-0" @click.stop="removePendingOrder(po.code)" title="Xóa">
                  <i class="fas fa-times"></i>
                </button>
              </div>
            </li>
          </ul>
        </div>
      <div class="card mb-3">
        <div class="card-header fw-bold d-flex justify-content-between align-items-center">
          <span>🛒 Giỏ hàng</span>
          <div class="d-flex align-items-center gap-2">
          <button class="btn btn-primary btn-sm" @click="showProductModal = true">
            <i class="fas fa-plus me-1"></i>Thêm sản phẩm
          </button>
          </div>
        </div>
        <div class="card-body p-2" style="max-height: 300px; overflow-y: auto;">
          <table class="table table-sm align-middle mb-0">
            <thead>
              <tr>
                <th>Tên sản phẩm</th>
                <th>Size</th>
                <th>SL</th>
                <th>Màu</th>
                <th>Khối lượng</th>
                <th>Giá</th>
                <th>Thành tiền</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, idx) in cart" :key="item.product.id + '-' + item.size">
                <td>
                  <div class="d-flex align-items-center">
                    <img :src="item.product.thumbnail || '/default.jpg'" class="me-2"
                      style="width: 40px; height: 40px; object-fit: cover; border-radius: 4px;">
                    <div>
                      <div class="fw-bold small">{{ item.product.name }}</div>
                      <small class="text-muted">{{ item.product.sku }}</small>
                    </div>
                  </div>
                </td>
                <td><span class="badge bg-secondary">{{ item.size }}</span></td>
                <td>
                  <input type="number" min="1" v-model.number="item.quantity"
                    @change="updateQuantity(idx, item.quantity)" style="width: 60px"
                    class="form-control form-control-sm" />
                </td>
                <td>
                  <span v-if="item.variant?.color" class="badge"
                        :style="{ backgroundColor: item.variant.color.value || '#6c757d', color: getContrastColor(item.variant.color.value) }">
                    {{ item.variant.color.name || item.variant.color.value }}
                  </span>
                  <span v-else class="badge bg-light text-dark">N/A</span>
                </td>
                <td>
                  <small class="text-muted">
                    {{ getProductWeight(item.product) }}g
                  </small>
                </td>
                <td>
                  <div v-if="item.variant?.discountPercentage > 0">
                    <span class="text-muted text-decoration-line-through small">{{ currency(item.variant.originalPrice || item.product.price || 0) }}</span>
                    <br>
                    <span class="text-danger fw-bold">{{ currency(item.variant?.price || item.product.price || 0) }}</span>
                    <br>
                    <span class="badge bg-danger small">-{{ item.variant.discountPercentage }}%</span>
                  </div>
                  <div v-else>
                    {{ currency(item.variant?.price || item.product.price || 0) }}
                  </div>
                </td>
                <td class="fw-bold text-danger">{{ currency((item.variant?.price || item.product.price || 0) *
                  item.quantity) }}</td>
                <td>
                  <button class="btn btn-sm btn-outline-danger" @click="removeFromCart(idx)">
                    <i class="fas fa-trash"></i>
                  </button>
                </td>
              </tr>
              <tr v-if="cart.length === 0">
                <td colspan="8" class="text-center text-muted py-4">
                  <i class="fas fa-shopping-cart fa-2x mb-2"></i>
                  <div>Giỏ hàng trống</div>
                  <small>Nhấn "Thêm sản phẩm" để bắt đầu</small>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="card-footer">
          <div class="row">
            <div class="col-md-12">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <span><b>Tổng tiền hàng:</b></span>
                <span class="text-danger fw-bold">{{ currency(totalAmount) }}</span>
              </div>
              <!-- Hiển thị tổng tiền tiết kiệm từ khuyến mại -->
              <div v-if="totalDiscountAmount > 0" class="d-flex justify-content-between align-items-center mb-1">
                <small class="text-success"><i class="fas fa-percentage me-1"></i>Tiết kiệm từ khuyến mại:</small>
                <small class="text-success">-{{ currency(totalDiscountAmount) }}</small>
              </div>
                             <div v-if="discount > 0" class="d-flex justify-content-between align-items-center mb-1">
                 <small class="text-success"><i class="fas fa-percentage me-1"></i>Giảm thêm:</small>
                 <small class="text-success">-{{ currency(discount) }}</small>
               </div>
               <div v-if="shippingType === 'delivery'" class="d-flex justify-content-between align-items-center mb-1">
                 <small class="text-info">
                   <i class="fas fa-shipping-fast me-1"></i>Phí ship:
                   <span v-if="isCalculatingShipping" class="ms-1">
                     <small><i class="fas fa-spinner fa-spin"></i> Đang tính...</small>
                   </span>
                 </small>
                 <small class="text-info">+{{ currency(shippingFee) }}</small>
               </div>
               <div v-if="tax > 0" class="d-flex justify-content-between align-items-center mb-1">
                 <small class="text-warning"><i class="fas fa-receipt me-1"></i>Thuế:</small>
                 <small class="text-warning">+{{ currency(tax) }}</small>
               </div>
               <!-- Hiển thị voucher discount nếu có -->
               <div v-if="appliedVoucher" class="d-flex justify-content-between align-items-center mb-1">
                 <small class="text-success"><i class="fas fa-tag me-1"></i>Giảm giá voucher:</small>
                 <small class="text-success">-{{ currency(appliedVoucher.discountAmount) }}</small>
               </div>
              <!-- Hiển thị tổng tiền cuối cùng sau khi trừ voucher -->
              <div class="d-flex justify-content-between align-items-center mt-2 pt-2 border-top">
                <span><b>Tổng thanh toán:</b></span>
                <span class="text-danger fw-bold fs-5">{{ currency(totalToPay) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Nửa dưới: Thông tin khách hàng và thanh toán -->
    <div class="col-12">
      <div class="row g-3">
        <!-- Thanh toán -->
        <div class="col-lg-7">
          <div class="card h-100">
            <div class="card-header fw-bold">💵 Thanh toán</div>
                         <div class="card-body">
               <div class="mb-3">
                 <label class="form-label">Hình thức thanh toán</label>
                 <select v-model="paymentMethod" class="form-select">
                   <option value="cash">💵 Tiền mặt</option>
                   <option value="bank">🏦 Chuyển khoản</option>
                 </select>
               </div>

               <!-- Hiển thị tiền khách đưa chỉ khi chọn tiền mặt -->
               <div v-if="paymentMethod === 'cash'" class="mb-3">
                 <label class="form-label">Tiền khách đưa</label>
                 <input v-model="customerCashDisplay" type="text" class="form-control"
                   placeholder="Nhập số tiền khách đưa" 
                   @input="handleCustomerCashInput"
                   @keydown="handleCustomerCashKeydown"
                   @paste="handleCustomerCashPaste"
                   @focus="$event.target.select()">
                 <div v-if="customerCash > 0 && changeAmount > 0" class="small mt-1 text-success">
                   <i class="fas fa-arrow-down me-1"></i>Tiền thối lại: <b>{{ currency(changeAmount) }}</b>
                 </div>
                 <div v-if="customerCash > 0 && changeAmount < 0" class="small mt-1 text-danger">
                   <i class="fas fa-arrow-up me-1"></i>Khách cần đưa thêm: <b>{{ currency(Math.abs(changeAmount)) }}</b>
                 </div>
               </div>

                               <div class="mb-3">
                  <div class="d-flex justify-content-between align-items-center mb-2">
                    <label class="form-label mb-0"><i class="fas fa-tag me-1"></i>Voucher/Giảm giá</label>
                    <button 
                      class="btn btn-outline-primary btn-sm" 
                      @click="toggleVoucherList"
                      :disabled="voucherLoading"
                    >
                      <i v-if="voucherLoading" class="fas fa-spinner fa-spin"></i>
                      <i v-else class="fas fa-list"></i>
                      {{ showVoucherList ? 'Ẩn danh sách' : 'Xem voucher' }}
                    </button>
                  </div>
                  
                  <!-- Hiển thị danh sách voucher có sẵn -->
                  <div v-if="showVoucherList" class="mb-3">
                    <div v-if="availableVouchers.length > 0">
                      <div class="d-flex justify-content-between align-items-center mb-2">
                        <small class="text-muted">Mã giảm giá có sẵn:</small>
                        <div class="btn-group btn-group-sm">
                          <button class="btn btn-outline-secondary" @click="fetchAvailableVouchers" :disabled="voucherLoading" title="Làm mới danh sách">
                            <i v-if="voucherLoading" class="fas fa-spinner fa-spin"></i>
                            <i v-else class="fas fa-sync-alt"></i>
                          </button>
                        </div>
                      </div>
                      <div class="row g-2">
                        <div v-for="voucher in availableVouchers" :key="voucher.id" class="col-12">
                          <div class="card border-0 bg-light p-2 cursor-pointer" 
                               @click="selectVoucher(voucher)"
                               :class="{ 'border-primary': selectedVoucherId === voucher.id }"
                               style="cursor: pointer;">
                            <div class="d-flex justify-content-between align-items-center">
                              <div class="flex-grow-1">
                                <div class="fw-bold text-primary">{{ voucher.name }}</div>
                                <small class="text-muted">
                                  Mã: <code>{{ voucher.code }}</code> | 
                                  {{ voucher.type ? `Giảm ${voucher.value}%` : `Giảm ${currency(voucher.value)}` }}
                                  <span v-if="voucher.minimumOrderValue"> | Tối thiểu: {{ currency(voucher.minimumOrderValue) }}</span>
                                </small>
                              </div>
                              <button class="btn btn-sm btn-outline-primary" @click.stop="selectVoucher(voucher)">
                                <i class="fas fa-plus"></i>
                              </button>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                    
                    <!-- Thông báo khi không có voucher -->
                    <div v-else-if="!voucherLoading" class="mb-3">
                      <div class="alert alert-info p-2 mb-0">
                        <i class="fas fa-info-circle me-1"></i>
                        <small>Không có mã giảm giá nào khả dụng cho đơn hàng này.</small>
                        <br>
                        <small class="text-muted">
                          <!-- User ID: {{ customerType === 'regular' && selectedCustomer ? selectedCustomer.id : 1 }} |  -->
                          Tổng đơn: {{ currency(totalAmount) }}
                        </small>
                      </div>
                    </div>
                    
                    <!-- Loading state -->
                    <div v-else class="text-center py-3">
                      <div class="spinner-border spinner-border-sm text-primary" role="status">
                        <span class="visually-hidden">Loading...</span>
                      </div>
                      <p class="mt-2 text-muted">Đang tải danh sách voucher...</p>
                    </div>
                  </div>
                  
                  <!-- Ô input để nhập mã thủ công -->
                  <div class="input-group mb-2">
                    <input v-model="voucherCode" class="form-control" placeholder="Nhập mã voucher..." @keyup.enter="applyVoucher">
                    <button class="btn btn-outline-primary" @click="applyVoucher" :disabled="voucherLoading || !voucherCode.trim()">
                      <i v-if="voucherLoading" class="fas fa-spinner fa-spin"></i>
                      <i v-else class="fas fa-tag"></i>
                          Áp dụng
                    </button>
                  </div>
                                   <div v-if="voucherError" class="alert alert-danger p-2 mb-2 fadeInUp">
                    <i class="fas fa-exclamation-circle me-1"></i>{{ voucherError }}
                  </div>
                  <div v-if="voucherSuccess" class="alert alert-success p-2 mb-2 fadeInUp">
                    <i class="fas fa-check-circle me-1"></i>{{ voucherSuccess }}
                  </div>
                                   <div v-if="appliedVoucher" class="alert alert-success p-2 mb-2 fadeInUp">
                    <div class="d-flex justify-content-between align-items-center">
                      <div>
                        <strong><i class="fas fa-tag me-1"></i>{{ appliedVoucher.name }}</strong><br>
                        <small class="text-muted">
                          {{ appliedVoucher.type ? `Giảm ${appliedVoucher.value}%` : `Giảm ${currency(appliedVoucher.value)}` }}
                          - Tiết kiệm: <strong class="text-success">{{ currency(appliedVoucher.discountAmount) }}</strong>
                        </small>
                      </div>
                      <button class="btn btn-sm btn-outline-danger" @click="removeVoucher" title="Xóa voucher">
                        <i class="fas fa-times"></i>
                      </button>
                    </div>
                  </div>
               </div>

                             <!-- Hiển thị QR code khi chọn chuyển khoản -->
               <div v-if="paymentMethod === 'bank'" class="mb-3">
                 <div class="card border-primary">
                   <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
                     <div>
                       <i class="fas fa-qrcode me-2"></i>QR Code Thanh Toán
                     </div>
                     <button class="btn btn-light btn-sm fw-bold" @click="generateQRCode" :disabled="qrCodeLoading">
                       <i v-if="qrCodeLoading" class="fas fa-spinner fa-spin me-1"></i>
                       <i v-else class="fas fa-sync-alt me-1"></i>
                       {{ qrCodeLoading ? 'Đang tạo...' : 'Tạo QR' }}
                     </button>
                   </div>
                   <div class="card-body text-center">
                     <!-- QR Code Container -->
                     <div class="qr-code-container">
                       <div v-if="qrCodeLoading" class="d-flex justify-content-center align-items-center" style="height: 200px;">
                         <div class="text-center">
                           <div class="spinner-border text-primary mb-2" role="status">
                             <span class="visually-hidden">Đang tạo QR code...</span>
                           </div>
                           <div class="small text-muted">Đang tạo QR code...</div>
                         </div>
                       </div>
                       <div v-else-if="qrCodeError" class="d-flex justify-content-center align-items-center" style="height: 200px;">
                         <div class="text-center text-danger">
                           <i class="fas fa-exclamation-triangle fa-2x mb-2"></i>
                           <div class="small">{{ qrCodeError }}</div>
                           <button class="btn btn-outline-danger btn-sm mt-2" @click="generateQRCode">
                             <i class="fas fa-retry me-1"></i>Thử lại
                           </button>
                         </div>
                       </div>
                       <div v-else-if="qrCodeData || qrCodeUrl" class="text-center">
                         <img :src="qrCodeData || qrCodeUrl" 
                              alt="QR Code Thanh Toán VietQR" 
                              class="img-fluid qr-code-image" 
                              style="max-width: 300px; height: auto; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);">
                         <div class="mt-3">
                           <button class="btn btn-outline-primary btn-sm" @click="downloadQRCode">
                             <i class="fas fa-download me-1"></i>Tải xuống
                           </button>
                         </div>
                       </div>
                       <div v-else class="d-flex justify-content-center align-items-center" style="height: 200px;">
                         <div class="text-center text-muted">
                           <i class="fas fa-qrcode fa-3x mb-2"></i>
                           <div>Nhấn "Tạo QR" để tạo mã QR thanh toán</div>
                         </div>
                       </div>
                     </div>
                     
                     <!-- Thông tin chuyển khoản -->
                     <div class="mt-3">
                       <p class="mb-1"><strong>Ngân hàng:</strong> MB Bank</p>
                       <p class="mb-1"><strong>Số tài khoản:</strong> 0365684005</p>
                       <p class="mb-1"><strong>Chủ tài khoản:</strong> NGUYEN MINH VU</p>
                       <p class="mb-1"><strong>Số tiền:</strong> <span class="text-danger fw-bold">{{ currency(totalToPay) }}</span></p>
                       <p class="mb-0"><strong>Nội dung:</strong> Thanh toan don hang</p>
                     </div>
                     
                     <!-- Action buttons -->
                   </div>
                 </div>
               </div>
       
                             <!-- Hiển thị thông tin tiền khách đưa chỉ khi chọn tiền mặt -->
               <div v-if="paymentMethod === 'cash' && customerCash > 0" class="mt-3">
                 <div class="d-flex justify-content-between align-items-center">
                   <small class="text-muted">Tiền khách đưa:</small>
                   <small class="text-primary fw-bold">{{ currency(customerCash) }}</small>
                 </div>
                 <!-- Chỉ hiển thị tiền thối khi khách đưa thừa -->
                 <div v-if="changeAmount > 0" class="d-flex justify-content-between align-items-center">
                   <small class="text-muted">Tiền thối:</small>
                   <small class="text-success fw-bold">
                     {{ currency(changeAmount) }}
                   </small>
                 </div>
                 <!-- Hiển thị thông báo khi khách đưa thiếu -->
                 <div v-if="changeAmount < 0" class="text-danger small text-center mt-1">
                   <i class="fas fa-exclamation-circle me-1"></i>Khách cần đưa thêm {{ currency(Math.abs(changeAmount))
                   }}
                 </div>
               </div>
            </div>
            <div class="card-footer">
              <div class="row g-3">
                <div class="col-md-8">
                  <div class="d-flex justify-content-between align-items-center">
                    <strong class="fs-6">Tổng thanh toán:</strong>
                    <strong class="text-danger fs-4">{{ currency(totalToPay) }}</strong>
                  </div>
                </div>
                <div class="col-md-4">
                  <div class="d-flex gap-2">
                    <button class="btn btn-warning btn-sm flex-fill" @click="clearCart" :disabled="!hasAnyOrderData">
                      <i class="fas fa-trash me-1"></i>Hủy Đơn 
                    </button>
                    <button class="btn btn-success btn-sm flex-fill" @click="checkout" :disabled="!canCheckout">
                      <i class="fas fa-credit-card me-1"></i>Thanh toán
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <!-- Thông tin khách hàng -->
        <div class="col-lg-5">
          <div class="card h-100">
            <div class="card-header fw-bold">👤 Thông tin khách hàng</div>
            <div class="card-body">
              <div class="mb-3">
                <label class="form-label">Loại khách hàng</label>
                <div class="d-flex gap-2 flex-wrap">
                  <div class="form-check">
                    <input class="form-check-input" type="radio" id="guest" value="guest" v-model="customerType">
                    <label class="form-check-label" for="guest">Khách vãng lai</label>
                  </div>
                  <div class="form-check">
                    <input class="form-check-input" type="radio" id="regular" value="regular" v-model="customerType">
                    <label class="form-check-label" for="regular">Khách quen</label>
                  </div>
                  <div class="form-check">
                    <input class="form-check-input" type="radio" id="new" value="new" v-model="customerType">
                    <label class="form-check-label" for="new">Khách mới</label>
                  </div>
                </div>
              </div>

              <div v-if="customerType === 'regular'" class="mb-3">
                <div class="position-relative">
                  <input v-model="customerSearch" class="form-control" placeholder="Tìm theo tên hoặc SĐT...">
                  <div v-if="isLoadingCustomers" class="position-absolute top-50 end-0 translate-middle-y me-2">
                    <div class="spinner-border spinner-border-sm text-primary" role="status">
                      <span class="visually-hidden">Đang tải...</span>
                    </div>
                  </div>
                </div>
                <ul v-if="filteredCustomers.length > 0" class="list-group mt-2"
                  style="max-height: 200px; overflow-y: auto;">
                  <li v-for="cus in filteredCustomers" :key="cus.id" class="list-group-item list-group-item-action"
                    @click="selectCustomer(cus)">
                    <div class="d-flex justify-content-between align-items-center">
                      <div>
                        <strong>{{ cus.fullname }}</strong>
                        <br>
                        <small class="text-muted">{{ cus.phoneNumber }}</small>
                      </div>
                      <small class="text-success">{{ cus.email }}</small>
                    </div>
                  </li>
                </ul>
                <div v-else-if="customerSearch && !isLoadingCustomers" class="text-muted small mt-1">
                  Không tìm thấy khách hàng
                </div>
              </div>

              <div v-if="customerType === 'new'" class="mb-3">
                <input v-model="customer.name" class="form-control mb-2" placeholder="Tên khách hàng">
                <input v-model="customer.phone" class="form-control mb-2" placeholder="Số điện thoại">
                <button class="btn btn-success btn-sm w-100" @click="createCustomer" :disabled="isCreatingCustomer">
                  <i v-if="isCreatingCustomer" class="fas fa-spinner fa-spin me-1"></i>
                  <i v-else class="fas fa-user-plus me-1"></i>
                  {{ isCreatingCustomer ? 'Đang tạo...' : 'Tạo khách hàng mới' }}
                </button>
              </div>

              <div v-if="customerType === 'regular' && selectedCustomer" class="mb-3">
                <div class="alert alert-info p-2 mb-0">
                  <strong>Khách hàng đã chọn:</strong><br>
                  <strong>{{ selectedCustomer.fullname }}</strong> - {{ selectedCustomer.phoneNumber }}
                  <br>
                  <small class="text-muted">{{ selectedCustomer.email }}</small>
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label">Tùy chọn giao hàng</label>
                <div class="d-flex gap-2 flex-wrap">
                  <div class="form-check">
                    <input class="form-check-input" type="radio" id="pickup" value="pickup" v-model="shippingType">
                    <label class="form-check-label" for="pickup">Mua mang về</label>
                  </div>
                  <div class="form-check">
                    <input class="form-check-input" type="radio" id="delivery" value="delivery" v-model="shippingType">
                    <label class="form-check-label" for="delivery">Giao tận nơi</label>
                  </div>
                </div>
              </div>

                                                           <div v-if="shippingType === 'delivery'" class="mb-3">
                  <div class="mb-2">
                    <label class="form-label">
                      <i class="fas fa-map-marker-alt me-1"></i>
                      Địa chỉ giao hàng
                    </label>
                    
                    <!-- 3 ô input cho địa chỉ -->
                    <div class="row g-2 mb-2">
                      <div class="col-md-4">
                        <label class="form-label small text-muted mb-1">
                          Tỉnh/Thành phố
                        </label>
                        <select v-model="selectedProvince" class="form-select" @change="onProvinceChange">
                          <option value="">Chọn tỉnh/thành phố</option>
                          <option v-for="province in provinces" :key="province.code" :value="province">
                            {{ province.name }}
                          </option>
                        </select>
                      </div>
                      <div class="col-md-4">
                        <label class="form-label small text-muted mb-1">
                            Quận/Huyện
                        </label>
                        <select v-model="selectedDistrict" class="form-select" @change="onDistrictChange" :disabled="!selectedProvince">
                          <option value="">Chọn quận/huyện</option>
                          <option v-for="district in districts" :key="district.code" :value="district">
                            {{ district.name }}
                          </option>
                        </select>
                      </div>
                      <div class="col-md-4">
                        <label class="form-label small text-muted mb-1">
                          Phường/Xã
                        </label>
                        <select v-model="selectedWard" class="form-select" @change="onWardChange" :disabled="!selectedDistrict">
                          <option value="">Chọn phường/xã</option>
                          <option v-for="ward in wards" :key="ward.code" :value="ward">
                            {{ ward.name }}
                          </option>
                        </select>
                      </div>
                    </div>
                    
                    <!-- Input địa chỉ chi tiết -->
                    <div class="mb-2">
                      <input 
                        v-model="detailedAddress" 
                        type="text" 
                        class="form-control" 
                        placeholder="Địa chỉ chi tiết (số nhà, tên đường, tên khu phố...)"
                        @input="updateShippingAddress"
                      >
                    </div>
                    
               
                    
                    <!-- Danh sách địa chỉ gợi ý -->
                    <div v-if="suggestedAddresses.length > 0" class="mt-2">
                      <div class="card border-light shadow-sm">
                        <div class="card-header bg-light py-2">
                          <small class="text-muted">
                            <i class="fas fa-lightbulb me-1"></i>Địa chỉ gợi ý
                          </small>
                        </div>
                        <div class="card-body p-0">
                          <div class="list-group list-group-flush">
                            <button 
                              v-for="(address, index) in suggestedAddresses" 
                              :key="index"
                              class="list-group-item list-group-item-action py-2 px-3 border-0"
                              @click="selectSuggestedAddress(address)"
                              style="cursor: pointer;"
                            >
                              <div class="d-flex align-items-center">
                                <i class="fas fa-map-marker-alt text-primary me-2"></i>
                                <div class="flex-grow-1">
                                  <div class="small fw-medium">{{ address.fullAddress }}</div>
                                  <div class="small text-muted">
                                    {{ address.wardName }}, {{ address.districtName }}, {{ address.provinceName }}
                                  </div>
                                </div>
                                <i class="fas fa-chevron-right text-muted"></i>
                              </div>
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                <input v-model="shippingNote" class="form-control mb-2" placeholder="Ghi chú giao hàng">
                <div class="input-group">
                  <span class="input-group-text">
                    <i class="fas fa-truck"></i>
                  </span>
                  <input type="text" class="form-control" :value="shippingFee > 0 ? currency(shippingFee) : 'Đang tính...'" placeholder="Phí ship" readonly>
                  <span v-if="isCalculatingShipping" class="input-group-text">
                    <i class="fas fa-spinner fa-spin"></i>
                  </span>
                </div>
                <div v-if="shippingError" class="alert alert-warning mt-2 p-2">
                  <small><i class="fas fa-exclamation-triangle me-1"></i>{{ shippingError }}</small>
                </div>
                                 <div v-if="shippingFee > 0" class="mt-2">
                   <small class="text-muted">
                     <i class="fas fa-info-circle me-1"></i>
                     Phí ship được tính theo cân nặng thực tế: {{ (getCartWeight() / 1000).toFixed(2) }}kg
                   </small>
                 </div>
                 <div v-if="shippingType === 'delivery'" class="mt-2">
                   <div v-if="!shippingAddress && !detailedAddress && (!selectedProvince || !selectedDistrict || !selectedWard)" class="small text-warning">
                     <i class="fas fa-exclamation-triangle me-1"></i>
                     Vui lòng nhập địa chỉ chi tiết hoặc chọn đầy đủ tỉnh/thành phố, quận/huyện, phường/xã
                   </div>
                   <div v-else-if="!selectedProvince || !selectedDistrict || !selectedWard" class="small text-warning">
                     <i class="fas fa-exclamation-triangle me-1"></i>
                     Vui lòng chọn đầy đủ tỉnh/thành phố, quận/huyện, phường/xã để tính phí vận chuyển
                   </div>
                   <div v-else-if="shippingAddress" class="small text-success">
                     <i class="fas fa-check-circle me-1"></i>
                     Địa chỉ giao hàng: {{ shippingAddress }}
                   </div>
                 </div>
              </div>
            </div>
          </div>
        </div>


      </div>
    </div>
  </div>

     <!-- Modal danh sách sản phẩm -->
   <div v-if="showProductModal" class="modal fade show d-block" tabindex="-1" style="background:rgba(0,0,0,0.5);">
     <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
       <div class="modal-content">
                    <div class="modal-header bg-primary text-white">
           <h5 class="modal-title fw-bold">📦 Danh sách sản phẩm</h5>
            <button type="button" class="btn-close btn-close-white" @click="showProductModal = false"></button>
          </div>
         <div class="modal-body p-4">
          <!-- Tìm kiếm & lọc -->
          <div class="card mb-4 border-0 shadow-sm">
            <div class="card-body">
              <div class="row g-3 align-items-end">
                <div class="col-md-3">
                  <label class="form-label fw-bold mb-2">🔍 Tìm kiếm</label>
                  <input v-model="search" @input="handleSearch" class="form-control"
                    placeholder="Tìm tên, mã sản phẩm..." />
            </div>
            <div class="col-md-2">
                  <label class="form-label fw-bold mb-2">👟 Loại</label>
                  <select v-model="filterType" class="form-select">
                    <option value="">Tất cả loại</option>
                    <option value="Male">Nam</option>
                    <option value="Female">Nữ</option>
                    <option value="Kids">Trẻ em</option>
                    <option value="Unisex">Unisex</option>
              </select>
            </div>
                <div class="col-md-3">
                  <label class="form-label fw-bold mb-2">🏷️Thương hiệu</label>
                  <select v-model="filterBrand" class="form-select">
                    <option value="">Tất cả thương hiệu</option>
                    <option v-for="brand in brands" :key="brand.id" :value="brand.id">{{ brand.name }}</option>
              </select>
            </div>
            <div class="col-md-2">
                  <label class="form-label fw-bold mb-2">📂 Danh mục</label>
                  <select v-model="filterCategory" class="form-select">
                    <option value="">Tất cả danh mục</option>
                    <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}
                    </option>
              </select>
            </div>
            <div class="col-md-2">
                  <label class="form-label fw-bold mb-2">📏 Size</label>
              <select v-model="filterSize" class="form-select">
                    <option value="">Tất cả size</option>
                <option v-for="size in sizes" :key="size.id" :value="size.id">{{ size.name || size.value }}</option>
              </select>
            </div>
          </div>
          <!-- Nút Reset tìm kiếm -->
          <div class="row mt-3">
            <div class="col-12 text-center">
              <button class="btn btn-outline-secondary btn-sm" @click="resetSearchFilters" title="Reset tất cả bộ lọc">
                <i class="fas fa-undo me-1"></i>Reset tìm kiếm
              </button>
            </div>
          </div>
                      </div>
          </div>

                     <!-- Lưới sản phẩm -->
           <div class="row g-3">
             <div class="col-lg-3 col-md-4 col-sm-6" v-for="product in filteredProducts" :key="product.id">
              <div class="card h-100 shadow-sm product-card border-0">
                <div class="position-relative">
                  <img :src="product.thumbnail || '/default.jpg'" class="card-img-top"
                     style="height: 180px; object-fit: cover;" alt="Ảnh sản phẩm">
                  <div class="position-absolute top-0 end-0 m-2">
                        <span v-if="!product.inStock" class="badge bg-danger">Hết hàng</span>
                        <span v-else class="badge bg-success">Còn hàng</span>
                      </div>
                  <!-- Badge khuyến mại -->
                  <div v-if="getProductDiscountPercentage(product) > 0" class="position-absolute top-0 start-0 m-2">
                    <span class="badge bg-danger fs-6">
                      <i class="fas fa-percentage me-1"></i>
                      -{{ getProductDiscountPercentage(product) }}%
                    </span>
                    </div>
                    </div>
                <div class="card-body p-3">
                  <div class="fw-bold mb-2" :title="product.displayName" style="word-wrap: break-word; line-height: 1.3;">{{ product.displayName }}</div>
                  <div class="small text-muted mb-2">Mã: {{ product.sku }}</div>
                  <div class="small mb-2">
                    <span class="badge bg-info me-1">{{ product.brand?.name || 'N/A' }}</span>
                    <span class="badge bg-secondary">{{ product.category?.name || 'N/A' }}</span>
                    </div>
                  <div class="small mb-2">
                    <strong>Size:</strong>
                    <span class="badge bg-secondary">{{ product.size?.name || product.size?.value || 'N/A' }}</span>
                    <span v-if="product.color" class="badge ms-1"
                          :style="{ backgroundColor: product.color.value || '#6c757d', color: getContrastColor(product.color.value) }">
                      {{ product.color?.name || product.color?.value }}
                    </span>
                    </div>
                  <div class="small mb-2">
                    <strong>Giá:</strong>
                    <div v-if="getProductDiscountPercentage(product) > 0">
                      <span class="text-muted text-decoration-line-through me-2">{{ currency(product.price) }}</span>
                      <span class="text-danger fw-bold fs-6">{{ currency(getProductDiscountedPrice(product)) }}</span>
                      </div>
                    <div v-else>
                      <span class="text-danger fw-bold fs-6">{{ currency(product.price) }}</span>
                    </div>
                  </div>
                  <div class="small">
                    <strong>Tồn kho:</strong>
                    <span :class="product.quantity > 0 ? 'text-success' : 'text-danger'">{{ product.quantity }} sản
                      phẩm</span>
                  </div>
                  <!-- Hiển thị thông tin khuyến mại -->
                  <div v-if="getProductDiscountPercentage(product) > 0" class="small mt-2">
                    <div class="alert alert-success p-2 mb-0">
                      <i class="fas fa-gift me-1"></i>
                      <strong>Khuyến mại:</strong> Giảm {{ getProductDiscountPercentage(product) }}% 
                      <br>
                      <small class="text-muted">Tiết kiệm: {{ currency((product.price * getProductDiscountPercentage(product)) / 100) }}</small>
                    </div>
                  </div>
                </div>
                <div class="card-footer bg-white border-0 text-center p-3">
                  <button 
                    class="btn btn-sm w-100" 
                            :class="{
                      'btn-primary': product.inStock && getCartQuantity(product) < product.quantity,
                      'btn-secondary': !product.inStock,
                      'btn-warning': product.inStock && getCartQuantity(product) >= product.quantity
                    }"
                    :disabled="!product.inStock || getCartQuantity(product) >= product.quantity || isAddingToCart"
                    @click="selectProduct(product)"
                  >
                      <i v-if="isAddingToCart" class="fas fa-spinner fa-spin me-1"></i>
                      <i v-else class="fas fa-shopping-cart me-1"></i>
                    <span v-if="!product.inStock">Hết hàng</span>
                    <span v-else-if="getCartQuantity(product) >= product.quantity">Đã đạt giới hạn</span>
                    <span v-else-if="isAddingToCart">Đang thêm...</span>
                    <span v-else>Chọn mua</span>
                    </button>
              </div>
            </div>
              </div>
            <div v-if="filteredProducts.length === 0 && !isLoading" class="col-12 text-center text-muted py-5">
              <i class="fas fa-search fa-3x mb-3 text-muted"></i>
              <div class="fw-bold mb-2">Không có sản phẩm phù hợp</div>
              <small>Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm</small>
            </div>
          </div>

          <!-- Phân trang -->
          <div v-if="totalPages > 1 && !isLoading" class="d-flex justify-content-center mt-4">
            <nav aria-label="Phân trang sản phẩm">
              <ul class="pagination pagination-sm">
                <li class="page-item" :class="{ disabled: currentPage === 1 }">
                  <button class="page-link" @click="prevPage" :disabled="currentPage === 1">
                    <i class="fas fa-chevron-left"></i>
                  </button>
                </li>
                <li v-for="page in visiblePages" :key="page" class="page-item"
                  :class="{ active: page === currentPage, disabled: page === '...' }">
                  <button v-if="page !== '...'" class="page-link" @click="goToPage(page)">
                    {{ page }}
                  </button>
                  <span v-else class="page-link">{{ page }}</span>
                </li>
                <li class="page-item" :class="{ disabled: currentPage === totalPages }">
                  <button class="page-link" @click="nextPage" :disabled="currentPage === totalPages">
                    <i class="fas fa-chevron-right"></i>
                  </button>
                </li>
              </ul>
            </nav>
          </div>
        </div>
             </div>
     </div>
   </div>
   
   <!-- Thông báo Toast -->
   <div class="position-fixed top-0 end-0 p-3 mt-5" style="z-index: 9999; right: 0;">
     <div
         class="toast align-items-center border-0"
         :class="{
           'bg-success': toastType === 'success',
           'bg-danger': toastType === 'error',
           'bg-info': toastType === 'info',
           'bg-warning': toastType === 'warning',
         }"
         role="alert"
         aria-live="assertive"
         aria-atomic="true"
         ref="toastRef"
         v-if="toastMessage"
     >
       <div class="d-flex">
         <div class="toast-body text-white">{{ toastMessage }}</div>
         <button type="button" class="btn-close btn-close-white me-2 m-auto" @click="hideToast"></button>
       </div>
     </div>
   </div>

   <!-- Address Modal -->
   <ListAddressModal
     v-if="showAddressModal"
     :userId="1"
     @close="showAddressModal = false"
     @save="handleSaveAddress"
   />
 </template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue';
import ShowToastComponent from '@/components/ShowToastComponent.vue';
import ListAddressModal from '@/components/ListAddressModal.vue';
import ConfirmModal from '@/components/ConfirmModal.vue';
import * as ProductApi from '@/service/ProductApi.js';
import * as BrandApi from '@/service/BrandApi.js';
import * as SizeApi from '@/service/SizeApi.js';
import * as CategoryApi from '@/service/CategoryApi.js';
import PaymentMethodApi from '@/service/PaymentMethodApi.js';
import * as OrderApi from '@/service/OrderApi.js';
import CouponApi from '@/service/CouponApi.js';
import { getAllUsers, createUser } from '@/service/UserApis.js';
import { ShippingApi } from '@/service/ShippingApi';
import { getSelectedAddress } from '@/service/AddressApi.js';
import { generateVietQR, generateSimpleVietQR, validatePaymentInfo } from '@/service/VietQRApi.js';

// Toast notification
const toastMessage = ref('');
const toastType = ref('success');
const toastRef = ref(null);

// Hàm hiển thị toast notification
const showToast = (message, type = 'success') => {
  toastMessage.value = message;
  toastType.value = type;
  if (window.bootstrap && toastRef.value) {
    const toast = new window.bootstrap.Toast(toastRef.value);
    toast.show();
  }
};

// Hàm hiển thị alert - giống review management
const showAlert = (message, type = 'success') => {
  toastMessage.value = message;
  toastType.value = type;
  nextTick(() => {
    if (window.bootstrap && toastRef.value) {
      const toast = new window.bootstrap.Toast(toastRef.value, {
        autohide: true,
        delay: 3000
      });
      toast.show();
    }
  });
};

// Hàm ẩn toast
const hideToast = () => {
  if (window.bootstrap && toastRef.value) {
    const toast = new window.bootstrap.Toast(toastRef.value);
    toast.hide();
  }
  toastMessage.value = '';
};

// ConfirmModal data
const showConfirmModal = ref(false);
const confirmModalTitle = ref('');
const confirmModalMessage = ref('');
const confirmModalType = ref('warning');
const confirmModalConfirmText = ref('Xác nhận');
const confirmModalCancelText = ref('Hủy');
const confirmModalLoading = ref(false);
const pendingAction = ref(null);
const pendingData = ref(null);

// Modal chọn số lượng
const showQuantityModal = ref(false);
const selectedProductForQuantity = ref(null);
const quantityToAdd = ref(1);

// Khu vực tìm kiếm & lọc
const search = ref('');
const filterType = ref('');
const filterBrand = ref('');
const filterSize = ref('');
 const filterPrice = ref('');
 const filterCategory = ref('');
 const filterColor = ref('');
 const filterMaterial = ref('');
 const filterSole = ref('');
 const priceRange = ref([100000, 3200000]);
 const brands = ref([]);
 const sizes = ref([]);
 const categories = ref([]);
 const colors = ref([]);
 const materials = ref([]);

// Sản phẩm
const products = ref([]);
const isLoading = ref(false);

// Phân trang
const currentPage = ref(1);
const pageSize = ref(24); // Hiển thị 24 sản phẩm mỗi trang (tăng từ 12)
const totalPages = ref(0);
const totalElements = ref(0);

// Lấy dữ liệu sản phẩm từ API
const fetchProducts = async () => {
  isLoading.value = true;
  try {
    const params = {
      keyword: search.value?.trim() || undefined,
      brand_id: filterBrand.value || undefined,
      category_id: filterCategory.value || undefined,
      gender: filterType.value || undefined,
      status: true, // Chỉ lấy sản phẩm đang kinh doanh
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      sortBy: 'id',
      sortDirection: 'desc'
    };

    // Remove undefined values
    Object.keys(params).forEach(key => params[key] === undefined && delete params[key]);

    const res = await ProductApi.getAllProducts(params);
    const data = res.data.data;
    products.value = data.content || [];
    totalPages.value = data.totalPages || 0;
    totalElements.value = data.totalElements || 0;

    // Debug: Log để kiểm tra dữ liệu
    console.log('API Response:', data);
    console.log('Products:', products.value);
    console.log('Filter Type:', filterType.value);
    console.log('Filtered Products:', filteredProducts.value);
  } catch (error) {
    console.error('Lỗi khi lấy danh sách sản phẩm:', error);
    products.value = [];
    totalPages.value = 0;
    totalElements.value = 0;
  } finally {
    isLoading.value = false;
  }
};

// Lấy dữ liệu filters
const fetchFilters = async () => {
  try {
    const [brandRes, sizeRes, categoryRes] = await Promise.all([
      BrandApi.getAllBrands(),
      SizeApi.getAllSizes(),
      CategoryApi.getAllCategories()
    ]);

    brands.value = brandRes.data.data || [];
    sizes.value = sizeRes.data.data || [];
    categories.value = categoryRes.data.data || [];
  } catch (error) {
    console.error('Lỗi khi lấy dữ liệu filters:', error);
  }
};

// Hàm tính phần trăm khuyến mại cho sản phẩm
const getProductDiscountPercentage = (product) => {
  // Kiểm tra promotions trong variant
  if (product.variantId && product.parentProduct?.variants) {
    const variant = product.parentProduct.variants.find(v => v.id === product.variantId);
    if (variant && variant.promotions && variant.promotions.length > 0) {
      // Lấy promotion có giá trị cao nhất
      const maxDiscount = Math.max(...variant.promotions.map(p => p.customValue || p.originalValue || 0));
      return maxDiscount > 0 ? maxDiscount : 0;
    }
  }
  
  // Kiểm tra active_promotions của sản phẩm gốc
  if (product.parentProduct?.active_promotions && product.parentProduct.active_promotions.length > 0) {
    const maxDiscount = Math.max(...product.parentProduct.active_promotions.map(p => p.value || 0));
    return maxDiscount > 0 ? maxDiscount : 0;
  }
  
  return 0;
};

// Hàm tính giá sau khuyến mại
const getProductDiscountedPrice = (product) => {
  const discountPercentage = getProductDiscountPercentage(product);
  if (discountPercentage <= 0) return product.price;
  
  const originalPrice = product.price || 0;
  const discountAmount = (originalPrice * discountPercentage) / 100;
  return originalPrice - discountAmount;
};

// Computed để lọc sản phẩm theo các điều kiện (chỉ lọc client-side cho các filter không gửi lên server)
const filteredProducts = computed(() => {
  // Tạo danh sách các biến thể như sản phẩm riêng biệt
  const variantProducts = [];

  products.value.forEach(product => {
    if (product.variants && Array.isArray(product.variants)) {
      product.variants.forEach(variant => {
        if (variant) {
          // Tạo object sản phẩm từ biến thể
          const variantProduct = {
            ...product, // Copy tất cả thuộc tính của sản phẩm gốc
            id: variant.id, // Sử dụng ID của biến thể
            variantId: variant.id,
            parentProduct: product, // Tham chiếu đến sản phẩm gốc
            // Override các thuộc tính với thông tin biến thể
            price: variant.sellPrice || product.price || 0,
            quantity: variant.quantity || 0,
            sku: variant.sku || variant.code || product.sku || product.code || product.id,
            size: variant.size,
            color: variant.color,
            // Tạo tên sản phẩm bao gồm thông tin biến thể
            displayName: `${product.name} - ${variant.size?.name || variant.size?.value || 'N/A'}${variant.color ? ` (${variant.color.name || variant.color.value})` : ''}`,
            // Thông tin hiển thị
            thumbnail: product.thumbnail,
            brand: product.brand,
            category: product.category,
            description: product.description,
            // Trạng thái dựa trên quantity của biến thể
            inStock: (variant.quantity || 0) > 0
          };

          variantProducts.push(variantProduct);
        }
      });
    } else {
      // Nếu sản phẩm không có biến thể, thêm sản phẩm gốc
      variantProducts.push({
        ...product,
        variantId: null,
        parentProduct: product,
        displayName: product.name,
        inStock: (product.quantity || 0) > 0
      });
    }
  });

  // Lọc theo các điều kiện
  return variantProducts.filter(p => {
    // Lọc theo size (nếu có filter size)
    const matchSize = !filterSize.value ||
      (p.size && (p.size.id === filterSize.value || p.size.name === filterSize.value || p.size.value === filterSize.value));

    // Lọc theo giá
    const matchPrice = !filterPrice.value || (p.price || 0) <= filterPrice.value;

    return matchSize && matchPrice;
  });
});







// Giỏ hàng
const cart = ref([]); // { product, variant, size, quantity, variantId }
const showProductModal = ref(false);
const isAddingToCart = ref(false); // Tránh spam click

function selectProduct(product) {
  if (!product) {
    console.error('Product is null or undefined');
    return;
  }

  // Tránh spam click
  if (isAddingToCart.value) {
    return;
  }

  // Kiểm tra xem sản phẩm có còn hàng không
  if (!product.inStock || product.quantity <= 0) {
    showAlert('Sản phẩm này đã hết hàng!', 'warning');
    return;
  }

  // Mở modal chọn số lượng
  openQuantityModal(product);
}

// Hàm tính số lượng sản phẩm đã có trong giỏ hàng
function getCartQuantity(product) {
  const existingItem = cart.value.find(item =>
    item.product.id === (product.parentProduct || product).id &&
    item.size === (product.size?.name || product.size?.value || 'N/A') &&
    item.variantId === product.variantId
  );
  return existingItem ? existingItem.quantity : 0;
}

// Cache để tối ưu performance
const productQuantityCache = ref(new Map());

// Hàm tính tổng số lượng sản phẩm đã có trong TẤT CẢ đơn chờ (có cache)
function calculateTotalQuantityInAllOrders(product) {
  const productKey = `${(product.parentProduct || product).id}-${product.size?.name || product.size?.value || 'N/A'}-${product.variantId}`;
  
  // Kiểm tra cache trước
  if (productQuantityCache.value.has(productKey)) {
    return productQuantityCache.value.get(productKey);
  }
  
  let totalQuantity = 0;
  
  // Tính từ TẤT CẢ đơn chờ (bao gồm cả đơn hiện tại)
  pendingOrders.value.forEach(order => {
    if (order.cart && Array.isArray(order.cart)) {
      order.cart.forEach(item => {
        if (item.product && item.variant && 
            item.product.id === (product.parentProduct || product).id &&
            item.size === (product.size?.name || product.size?.value || 'N/A') &&
            item.variantId === product.variantId) {
          totalQuantity += item.quantity || 0;
        }
      });
    }
  });
  
  // Cũng tính từ giỏ hàng hiện tại nếu có
  if (cart.value && cart.value.length > 0) {
    cart.value.forEach(item => {
      if (item.product && item.variant && 
          item.product.id === (product.parentProduct || product).id &&
          item.size === (product.size?.name || product.size?.value || 'N/A') &&
          item.variantId === product.variantId) {
        totalQuantity += item.quantity || 0;
      }
    });
  }
  
  // Lưu vào cache
  productQuantityCache.value.set(productKey, totalQuantity);
  
  return totalQuantity;
}

// Hàm xóa cache khi có thay đổi
function clearProductQuantityCache() {
  productQuantityCache.value.clear();
}

// Hàm mở modal chọn số lượng
function openQuantityModal(product) {
  selectedProductForQuantity.value = product;
  quantityToAdd.value = 1;
  
  // Debug: Log thông tin sản phẩm
  const totalQuantityInAllOrders = calculateTotalQuantityInAllOrders(product);
  const availableQuantity = Math.max(0, product.quantity - totalQuantityInAllOrders);
  
  console.log('=== DEBUG QUANTITY MODAL ===');
  console.log('Product:', product.displayName);
  console.log('Product quantity:', product.quantity);
  console.log('Total in other orders:', totalQuantityInAllOrders);
  console.log('Available quantity:', availableQuantity);
  console.log('Active pending code:', activePendingCode.value);
  console.log('Pending orders:', pendingOrders.value.map(o => ({ code: o.code, cartLength: o.cart?.length })));
  console.log('===========================');
  
  showQuantityModal.value = true;
}

// Hàm đóng modal chọn số lượng
function closeQuantityModal() {
  showQuantityModal.value = false;
  selectedProductForQuantity.value = null;
  quantityToAdd.value = 1;
}

// Hàm tăng số lượng
function increaseQuantity() {
  if (selectedProductForQuantity.value) {
    const totalQuantityInAllOrders = calculateTotalQuantityInAllOrders(selectedProductForQuantity.value);
    const availableQuantity = Math.max(0, selectedProductForQuantity.value.quantity - totalQuantityInAllOrders);
    
    if (quantityToAdd.value < availableQuantity) {
      quantityToAdd.value++;
    }
  }
}

// Hàm giảm số lượng
function decreaseQuantity() {
  if (quantityToAdd.value > 1) {
    quantityToAdd.value--;
  }
}

// Hàm validate số lượng
function validateQuantity() {
  if (selectedProductForQuantity.value) {
    const totalQuantityInAllOrders = calculateTotalQuantityInAllOrders(selectedProductForQuantity.value);
    const availableQuantity = Math.max(0, selectedProductForQuantity.value.quantity - totalQuantityInAllOrders);
    
    if (quantityToAdd.value < 1) {
      quantityToAdd.value = 1;
    } else if (quantityToAdd.value > availableQuantity) {
      quantityToAdd.value = availableQuantity;
    }
  }
}

// Hàm thêm vào giỏ hàng với số lượng đã chọn
function addToCartWithQuantity() {
  if (!selectedProductForQuantity.value) return;
  
  const product = selectedProductForQuantity.value;
  const quantity = quantityToAdd.value;
  
  // Tính tổng số lượng đã có trong tất cả đơn chờ
  const totalQuantityInAllOrders = calculateTotalQuantityInAllOrders(product);
  const availableQuantity = Math.max(0, product.quantity - totalQuantityInAllOrders);
  
  // Kiểm tra số lượng tồn kho còn lại
  if (quantity > availableQuantity) {
    showAlert(`Chỉ còn ${availableQuantity} sản phẩm có thể thêm! (Đã có ${totalQuantityInAllOrders} trong các đơn chờ khác)`, 'warning');
    return;
  }
  
  // Tạo cart item
  const cartItem = {
    product: product.parentProduct || product,
    variant: product,
    variantId: product.variantId,
    size: product.size?.name || product.size?.value || 'N/A',
    quantity: quantity,
    price: getProductDiscountedPrice(product),
    originalPrice: product.price,
    discountPercentage: getProductDiscountPercentage(product)
  };
  
  // Kiểm tra xem sản phẩm đã có trong giỏ hàng chưa
  const existingIndex = cart.value.findIndex(item =>
    item.product.id === cartItem.product.id &&
    item.size === cartItem.size &&
    item.variantId === cartItem.variantId
  );

  if (existingIndex !== -1) {
    // Cập nhật số lượng nếu đã có
    const newQuantity = cart.value[existingIndex].quantity + quantity;
    if (newQuantity > availableQuantity) {
      showAlert(`Tổng số lượng không được vượt quá ${availableQuantity} sản phẩm!`, 'warning');
      return;
    }
    cart.value[existingIndex].quantity = newQuantity;
  } else {
    // Thêm mới vào giỏ hàng
    cart.value.push(cartItem);
  }

  // Xóa cache sau khi thay đổi giỏ hàng
  clearProductQuantityCache();
  
  // Hiển thị thông báo thành công
  showAlert(`Đã thêm ${quantity} ${product.displayName} vào giỏ hàng!`, 'success');
  
  // Lưu trạng thái đơn hàng hiện tại vào localStorage
  saveCurrentOrderState();
  
  // Đóng modal
  closeQuantityModal();
}

// Function này không còn cần thiết vì chúng ta đã thêm trực tiếp vào giỏ hàng
// function confirmAddToCart() {
//   // Đã được xử lý trong selectProduct
// }

function updateQuantity(idx, qty) {
  if (qty < 1) qty = 1;
  const item = cart.value[idx];
  // Kiểm tra số lượng tồn kho từ sản phẩm gốc hoặc variant
  const availableQuantity = item.variant?.quantity || item.product?.quantity || 0;
  if (qty > availableQuantity) {
    showAlert(`Chỉ còn ${availableQuantity} sản phẩm cho size này!`, 'warning');
    qty = availableQuantity;
  }
  if (cart.value[idx]) {
    cart.value[idx].quantity = qty;
    
    // Cập nhật localStorage sau khi thay đổi số lượng
    saveCurrentOrderState();
  }
}

function removeFromCart(idx) {
  const item = cart.value[idx];
  showConfirmModalAction(
    'removeFromCart',
    idx,
    'Xóa sản phẩm',
    `Bạn có chắc chắn muốn xóa sản phẩm "${item.product.name}" khỏi giỏ hàng?`,
    'danger',
    'Xóa',
    'Giữ lại'
  );
}

function clearCart() {
  showConfirmModalAction(
    'clearCart',
    null,
    'Hủy đơn hàng',
    'Bạn có chắc chắn muốn hủy đơn hàng này? Tất cả thông tin sẽ bị xóa.',
    'danger',
    'Hủy đơn hàng',
    'Giữ lại'
  );
}

// Function để reset form (không cần xác nhận)
function resetForm() {
  // Reset giỏ hàng
  cart.value = [];

  // Reset thông tin khách hàng
  customerType.value = 'guest';
  customerSearch.value = '';
  selectedCustomer.value = null;
  customer.name = '';
  customer.phone = '';

  // Reset thông tin giao hàng
  shippingType.value = 'pickup';
  shippingAddress.value = '';
  shippingNote.value = '';
  shippingFee.value = 0;
  shippingFeeDisplay.value = '';
  isCalculatingShipping.value = false;
  shippingError.value = '';
  selectedAddressObj.value = null;
  showAddressModal.value = false;
  selectedAddress.value = null;
  suggestedAddresses.value = [];
  
  // Reset 3 ô input địa chỉ
  selectedProvince.value = null;
  selectedDistrict.value = null;
  selectedWard.value = null;
  detailedAddress.value = '';
  districts.value = [];
  wards.value = [];

  // Reset thông tin thanh toán
  customerCash.value = 0;
  customerCashDisplay.value = '';
  discount.value = 0;
  tax.value = 0;
  paymentMethod.value = 'cash';

  // Reset voucher
  voucherCode.value = '';
  voucherError.value = '';
  voucherSuccess.value = '';
  appliedVoucher.value = null;
  selectedVoucherId.value = '';
  showVoucherList.value = false;
  
  // Reset QR code
  qrCodeData.value = null;
  qrCodeUrl.value = '';
  qrCodeError.value = '';
  qrCodeLoading.value = false;
  
  // Xóa trạng thái đơn hàng hiện tại khỏi localStorage
  localStorage.removeItem('pos_current_order_state');
}

const totalAmount = computed(() =>
  cart.value.reduce((sum, item) => {
    const price = item.variant?.price || item.product.price || 0;
    return sum + (price * item.quantity);
  }, 0)
);

// Tính tổng tiền tiết kiệm từ khuyến mại
const totalDiscountAmount = computed(() =>
  cart.value.reduce((sum, item) => {
    const originalPrice = item.variant?.originalPrice || item.product.price || 0;
    const discountedPrice = item.variant?.price || item.product.price || 0;
    const discountPerItem = originalPrice - discountedPrice;
    return sum + (discountPerItem * item.quantity);
  }, 0)
);

// Kiểm tra xem có bất kỳ thông tin đơn hàng nào không (sản phẩm HOẶC thông tin khách hàng)
const hasAnyOrderData = computed(() => {
  // Có sản phẩm trong giỏ hàng
  if (cart.value.length > 0) return true;
  
  // Có thông tin khách hàng
  if (customerType.value !== 'guest') return true;
  if (customer.name || customer.phone) return true;
  if (selectedCustomer.value) return true;
  
  // Có thông tin giao hàng
  if (shippingType.value === 'delivery') return true;
  if (shippingAddress.value || shippingNote.value) return true;
  if (selectedProvince.value || selectedDistrict.value || selectedWard.value || detailedAddress.value) return true;
  
  // Có thông tin thanh toán
  if (discount.value > 0 || tax.value > 0) return true;
  if (paymentMethod.value !== 'cash') return true;
  
  // Có voucher
  if (appliedVoucher.value) return true;
  
  return false;
});

// Kiểm tra xem có đơn chờ nào không
const hasAnyOrders = computed(() => {
  const hasPendingOrders = pendingOrders.value.length > 0;
  const hasCurrentCart = cart.value.length > 0;
  const hasActiveOrder = activePendingCode.value !== null;
  
  console.log('=== DEBUG hasAnyOrders ===');
  console.log('Has pending orders:', hasPendingOrders);
  console.log('Has current cart:', hasCurrentCart);
  console.log('Has active order:', hasActiveOrder);
  console.log('Result:', hasPendingOrders || hasCurrentCart || hasActiveOrder);
  console.log('========================');
  
  return hasPendingOrders || hasCurrentCart || hasActiveOrder;
});

// Kiểm tra xem có thể thanh toán được không (cần đủ thông tin bắt buộc)
const canCheckout = computed(() => {
  // Phải có sản phẩm trong giỏ hàng
  if (cart.value.length === 0) return false;
  
  // Nếu là khách mới, phải có tên và số điện thoại
  if (customerType.value === 'new' && (!customer.name || !customer.phone)) return false;
  
  // Nếu là khách quen, phải chọn được khách hàng
  if (customerType.value === 'regular' && !selectedCustomer.value) return false;
  
  // Nếu giao hàng, phải có địa chỉ đầy đủ
  if (shippingType.value === 'delivery') {
    if (!selectedProvince.value || !selectedDistrict.value || !selectedWard.value || !detailedAddress.value) {
      return false;
    }
  }
  
  return true;
});

// Thông tin khách hàng
const customerType = ref('guest');
const customerSearch = ref('');
const customers = ref([]);
const isLoadingCustomers = ref(false);
const filteredCustomers = computed(() => {
  if (!customerSearch.value) return [];
  return customers.value.filter(cus =>
    cus.phoneNumber?.includes(customerSearch.value) ||
    cus.fullname?.toLowerCase().includes(customerSearch.value.toLowerCase())
  );
});
const selectedCustomer = ref(null);
function selectCustomer(cus) {
  selectedCustomer.value = cus;
  customerSearch.value = '';
  
  // Tải địa chỉ của khách hàng đã chọn
  if (cus.id) {
    loadCustomerAddress(cus.id);
  }
}
const customer = reactive({ name: '', phone: '' });
const shippingType = ref('pickup');
const shippingAddress = ref('');
const shippingNote = ref('');
const shippingFee = ref(0);
const shippingFeeDisplay = ref('');

// Thêm các biến cho tính phí ship theo cân nặng và địa chỉ
const isCalculatingShipping = ref(false);
const shippingError = ref('');
const selectedAddressObj = ref(null);
const showAddressModal = ref(false);

// Biến để lưu địa chỉ đã chọn từ danh sách
const selectedAddress = ref(null);

// Danh sách địa chỉ gợi ý
const suggestedAddresses = ref([]);

// Biến cho 3 ô input địa chỉ
const selectedProvince = ref(null);
const selectedDistrict = ref(null);
const selectedWard = ref(null);
const detailedAddress = ref('');
const provinces = ref([]);
const districts = ref([]);
const wards = ref([]);

// Tính cân nặng giỏ hàng dựa trên trọng lượng thực tế của sản phẩm
const getCartWeight = () => {
  const total = cart.value.reduce(
    (sum, item) => {
      // Lấy trọng lượng thực tế của sản phẩm (kg) và chuyển sang gram
      const productWeight = (item.product.weight || 0.8) * 1000; // kg -> gram, mặc định 0.8kg nếu không có
      return sum + productWeight * (item.quantity || 0);
    },
    0
  );
  // Nếu giỏ hàng rỗng vì lý do nào đó, GHN không cho 0 → mặc định 800g
  return total > 0 ? total : 800;
};

// Hàm lấy trọng lượng của một sản phẩm (gram)
const getProductWeight = (product) => {
  return (product.weight || 0.8) * 1000; // kg -> gram, mặc định 0.8kg nếu không có
};

const toNum = (v) => {
  const n = Number(v);
  return Number.isFinite(n) ? n : undefined;
};

// build payload cho /shipping/fee-from-openapi (BE map sang GHN)
const buildOpenApiPayloadFromAddress = (addr = {}) => {
  // Kiểm tra các trường bắt buộc (log cảnh báo, vẫn gửi nếu thiếu để BE chủ động báo lỗi)
  if (!addr.provinceName || !addr.districtName || !addr.wardName) {
    console.warn('Thiếu thông tin địa chỉ để tính phí vận chuyển:', addr);
  }

  return {
    // có code thì gửi, không có cũng không sao
    provinceCode: toNum(addr.provinceCode),
    districtCode: toNum(addr.districtCode),

    // GHN yêu cầu wardCode là STRING (ví dụ "90737") — không ép số
    wardCode: addr.wardCode != null ? String(addr.wardCode) : undefined,

    // name lấy từ Address đã lưu theo provinces.open-api.vn
    provinceName: addr.provinceName,
    districtName: addr.districtName,
    wardName: addr.wardName,

    weight: getCartWeight(),
    serviceTypeId: null,   // để BE chọn dịch vụ rẻ nhất
    insuranceValue: 0
  };
};

// Gọi API tính phí
const calculateShippingFeeFromAddress = async (addressObj) => {
  if (!addressObj) return;
  isCalculatingShipping.value = true;
  shippingError.value = '';
  try {
    // Đảm bảo có đủ thông tin địa chỉ để tính phí
    if (!addressObj.provinceName || !addressObj.districtName || !addressObj.wardName) {
      throw new Error('Địa chỉ không đủ thông tin để tính phí vận chuyển');
    }

    const payload = buildOpenApiPayloadFromAddress(addressObj);
    console.log('Calculating shipping fee with payload:', payload);

    const res = await ShippingApi.calcFeeFromOpenApi(payload);
    const data = res?.data?.data || res?.data;
    console.log('Shipping fee response:', data);

    const fee = Number(data?.total);
    shippingFee.value = Number.isFinite(fee) ? fee : 0;
    console.log('Set shipping fee to:', shippingFee.value);
  } catch (e) {
    console.error('Shipping fee error:', e);
    // Đặt phí vận chuyển về 0 khi có lỗi
    shippingFee.value = 0;
    shippingError.value =
      e?.response?.data?.message ||
      e?.message ||
      'Không tính được phí vận chuyển, vui lòng kiểm tra lại địa chỉ';
  } finally {
    isCalculatingShipping.value = false;
  }
};

// Hàm xử lý khi thay đổi địa chỉ
const handleAddressChange = async (addressText) => {
  shippingAddress.value = addressText;
  
  // Tạo danh sách địa chỉ gợi ý dựa trên input
  if (addressText && addressText.length > 2) {
    generateSuggestedAddresses(addressText);
  } else {
    suggestedAddresses.value = [];
  }
  
  // Nếu địa chỉ có format chuẩn (có thể parse được), thử tính phí ship
  if (addressText && addressText.includes(',')) {
    const parts = addressText.split(',').map(part => part.trim());
    if (parts.length >= 3) {
      const addressObj = {
        wardName: parts[0],
        districtName: parts[1], 
        provinceName: parts[2]
      };
      selectedAddressObj.value = addressObj;
      await calculateShippingFeeFromAddress(addressObj);
    }
  } else {
    // Reset phí ship nếu địa chỉ không đủ thông tin
    shippingFee.value = 0;
    selectedAddressObj.value = null;
  }
};

// Hàm tạo danh sách địa chỉ gợi ý
const generateSuggestedAddresses = (searchText) => {
  // Danh sách địa chỉ mẫu (có thể thay thế bằng API call)
  const sampleAddresses = [
    {
      fullAddress: '123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh',
      streetAddress: '123 Nguyễn Huệ',
      wardName: 'Phường Bến Nghé',
      districtName: 'Quận 1',
      provinceName: 'TP. Hồ Chí Minh'
    },
    {
      fullAddress: '456 Lê Lợi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh',
      streetAddress: '456 Lê Lợi',
      wardName: 'Phường Bến Thành',
      districtName: 'Quận 1',
      provinceName: 'TP. Hồ Chí Minh'
    },
    {
      fullAddress: '789 Trần Hưng Đạo, Phường Cầu Kho, Quận 1, TP. Hồ Chí Minh',
      streetAddress: '789 Trần Hưng Đạo',
      wardName: 'Phường Cầu Kho',
      districtName: 'Quận 1',
      provinceName: 'TP. Hồ Chí Minh'
    },
    {
      fullAddress: '321 Võ Văn Tần, Phường 6, Quận 3, TP. Hồ Chí Minh',
      streetAddress: '321 Võ Văn Tần',
      wardName: 'Phường 6',
      districtName: 'Quận 3',
      provinceName: 'TP. Hồ Chí Minh'
    },
    {
      fullAddress: '654 Nguyễn Thị Minh Khai, Phường 6, Quận 3, TP. Hồ Chí Minh',
      streetAddress: '654 Nguyễn Thị Minh Khai',
      wardName: 'Phường 6',
      districtName: 'Quận 3',
      provinceName: 'TP. Hồ Chí Minh'
    }
  ];

  // Lọc địa chỉ phù hợp với từ khóa tìm kiếm
  const filtered = sampleAddresses.filter(address => 
    address.fullAddress.toLowerCase().includes(searchText.toLowerCase()) ||
    address.streetAddress.toLowerCase().includes(searchText.toLowerCase()) ||
    address.wardName.toLowerCase().includes(searchText.toLowerCase()) ||
    address.districtName.toLowerCase().includes(searchText.toLowerCase())
  );

  suggestedAddresses.value = filtered.slice(0, 5); // Giới hạn 5 kết quả
};

// Hàm xử lý khi chọn tỉnh/thành phố
const onProvinceChange = async () => {
  selectedDistrict.value = null;
  selectedWard.value = null;
  districts.value = [];
  wards.value = [];
  
  if (selectedProvince.value) {
    // Load districts cho province đã chọn
    await loadDistricts(selectedProvince.value.code);
  }
  
  updateShippingAddress();
};

// Hàm xử lý khi chọn quận/huyện
const onDistrictChange = async () => {
  selectedWard.value = null;
  wards.value = [];
  
  if (selectedDistrict.value) {
    // Load wards cho district đã chọn
    await loadWards(selectedDistrict.value.code);
  }
  
  updateShippingAddress();
};

// Hàm xử lý khi chọn phường/xã
const onWardChange = () => {
  updateShippingAddress();
};

// Hàm cập nhật địa chỉ giao hàng từ 3 ô input
const updateShippingAddress = async () => {
  const addressParts = [];
  
  if (detailedAddress.value && detailedAddress.value.trim() !== '') {
    addressParts.push(detailedAddress.value.trim());
  }
  
  if (selectedWard.value && selectedWard.value.name) {
    addressParts.push(selectedWard.value.name);
  }
  
  if (selectedDistrict.value && selectedDistrict.value.name) {
    addressParts.push(selectedDistrict.value.name);
  }
  
  if (selectedProvince.value && selectedProvince.value.name) {
    addressParts.push(selectedProvince.value.name);
  }
  
  shippingAddress.value = addressParts.join(', ');
  
  console.log('=== DEBUG UPDATE SHIPPING ADDRESS ===');
  console.log('Address parts:', addressParts);
  console.log('Shipping address:', shippingAddress.value);
  console.log('Selected province:', selectedProvince.value);
  console.log('Selected district:', selectedDistrict.value);
  console.log('Selected ward:', selectedWard.value);
  
  // Tính phí ship nếu có đủ thông tin
  if (selectedProvince.value && selectedDistrict.value && selectedWard.value && 
      selectedProvince.value.name && selectedDistrict.value.name && selectedWard.value.name) {
    const addressObj = {
      provinceName: selectedProvince.value.name,
      districtName: selectedDistrict.value.name,
      wardName: selectedWard.value.name,
      provinceCode: selectedProvince.value.code,
      districtCode: selectedDistrict.value.code,
      wardCode: selectedWard.value.code
    };
    
    selectedAddressObj.value = addressObj;
    await calculateShippingFeeFromAddress(addressObj);
  } else {
    shippingFee.value = 0;
    selectedAddressObj.value = null;
  }
};

// API base URL cho địa chỉ
const BASE_URL_API = 'https://provinces.open-api.vn/api';

// Hàm load danh sách tỉnh/thành phố
const loadProvinces = async () => {
  try {
    const res = await fetch(`${BASE_URL_API}/p/`);
    provinces.value = await res.json();
  } catch (error) {
    console.error('Lỗi khi tải danh sách tỉnh/thành phố:', error);
    showAlert('Không thể tải danh sách tỉnh/thành phố', 'danger');
    provinces.value = [];
  }
};

// Hàm load danh sách quận/huyện
const loadDistricts = async (provinceCode) => {
  try {
    const res = await fetch(`${BASE_URL_API}/p/${provinceCode}?depth=2`);
    const data = await res.json();
    districts.value = data.districts || [];
  } catch (error) {
    console.error('Lỗi khi tải danh sách quận/huyện:', error);
    showAlert('Không thể tải danh sách quận/huyện', 'danger');
    districts.value = [];
  }
};

// Hàm load danh sách phường/xã
const loadWards = async (districtCode) => {
  try {
    const res = await fetch(`${BASE_URL_API}/d/${districtCode}?depth=2`);
    const data = await res.json();
    wards.value = data.wards || [];
  } catch (error) {
    console.error('Lỗi khi tải danh sách phường/xã:', error);
    showAlert('Không thể tải danh sách phường/xã', 'danger');
    wards.value = [];
  }
};

// Hàm chọn địa chỉ từ danh sách gợi ý
const selectSuggestedAddress = async (address) => {
  shippingAddress.value = address.fullAddress;
  selectedAddressObj.value = address;
  suggestedAddresses.value = []; // Ẩn danh sách gợi ý
  
  // Tính phí ship với địa chỉ đã chọn
  await calculateShippingFeeFromAddress(address);
  showAlert('Đã chọn địa chỉ giao hàng', 'success');
};

// Hàm chọn địa chỉ mẫu từ modal
const selectSampleAddress = async (addressText) => {
  shippingAddress.value = addressText;
  showAddressModal.value = false;
  
  // Parse địa chỉ và tính phí ship
  if (addressText && addressText.includes(',')) {
    const parts = addressText.split(',').map(part => part.trim());
    if (parts.length >= 3) {
      const addressObj = {
        wardName: parts[0],
        districtName: parts[1], 
        provinceName: parts[2]
      };
      selectedAddressObj.value = addressObj;
      await calculateShippingFeeFromAddress(addressObj);
      showAlert('Đã cập nhật địa chỉ giao hàng', 'success');
    }
  }
};

// Hàm xử lý khi chọn địa chỉ từ danh sách địa chỉ đã lưu
const handleSaveAddress = async (address) => {
  if (!address) {
    showAddressModal.value = false;
    return;
  }

  // Tạo chuỗi địa chỉ hiển thị
  const line = [
    address.streetAddress,
    address.wardName,
    address.districtName,
    address.provinceName
  ].filter(Boolean).join(', ');

  // Kiểm tra đủ thông tin để tính phí vận chuyển
  if (!address.provinceName || !address.districtName || !address.wardName) {
    showAlert('Địa chỉ không đủ thông tin để tính phí vận chuyển', 'warning');
    // Vẫn cập nhật địa chỉ nhưng không tính phí
    shippingAddress.value = line;
    selectedAddressObj.value = null;
    showAddressModal.value = false;
    return;
  }

  shippingAddress.value = line;
  selectedAddressObj.value = address;  // lưu object để recalc

  showAlert('Đã cập nhật địa chỉ giao hàng', 'success');
  // Gọi hàm tính phí vận chuyển với địa chỉ mới
  await calculateShippingFeeFromAddress(address);
  showAddressModal.value = false;
};

// Lấy danh sách khách hàng
const fetchCustomers = async () => {
  isLoadingCustomers.value = true;
  try {
    const response = await getAllUsers('Customer', 0, 100, '', true);
    customers.value = response.data.data?.content || [];
  } catch (error) {
    console.error('Lỗi khi lấy danh sách khách hàng:', error);
    customers.value = [];
  } finally {
    isLoadingCustomers.value = false;
  }
};

// Lấy danh sách phương thức thanh toán
const fetchPaymentMethods = async () => {
  try {
    const response = await PaymentMethodApi.getAllPaymentMethods();
    paymentMethods.value = response.data.data || [];
    console.log('Payment methods:', paymentMethods.value);
  } catch (error) {
    console.error('Lỗi khi lấy danh sách phương thức thanh toán:', error);
    paymentMethods.value = [];
  }
};

// Watch để tự động load lại voucher khi thay đổi giỏ hàng hoặc khách hàng
watch([cart, selectedCustomer, customerType], () => {
  if (cart.value.length > 0) {
    fetchAvailableVouchers();
  }
}, { deep: true });



// Voucher/Coupon
const voucherCode = ref('');
const voucherLoading = ref(false);
const voucherError = ref('');
const voucherSuccess = ref('');
const appliedVoucher = ref(null);
const selectedVoucherId = ref('');
const availableVouchers = ref([]);
const showVoucherList = ref(false);

// Thanh toán
const customerCash = ref(0);
const customerCashDisplay = ref('');
const discount = ref(0);
const tax = ref(0);
const paymentMethod = ref('cash');
const paymentMethods = ref([]);

// VietQR
const qrCodeData = ref(null);
const qrCodeLoading = ref(false);
const qrCodeError = ref('');
const qrCodeUrl = ref('');

// Tính tổng tiền trước khi trừ voucher
const totalBeforeVoucher = computed(() => {
  const subtotal = totalAmount.value;
  const manualDiscount = Number(discount.value) || 0;
  const shipping = Number(shippingFee.value) || 0;
  const taxAmount = Number(tax.value) || 0;
  return subtotal + shipping + taxAmount - manualDiscount;
});

// Tính tổng tiền cần thanh toán
const totalToPay = computed(() => {
  const subtotal = totalAmount.value;
  const voucherDiscount = appliedVoucher.value?.discountAmount || 0;
  const manualDiscount = Number(discount.value) || 0;
  const totalDiscount = voucherDiscount + manualDiscount;
  const shipping = Number(shippingFee.value) || 0;
  const taxAmount = Number(tax.value) || 0;

  // Debug log để kiểm tra
  console.log('Tính toán tổng tiền:', {
    subtotal,
    voucherDiscount,
    manualDiscount,
    totalDiscount,
    shipping,
    taxAmount,
    finalTotal: subtotal + shipping + taxAmount - totalDiscount
  });

  return subtotal + shipping + taxAmount - totalDiscount;
});

// Tính tiền thối
const changeAmount = computed(() => {
  const cash = Number(customerCash.value) || 0;
  return cash - totalToPay.value;
});

// Chọn voucher từ danh sách
const selectVoucher = (voucher) => {
  voucherCode.value = voucher.code;
  selectedVoucherId.value = voucher.id;
  applyVoucher();
};

// Áp dụng voucher
const applyVoucher = async () => {
  voucherError.value = '';
  voucherSuccess.value = '';
  appliedVoucher.value = null;

  if (!voucherCode.value.trim()) {
    voucherError.value = "Vui lòng nhập mã voucher!";
    return;
  }

  // Kiểm tra giỏ hàng có sản phẩm không
  if (cart.value.length === 0) {
    voucherError.value = "Giỏ hàng trống! Vui lòng thêm sản phẩm trước khi áp dụng voucher.";
    return;
  }

  voucherLoading.value = true;
  try {
    // Sử dụng userId của khách hàng đã chọn hoặc mặc định cho khách vãng lai
    const id_user = customerType.value === 'regular' && selectedCustomer.value ? selectedCustomer.value.id : 1;
    console.log('Đang kiểm tra voucher:', voucherCode.value.trim(), 'với user_id:', id_user);
    const res = await CouponApi.getCouponByCode(voucherCode.value.trim(), id_user);
    console.log('API Response:', res);
    const voucher = res.data.data;

    if (voucher && voucher.id) {
      // Kiểm tra trạng thái voucher
      if (voucher.status === 0) {
        voucherError.value = "Mã voucher này chưa bắt đầu!";
        return;
      } else if (voucher.status === 2) {
        voucherError.value = "Mã voucher này đã hết hạn!";
        return;
      } else if (voucher.status === 3) {
        voucherError.value = "Mã voucher này đã bị xóa!";
        return;
      } else if (voucher.status === 4) {
        voucherError.value = "Số lượng mã voucher đã hết!";
        return;
      } else if (voucher.status !== 1) {
        voucherError.value = "Mã voucher không hợp lệ!";
        return;
      }

             // Kiểm tra điều kiện áp dụng (nếu có)
       const orderTotal = totalAmount.value;
       if (voucher.minimumOrderValue && orderTotal < voucher.minimumOrderValue) {
         voucherError.value = `Đơn hàng tối thiểu phải từ ${currency(voucher.minimumOrderValue)} để áp dụng voucher này!`;
         return;
       }



       // Tính toán giảm giá theo logic CheckOut.vue
       const total = totalAmount.value + shippingFee.value + tax.value;
      let discountAmount = 0;

      if (voucher.type) {
        // Giảm theo phần trăm
        discountAmount = total * (voucher.value / 100);
        if (voucher.valueLimit && discountAmount > voucher.valueLimit) {
          discountAmount = voucher.valueLimit;
        }
      } else {
        // Giảm theo số tiền cố định
        discountAmount = voucher.value;
      }

      // Nếu vượt quá tổng thì discount tối đa = tổng
      if (discountAmount > total) discountAmount = total;

      appliedVoucher.value = {
        ...voucher,
        code: voucher.code || voucherCode.value.trim(), // Đảm bảo code được lưu
        discountAmount,
      };

      // Debug log để kiểm tra
      console.log('Voucher đã được áp dụng:', {
        voucher: appliedVoucher.value,
        discountAmount: appliedVoucher.value.discountAmount,
        totalToPay: totalToPay.value,
        voucherData: {
          id: voucher.id,
          code: voucher.code,
          name: voucher.name,
          value: voucher.value,
          type: voucher.type,
          discountAmount: discountAmount
        },
        appliedVoucherCode: appliedVoucher.value.code,
        originalVoucherCode: voucherCode.value.trim()
      });

  

   
      
      // Hiển thị toast thông báo thành công
      showAlert(`Áp dụng voucher "${voucher.name}" thành công!`, 'success');
    } else {
      voucherError.value = "Mã voucher không hợp lệ!";
    }
  } catch (error) {
    console.error('Lỗi khi kiểm tra voucher:', error);
    
    const msg =
      error?.response?.data?.message ??
      error?.data?.message ??
      error?.response?.data?.error ??
      error?.response?.statusText ??
      error?.message ??
      'Mã voucher không hợp lệ hoặc đã hết hạn!';
    
    voucherError.value = msg;
    appliedVoucher.value = null;
    voucherSuccess.value = '';
  } finally {
    voucherLoading.value = false;
  }
};

// Hàm format VND
const formatVND = (value) => {
  if (!value) return '';
  // Loại bỏ tất cả ký tự không phải số
  const numericValue = value.toString().replace(/[^\d]/g, '');
  if (!numericValue) return '';
  // Format thành chuỗi VND
  return numericValue.replace(/\B(?=(\d{3})+(?!\d))/g, '.') + '₫';
};

// Hàm parse VND về số
const parseVND = (formattedValue) => {
  if (!formattedValue) return 0;
  // Loại bỏ tất cả ký tự không phải số
  return Number(formattedValue.replace(/[^\d]/g, '')) || 0;
};

// Hàm xử lý input cho input tiền khách đưa
const handleCustomerCashInput = (event) => {
  const value = event.target.value;
  // Loại bỏ tất cả ký tự không phải số và dấu chấm
  const numericValue = value.replace(/[^\d]/g, '');
  // Format thành chuỗi VND
  customerCashDisplay.value = formatVND(numericValue);
};

// Hàm xử lý keydown cho input tiền khách đưa - chỉ cho phép số và phím điều hướng
const handleCustomerCashKeydown = (event) => {
  // Cho phép các phím điều hướng, xóa, backspace, tab, enter
  const allowedKeys = ['Backspace', 'Delete', 'Tab', 'Enter', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Home', 'End'];
  
  // Cho phép các số từ 0-9
  const isNumber = /^[0-9]$/.test(event.key);
  
  if (!allowedKeys.includes(event.key) && !isNumber) {
    event.preventDefault();
    return false;
  }
};

// Hàm xử lý paste cho input tiền khách đưa - chỉ giữ lại số
const handleCustomerCashPaste = (event) => {
  event.preventDefault();
  
  // Lấy nội dung từ clipboard
  const pastedText = (event.clipboardData || window.clipboardData).getData('text');
  
  // Chỉ giữ lại các ký tự số
  const numericOnly = pastedText.replace(/[^\d]/g, '');
  
  if (numericOnly) {
    // Cập nhật giá trị input với chỉ số
    const input = event.target;
    const start = input.selectionStart;
    const end = input.selectionEnd;
    const currentValue = input.value;
    
    // Tạo giá trị mới bằng cách thay thế phần được chọn
    const newValue = currentValue.substring(0, start) + numericOnly + currentValue.substring(end);
    
    // Cập nhật giá trị và format
    customerCashDisplay.value = formatVND(newValue);
    
    // Đặt lại vị trí con trỏ
    setTimeout(() => {
      const newCursorPos = start + numericOnly.length;
      input.setSelectionRange(newCursorPos, newCursorPos);
    }, 0);
  }
};

// Watch cho customerCashDisplay
watch(customerCashDisplay, (newValue) => {
  customerCash.value = parseVND(newValue);
});

// Watch cho shippingFeeDisplay
watch(shippingFeeDisplay, (newValue) => {
  shippingFee.value = parseVND(newValue);
});

// Auto recalc khi giỏ hàng hoặc địa chỉ đổi
watch([cart, selectedAddressObj], async ([items, addr]) => {
  if (!addr?.provinceName || !addr?.districtName || !addr?.wardName) return;
  if (!Array.isArray(items) || items.length === 0) return;
  if (isCalculatingShipping.value) return;
  try {
    await calculateShippingFeeFromAddress(addr);
  } catch { /* no-op */ }
}, { deep: true });

// Watch cho showProductModal để quản lý body scroll
watch(showProductModal, (newValue) => {
  if (newValue) {
    document.body.classList.add('modal-open');
  } else {
    document.body.classList.remove('modal-open');
  }
});

// Watch cho showAddressModal để quản lý body scroll
watch(showAddressModal, (newValue) => {
  if (newValue) {
    document.body.classList.add('modal-open');
  } else {
    document.body.classList.remove('modal-open');
  }
});

// Watch cho customerType để reset địa chỉ khi chuyển về khách vãng lai
watch(customerType, (newValue) => {
  if (newValue === 'guest') {
    // Reset về địa chỉ mặc định khi chuyển về khách vãng lai
    loadDefaultAddress();
  }
});

// Watch cho paymentMethod để tự động tạo QR code khi chọn chuyển khoản
watch(paymentMethod, (newValue) => {
  if (newValue === 'bank' && totalToPay.value > 0) {
    // Tự động tạo QR code khi chọn chuyển khoản và có tổng tiền
    setTimeout(() => {
      generateQRCode();
    }, 500); // Delay 500ms để UI render xong
  } else if (newValue === 'cash') {
    // Reset QR code khi chuyển về tiền mặt
    qrCodeData.value = null;
    qrCodeUrl.value = '';
    qrCodeError.value = '';
  }
});

// Watch cho totalToPay để tự động cập nhật QR code khi tổng tiền thay đổi
watch(totalToPay, (newValue) => {
  if (paymentMethod.value === 'bank' && newValue > 0 && (qrCodeData.value || qrCodeUrl.value)) {
    // Tự động tạo lại QR code khi tổng tiền thay đổi
    setTimeout(() => {
      generateQRCode();
    }, 1000); // Delay 1s để tránh tạo QR quá nhiều
  }
});



// Xóa voucher
const removeVoucher = () => {
  const voucherName = appliedVoucher.value?.name || 'Voucher';
  appliedVoucher.value = null;
  voucherCode.value = '';
  voucherError.value = '';
  voucherSuccess.value = '';
  selectedVoucherId.value = '';
  
  // Hiển thị thông báo
  showAlert(`Đã xóa voucher "${voucherName}"!`, 'info');
  
  // Cập nhật localStorage sau khi xóa voucher
  saveCurrentOrderState();
  
  // Làm mới danh sách voucher có sẵn
  fetchAvailableVouchers();
};

// Toggle hiển thị danh sách voucher
const toggleVoucherList = () => {
  showVoucherList.value = !showVoucherList.value;
  if (showVoucherList.value && availableVouchers.value.length === 0) {
    fetchAvailableVouchers();
  }
};

// Xử lý khi thay đổi voucher trong combobox
const onVoucherChange = () => {
  if (selectedVoucherId.value) {
    applySelectedVoucher();
  }
};

// Áp dụng voucher được chọn từ combobox
const applySelectedVoucher = async () => {
  if (!selectedVoucherId.value) {
    showAlert('Vui lòng chọn voucher!', 'warning');
    return;
  }

  // Kiểm tra giỏ hàng có sản phẩm không
  if (cart.value.length === 0) {
    showAlert('Giỏ hàng trống! Vui lòng thêm sản phẩm trước khi áp dụng voucher.', 'warning');
    return;
  }

  // Tìm voucher được chọn
  const selectedVoucher = availableVouchers.value.find(v => v.id == selectedVoucherId.value);
  if (!selectedVoucher) {
    showAlert('Voucher không tồn tại!', 'error');
    return;
  }

  // Kiểm tra điều kiện áp dụng
  const orderTotal = totalAmount.value;
  if (selectedVoucher.minimumOrderValue && orderTotal < selectedVoucher.minimumOrderValue) {
    showAlert(`Đơn hàng tối thiểu phải từ ${currency(selectedVoucher.minimumOrderValue)} để áp dụng voucher này!`, 'warning');
    return;
  }

  // Tính toán giảm giá
  const total = totalAmount.value + shippingFee.value + tax.value;
  let discountAmount = 0;

  if (selectedVoucher.type) {
    // Giảm theo phần trăm
    discountAmount = total * (selectedVoucher.value / 100);
    if (selectedVoucher.valueLimit && discountAmount > selectedVoucher.valueLimit) {
      discountAmount = selectedVoucher.valueLimit;
    }
  } else {
    // Giảm theo số tiền cố định
    discountAmount = selectedVoucher.value;
  }

  // Nếu vượt quá tổng thì discount tối đa = tổng
  if (discountAmount > total) discountAmount = total;

  appliedVoucher.value = {
    ...selectedVoucher,
    code: selectedVoucher.code,
    discountAmount,
  };

  // Hiển thị thông báo thành công
  showAlert(`Áp dụng voucher "${selectedVoucher.name}" thành công!`, 'success');
  
  // Cập nhật localStorage
  saveCurrentOrderState();
};

// Test API voucher để debug
const testVoucherAPI = async () => {
  console.log('=== TEST VOUCHER API ===');
  
  try {
    const user_id = customerType.value === 'regular' && selectedCustomer.value ? selectedCustomer.value.id : 1;
    const orderTotal = totalAmount.value;
    
    console.log('Current state:', {
      user_id,
      orderTotal,
      customerType: customerType.value,
      selectedCustomer: selectedCustomer.value,
      cartLength: cart.value.length
    });
    
    console.log('1. Test CouponApi.getAllCoupons() không có tham số:');
    const response1 = await CouponApi.getAllCoupons();
    console.log('Response 1:', response1);
    console.log('Data 1:', response1?.data?.data);
    
    console.log('2. Test CouponApi.getAllCoupons(user_id, orderTotal):');
    const response2 = await CouponApi.getAllCoupons(user_id, orderTotal);
    console.log('Response 2:', response2);
    console.log('Data 2:', response2?.data?.data);
    
    console.log('3. Test CouponApi.getAllCoupons(1, 100000):');
    const response3 = await CouponApi.getAllCoupons(1, 100000);
    console.log('Response 3:', response3);
    console.log('Data 3:', response3?.data?.data);
    
    console.log('4. Kiểm tra CouponApi object:');
    console.log('CouponApi:', CouponApi);
    
    // Hiển thị kết quả trong alert
    const totalVouchers = (response1?.data?.data?.length || 0) + 
                         (response2?.data?.data?.length || 0) + 
                         (response3?.data?.data?.length || 0);
    
    showAlert(`Test API hoàn thành! Tìm thấy ${totalVouchers} voucher. Kiểm tra console để xem chi tiết.`, 'info');
  } catch (error) {
    console.error('Lỗi khi test API voucher:', error);
    console.error('Error details:', {
      message: error.message,
      response: error.response?.data,
      status: error.response?.status,
      url: error.config?.url
    });
    showAlert(`Lỗi khi test API: ${error.message}`, 'error');
  }
};

// Lấy danh sách voucher có sẵn
const fetchAvailableVouchers = async () => {
  try {
    voucherLoading.value = true;
    
    // Lấy user_id và orderTotal
    const user_id = customerType.value === 'regular' && selectedCustomer.value ? selectedCustomer.value.id : 1;
    const orderTotal = totalAmount.value;
    
    console.log('=== FETCH AVAILABLE VOUCHERS ===');
    console.log('user_id:', user_id);
    console.log('orderTotal:', orderTotal);
    
    // Gọi API với tham số
    let response = await CouponApi.getAllCoupons(user_id, orderTotal);
    console.log('API Response với tham số:', response);
    
    let vouchers = response.data?.data || [];
    
    // Nếu không có voucher nào, thử gọi API không có tham số
    if (!vouchers || vouchers.length === 0) {
      console.log('Không có voucher từ API với tham số, thử gọi API không có tham số...');
      try {
        response = await CouponApi.getAllCoupons();
        console.log('API Response không có tham số:', response);
        vouchers = response.data?.data || [];
      } catch (fallbackError) {
        console.log('API không có tham số cũng lỗi:', fallbackError);
      }
    }
    
    // Lọc chỉ lấy voucher có status = 1 (đang hoạt động)
    const activeVouchers = vouchers.filter(voucher => voucher.status === 1);
    console.log('Vouchers sau khi lọc status:', activeVouchers);
    
    // Gán kết quả từ API (không sử dụng dữ liệu mẫu)
    availableVouchers.value = activeVouchers;
    console.log('Final availableVouchers from API:', availableVouchers.value);
    
  } catch (error) {
    console.error('Lỗi khi lấy danh sách voucher:', error);
    console.error('Error details:', {
      message: error.message,
      response: error.response?.data,
      status: error.response?.status
    });
    
    // Không sử dụng dữ liệu mẫu, để trống để hiển thị thông báo
    availableVouchers.value = [];
  } finally {
    voucherLoading.value = false;
  }
};



// Tạo QR code bằng VietQR API
const generateQRCode = async () => {
  if (totalToPay.value <= 0) {
    showAlert('Vui lòng thêm sản phẩm vào giỏ hàng trước khi tạo QR code!', 'warning');
    return;
  }

  qrCodeLoading.value = true;
  qrCodeError.value = '';
  qrCodeData.value = null;
  qrCodeUrl.value = '';

  try {
    // Validate thông tin thanh toán
    const validation = validatePaymentInfo({
      amount: totalToPay.value,
      addInfo: 'Thanh toan don hang'
    });

    if (!validation.isValid) {
      throw new Error(validation.errors.join(', '));
    }

    // Thử tạo QR code bằng API VietQR trước
    try {
      const result = await generateVietQR({
        amount: totalToPay.value,
        addInfo: 'Thanh toan don hang'
      });

      if (result.success && result.qrDataURL) {
        qrCodeData.value = result.qrDataURL;
        showAlert('Đã tạo QR code thành công!', 'success');
        return;
      }
    } catch (apiError) {
      console.warn('VietQR API failed, using fallback:', apiError);
    }

    // Fallback: Sử dụng URL đơn giản nếu API thất bại
    qrCodeUrl.value = generateSimpleVietQR({
      amount: totalToPay.value,
      addInfo: 'Thanh toan don hang'
    });

    showAlert('Đã tạo QR code thành công!', 'success');

  } catch (error) {
    console.error('Lỗi khi tạo QR code:', error);
    qrCodeError.value = error.message || 'Không thể tạo QR code. Vui lòng thử lại!';
    showAlert(qrCodeError.value, 'error');
  } finally {
    qrCodeLoading.value = false;
  }
};

// Tải xuống QR code
const downloadQRCode = () => {
  const qrImageSrc = qrCodeData.value || qrCodeUrl.value;
  if (!qrImageSrc) {
    showAlert('Không có QR code để tải xuống!', 'warning');
    return;
  }

  try {
    // Tạo link tải xuống
    const link = document.createElement('a');
    link.href = qrImageSrc;
    link.download = `qr-code-${Date.now()}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    showAlert('Đã tải xuống QR code!', 'success');
  } catch (error) {
    console.error('Lỗi khi tải xuống QR code:', error);
    showAlert('Không thể tải xuống QR code!', 'error');
  }
};



// Tạo khách hàng mới
const isCreatingCustomer = ref(false);
const createCustomer = async () => {
  if (!customer.name || !customer.phone) {
    showAlert('Vui lòng nhập đầy đủ tên và số điện thoại khách hàng!', 'warning');
    return;
  }

  isCreatingCustomer.value = true;
  try {
    const formData = new FormData();
    formData.append('fullname', customer.name.trim());
    formData.append('phoneNumber', customer.phone.trim());
    formData.append('email', `${customer.phone}@guest.com`); // Email tạm thời
    formData.append('password', '123456'); // Mật khẩu mặc định
    formData.append('address', 'Chưa cập nhật');
    formData.append('gender', 'true');
    formData.append('roleName', 'Customer');

    // Gọi API tạo khách hàng mới
    const response = await createUser(formData);

    if (response.data && response.data.data) {
      const newCustomer = response.data.data;

      // Thêm vào danh sách khách hàng
      customers.value.unshift(newCustomer);

      // Chọn khách hàng vừa tạo
      selectedCustomer.value = newCustomer;
      customerType.value = 'regular';

      // Reset form khách mới
      customer.name = '';
      customer.phone = '';

      showAlert('Tạo khách hàng mới thành công!', 'success');
    }
  } catch (error) {
    console.error('Lỗi khi tạo khách hàng:', error);
    
    // Hiển thị thông báo lỗi cụ thể từ API
    let errorMessage = 'Có lỗi xảy ra khi tạo khách hàng mới!';
    
    // Kiểm tra response data từ API
    if (error.response?.data) {
      const responseData = error.response.data;
      
      // Nếu có message chính
      if (responseData.message) {
        errorMessage = responseData.message;
      }
      
      // Nếu có data chứa các lỗi validation cụ thể
      if (responseData.data && typeof responseData.data === 'object') {
        const validationErrors = [];
        Object.keys(responseData.data).forEach(field => {
          const fieldError = responseData.data[field];
          if (typeof fieldError === 'string') {
            validationErrors.push(fieldError);
          } else if (Array.isArray(fieldError)) {
            validationErrors.push(...fieldError);
          }
        });
        
        if (validationErrors.length > 0) {
          errorMessage = validationErrors.join(', ');
        }
      }
      
      // Nếu có errors array
      if (responseData.errors && Array.isArray(responseData.errors)) {
        errorMessage = responseData.errors.map(err => err.message || err).join(', ');
      }
      
      // Nếu có error string
      if (responseData.error) {
        errorMessage = responseData.error;
      }
    } else if (error.message) {
      errorMessage = error.message;
    }
    
    showAlert(errorMessage, 'error');
  } finally {
    isCreatingCustomer.value = false;
  }
};

async function checkout() {
  console.log('=== DEBUG CHECKOUT ===');
  console.log('Cart length:', cart.value.length);
  console.log('Shipping type:', shippingType.value);
  console.log('Shipping address:', shippingAddress.value);
  console.log('Detailed address:', detailedAddress.value);
  console.log('Selected province:', selectedProvince.value);
  console.log('Selected district:', selectedDistrict.value);
  console.log('Selected ward:', selectedWard.value);
  
  // Kiểm tra giỏ hàng có sản phẩm không
  if (cart.value.length === 0) {
    showAlert('Giỏ hàng trống! Vui lòng thêm sản phẩm vào giỏ hàng trước khi thanh toán.', 'warning');
    return;
  }

  // Kiểm tra thông tin khách hàng
  if (customerType.value === 'new' && (!customer.name || !customer.phone)) {
    showAlert('Vui lòng nhập đầy đủ thông tin khách hàng!', 'warning');
    return;
  }

  if (customerType.value === 'regular' && !selectedCustomer.value) {
    showAlert('Vui lòng chọn khách hàng!', 'warning');
    return;
  }

  // Nếu chọn chuyển khoản, tiếp tục xử lý thanh toán bình thường
  // Không cần validate gì thêm, chỉ cần hiện QR code là được

  // Kiểm tra tiền khách đưa có đủ không (chỉ cho tiền mặt)
  if (paymentMethod.value === 'cash' && changeAmount.value < 0) {
    showAlert(`Tiền khách đưa không đủ! Khách cần đưa thêm ${currency(Math.abs(changeAmount.value))} để hoàn thành thanh toán.`, 'warning');
    return;
  }

  // Kiểm tra thông tin giao hàng
  if (shippingType.value === 'delivery') {
    // Kiểm tra xem có địa chỉ chi tiết hoặc đã chọn đủ 3 trường địa chỉ chưa
    const hasDetailedAddress = detailedAddress.value && detailedAddress.value.trim() !== '';
    const hasFullAddress = selectedProvince.value && selectedDistrict.value && selectedWard.value;
    const hasAnyAddress = shippingAddress.value || hasDetailedAddress || hasFullAddress;
    
    if (!hasAnyAddress) {
      showAlert('Vui lòng nhập địa chỉ giao hàng! Bạn có thể nhập địa chỉ chi tiết hoặc chọn đầy đủ tỉnh/thành phố, quận/huyện, phường/xã.', 'warning');
      return;
    }
    
    // Nếu có địa chỉ nhưng không đủ để tính phí ship, vẫn cho phép thanh toán nhưng cảnh báo
    if (!hasFullAddress) {
      const confirmContinue = confirm('Bạn chưa chọn đầy đủ tỉnh/thành phố, quận/huyện, phường/xã để tính phí vận chuyển. Phí ship sẽ được tính sau. Bạn có muốn tiếp tục thanh toán không?');
      if (!confirmContinue) {
        return;
      }
    }
  }

  // Hiển thị modal xác nhận thanh toán
  const customerName = customerType.value === 'new' ? customer.name : 
                      customerType.value === 'regular' ? selectedCustomer.value?.fullname : 'Khách vãng lai';
  
  showConfirmModalAction(
    'checkout',
    null,
    'Xác nhận thanh toán',
    `Bạn có chắc chắn muốn thanh toán đơn hàng với tổng tiền ${currency(totalToPay.value)} cho khách hàng "${customerName}"?`,
    'success',
    'Thanh toán',
    'Hủy'
  );
}

function createNewOrder() {
  try {
    // Kiểm tra giới hạn tối đa 5 đơn chờ
    if (pendingOrders.value.length >= 5) {
      showAlert('Chỉ được tạo tối đa 5 đơn chờ! Vui lòng hoàn thành hoặc xóa đơn chờ cũ.', 'warning');
      return;
    }

    // Tăng counter liên tục cho đơn hàng mới
    orderCounter.value++;
    localStorage.setItem('pos_order_counter', orderCounter.value.toString());
    
    // Xóa cache khi tạo đơn mới
    clearProductQuantityCache();

    // Tạo đơn mới ngay cả khi chưa có dữ liệu
    const pending = serializeCurrentOrder();
    pendingOrders.value = [pending, ...pendingOrders.value.filter(o => o.code !== pending.code)];
    activePendingCode.value = null; // clear active vì ta đang bắt đầu đơn mới
    persistPending();

    showAlert('Đã tạo đơn mới!', 'success');
    // Xóa giỏ hàng để tiếp tục bán hàng mới
    clearCartConfirmed(true);
  } catch (e) {
    console.error('Lỗi tạo đơn:', e);
    showAlert('Không thể tạo đơn. Vui lòng thử lại!', 'error');
  }
}



function createShippingOrder() {
  // TODO: Gọi API tạo đơn ship
  showAlert('Đã tạo đơn ship!', 'success');
}

// Đơn chờ
const pendingOrders = ref([]);
const activePendingCode = ref(null);
const orderCounter = ref(1);

// Load đơn chờ từ localStorage khi khởi tạo
try {
  const today = new Date().toDateString();
  const savedDate = localStorage.getItem('pos_last_date');
  
  // Nếu sang ngày mới, xóa tất cả đơn chờ và reset counter
  if (savedDate !== today) {
    localStorage.removeItem('pos_pending_orders');
    localStorage.removeItem('pos_active_pending_code');
    localStorage.removeItem('pos_order_counter');
    localStorage.setItem('pos_last_date', today);
    pendingOrders.value = [];
    activePendingCode.value = null;
    orderCounter.value = 1;
  } else {
    // Cùng ngày, load dữ liệu cũ
  const saved = localStorage.getItem('pos_pending_orders');
  if (saved) {
    pendingOrders.value = JSON.parse(saved) || [];
  }
  const savedActive = localStorage.getItem('pos_active_pending_code');
  if (savedActive) activePendingCode.value = savedActive;
    
    // Load counter từ localStorage
    const savedCounter = localStorage.getItem('pos_order_counter');
    if (savedCounter) {
      orderCounter.value = parseInt(savedCounter) || 1;
    }
  }
} catch { /* no-op */ }

// Lưu trạng thái đơn hàng hiện tại vào localStorage
const saveCurrentOrderState = () => {
  try {
    if (cart.value.length > 0 || customerType.value !== 'guest' || shippingType.value !== 'pickup') {
      const currentState = {
        cart: cart.value,
        customerType: customerType.value,
        selectedCustomer: selectedCustomer.value,
        customer: { ...customer },
        shippingType: shippingType.value,
        shippingAddress: shippingAddress.value,
        shippingNote: shippingNote.value,
        shippingFee: shippingFee.value,
        selectedAddressObj: selectedAddressObj.value,
        selectedProvince: selectedProvince.value,
        selectedDistrict: selectedDistrict.value,
        selectedWard: selectedWard.value,
        detailedAddress: detailedAddress.value,
        discount: Number(discount.value) || 0,
        tax: Number(tax.value) || 0,
        paymentMethod: paymentMethod.value,
        appliedVoucher: appliedVoucher.value ? { ...appliedVoucher.value } : null,
        selectedVoucherId: selectedVoucherId.value
      };
      localStorage.setItem('pos_current_order_state', JSON.stringify(currentState));
    }
  } catch (e) {
    console.error('Lỗi lưu trạng thái đơn hàng:', e);
  }
};

// Khôi phục trạng thái đơn hàng từ localStorage
const restoreCurrentOrderState = () => {
  try {
    const saved = localStorage.getItem('pos_current_order_state');
    if (saved) {
      const state = JSON.parse(saved);
      if (state.cart && state.cart.length > 0) {
        cart.value = state.cart;
        customerType.value = state.customerType || 'guest';
        selectedCustomer.value = state.selectedCustomer || null;
        customer.name = state.customer?.name || '';
        customer.phone = state.customer?.phone || '';
        shippingType.value = state.shippingType || 'pickup';
        shippingAddress.value = state.shippingAddress || '';
        shippingNote.value = state.shippingNote || '';
        shippingFee.value = state.shippingFee || 0;
        selectedAddressObj.value = state.selectedAddressObj || null;
        selectedProvince.value = state.selectedProvince || null;
        selectedDistrict.value = state.selectedDistrict || null;
        selectedWard.value = state.selectedWard || null;
        detailedAddress.value = state.detailedAddress || '';
        discount.value = Number(state.discount) || 0;
        tax.value = Number(state.tax) || 0;
        paymentMethod.value = state.paymentMethod || 'cash';
        appliedVoucher.value = state.appliedVoucher ? { ...state.appliedVoucher } : null;
        selectedVoucherId.value = state.selectedVoucherId || '';
      }
    }
  } catch (e) {
    console.error('Lỗi khôi phục trạng thái đơn hàng:', e);
  }
};

const persistPending = () => {
  localStorage.setItem('pos_pending_orders', JSON.stringify(pendingOrders.value));
  if (activePendingCode.value) {
    localStorage.setItem('pos_active_pending_code', activePendingCode.value);
  } else {
    localStorage.removeItem('pos_active_pending_code');
  }
};

const serializeCurrentOrder = () => {
  const now = new Date();
  
  // Tính tổng tiền từ giỏ hàng
  const cartTotal = cart.value.reduce((sum, item) => {
    const itemPrice = item.variant?.price || item.product?.price || 0;
    return sum + (itemPrice * item.quantity);
  }, 0);
  
  // Tính tổng cuối cùng bao gồm phí ship, thuế, giảm giá
  const finalTotal = cartTotal + (Number(shippingFee.value) || 0) + (Number(tax.value) || 0) - (Number(discount.value) || 0);
  
  return {
    code: activePendingCode.value || `P${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getSeconds()).padStart(2, '0')}-HD${orderCounter.value}`,
    createdAt: now.toISOString(),
    customerType: customerType.value,
    selectedCustomer: selectedCustomer.value,
    customer: { ...customer },
    shippingType: shippingType.value,
    shippingAddress: shippingAddress.value,
    shippingNote: shippingNote.value,
    shippingFee: shippingFee.value,
    selectedAddressObj: selectedAddressObj.value,
    selectedProvince: selectedProvince.value,
    selectedDistrict: selectedDistrict.value,
    selectedWard: selectedWard.value,
    detailedAddress: detailedAddress.value,
    cart: JSON.parse(JSON.stringify(cart.value)),
    discount: Number(discount.value) || 0,
    tax: Number(tax.value) || 0,
    paymentMethod: paymentMethod.value,
    appliedVoucher: appliedVoucher.value ? { ...appliedVoucher.value } : null,
    selectedVoucherId: selectedVoucherId.value,
    total: Math.max(0, finalTotal) // Đảm bảo total không âm
  };
};

const loadOrderState = (order) => {
  console.log('=== DEBUG loadOrderState ===');
  console.log('Loading order:', order.code);
  console.log('Order cart length:', order.cart?.length || 0);
  console.log('Order total:', order.total);
  
  cart.value = JSON.parse(JSON.stringify(order.cart || []));
  customerType.value = order.customerType || 'guest';
  selectedCustomer.value = order.selectedCustomer || null;
  customer.name = order.customer?.name || '';
  customer.phone = order.customer?.phone || '';
  shippingType.value = order.shippingType || 'pickup';
  shippingAddress.value = order.shippingAddress || '';
  shippingNote.value = order.shippingNote || '';
  shippingFee.value = order.shippingFee || 0;
  selectedAddressObj.value = order.selectedAddressObj || null;
  selectedProvince.value = order.selectedProvince || null;
  selectedDistrict.value = order.selectedDistrict || null;
  selectedWard.value = order.selectedWard || null;
  detailedAddress.value = order.detailedAddress || '';
  discount.value = Number(order.discount) || 0;
  tax.value = Number(order.tax) || 0;
  paymentMethod.value = order.paymentMethod || 'cash';
  appliedVoucher.value = order.appliedVoucher ? { ...order.appliedVoucher } : null;
  selectedVoucherId.value = order.selectedVoucherId || '';
  
  console.log('Loaded cart length:', cart.value.length);
  console.log('Loaded customer type:', customerType.value);
  console.log('Loaded shipping type:', shippingType.value);
  console.log('===========================');
};
function resumePendingOrder(order) {
  if (!order) return;

  console.log('=== DEBUG resumePendingOrder ===');
  console.log('Switching to order:', order.code);
  console.log('Current active code:', activePendingCode.value);
  console.log('Current cart length:', cart.value.length);

  // Nếu đơn hiện tại có dữ liệu, lưu lại vào pending (tab-switch behavior)
  if (Array.isArray(cart.value) && cart.value.length > 0) {
    console.log('Saving current order state...');
    const current = serializeCurrentOrder();
    if (activePendingCode.value) {
      // Cập nhật đơn hiện tại trong danh sách
      const existingIndex = pendingOrders.value.findIndex(o => o.code === activePendingCode.value);
      if (existingIndex !== -1) {
        pendingOrders.value[existingIndex] = current;
      } else {
        pendingOrders.value = [current, ...pendingOrders.value];
      }
    } else {
      // Thêm đơn mới vào danh sách
      pendingOrders.value = [current, ...pendingOrders.value];
    }
    console.log('Current order saved');
  }

        // Tải đơn mục tiêu, đặt active
      console.log('Loading order state for:', order.code);
      loadOrderState(order);
      activePendingCode.value = order.code;
      persistPending();
      
      // Xóa cache khi chuyển đơn chờ
      clearProductQuantityCache();
      
      console.log('Order switched successfully');
      console.log('New active code:', activePendingCode.value);
      console.log('New cart length:', cart.value.length);
      console.log('===============================');

}
function removePendingOrder(code) {
  // Tìm thông tin đơn chờ để hiển thị trong modal xác nhận
  const pendingOrder = pendingOrders.value.find(o => o.code === code);
  const orderInfo = pendingOrder ? 
    `Đơn chờ ${code} (${pendingOrder.cart?.length || 0} sản phẩm - ${currency(pendingOrder.total || 0)})` : 
    `Đơn chờ ${code}`;
  
  showConfirmModalAction(
    'removePendingOrder',
    code,
    'Xóa đơn chờ',
    `Bạn có chắc chắn muốn xóa ${orderInfo}? Hành động này không thể hoàn tác.`,
    'danger',
    'Xóa',
    'Giữ lại'
  );
}

// Định dạng tiền tệ
function currency(val) {
  if (val === null || val === undefined || isNaN(val)) return '0₫';
  const numVal = Number(val);
  if (numVal < 0) return '0₫';
  return numVal.toLocaleString('vi-VN') + '₫';
}

// Hàm tính màu chữ tương phản
function getContrastColor(hexColor) {
  if (!hexColor) return '#000000';
  
  // Chuyển hex sang RGB
  const hex = hexColor.replace('#', '');
  const r = parseInt(hex.substr(0, 2), 16);
  const g = parseInt(hex.substr(2, 2), 16);
  const b = parseInt(hex.substr(4, 2), 16);
  
  // Tính độ sáng
  const brightness = (r * 299 + g * 587 + b * 114) / 1000;
  
  // Trả về màu chữ tương phản
  return brightness > 128 ? '#000000' : '#ffffff';
}

// Watch để tự động tìm kiếm
let searchTimeout = null;
const handleSearch = () => {
  if (searchTimeout) clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    currentPage.value = 1; // Reset về trang 1 khi tìm kiếm
    fetchProducts();
     }, 500);
 };

 const updatePriceRange = () => {
   currentPage.value = 1;
   fetchProducts();
 };

 const resetSearchFilters = () => {
   search.value = '';
   filterCategory.value = '';
   filterColor.value = '';
   filterMaterial.value = '';
   filterSize.value = '';
   filterSole.value = '';
   filterBrand.value = '';
   priceRange.value = [100000, 3200000];
   currentPage.value = 1;
   fetchProducts();
 };

// Watch để tự động lưu trạng thái đơn hàng khi có thay đổi
watch([cart, customerType, selectedCustomer, customer, shippingType, shippingAddress, shippingNote, shippingFee, selectedAddressObj, selectedProvince, selectedDistrict, selectedWard, detailedAddress, discount, tax, paymentMethod, appliedVoucher, selectedVoucherId], () => {
  // Debounce để tránh lưu quá nhiều
  if (searchTimeout) clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    saveCurrentOrderState();
  }, 1000);
}, { deep: true });

// Watch cho cart và customerType để cập nhật danh sách voucher
watch([cart, customerType, selectedCustomer], () => {
  // Debounce để tránh gọi API quá nhiều
  if (searchTimeout) clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    fetchAvailableVouchers();
  }, 2000);
}, { deep: true });

// Watch cho các filter khác
watch([filterBrand, filterCategory, filterType], () => {
  currentPage.value = 1; // Reset về trang 1 khi thay đổi filter
  fetchProducts();
});

// Watch cho các filter client-side
watch([filterSize, filterPrice], () => {
  // Không cần gọi API, chỉ cần re-render
});

// Watch cho pageSize
watch(pageSize, () => {
  currentPage.value = 1; // Reset về trang 1 khi thay đổi pageSize
  fetchProducts();
});

// Function điều hướng phân trang
const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value && page !== currentPage.value) {
    currentPage.value = page;
    fetchProducts();
  }
};

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++;
    fetchProducts();
  }
};

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--;
    fetchProducts();
  }
};

// Computed để tạo danh sách các trang hiển thị
const visiblePages = computed(() => {
  const current = currentPage.value;
  const total = totalPages.value;
  const pages = [];

  // Luôn hiển thị trang đầu
  if (current > 3) {
    pages.push(1);
    if (current > 4) {
      pages.push('...');
    }
  }

  // Hiển thị các trang xung quanh trang hiện tại
  const start = Math.max(1, current - 2);
  const end = Math.min(total, current + 2);

  for (let i = start; i <= end; i++) {
    if (!pages.includes(i)) {
      pages.push(i);
    }
  }

  // Luôn hiển thị trang cuối
  if (current < total - 2) {
    if (current < total - 3) {
      pages.push('...');
    }
    if (!pages.includes(total)) {
      pages.push(total);
    }
  }

     return pages;
 });

// Hàm lấy địa chỉ mặc định
const loadDefaultAddress = async () => {
  try {
    const address = await getSelectedAddress(1); // userId = 1 cho khách vãng lai
    if (address) {
      selectedAddress.value = address;
      // Tạo chuỗi địa chỉ hiển thị
      const line = [
        address.streetAddress,
        address.wardName,
        address.districtName,
        address.provinceName
      ].filter(Boolean).join(', ');
      
      shippingAddress.value = line;
      selectedAddressObj.value = address;
      
      // Tính phí ship nếu có đủ thông tin
      if (address.provinceName && address.districtName && address.wardName) {
        await calculateShippingFeeFromAddress(address);
      }
    }
  } catch (error) {
    console.log('Không có địa chỉ mặc định hoặc lỗi khi tải:', error);
  }
};

// Hàm lấy địa chỉ của khách hàng cụ thể
const loadCustomerAddress = async (userId) => {
  try {
    const address = await getSelectedAddress(userId);
    if (address) {
      selectedAddress.value = address;
      // Tạo chuỗi địa chỉ hiển thị
      const line = [
        address.streetAddress,
        address.wardName,
        address.districtName,
        address.provinceName
      ].filter(Boolean).join(', ');
      
      shippingAddress.value = line;
      selectedAddressObj.value = address;
      
      // Tính phí ship nếu có đủ thông tin
      if (address.provinceName && address.districtName && address.wardName) {
        await calculateShippingFeeFromAddress(address);
      }
      
      showAlert('Đã tải địa chỉ của khách hàng', 'success');
    } else {
      // Reset địa chỉ nếu khách hàng không có địa chỉ
      shippingAddress.value = '';
      selectedAddressObj.value = null;
      shippingFee.value = 0;
      showAlert('Khách hàng này chưa có địa chỉ giao hàng', 'warning');
    }
  } catch (error) {
    console.log('Lỗi khi tải địa chỉ khách hàng:', error);
    showAlert('Không thể tải địa chỉ khách hàng', 'error');
  }
};



// ConfirmModal functions
const showConfirmModalAction = (action, data, title, message, type = 'warning', confirmText = 'Xác nhận', cancelText = 'Hủy') => {
  pendingAction.value = action;
  pendingData.value = data;
  confirmModalTitle.value = title;
  confirmModalMessage.value = message;
  confirmModalType.value = type;
  confirmModalConfirmText.value = confirmText;
  confirmModalCancelText.value = cancelText;
  showConfirmModal.value = true;
};

const handleConfirmAction = async () => {
  confirmModalLoading.value = true;
  try {
    switch (pendingAction.value) {
      case 'clearCart':
        await clearCartConfirmed();
        break;
      case 'checkout':
        await checkoutConfirmed();
        break;
      case 'removeFromCart':
        await removeFromCartConfirmed();
        break;
      case 'removePendingOrder':
        await removePendingOrderConfirmed();
        break;
      default:
        console.warn('Unknown action:', pendingAction.value);
    }
  } catch (error) {
    console.error('Error in confirm action:', error);
    
    // Hiển thị thông báo lỗi cụ thể từ API
    let errorMessage = 'Có lỗi xảy ra khi thực hiện hành động';
    
    // Kiểm tra response data từ API
    if (error.response?.data) {
      const responseData = error.response.data;
      
      // Nếu có message chính
      if (responseData.message) {
        errorMessage = responseData.message;
      }
      
      // Nếu có data chứa các lỗi validation cụ thể
      if (responseData.data && typeof responseData.data === 'object') {
        const validationErrors = [];
        Object.keys(responseData.data).forEach(field => {
          const fieldError = responseData.data[field];
          if (typeof fieldError === 'string') {
            validationErrors.push(fieldError);
          } else if (Array.isArray(fieldError)) {
            validationErrors.push(...fieldError);
          }
        });
        
        if (validationErrors.length > 0) {
          errorMessage = validationErrors.join(', ');
        }
      }
      
      // Nếu có errors array
      if (responseData.errors && Array.isArray(responseData.errors)) {
        errorMessage = responseData.errors.map(err => err.message || err).join(', ');
      }
      
      // Nếu có error string
      if (responseData.error) {
        errorMessage = responseData.error;
      }
    } else if (error.message) {
      errorMessage = error.message;
    }
    
    showAlert(errorMessage, 'error');
  } finally {
    confirmModalLoading.value = false;
    hideConfirmModal();
  }
};

// Hàm format ngày giờ
function formatDateTime(iso) {
  try {
    if (!iso) return '';
    const d = new Date(iso);
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();
    const hh = String(d.getHours()).padStart(2, '0');
    const mm = String(d.getMinutes()).padStart(2, '0');
    return `${day}/${month}/${year} ${hh}:${mm}`;
  } catch { return ''; }
}



const hideConfirmModal = () => {
  showConfirmModal.value = false;
  pendingAction.value = null;
  pendingData.value = null;
};

// Confirmed actions
const clearCartConfirmed = (suppressAlert = false) => {
  // Reset giỏ hàng
  cart.value = [];
  
  // Xóa cache sau khi thay đổi giỏ hàng
  clearProductQuantityCache();

  // Reset thông tin khách hàng
  customerType.value = 'guest';
  customerSearch.value = '';
  selectedCustomer.value = null;
  customer.name = '';
  customer.phone = '';

  // Reset thông tin giao hàng
  shippingType.value = 'pickup';
  shippingAddress.value = '';
  shippingNote.value = '';
  shippingFee.value = 0;
  shippingFeeDisplay.value = '';
  suggestedAddresses.value = [];
  
  // Reset 3 ô input địa chỉ
  selectedProvince.value = null;
  selectedDistrict.value = null;
  selectedWard.value = null;
  detailedAddress.value = '';
  districts.value = [];
  wards.value = [];

  // Reset thông tin thanh toán
  customerCash.value = 0;
  customerCashDisplay.value = '';
  discount.value = 0;
  tax.value = 0;
  paymentMethod.value = 'cash';

  // Reset voucher
  voucherCode.value = '';
  voucherError.value = '';
  voucherSuccess.value = '';
  appliedVoucher.value = null;
  selectedVoucherId.value = '';

  // Reset QR code
  qrCodeData.value = null;
  qrCodeUrl.value = '';
  qrCodeError.value = '';
  qrCodeLoading.value = false;

  // Nếu đang có đơn chờ active, xóa khỏi danh sách và localStorage
  if (activePendingCode.value) {
    pendingOrders.value = pendingOrders.value.filter(o => o.code !== activePendingCode.value);
    activePendingCode.value = null;
    persistPending();
  }

  // Xóa trạng thái đơn hàng hiện tại khỏi localStorage
  localStorage.removeItem('pos_current_order_state');

  // Hiển thị thông báo thành công (chỉ khi không suppress)
  if (!suppressAlert) {
  showAlert('Đã hủy đơn hàng thành công!', 'success');
  }
};

const checkoutConfirmed = async () => {
  // Gọi hàm checkout thực tế
  await processCheckout();
};

// Hàm xử lý thanh toán thực tế
const processCheckout = async () => {
  try {
    // Kiểm tra voucher nếu có áp dụng
    if (appliedVoucher.value) {
      console.log('DEBUG - Voucher sẽ được gửi:', {
        code: appliedVoucher.value.code,
        id: appliedVoucher.value.id,
        discountAmount: appliedVoucher.value.discountAmount,
        name: appliedVoucher.value.name
      });
    } else {
      console.log('DEBUG - Không có voucher được áp dụng');
    }

    // Chuẩn bị dữ liệu đơn hàng - theo cấu trúc API
    const orderDetails = cart.value.map(item => {
      // Tìm variant tương ứng với size đã chọn
      const variant = item.product.variants?.find(v =>
        (v.size?.name === item.size || v.size?.value === item.size)
      );

      if (!variant?.id) {
        throw new Error(`Không tìm thấy variant cho sản phẩm ${item.product.name} với size ${item.size}`);
      }

      return {
        productVariantId: variant.id,
        quantity: item.quantity
        // Không cần price vì backend sẽ lấy từ variant
      };
    });

    // Tính tổng tiền
    const finalTotal = totalToPay.value;

    // Lấy thông tin khách hàng
    let customerInfo = {};
    if (customerType.value === 'new') {
      customerInfo = {
        fullname: customer.name,
        phoneNumber: customer.phone
      };
    } else if (customerType.value === 'regular' && selectedCustomer.value) {
      customerInfo = {
        fullname: selectedCustomer.value.fullname,
        phoneNumber: selectedCustomer.value.phoneNumber
      };
    } else {
      customerInfo = {
        fullname: 'Khách vãng lai',
        phoneNumber: 'N/A'
      };
    }

    // Tìm payment method phù hợp
    let paymentMethodId = 1; // Mặc định là tiền mặt
    if (paymentMethod.value === 'bank') {
      const onlinePaymentMethod = paymentMethods.value.find(method => 
        method.name.toLowerCase().includes('online') || 
        method.name.toLowerCase().includes('vnpay') ||
        method.name.toLowerCase().includes('bank')
      );
      if (onlinePaymentMethod) {
        paymentMethodId = onlinePaymentMethod.id;
      }
    }

    // Debug: Kiểm tra appliedVoucher trước khi tạo orderRequest
    console.log('DEBUG - appliedVoucher trước khi tạo orderRequest:', appliedVoucher.value);
    console.log('DEBUG - appliedVoucher.value?.code:', appliedVoucher.value?.code);
    console.log('DEBUG - appliedVoucher.value?.id:', appliedVoucher.value?.id);
    console.log('DEBUG - appliedVoucher.value?.name:', appliedVoucher.value?.name);
    console.log('DEBUG - appliedVoucher.value?.discountAmount:', appliedVoucher.value?.discountAmount);

    // Validation voucher trước khi gửi
    if (appliedVoucher.value) {
      console.log('Validation voucher trước khi gửi:', {
        hasVoucher: !!appliedVoucher.value,
        voucherCode: appliedVoucher.value.code,
        voucherId: appliedVoucher.value.id,
        discountAmount: appliedVoucher.value.discountAmount,
        totalToPay: finalTotal,
        voucherName: appliedVoucher.value.name
      });
      
      // Kiểm tra xem voucher có đầy đủ thông tin không
      if (!appliedVoucher.value.code) {
        console.warn('WARNING: Voucher không có code!');
      }
      if (!appliedVoucher.value.id) {
        console.warn('WARNING: Voucher không có id!');
      }
    }

    // Tạo request đơn hàng - theo cấu trúc API đơn giản
    const orderRequest = {
      userId: customerType.value === 'regular' && selectedCustomer.value ? selectedCustomer.value.id : 1, // Sử dụng ID khách hàng đã chọn hoặc mặc định cho khách vãng lai
      paymentMethodId: paymentMethodId,
      fullname: customerInfo.fullname,
      phoneNumber: customerInfo.phoneNumber,
      address: shippingType.value === 'delivery' ? shippingAddress.value : 'Mua tại quầy',
      shippingAddress: shippingType.value === 'delivery' ? shippingAddress.value : 'Mua tại quầy',
      note: shippingNote.value || `Đơn hàng bán tại quầy - ${paymentMethod.value === 'bank' ? 'Thanh toán chuyển khoản' : 'Thanh toán tiền mặt'}`,
      shippingMethod: shippingType.value === 'delivery' ? 'delivery' : 'pickup',
      totalMoney: finalTotal,
      orderDetails: orderDetails,
      cartItemId: [],
      // Thêm thông tin voucher nếu có - đảm bảo gửi đúng format
      couponCode: appliedVoucher.value?.code || null,
      couponId: appliedVoucher.value?.id || null, // Thêm couponId nếu backend cần
      discountAmount: appliedVoucher.value?.discountAmount || 0, // Thêm discountAmount
      // Thêm thông tin phí vận chuyển và loại đơn hàng
      shippingFee: shippingFee.value || 0,
      type: false, // Đơn hàng bán tại quầy
      // Đặt trạng thái hoàn thành ngay cho bán tại quầy
      status: 'COMPLETED'
    };

    // Debug: Kiểm tra voucher trong orderRequest
    if (appliedVoucher.value) {
      console.log('DEBUG - Voucher trong orderRequest:', {
        couponCode: orderRequest.couponCode,
        couponId: orderRequest.couponId,
        discountAmount: orderRequest.discountAmount,
        originalVoucherCode: appliedVoucher.value.code,
        originalVoucherId: appliedVoucher.value.id
      });
    }

    console.log('Order Request:', orderRequest);
    console.log('Order Details:', orderDetails);
    console.log('Cart items:', cart.value);
    console.log('DEBUG - couponCode trong orderRequest:', orderRequest.couponCode);
    console.log('DEBUG - couponId trong orderRequest:', orderRequest.couponId);
    console.log('DEBUG - discountAmount trong orderRequest:', orderRequest.discountAmount);
    console.log('DEBUG - appliedVoucher.value?.code:', appliedVoucher.value?.code);
    console.log('DEBUG - appliedVoucher.value?.id:', appliedVoucher.value?.id);

    // Gọi API tạo đơn hàng
    const response = await OrderApi.createOrder(orderRequest);
    console.log('Order Response 1234553455675677:', response);
    console.log('DEBUG - Response data:', response.data);
    console.log('DEBUG - Response data.data:', response.data?.data);
    console.log('DEBUG - Coupon trong response:', response.data?.data?.coupon);
    console.log('DEBUG - CouponCode trong response:', response.data?.data?.couponCode);

    if (response.data && response.data.data) {
      const orderId = response.data.data.id;

      // Cập nhật trạng thái đơn hàng thành COMPLETED ngay lập tức
      try {
        await OrderApi.updateOrderStatus(orderId, 'COMPLETED');
        console.log('Đã cập nhật trạng thái đơn hàng thành COMPLETED');
      } catch (statusError) {
        console.warn('Không thể cập nhật trạng thái đơn hàng:', statusError);
        // Vẫn tiếp tục dù có lỗi cập nhật status
      }

      let successMessage = paymentMethod.value === 'bank' 
        ? 'Thanh toán chuyển khoản thành công! Đơn hàng đã được tạo và hoàn thành.'
        : 'Thanh toán tiền mặt thành công! Đơn hàng đã được tạo và hoàn thành.';
      
      // Thêm thông tin voucher vào message nếu có
      if (appliedVoucher.value) {
        successMessage += ` (Đã áp dụng voucher: ${appliedVoucher.value.name} - Giảm ${currency(appliedVoucher.value.discountAmount)})`;
        console.log('DEBUG - Voucher applied in order:', {
          voucherName: appliedVoucher.value.name,
          voucherCode: appliedVoucher.value.code,
          discountAmount: appliedVoucher.value.discountAmount,
          orderId: orderId
        });
      }
      
      showAlert(successMessage, 'success');

      // Chuyển sang trang chi tiết đơn hàng
      window.open(`/admin/orders/${orderId}`, '_blank');

      // Nếu đơn hiện tại là đơn chờ đang hoạt động, xóa khỏi danh sách và đồng bộ localStorage
      if (typeof activePendingCode !== 'undefined' && activePendingCode.value) {
        pendingOrders.value = pendingOrders.value.filter(o => o.code !== activePendingCode.value);
        activePendingCode.value = null;
        if (typeof persistPending === 'function') {
          persistPending();
        } else {
          localStorage.setItem('pos_pending_orders', JSON.stringify(pendingOrders.value));
          localStorage.removeItem('pos_active_pending_code');
        }
      }

      // Reset form sau khi thanh toán thành công
      resetForm();
    } else {
      throw new Error('Không nhận được dữ liệu đơn hàng từ server');
    }

  } catch (error) {
    console.error('Lỗi khi tạo đơn hàng:', error);
    console.error('Error response:', error.response?.data);
    console.error('Error status:', error.response?.status);
    console.error('Error message:', error.response?.data?.message);
    console.error('DEBUG - Voucher info in error:', {
      appliedVoucher: appliedVoucher.value,
      couponCode: appliedVoucher.value?.code,
      couponId: appliedVoucher.value?.id,
      discountAmount: appliedVoucher.value?.discountAmount
    });

    let errorMessage = 'Có lỗi xảy ra khi tạo đơn hàng';
    
    // Kiểm tra response data từ API
    if (error.response?.data) {
      const responseData = error.response.data;
      
      // Nếu có message chính
      if (responseData.message) {
        errorMessage = responseData.message;
      }
      
      // Nếu có data chứa các lỗi validation cụ thể
      if (responseData.data && typeof responseData.data === 'object') {
        const validationErrors = [];
        Object.keys(responseData.data).forEach(field => {
          const fieldError = responseData.data[field];
          if (typeof fieldError === 'string') {
            validationErrors.push(fieldError);
          } else if (Array.isArray(fieldError)) {
            validationErrors.push(...fieldError);
          }
        });
        
        if (validationErrors.length > 0) {
          errorMessage = validationErrors.join(', ');
        }
      }
      
      // Nếu có errors array
      if (responseData.errors && Array.isArray(responseData.errors)) {
        errorMessage = responseData.errors.map(err => err.message || err).join(', ');
      }
      
      // Nếu có error string
      if (responseData.error) {
        errorMessage = responseData.error;
      }
    } else if (error.message) {
      errorMessage = error.message;
    }

    showAlert(errorMessage, 'error');
  }
};

const removeFromCartConfirmed = () => {
  const index = pendingData.value;
  if (index !== null && index !== undefined) {
    cart.value.splice(index, 1);
    
    // Xóa cache sau khi thay đổi giỏ hàng
    clearProductQuantityCache();
    
    // Cập nhật localStorage sau khi xóa sản phẩm
    saveCurrentOrderState();
    
    // Nếu giỏ hàng trống và không có thông tin đơn hàng khác, xóa localStorage
    if (cart.value.length === 0 && !hasAnyOrderData.value) {
      localStorage.removeItem('pos_current_order_state');
    }
    
    showAlert('Đã xóa sản phẩm khỏi giỏ hàng!', 'info');
  }
};

const removePendingOrderConfirmed = () => {
  const code = pendingData.value;
  if (code) {
    // Xóa đơn chờ khỏi danh sách
    pendingOrders.value = pendingOrders.value.filter(o => o.code !== code);
    
    // Nếu đơn chờ đang active bị xóa, clear active và xóa dữ liệu trong giỏ hàng
    if (activePendingCode.value === code) {
      activePendingCode.value = null;
      
      // Xóa dữ liệu trong giỏ hàng và form
      cart.value = [];
      customerType.value = 'guest';
      customerSearch.value = '';
      selectedCustomer.value = null;
      customer.name = '';
      customer.phone = '';
      shippingType.value = 'pickup';
      shippingAddress.value = '';
      shippingNote.value = '';
      shippingFee.value = 0;
      shippingFeeDisplay.value = '';
      selectedAddressObj.value = null;
      selectedProvince.value = null;
      selectedDistrict.value = null;
      selectedWard.value = null;
      detailedAddress.value = '';
      districts.value = [];
      wards.value = [];
      customerCash.value = 0;
      customerCashDisplay.value = '';
      discount.value = 0;
      tax.value = 0;
      paymentMethod.value = 'cash';
      voucherCode.value = '';
      voucherError.value = '';
      voucherSuccess.value = '';
      appliedVoucher.value = null;
      selectedVoucherId.value = '';
      
      // Xóa trạng thái đơn hàng hiện tại khỏi localStorage
      localStorage.removeItem('pos_current_order_state');
    }
    
    // Cập nhật localStorage
    persistPending();
    
    showAlert('Đã xóa đơn chờ ' + code, 'success');
  }
};

// Load data khi component mount
onMounted(async () => {
  await fetchFilters();
  await fetchProducts();
  await fetchCustomers();
  await fetchPaymentMethods();
  await loadDefaultAddress(); // Tải địa chỉ mặc định
  await loadProvinces(); // Load danh sách tỉnh/thành phố
  
  // Khôi phục trạng thái đơn hàng hiện tại từ localStorage
  restoreCurrentOrderState();
  
  // Load danh sách voucher có sẵn
  await fetchAvailableVouchers();

  // Thêm event listener cho phím Escape
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && showProductModal.value) {
      showProductModal.value = false;
    }
  });
});
</script>

<script>
export default {
  filters: {
    currency(val) {
      if (!val) return '0₫';
      return val.toLocaleString('vi-VN') + '₫';
    }
  }
}
</script>

<style scoped>
.card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.table-sm th,
.table-sm td {
  vertical-align: middle;
}

input[type="number"]::-webkit-inner-spin-button,
input[type="number"]::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.product-card {
  transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
}

.product-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}


.badge {
  font-size: 0.75em;
}

/* Style cho badge màu */
.badge[style*="background-color"] {
  border: 1px solid #dee2e6;
  min-width: 60px;
  text-align: center;
}

.text-truncate {
  max-width: 100%;
}

/* Phân trang */
.pagination {
  margin-bottom: 0;
}

.page-link {
  color: #6c757d;
  border: 1px solid #dee2e6;
  padding: 0.375rem 0.75rem;
  font-size: 0.875rem;
}

.page-link:hover {
  color: #495057;
  background-color: #e9ecef;
  border-color: #dee2e6;
}

.page-item.active .page-link {
  background-color: #0d6efd;
  border-color: #0d6efd;
  color: white;
}

.page-item.disabled .page-link {
  color: #6c757d;
  pointer-events: none;
  background-color: #fff;
  border-color: #dee2e6;
}

/* Loading animation */
.spinner-border {
  width: 2rem;
  height: 2rem;
}

/* Modal styles */
.modal {
  z-index: 1050;
  display: flex !important;
  align-items: center;
  justify-content: center;
}

.modal-backdrop {
  z-index: 1040;
  background-color: rgba(0, 0, 0, 0.5);
}

.modal-dialog {
  z-index: 1060;
  margin: 0 auto;
  max-width: 95vw;
  max-height: 95vh;
}

.modal-content {
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  max-height: 95vh;
}

.modal-header {
  border-bottom: 1px solid #dee2e6;
  background-color: #f8f9fa;
}

.modal-body {
  max-height: 70vh;
  overflow-y: auto;
}

/* XL modal styles */
.modal-xl .modal-content {
  border-radius: 12px;
  max-height: 90vh;
  width: 100%;
}

.modal-xl .modal-body {
  max-height: calc(90vh - 120px);
  padding: 1.5rem;
  overflow-y: auto;
}

.modal-xl .modal-header {
  padding: 1rem 1.5rem;
  border-radius: 12px 12px 0 0;
}

.modal-xl .modal-footer {
  padding: 1rem 1.5rem;
  border-radius: 0 0 12px 12px;
}

/* Đảm bảo modal luôn ở giữa màn hình */
.modal.show {
  display: flex !important;
  align-items: center;
  justify-content: center;
}

.modal-dialog-centered {
  display: flex;
  align-items: center;
  min-height: calc(100% - 1rem);
}

/* Body scroll khi modal mở */
body.modal-open {
  overflow: hidden;
}

/* Style cho QR code container */
.qr-code-container {
  padding: 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 12px;
  border: 2px dashed #dee2e6;
  margin-bottom: 15px;
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.qr-code-container:hover {
  border-color: #0d6efd;
  background: linear-gradient(135deg, #f0f8ff 0%, #e6f3ff 100%);
}

.qr-code-container img {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: transform 0.3s ease;
}

.qr-code-container img:hover {
  transform: scale(1.05);
}

/* Animation cho QR code loading */
.qr-code-container .spinner-border {
  width: 3rem;
  height: 3rem;
  border-width: 0.3em;
}

/* Style cho QR code error state */
.qr-code-container .fa-exclamation-triangle {
  color: #dc3545;
  opacity: 0.8;
}

/* Style cho QR code placeholder */
.qr-code-container .fa-qrcode {
  color: #6c757d;
  opacity: 0.5;
}

/* Style cho nút Tạo QR */
.btn-light {
  background-color: #ffffff !important;
  border-color: #ffffff !important;
  color:rgb(10, 10, 10) !important;
  font-weight: 600 !important;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1) !important;
  transition: all 0.3s ease !important;
}

.btn-light:hover:not(:disabled) {
  background-color: #f8f9fa !important;
  border-color: #f8f9fa !important;
  color: #0a58ca !important;
  transform: translateY(-1px) !important;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15) !important;
}

.btn-light:active:not(:disabled) {
  transform: translateY(0) !important;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1) !important;
}

.btn-light:disabled {
  background-color: #e9ecef !important;
  border-color: #e9ecef !important;
  color: #6c757d !important;
  opacity: 0.6 !important;
  cursor: not-allowed !important;
}

/* Style cho thông tin chuyển khoản */
.card.border-primary {
  border-width: 2px !important;
}

.card.border-primary .card-header {
  border-bottom: 2px solid #0d6efd;
}

/* Toast notification styles - chỉ 2 màu: xanh thành công, đỏ lỗi */
.toast {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  animation: slideInRight 0.3s ease-out;
  min-width: 300px;
  max-width: 400px;
}

.toast.bg-success {
  background: linear-gradient(135deg, #198754 ) !important;
  border-left: 4px solid #198754;
}

.toast.bg-danger {
  background: linear-gradient(135deg, #dc3545 ) !important;
  border-left: 4px solid #dc3545;
}

.toast.bg-warning {
  background: linear-gradient(135deg, #dc3545 ) !important;
  border-left: 4px solid #dc3545;
}

.toast.bg-info {
  background: linear-gradient(135deg, #198754 ) !important;
  border-left: 4px solid #198754;
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.toast-body {
  font-weight: 500;
  font-size: 14px;
  padding: 12px 16px;
}

.btn-close-white {
  filter: brightness(0) invert(1);
  opacity: 0.8;
}

.btn-close-white:hover {
  opacity: 1;
}

/* Animation cho voucher alerts */
.fadeInUp {
  animation: fadeInUp 0.3s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Style cho voucher input group */
.input-group .form-control:focus {
  border-color: #0d6efd;
  box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.25);
}

.input-group .btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Style cho voucher alerts - chỉ 2 màu: xanh thành công, đỏ lỗi */
.alert-success {
  border-left: 4px solid #198754;
}

.alert-danger {
  border-left: 4px solid #dc3545;
}

.alert-warning {
  border-left: 4px solid #dc3545;
}

.alert-info {
  border-left: 4px solid #198754;
}

/* Style cho badge khuyến mại */
.badge.bg-danger {
  background: linear-gradient(135deg, #dc3545, #c82333) !important;
  box-shadow: 0 2px 4px rgba(220, 53, 69, 0.3);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1);
  }
}

/* Style cho giá gạch ngang */
.text-decoration-line-through {
  text-decoration: line-through;
  opacity: 0.7;
}
</style>
