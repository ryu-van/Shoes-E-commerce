<template>
  <div class="py-4 px-4" style="width: 100%; margin-top: -20px">
    <h2 class="fw-extrabold mb-4">💬 Quản lý đánh giá</h2>

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

    <!-- Bộ lọc -->
    <div class="row gy-2 gx-3 align-items-end mb-3 flex-wrap">
      <div class="col-md-3">
        <input 
          type="text" 
          class="form-control" 
          placeholder="🔍 Tìm theo sản phẩm, người đánh giá..." 
          v-model="search"
        >
        </div>
      <div class="col-md-2">
        <input 
          type="date" 
          class="form-control" 
          v-model="filterDateFrom"
          title="Từ ngày"
        >
        </div>
      <div class="col-md-2">
        <input 
          type="date" 
          class="form-control" 
          v-model="filterDateTo"
          title="Đến ngày"
        >
              </div>
      <div class="col-md-2">
        <select class="form-select" v-model="filterStatus">
          <option value="">Tất cả trạng thái</option>
          <option value="show">Hiển thị</option>
          <option value="hide">Đã ẩn</option>
        </select>
                  </div>
      <div class="col-md-2">
        <select class="form-select" v-model="filterRating">
          <option value="">Tất cả điểm</option>
          <option v-for="n in 5" :key="n" :value="n">{{ n }} sao</option>
                    </select>
                  </div>
      <div class="col-md-1 d-flex justify-content-end">
        <button class="btn btn-secondary w-100" style="height: 38px" @click="resetFilter">
          Clear
                    </button>
                  </div>
                </div>

    <!-- Thống kê nhanh -->
    <div class="row mb-4">
      <div class="col-md-4">
        <div class="card stats-card">
          <div class="card-body">
            <div class="d-flex justify-content-between">
              <div>
                <h6>Tổng đánh giá</h6>
                <h4>{{ filteredReviews.length }}</h4>
              </div>
              <div class="text-primary">
                <i class="fas fa-comments fa-2x"></i>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card stats-card">
          <div class="card-body">
            <div class="d-flex justify-content-between">
              <div>
                <h6>Hiển thị</h6>
                <h4>{{ showReviews }}</h4>
              </div>
              <div class="text-success">
                <i class="fas fa-eye fa-2x"></i>
              </div>
            </div>
          </div>
                  </div>
                </div>
      <div class="col-md-4">
        <div class="card stats-card">
          <div class="card-body">
            <div class="d-flex justify-content-between">
              <div>
                <h6>Đã ẩn</h6>
                <h4>{{ hideReviews }}</h4>
                </div>
              <div class="text-danger">
                <i class="fas fa-eye-slash fa-2x"></i>
                    </div>
                      </div>
                      </div>
                    </div>
                  </div>
                </div>

    <!-- Đang tải -->
    <div v-if="loading" class="text-center my-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Đang tải...</span>
      </div>
    </div>

    <!-- không có dữ liệu -->
    <div v-else-if="!filteredReviews.length" class="text-center my-5">
      <div class="alert alert-info">
        <i class="fas fa-info-circle me-2"></i>
        Không có đánh giá nào để hiển thị.
      </div>
    </div>

    <!-- Bảng dữ liệu -->
    <div v-else style="margin-top: 32px" class="table-responsive">
      <table class="table table-hover table-bordered align-middle">
        <thead class="table-dark">
          <tr>
            <th class="text-center" style="width: 5%">STT</th>
            <th class="text-center">Sản phẩm</th>
            <th class="text-center">Người đánh giá</th>
            <th class="text-center">Điện thoại</th>
            <th class="text-center">Email</th>
            <th class="text-center">Nội dung</th>
            <th class="text-center">Điểm</th>
            <th class="text-center">Ngày tạo</th>
            <th class="text-center">Trạng thái</th>
            <th class="text-center" style="width: 15%">Hành Động</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(review, index) in paginatedReviews" :key="review.id">
            <td class="text-center">{{ index + 1 + (currentPage - 1) * itemsPerPage }}</td>
            <td>{{ review.productName }}</td>
            <td>{{ review.userFullName }}</td>
            <td>{{ review.userPhone }}</td>
            <td>{{ review.userEmail }}</td>
                         <td>
               <div class="content-preview" :title="review.content">
                 {{ truncateContent(review.content, 50) }}
               </div>
             </td>
            <td class="text-center">{{ review.rating }}</td>
            <td class="text-center">{{ formatDate(review.createdAt) }}</td>
            <td class="text-center">
              <span v-if="review.isHidden" class="badge bg-danger">Ẩn</span>
              <span v-else class="badge bg-success">Hiển thị</span>
            </td>
            <td class="text-center">
              <button class="btn btn-sm btn-warning me-1" @click="viewReview(review)">
                <i class="bi bi-eye"></i>Chi Tiết
              </button>
              <button class="btn btn-sm btn-success" @click="showToggleConfirm(review)">
                <i :class="review.isHidden ? 'bi bi-eye' : 'bi bi-eye-slash'"></i>
                {{ review.isHidden ? 'Hiện' : 'Ẩn' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Phân trang -->
    <div v-if="filteredReviews.length" class="mt-4">
                  <div class="pagination-wrapper">
                    <div class="page-size-selector">
                      <span class="me-2 text-muted">Hiển thị</span>
                      <select class="form-select form-select-sm me-2 custom-select" v-model="itemsPerPage">
                        <option v-for="size in pageSizeOptions" :key="size" :value="size">
                          {{ size }}
                        </option>
                      </select>
          <span class="text-muted">đánh giá</span>
                    </div>
                    <div class="pagination-controls">
                      <nav aria-label="Page navigation">
                        <ul class="pagination pagination-sm mb-0 custom-pagination">
                          <li class="page-item" :class="{ disabled: currentPage === 1 }">
                            <button class="page-link custom-page-link" @click="changePage(currentPage - 1)" :disabled="currentPage === 1">
                              «
                            </button>
                          </li>
                          <li v-for="page in visiblePages" :key="page" class="page-item" :class="{active: page === currentPage, disabled: page === '...'}">
                            <button
                                v-if="page !== '...'"
                                class="page-link custom-page-link"
                                :class="{ 'active-page': page === currentPage }"
                                @click="changePage(page)"
                            >
                              {{ page }}
                            </button>
                            <span v-else class="page-link custom-page-link disabled">...</span>
                          </li>
                          <li class="page-item" :class="{ disabled: currentPage === totalPages }">
                            <button class="page-link custom-page-link" @click="changePage(currentPage + 1)" :disabled="currentPage === totalPages">
                              »
                            </button>
                          </li>
                        </ul>
                      </nav>
                    </div>
                    <div class="pagination-info">
                      <span class="text-muted">
            Hiển thị {{ paginationInfo.start }} - {{ paginationInfo.end }} trong tổng số {{ paginationInfo.total }} đánh giá
                      </span>
                    </div>
                  </div>
                </div>

        <!-- Modal chi tiết đánh giá -->
        <div v-if="selectedReview" class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5);">
          <div class="modal-dialog modal-lg">
            <div class="modal-content">
              <div class="modal-header">
            <h5 class="modal-title">Chi tiết đánh giá</h5>
                <button type="button" class="btn-close" @click="selectedReview = null"></button>
              </div>
              <div class="modal-body">
            <p><strong>Sản phẩm:</strong> {{ selectedReview.productName }}</p>
            <p><strong>Người đánh giá:</strong> {{ selectedReview.userFullName }}</p>
            <p><strong>Điện thoại:</strong> {{ selectedReview.userPhone }}</p>
            <p><strong>Email:</strong> {{ selectedReview.userEmail }}</p>
                         <div class="mb-3">
               <label class="form-label"><strong>Nội dung:</strong></label>
               <textarea v-model="selectedReview.content" class="form-control" rows="4" disabled></textarea>
             </div>
             <p><strong>Điểm đánh giá:</strong> {{ selectedReview.rating || 'N/A' }} sao</p>
             <p><strong>Ngày tạo:</strong> {{ formatDate(selectedReview.createdAt) }}</p>
             <p><strong>Trạng thái:</strong> <span v-if="selectedReview.isHidden" class="badge bg-danger">Ẩn</span><span v-else class="badge bg-success">Hiển thị</span></p>
            <hr>
            <div class="mb-3">
              <label class="form-label"><strong>Phản hồi đánh giá:</strong></label>
              <textarea v-model="replyContent" class="form-control" rows="3" placeholder="Nhập phản hồi..."></textarea>
              <button class="btn btn-primary mt-2" @click="sendReply(selectedReview)" :disabled="replying">
                      {{ replying ? 'Đang gửi...' : 'Gửi phản hồi' }}
                    </button>
                  </div>
                         <div v-if="selectedReview.replies && selectedReview.replies.length > 0">
               <h6>Danh sách phản hồi:</h6>
               <div class="replies-list">
                 <div v-for="reply in selectedReview.replies" :key="reply.id" class="reply-item">
                   <div class="reply-header">
                     <strong>{{ reply.userFullName || reply.userFullname || reply.user?.fullname || reply.user?.fullName || 'Admin' }}</strong>
                     <span class="text-muted ms-2">({{ formatDate(reply.createdAt) }})</span>
                   </div>
                   <div class="reply-content">{{ reply.content }}</div>
                 </div>
               </div>
             </div>
             <div v-else>
               <p class="text-muted">Chưa có phản hồi nào cho đánh giá này.</p>
             </div>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-secondary" @click="selectedReview = null">Đóng</button>
              </div>
            </div>
                      </div>
                    </div>
                     
    <!-- Thông báo -->
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
  </div>
</template>

<script>
import { getAllReviewInfo, updateReview, replyToReview, getReviewInfoById } from '@/service/ReviewApi.js';
import ConfirmModal from '@/components/ConfirmModal.vue';

export default {
  name: 'ReviewManagement',
  components: {
    ConfirmModal
  },
  data() {
    return {
      reviews: [],
      loading: false,
      search: '',
      filterDateFrom: '',
      filterDateTo: '',
      filterStatus: '',
      filterRating: '',
      selectedReview: null,
      selectedProductId: '',
      notification: { message: '' },
      currentPage: 1,
      itemsPerPage: 5,
      pageSizeOptions: [5, 10, 15, 20, 25],
      toastMessage: '',
      toastType: 'success',
      replyContent: '',
      replying: false,
      // ConfirmModal data
      showConfirmModal: false,
      confirmModalTitle: '',
      confirmModalMessage: '',
      confirmModalType: 'warning',
      confirmModalConfirmText: 'Xác nhận',
      confirmModalCancelText: 'Hủy',
      confirmModalLoading: false,
      pendingAction: null,
      pendingReview: null,
    };
  },
  computed: {
    filteredReviews() {
      let reviews = this.reviews;
      if (this.search) {
        const normalize = str => str ? str.toString().toLowerCase().replace(/\s+/g, '') : '';
        const searchLower = normalize(this.search);
        reviews = reviews.filter(r =>
          normalize(r.productName).includes(searchLower) ||
          normalize(r.userFullName).includes(searchLower)
        );
      }
      if (this.filterStatus) {
        if (this.filterStatus === 'show') reviews = reviews.filter(r => !r.isHidden);
        if (this.filterStatus === 'hide') reviews = reviews.filter(r => r.isHidden);
      }
      if (this.filterRating) {
        reviews = reviews.filter(r => r.rating == this.filterRating);
      }
      if (this.filterDateFrom) {
        reviews = reviews.filter(r => new Date(r.createdAt) >= new Date(this.filterDateFrom));
      }
      if (this.filterDateTo) {
        reviews = reviews.filter(r => new Date(r.createdAt) <= new Date(this.filterDateTo));
      }
      return reviews.slice().sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    },
    paginatedReviews() {
      const start = (this.currentPage - 1) * this.itemsPerPage;
      const end = start + this.itemsPerPage;
      return this.filteredReviews.slice(start, end);
    },
    totalPages() {
      return Math.ceil(this.filteredReviews.length / this.itemsPerPage) || 1;
    },
    paginationInfo() {
      const start = (this.currentPage - 1) * this.itemsPerPage + 1;
      const end = Math.min(this.currentPage * this.itemsPerPage, this.filteredReviews.length);
      return {
        start,
        end,
        total: this.filteredReviews.length
      };
    },
    visiblePages() {
      const current = this.currentPage;
      const total = this.totalPages;
      const pages = [];
      if (current > 3) {
        pages.push(1);
        if (current > 4) {
          pages.push('...');
        }
      }
      const start = Math.max(1, current - 2);
      const end = Math.min(total, current + 2);
      for (let i = start; i <= end; i++) {
        if (!pages.includes(i)) {
          pages.push(i);
        }
      }
      if (current < total - 2) {
        if (current < total - 3) {
          pages.push('...');
        }
        if (!pages.includes(total)) {
          pages.push(total);
        }
      }
      return pages;
    },
    showReviews() {
      return this.filteredReviews.filter(r => !r.isHidden).length;
    },
    hideReviews() {
      return this.filteredReviews.filter(r => r.isHidden).length;
    },
    avgRating() {
      if (!this.filteredReviews.length) return 0;
      const sum = this.filteredReviews.reduce((acc, r) => acc + (r.rating || 0), 0);
      return (sum / this.filteredReviews.length).toFixed(2);
    },
  },
  watch: {
    itemsPerPage() {
      this.currentPage = 1;
    },
  },
  methods: {
    async fetchAllReviews() {
      this.loading = true;
      try {
        const response = await getAllReviewInfo();
        this.reviews = response.data || [];
      } catch (error) {
        this.showAlert('Lỗi khi tải đánh giá: ' + error.message, 'error');
      } finally {
        this.loading = false;
      }
    },
         formatDate(dateString) {
       if (!dateString) return 'N/A';
       try {
         const date = new Date(dateString);
         if (isNaN(date.getTime())) return 'N/A';
         return date.toLocaleDateString('vi-VN');
       } catch (error) {
         return 'N/A';
       }
     },
     truncateContent(content, maxLength = 50) {
       if (!content) return 'N/A';
       if (content.length <= maxLength) return content;
       return content.substring(0, maxLength) + '...';
     },
    showAlert(message, type = 'success') {
      this.toastMessage = message;
      this.toastType = type;
      this.$nextTick(() => {
        const toast = window.bootstrap ? new window.bootstrap.Toast(this.$refs.toastRef) : null;
        if (toast) toast.show();
      });
    },
    hideToast() {
      const toast = window.bootstrap ? new window.bootstrap.Toast(this.$refs.toastRef) : null;
      if (toast) toast.hide();
      this.toastMessage = '';
    },
    resetFilter() {
      this.search = '';
      this.filterDateFrom = '';
      this.filterDateTo = '';
      this.filterStatus = '';
      this.filterRating = '';
      this.currentPage = 1;
    },
    changePage(page) {
      if (page >= 1 && page <= this.totalPages && page !== '...') {
        this.currentPage = page;
      }
    },
         async viewReview(review) {
       try {
         console.log('Đang lấy chi tiết review với ID:', review.id);
         const res = await getReviewInfoById(review.id);
         console.log('Response từ API:', res);
         
         if (res && res.data) {
           this.selectedReview = res.data;
           console.log('Đã set selectedReview:', this.selectedReview);
         } else {
           // Nếu không có data từ API, sử dụng dữ liệu hiện tại
           this.selectedReview = {
             ...review,
             replies: [] // Thêm mảng replies rỗng nếu chưa có
           };
           console.log('Sử dụng dữ liệu hiện tại:', this.selectedReview);
         }
       } catch (error) {
         console.error('Lỗi chi tiết:', error);
         // Nếu API lỗi, vẫn hiển thị modal với dữ liệu hiện tại
         this.selectedReview = {
           ...review,
           replies: []
         };
         this.showAlert('Không thể lấy thông tin chi tiết từ server, hiển thị dữ liệu hiện tại', 'warning');
       }
     },
    showToggleConfirm(review) {
      this.pendingReview = review;
      this.pendingAction = 'toggle';
      
      if (review.isHidden) {
        // Hiện bình luận
        this.confirmModalTitle = 'Hiển thị bình luận';
        this.confirmModalMessage = `Bạn có chắc chắn muốn hiển thị bình luận của "${review.userFullName}" cho sản phẩm "${review.productName}"?`;
        this.confirmModalType = 'success';
        this.confirmModalConfirmText = 'Hiển thị';
        this.confirmModalCancelText = 'Hủy';
      } else {
        // Ẩn bình luận
        this.confirmModalTitle = 'Ẩn bình luận';
        this.confirmModalMessage = `Bạn có chắc chắn muốn ẩn bình luận của "${review.userFullName}" cho sản phẩm "${review.productName}"?`;
        this.confirmModalType = 'danger';
        this.confirmModalConfirmText = 'Ẩn';
        this.confirmModalCancelText = 'Hủy';
      }
      
      this.showConfirmModal = true;
    },

    async handleConfirmAction() {
      if (this.pendingAction === 'toggle' && this.pendingReview) {
        this.confirmModalLoading = true;
        try {
          await updateReview({
            id: this.pendingReview.id,
            content: this.pendingReview.content,
            isHidden: !this.pendingReview.isHidden,
            rating: this.pendingReview.rating,
          });
          
          const actionText = this.pendingReview.isHidden ? 'hiển thị' : 'ẩn';
          this.showAlert(`Đã ${actionText} bình luận thành công!`, 'success');
          this.fetchAllReviews();
        } catch (error) {
          this.showAlert('Lỗi khi cập nhật trạng thái: ' + error.message, 'error');
        } finally {
          this.confirmModalLoading = false;
          this.hideConfirmModal();
        }
      }
    },

    hideConfirmModal() {
      this.showConfirmModal = false;
      this.pendingAction = null;
      this.pendingReview = null;
    },

    async toggleReviewStatus(review) {
      try {
        await updateReview({
          id: review.id,
          content: review.content,
          isHidden: !review.isHidden,
          rating: review.rating,
        });
        this.showAlert('Cập nhật trạng thái thành công!', 'success');
        this.fetchAllReviews();
      } catch (error) {
        this.showAlert('Lỗi khi cập nhật trạng thái: ' + error.message, 'error');
      }
    },
    async sendReply(review) {
      if (!this.replyContent.trim()) {
        this.showAlert('Vui lòng nhập nội dung phản hồi!', 'warning');
        return;
      }
      this.replying = true;
      try {
        const adminId = JSON.parse(localStorage.getItem('user')).id;
        await replyToReview(review.id, adminId, this.replyContent);
        this.replyContent = '';
        this.showAlert('Phản hồi thành công!', 'success');
        this.selectedReview = null;
        this.fetchAllReviews();
      } catch (error) {
        this.showAlert('Lỗi khi gửi phản hồi: ' + (error.response?.data?.message || error.message), 'error');
      } finally {
        this.replying = false;
      }
    },
    async exportReviews() {
      // Placeholder: bạn có thể tích hợp xuất file Excel ở đây
      this.showAlert('Chức năng xuất file sẽ được phát triển!', 'info');
    },
  },
  mounted() {
    this.fetchAllReviews();
  },
};
</script>

<style scoped>
/* Copy toàn bộ style từ InvoiceManagement.vue để đồng bộ giao diện */
.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
  border-top: 1px solid #e9ecef;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 12px;
  margin-top: 20px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  animation: fadeIn 0.3s ease-out;
}
.page-size-selector {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #6c757d;
}
.custom-select {
  width: 80px !important;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  transition: all 0.3s ease;
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
.custom-select:focus {
  border-color: #212529;
  box-shadow: 0 0 0 3px rgba(33, 37, 41, 0.1);
}
.pagination-controls {
  flex: 1;
  display: flex;
  justify-content: center;
  max-width: 600px;
}
.custom-pagination {
  gap: 4px;
}
.custom-page-link {
  border: 2px solid #e9ecef;
  color: #495057;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
  font-weight: 500;
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  min-width: 40px;
  text-align: center;
}
.custom-page-link:hover:not(:disabled):not(.disabled) {
  background: #212529;
  border-color: #212529;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(33, 37, 41, 0.3);
}
.custom-page-link:disabled {
  background: #f8f9fa;
  border-color: #e9ecef;
  color: #adb5bd;
  cursor: not-allowed;
}
.active-page {
  background: linear-gradient(135deg, #212529 0%, #000000 100%) !important;
  border-color: #212529 !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(33, 37, 41, 0.4) !important;
  transform: translateY(-1px);
}
.page-item.active .custom-page-link {
  background: linear-gradient(135deg, #212529 0%, #000000 100%) !important;
  border-color: #212529 !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(33, 37, 41, 0.4) !important;
}
.pagination-info {
  font-size: 14px;
  color: #6c757d;
  font-weight: 500;
}
.table-responsive {
  margin-top: 32px;
}
.table {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  animation: fadeIn 0.3s ease-out;
}
.table th {
  background-color: #212529 !important;
  color: #ffffff !important;
  font-weight: 600;
  border: none;
  padding: 12px 8px;
  text-align: center;
  vertical-align: middle;
}
.table td {
  padding: 12px 8px;
  vertical-align: middle;
  border-top: 1px solid #dee2e6;
  text-align: center;
}
.table tbody tr:hover {
  background-color: #f8f9fa;
}
.stats-card {
  border-left: 4px solid #007bff;
  transition: transform 0.2s;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border: 1px solid #e9ecef;
}
.stats-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
.stats-card .card-body {
  padding: 1.25rem;
}
.btn-group .btn {
  margin-right: 2px;
  border-radius: 6px;
}
.btn-sm {
  padding: 4px 8px;
  font-size: 12px;
  border-radius: 4px;
}
.btn-info {
  background-color: #0dcaf0;
  border-color: #0dcaf0;
  color: #000;
}
.badge {
  font-size: 13px;
  padding: 5px 10px;
  border-radius: 20px;
}
.bg-success {
  background-color: #198754 !important;
}
.bg-danger {
  background-color: #dc3545 !important;
}
.form-control, .form-select {
  border: 1px solid #ced4da;
  border-radius: 6px;
  transition: border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out;
}
 .form-control:focus, .form-select:focus {
   border-color: #86b7fe;
   outline: 0;
   box-shadow: 0 0 0 0.25rem rgba(13, 110, 253, 0.25);
 }
 .content-preview {
   max-width: 200px;
   overflow: hidden;
   text-overflow: ellipsis;
   white-space: nowrap;
   cursor: help;
   transition: all 0.3s ease;
 }
 .content-preview:hover {
   background-color: #f8f9fa;
   border-radius: 4px;
   padding: 2px 4px;
 }
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
@media (max-width: 768px) {
  .pagination-wrapper {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
  .pagination-controls {
    order: 1;
  }
  .page-size-selector {
    order: 2;
    justify-content: center;
  }
  .pagination-info {
    order: 3;
    text-align: center;
  }
  .modal-dialog {
    max-width: 95%;
  }
  .modal-body .table th,
  .modal-body .table td {
    min-width: 80px;
    font-size: 14px;
  }
  .pagination {
    font-size: 14px;
  }
  .page-link {
    padding: 5px 10px;
  }
  .table th,
  .table td {
    font-size: 12px;
    padding: 8px 4px;
  }
  .btn-group .btn {
    margin-bottom: 4px;
  }
}
</style>