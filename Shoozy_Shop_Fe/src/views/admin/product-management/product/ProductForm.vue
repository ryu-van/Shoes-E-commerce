<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getActiveCategories } from '@/service/CategoryApi.js';
import { getActiveBrands } from '@/service/BrandApi.js';
import { getActiveSizes } from '@/service/SizeApi.js';
import { getActiveColors } from '@/service/ColorApi.js';
import { getActiveMaterials } from '@/service/MaterialApi.js';
import { createProduct, getProductById, updateProduct } from '@/service/ProductApi.js';
import { createProductVariant, deleteProductVariant, updateProductVariant } from '@/service/ProductVariantApi.js';
import axios from "axios";
import ShowToastComponent from "@/components/ShowToastComponent.vue";
import {
  deleteProductVariantImage,
  getImagesByColor,
  getImagesByIds,
  uploadImagesForVariants, uploadImagesOnly, validateImageFile
} from "@/service/ImageApi.js";

const route = useRoute();
const router = useRouter();
const productId = ref(null);
const isProductCreated = ref(false);
const isSaving = ref(false);
const isEditMode = ref(false);
const productVariants = ref([]);

const categories = ref([]);
const brands = ref([]);
const sizes = ref([]);
const colors = ref([]);
const materials = ref([]);

const variantImages = ref({});
const sameColorImages = ref([]);

const imageUploadModalRef = ref(null);
const currentColorName = ref('');
const currentColorInfo = ref(null);
const currentVariantIds = ref([]);

const uploadingImages = ref(false);
const selectedImageIds = ref([]);
const uploadFiles = ref([]);
const fileInputRef = ref(null);
const uploadedImages = ref([]);
const newUploadFiles = ref([]);
const isUploadingNewFiles = ref(false);

const toastRef = ref(null);

const showToast = (message, type) => {
  toastRef.value?.showToast(message, type);
};

// Thêm computed property để lấy ảnh hiện tại của color
const currentColorImages = computed(() => {
  if (!currentColorName.value || !variantImages.value[currentColorName.value]) {
    return [];
  }
  return variantImages.value[currentColorName.value].map(img => ({
    id: img.id,
    url: img.imageUrl,
    name: img.imageName,
    isExisting: true
  }));
});

// Computed property để lọc ảnh cùng màu, loại bỏ ảnh đã có
const filteredSameColorImages = computed(() => {
  if (!sameColorImages.value || !currentColorImages.value) {
    return sameColorImages.value;
  }

  const existingImageIds = new Set(currentColorImages.value.map(img => img.id));
  return sameColorImages.value.filter(img => !existingImageIds.has(img.id));
});

// Load images by color
const loadImagesByColor = async (colorInfo) => {
  try {
    const response = await getImagesByColor(colorInfo.id);
    sameColorImages.value = response.data.data.map(img => ({
      id: img.imageId,
      url: img.imageUrl,
      name: img.imageUrl ? decodeURIComponent(img.imageUrl.split('/').pop()) : 'Unknown',
      selected: false,
      isNew: false
    }));
  } catch (error) {
    console.error('Lỗi khi tải ảnh theo màu:', error);
    showToast('Có lỗi xảy ra khi tải ảnh theo màu', 'error');
  }
};

// Enhanced showImageUploadModal
const enhanced_showImageUploadModal = async (colorName, colorInfo) => {
  currentColorName.value = colorName;
  currentColorInfo.value = colorInfo;
  currentVariantIds.value = groupedVariantsByColor.value[colorName].map(variant => variant.id);
  selectedImageIds.value = [];
  uploadFiles.value = [];
  uploadedImages.value = [];
  selectedImages.value = [];
  sameColorImages.value = [];
  await loadImagesByColor(colorInfo);
  const modalElement = imageUploadModalRef.value;
  if (modalElement) {
    new bootstrap.Modal(modalElement).show();
  }
};

const hideImageUploadModal = () => {
  const modalElement = imageUploadModalRef.value;
  if (modalElement) {
    const modalInstance = bootstrap.Modal.getInstance(modalElement);
    if (modalInstance) {
      modalInstance.hide();
    }
  }

  // Reset tất cả state
  currentColorName.value = '';
  currentColorInfo.value = null;
  currentVariantIds.value = [];
  selectedImageIds.value = [];
  uploadFiles.value = [];
  uploadedImages.value = [];
  newUploadFiles.value = [];

  // Reset trạng thái selected của tất cả ảnh
  sameColorImages.value.forEach(img => {
    img.selected = false;
  });
  sameColorImages.value = [];
};

// Hàm toggle chọn ảnh
const toggleImageSelection = (imageId) => {
  const imageItem = filteredSameColorImages.value.find(img => img.id === imageId);
  if (!imageItem) return;

  // Nếu đang bỏ chọn thì không cần kiểm tra giới hạn
  if (imageItem.selected) {
    imageItem.selected = false;
    // Xóa khỏi selectedImageIds
    const selectedIdIndex = selectedImageIds.value.indexOf(imageId);
    if (selectedIdIndex > -1) {
      selectedImageIds.value.splice(selectedIdIndex, 1);
    }
    return;
  }

  // Kiểm tra giới hạn khi chọn mới
  const maxImages = currentVariantIds.value.reduce((max, variantId) => {
    const imageCount = (variantImages.value[currentColorName.value] || []).filter(img => img.variantId === variantId).length;
    return Math.max(max, imageCount);
  }, 0);

  // Đếm số ảnh đã chọn hiện tại
  const currentSelectedCount = filteredSameColorImages.value.filter(img => img.selected).length;
  const totalAfterSelection = maxImages + currentSelectedCount + 1; // +1 cho ảnh sắp chọn

  if (totalAfterSelection > 5) {
    showToast('Mỗi biến thể chỉ được phép có tối đa 5 ảnh', 'warning');
    return;
  }

  // Chọn ảnh
  imageItem.selected = true;
  if (!selectedImageIds.value.includes(imageId)) {
    selectedImageIds.value.push(imageId);
  }
};

// Sửa lại computed selectedImages để đảm bảo đồng bộ
const selectedImages = computed(() => {
  return filteredSameColorImages.value.filter(img => img.selected);
});

// Sửa lại hàm saveImageChanges để lấy đúng danh sách ảnh đã chọn
const saveImageChanges = async () => {
  const selectedImageIds = filteredSameColorImages.value
      .filter(img => img.selected)
      .map(img => img.id);

  if (selectedImageIds.length === 0) {
    showToast('Vui lòng chọn ít nhất một ảnh', 'warning');
    return;
  }

  // Kiểm tra giới hạn cuối cùng
  const maxImages = currentVariantIds.value.reduce((max, variantId) => {
    const imageCount = (variantImages.value[currentColorName.value] || []).filter(img => img.variantId === variantId).length;
    return Math.max(max, imageCount);
  }, 0);

  const newTotalImages = maxImages + selectedImageIds.length;
  if (newTotalImages > 5) {
    showToast('Mỗi biến thể chỉ được phép có tối đa 5 ảnh', 'warning');
    return;
  }

  uploadingImages.value = true;

  try {
    await uploadImagesForVariants(selectedImageIds, currentVariantIds.value);
    showToast('Thêm ảnh thành công!', 'success');
    hideImageUploadModal();
    await fetchProduct(productId.value);
  } catch (error) {
    console.error('Lỗi khi gán ảnh:', error);
    const errorMessage = error.response?.data.data?.message || 'Có lỗi xảy ra khi thêm ảnh';
    showToast(errorMessage, 'error');
  } finally {
    uploadingImages.value = false;
  }
};

// Cập nhật hàm handleFileSelection để xử lý upload ngay
const handleFileSelection = async (event) => {
  const files = Array.from(event.target.files);

  if (files.length === 0) return;

  // Validate files trước khi upload
  const validFiles = [];
  for (const file of files) {
    try {
      validateImageFile(file);
      validFiles.push(file);
    } catch (error) {
      showToast(`${error.message}`, 'error');
    }
  }

  if (validFiles.length === 0) {
    event.target.value = '';
    return;
  }

  // Check số lượng ảnh hiện tại
  const maxImages = currentVariantIds.value.reduce((max, variantId) => {
    const imageCount = (variantImages.value[currentColorName.value] || []).filter(img => img.variantId === variantId).length;
    return Math.max(max, imageCount);
  }, 0);

  const currentTotal = maxImages + selectedImages.value.length + uploadedImages.value.length;
  const availableSlots = 5 - currentTotal;

  if (availableSlots <= 0) {
    showToast('Mỗi biến thể chỉ được phép có tối đa 5 ảnh', 'warning');
    event.target.value = '';
    return;
  }

  const filesToUpload = validFiles.slice(0, availableSlots);

  if (filesToUpload.length < validFiles.length) {
    showToast(`Chỉ có thể upload ${filesToUpload.length} ảnh do giới hạn tối đa 5 ảnh`, 'warning');
  }

  await uploadNewFiles(filesToUpload);
  event.target.value = '';
};

// Hàm upload file mới lên hệ thống
const uploadNewFiles = async (files) => {
  debugger
  if (files.length === 0) return;
  isUploadingNewFiles.value = true;
  try {
    // Upload files lên server trước
    const response = await uploadImagesOnly(files);
    // Thêm ảnh mới vào danh sách để chọn
    const newImages = response.data.data.map(img => ({
      id: img.id,
      url: img.imageUrl,
      name: img.imageName || decodeURIComponent(img.imageUrl.split('/').pop()),
      selected: false,
      isNew: true
    }));

    console.log('Ảnh mới đã upload:', newImages);

    // Thêm vào danh sách ảnh cùng màu
    sameColorImages.value = [...newImages, ...sameColorImages.value];

    showToast(`Upload thành công ${newImages.length} ảnh!`, 'success');

  } catch (error) {
    console.error('Lỗi khi upload ảnh:', error);
    const errorMessage = error.response?.data.data?.message || 'Có lỗi xảy ra khi upload ảnh';
    showToast(errorMessage, 'error');
  } finally {
    isUploadingNewFiles.value = false;
  }
};

// Cập nhật allDisplayImages để hiển thị ảnh hiện tại + ảnh đã chọn
const allDisplayImages = computed(() => {
  const existing = currentColorImages.value || [];
  const selected = selectedImages.value || [];
  return [...existing, ...selected];
});

const errors = ref({});
const errorsVariant = ref({});

const editVariantModalRef = ref(null);
const editVariantForm = ref({
  id: null,
  costPrice: '',
  sellPrice: '',
  quantity: ''
});

const editVariantErrors = ref({});

const productForm = ref({
  name: '',
  brandId: null,
  categoryId: null,
  materialId: null,
  description: '',
  gender: null,
  weight: '',
  status: ''
});


const variantModalRef = ref(null);
const variantForm = ref({
  product_id: null,
  size_ids: [],
  color_ids: [],
  costPrice: 0,
  sellPrice: 0,
  quantity: 1
});

const validateProductForm = () => {
  errors.value = {};

  // Validate tên sản phẩm
  if (!productForm.value.name) {
    errors.value.name = 'Tên sản phẩm không được để trống';
  } else if (productForm.value.name.length > 255) {
    errors.value.name = 'Tên sản phẩm không được dài quá 255 ký tự';
  } else {
    // Kiểm tra ký tự đặc biệt - chỉ cho phép chữ cái, số, khoảng trắng, dấu gạch ngang và dấu gạch dưới
    const specialCharRegex = /^[a-zA-ZÀ-ỹ0-9\s\-_]+$/;
    if (!specialCharRegex.test(productForm.value.name)) {
      errors.value.name = 'Tên sản phẩm chỉ được chứa chữ cái, số, khoảng trắng, dấu gạch ngang (-) và dấu gạch dưới (_)';
    }
  }

  if (!productForm.value.brandId) errors.value.brandId = 'Vui lòng chọn thương hiệu';
  if (!productForm.value.categoryId) errors.value.categoryId = 'Vui lòng chọn danh mục';
  if (!productForm.value.materialId) errors.value.materialId = 'Vui lòng chọn chất liệu';
  if (!productForm.value.gender) errors.value.gender = 'Vui lòng chọn giới tính';
  if (!productForm.value.description) errors.value.description = 'Mô tả không được để trống';

  // Validate weight
  if (!productForm.value.weight) {
    errors.value.weight = 'Trọng lượng không được để trống';
  } else if (Number(productForm.value.weight) <= 0) {
    errors.value.weight = 'Trọng lượng phải lớn hơn 0';
  } else if (Number(productForm.value.weight) > 10) {
    errors.value.weight = 'Trọng lượng không được vượt quá 10kg';
  }

  return Object.keys(errors.value).length === 0;
};

const validateVariantForm = () => {
  const validationErrors = {};
  if (!variantForm.value.size_ids.length) {
    validationErrors.size_ids = 'Vui lòng chọn ít nhất một kích thước';
  }
  if (!variantForm.value.color_ids.length) {
    validationErrors.color_ids = 'Vui lòng chọn ít nhất một màu sắc';
  }
  errorsVariant.value = validationErrors;
  return Object.keys(validationErrors).length === 0;
};

const validateEditVariantForm = () => {
  const validationErrors = {};

  // Validate cost price
  if (editVariantForm.value.costPrice === null || editVariantForm.value.costPrice === '') {
    validationErrors.costPrice = 'Giá nhập không được để trống';
  } else if (Number(editVariantForm.value.costPrice) < 0) {
    validationErrors.costPrice = 'Giá nhập phải là số ≥ 0';
  } else if (Number(editVariantForm.value.costPrice) > 1000000000) {
    validationErrors.costPrice = 'Giá nhập không được vượt quá 1 tỷ';
  }

  // Validate sell price
  if (editVariantForm.value.sellPrice === null || editVariantForm.value.sellPrice === '') {
    validationErrors.sellPrice = 'Giá bán không được để trống';
  } else if (Number(editVariantForm.value.sellPrice) < 0) {
    validationErrors.sellPrice = 'Giá bán phải là số ≥ 0';
  } else if (Number(editVariantForm.value.sellPrice) > 1000000000) {
    validationErrors.sellPrice = 'Giá bán không được vượt quá 1 tỷ';
  }

  // Validate quantity
  if (editVariantForm.value.quantity === null || editVariantForm.value.quantity === '') {
    validationErrors.quantity = 'Số lượng không được để trống';
  } else if (Number(editVariantForm.value.quantity) < 0) {
    validationErrors.quantity = 'Số lượng phải là số ≥ 0';
  } else if (Number(editVariantForm.value.quantity) > 1000000) {
    validationErrors.quantity = 'Số lượng không được vượt quá 1 triệu';
  }

  // Optional: Validate cost price vs sell price
  if (Number(editVariantForm.value.costPrice) > Number(editVariantForm.value.sellPrice)) {
    validationErrors.sellPrice = 'Giá bán phải lớn hơn hoặc bằng giá nhập';
  }

  editVariantErrors.value = validationErrors;
  return Object.keys(validationErrors).length === 0;
};

const fetchDataFilters = async () => {
  try {
    const [catRes, brandRes, materialRes] = await Promise.all([getActiveCategories(), getActiveBrands(), getActiveMaterials()]);
    categories.value = catRes.data.data;
    brands.value = brandRes.data.data;
    materials.value = materialRes.data.data;
  } catch (error) {
    console.error('Lỗi tải danh mục hoặc thương hiệu:', error);
    showToast('Có lỗi xảy ra khi tải dữ liệu danh mục và thương hiệu', 'error');
  }
};

const fetchVariantOptions = async () => {
  try {
    const [sizeRes, colorRes] = await Promise.all([
      getActiveSizes(), getActiveColors()
    ]);
    sizes.value = sizeRes.data.data;
    colors.value = colorRes.data.data;
  } catch (error) {
    console.error('Lỗi tải dữ liệu biến thể:', error);
    showToast('Có lỗi xảy ra khi tải dữ liệu biến thể', 'error');
  }
};

const fetchProduct = async (id) => {
  try {
    const res = await getProductById(id);
    const data = res.data.data;
    productForm.value.name = data.name;
    productForm.value.brandId = data.brand.id;
    productForm.value.categoryId = data.category.id;
    productForm.value.description = data.description;
    productForm.value.materialId = data.material.id;
    productForm.value.status = data.status;
    productForm.value.gender = data.gender;
    productForm.value.weight = data.weight;
    isProductCreated.value = true;
    isEditMode.value = true;
    productVariants.value = data.variants;
    loadAllVariantImages();
  } catch (error) {
    console.error('Lỗi tải sản phẩm: ', error);
    showToast('Có lỗi xảy ra khi tải thông tin sản phẩm', 'error');
  }
};

const groupedVariantsByColor = computed(() => {
  const groups = {};
  productVariants.value.forEach(variant => {
    const colorName = variant.color?.name || 'Không xác định';
    if (!groups[colorName]) {
      groups[colorName] = [];
    }
    groups[colorName].push(variant);
  });
  return groups;
});

const loadAllVariantImages = () => {
  try {
    const grouped = groupedVariantsByColor.value;
    variantImages.value = {};

    Object.entries(grouped).forEach(([colorName, variants]) => {
      const allImages = [];
      const uniqueImages = new Set();

      variants.forEach(variant => {
        if (variant.images && Array.isArray(variant.images)) {
          variant.images.forEach((imageObj) => {
            const imageUrl = imageObj.url;
            if (!uniqueImages.has(imageUrl)) {
              uniqueImages.add(imageUrl);
              allImages.push({
                id: imageObj.id,
                imageUrl,
                imageName: decodeURIComponent(imageUrl.split('/').pop()),
                variantId: variant.id
              });
            }
          });
        }
      });

      variantImages.value[colorName] = allImages.slice(0, 5 * variants.length); // Limit to 5 images per variant
    });
  } catch (error) {
    console.error('Lỗi tải ảnh variants:', error);
    showToast('Có lỗi xảy ra khi tải ảnh sản phẩm', 'error');
  }
};

const isProductNameExisted = async (name) => {
  try {
    const res = await axios.get('http://localhost:8080/api/v1/products/check-name', {params: {name}});
    return res.data.data === true;
  } catch (error) {
    console.error('Lỗi kiểm tra tên sản phẩm:', error);
    return false;
  }
};

const deleteVariant = async (variantId) => {
  if (!confirm('Bạn có chắc chắn muốn xóa biến thể này?')) {
    return;
  }

  try {
    await deleteProductVariant(variantId);
    showToast('Xóa biến thể thành công!', 'success');
    await fetchProduct(productId.value);
  } catch (error) {
    console.error('Lỗi xóa biến thể:', error);
    const errorMessage = error.response?.data.data?.message || 'Có lỗi xảy ra khi xóa biến thể';
    showToast(errorMessage, 'error');
  }
};

const submitForm = async () => {
  errors.value = {};
  if (!validateProductForm()) return;

  isSaving.value = true;
  try {
    if (isEditMode.value) {
      // Chế độ cập nhật sản phẩm đã tồn tại
      await updateProduct(productId.value, productForm.value);
      showToast('Cập nhật thông tin sản phẩm thành công!', 'success');
    } else {
      // Chế độ tạo sản phẩm mới
      const nameTaken = await isProductNameExisted(productForm.value.name);
      if (nameTaken) {
        errors.value.name = 'Tên sản phẩm đã tồn tại';
        return;
      }

      const productRes = await createProduct(productForm.value);
      productId.value = productRes.data.data.id;
      isProductCreated.value = true;

      // QUAN TRỌNG: Chuyển sang chế độ edit sau khi tạo sản phẩm thành công
      isEditMode.value = true;

      showToast('Thêm mới sản phẩm thành công! Bây giờ bạn có thể thêm biến thể.', 'success');
    }
  } catch (error) {
    console.error('Lỗi xử lý sản phẩm:', error);
    const message = error.response?.data.data?.message;
    if (message && message.includes('already exists')) {
      errors.value.name = 'Tên sản phẩm đã tồn tại';
    } else {
      showToast('Có lỗi xảy ra khi xử lý sản phẩm', 'error');
    }
  } finally {
    isSaving.value = false;
  }
};

const showVariantModal = () => {
  const modalElement = variantModalRef.value;
  if (modalElement) {
    new bootstrap.Modal(modalElement).show();
  }
  errorsVariant.value = {};
  variantForm.value = {
    product_id: productId.value,
    size_ids: [],      // Clear tất cả size đã chọn
    color_ids: [],     // Clear tất cả màu đã chọn
  };
};

const hideVariantModal = () => {
  const modalElement = variantModalRef.value;
  if (modalElement) {
    const modalInstance = bootstrap.Modal.getInstance(modalElement);
    if (modalInstance) {
      modalInstance.hide();
    }
  }
  errorsVariant.value = {};
};

const showEditVariantModal = () => {
  const modalElement = editVariantModalRef.value;
  if (modalElement) {
    new bootstrap.Modal(modalElement).show();
  }
};

const hideEditVariantModal = () => {
  const modalElement = editVariantModalRef.value;
  if (modalElement) {
    const modalInstance = bootstrap.Modal.getInstance(modalElement);
    if (modalInstance) {
      modalInstance.hide();
    }
  }
};

// Thêm hàm kiểm tra biến thể trùng lặp
const checkExistingVariant = (colorId, sizeId) => {
  return productVariants.value.find(variant =>
      variant.color.id === colorId && variant.size.id === sizeId
  );
};

// Sửa đổi hàm generateVariantCombinations để kiểm tra trùng lặp
const generateVariantCombinations = () => {
  const combinations = [];
  const duplicates = [];

  for (const colorId of variantForm.value.color_ids) {
    for (const sizeId of variantForm.value.size_ids) {
      const existingVariant = checkExistingVariant(colorId, sizeId);

      if (existingVariant) {
        // Nếu biến thể đã tồn tại, thêm vào danh sách trùng lặp
        const colorName = colors.value.find(c => c.id === colorId)?.name || 'Unknown';
        const sizeName = sizes.value.find(s => s.id === sizeId)?.value || 'Unknown';
        duplicates.push({
          variant: existingVariant,
          colorName,
          sizeName
        });
      } else {
        // Nếu chưa tồn tại, thêm vào danh sách tạo mới
        combinations.push({
          product_id: productId.value,
          color_id: colorId,
          size_id: sizeId,
          costPrice: 0,
          sellPrice: 0,
          quantity: 1
        });
      }
    }
  }

  return { combinations, duplicates };
};

// Sửa đổi hàm createVariants để xử lý trùng lặp
const createVariants = async () => {
  if (!validateVariantForm()) return;
  if (!productId.value) {
    showToast('Vui lòng tạo sản phẩm trước khi thêm biến thể', 'warning');
    return;
  }

  try {
    const { combinations, duplicates } = generateVariantCombinations();

    // Xử lý biến thể trùng lặp - cộng dồn số lượng
    if (duplicates.length > 0) {
      const updatePromises = duplicates.map(({ variant }) => {
        const updateData = {
          costPrice: variant.costPrice || 0,
          sellPrice: variant.sellPrice || 0,
          quantity: variant.quantity + 1 // Cộng thêm 1 vào số lượng hiện tại
        };
        return updateProductVariant(variant.id, updateData);
      });

      await Promise.all(updatePromises);

      const duplicateNames = duplicates.map(d => `${d.colorName} - ${d.sizeName}`).join(', ');
      showToast(`Đã cộng thêm số lượng cho ${duplicates.length} biến thể đã tồn tại: ${duplicateNames}`, 'success');
    }

    // Tạo biến thể mới
    if (combinations.length > 0) {
      const createPromises = combinations.map(variantData => createProductVariant(variantData));
      await Promise.all(createPromises);
      showToast(`Đã tạo ${combinations.length} biến thể mới thành công!`, 'success');
    }

    if (combinations.length === 0 && duplicates.length === 0) {
      showToast('Không có biến thể nào để xử lý', 'warning');
      return;
    }

    hideVariantModal();
    await fetchProduct(productId.value);

  } catch (error) {
    console.error('Lỗi tạo biến thể:', error);
    showToast('Có lỗi xảy ra khi xử lý biến thể', 'error');
  }
};

// saveAllData để xử lý trùng lặp khi tạo sản phẩm mới
const saveAllData = async () => {
  if (!validateProductForm()) return;

  if (isEditMode.value) {
    await submitForm();
    return;
  }

  if (variantForm.value.color_ids.length === 0 || variantForm.value.size_ids.length === 0) {
    showToast('Vui lòng chọn đầy đủ màu, size để tạo biến thể', 'warning');
    return;
  }

  isSaving.value = true;
  try {
    const nameTaken = await isProductNameExisted(productForm.value.name);
    if (nameTaken) {
      errors.value.name = 'Tên sản phẩm đã tồn tại';
      return;
    }

    const productRes = await createProduct(productForm.value);
    const newProductId = productRes.data.data.id;
    productId.value = newProductId;

    // Khi tạo sản phẩm mới, không có biến thể trùng lặp nên chỉ tạo mới
    const { combinations } = generateVariantCombinations();
    const finalCombinations = combinations.map(v => ({
      ...v,
      product_id: newProductId
    }));

    const createPromises = finalCombinations.map(variant => createProductVariant(variant));
    await Promise.all(createPromises);

    showToast('Lưu sản phẩm và các biến thể thành công!', 'success');
    setTimeout(() => {
      router.push('/admin/products');
    }, 1500);
  } catch (error) {
    console.error('Lỗi khi lưu dữ liệu:', error);
    showToast('Có lỗi xảy ra khi lưu dữ liệu', 'error');
  } finally {
    isSaving.value = false;
  }
};

const updateVariant = async () => {
  if (!validateEditVariantForm()) return;

  try {
    const updateData = {
      costPrice: parseFloat(editVariantForm.value.costPrice),
      sellPrice: parseFloat(editVariantForm.value.sellPrice),
      quantity: parseInt(editVariantForm.value.quantity)
    };
    await updateProductVariant(editVariantForm.value.id, updateData);
    showToast('Cập nhật biến thể thành công!', 'success');
    hideEditVariantModal();
    await fetchProduct(productId.value);
  } catch (error) {
    console.error('Lỗi cập nhật biến thể:', error);
    showToast('Có lỗi xảy ra khi cập nhật biến thể', 'error');
  }
};

const editVariant = (variant) => {
  editVariantForm.value = {
    id: variant.id,
    costPrice: variant.costPrice || 0,
    sellPrice: variant.sellPrice || 0,
    quantity: variant.quantity,
    color: variant.color,
    size: variant.size,
  };
  editVariantErrors.value = {};
  showEditVariantModal();
};

const triggerFileInput = () => {
  fileInputRef.value?.click();
};

onMounted(async () => {
  try {
    await Promise.all([fetchDataFilters(), fetchVariantOptions()]);
    if (route.params.id) {
      productId.value = route.params.id;
      await fetchProduct(productId.value);
    }
  } catch (error) {
    console.error('Lỗi khởi tạo component:', error);
    showToast('Có lỗi xảy ra khi tải dữ liệu', 'error');
  }
});
</script>

<template>
  <div class="container w-100 py-4 px-4 border rounded bg-white shadow-sm"
       style="max-width: none; width: 100vw; margin-top: 3px">
    <h2 class="mb-4">{{ isEditMode ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm mới' }}</h2>
    <form @submit.prevent="submitForm" novalidate>
      <div class="row">
        <!-- Cột trái -->
        <div class="col-md-6">

          <div class="mb-3">
            <div class="d-flex justify-content-between">
              <label for="name" class="form-label">Tên sản phẩm</label>
              <small class="text-muted">{{ productForm.name?.length || 0 }}/255 ký tự</small>
            </div>

            <input id="name" type="text" class="form-control" v-model="productForm.name"
                   :class="{ 'is-invalid': errors.name }" :disabled="isSaving" @input="errors.name = ''"
                   maxlength="255" />

            <div class="invalid-feedback">{{ errors.name }}</div>
          </div>

          <div class="mb-3">
            <label class="form-label">Thương hiệu</label>
            <select class="form-select" v-model="productForm.brandId" :class="{ 'is-invalid': errors.brandId }"
                    :disabled="isSaving" @change="errors.brandId = ''">
              <option value="">-- Chọn thương hiệu --</option>
              <option v-for="brand in brands" :key="brand.id" :value="brand.id">{{ brand.name }}</option>
            </select>
            <div class="invalid-feedback">{{ errors.brandId }}</div>
          </div>

          <div class="mb-3">
            <label class="form-label">Danh mục</label>
            <select class="form-select" v-model="productForm.categoryId" :class="{ 'is-invalid': errors.categoryId }"
                    :disabled="isSaving" @change="errors.categoryId = ''">
              <option value="">-- Chọn danh mục --</option>
              <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
            </select>
            <div class="invalid-feedback">{{ errors.categoryId }}</div>
          </div>

          <div class="mb-3" v-if="isEditMode">
            <label class="form-label">Trạng thái</label>
            <select class="form-select" v-model="productForm.status">
              <option :value="true">Đang kinh doanh</option>
              <option :value="false">Ngừng kinh doanh</option>
            </select>
          </div>
        </div>

        <!-- Cột phải -->
        <div class="col-md-6">
          <div class="mb-3">
            <label class="form-label">Chất liệu</label>
            <select class="form-select" v-model="productForm.materialId" :class="{ 'is-invalid': errors.materialId }"
                    :disabled="isSaving" @change="errors.materialId = ''">
              <option value="">-- Chọn chất liệu --</option>
              <option v-for="material in materials" :key="material.id" :value="material.id">{{ material.name }}</option>
            </select>
            <div class="invalid-feedback">{{ errors.materialId }}</div>
          </div>

          <div class="mb-3">
            <label class="form-label">Giới tính</label>
            <select class="form-select" v-model="productForm.gender" :class="{ 'is-invalid': errors.gender }"
                    :disabled="isSaving" @change="errors.gender = ''">
              <option value="">-- Chọn giới tính --</option>
              <option value="Male">Nam</option>
              <option value="Female">Nữ</option>
              <option value="Kids">Trẻ em</option>
              <option value="Unisex">Unisex</option>
            </select>
            <div class="invalid-feedback">{{ errors.gender }}</div>
          </div>

          <div class="mb-3">
            <label for="weight" class="form-label">Trọng lượng (kg)</label>
            <input id="weight" type="number" class="form-control" v-model.number="productForm.weight"
                   :class="{ 'is-invalid': errors.weight }" :disabled="isSaving"
                   @input="errors.weight = ''" min="0" max="100" step="0.01"
                   placeholder="Nhập trọng lượng (kg)" />
            <div class="invalid-feedback">{{ errors.weight }}</div>
            <small class="text-muted">Đơn vị: kilogram (kg)</small>
          </div>
        </div>
      </div>

      <!-- Mô tả chiếm full width -->
      <div class="mb-3">
        <label class="form-label">Mô tả</label>
        <textarea class="form-control" v-model="productForm.description" rows="4"
                  :class="{ 'is-invalid': errors.description }" :disabled="isSaving" @input="errors.description = ''"/>
        <div class="invalid-feedback">{{ errors.description }}</div>
      </div>

      <div class="d-flex gap-3">
        <button class="btn btn-primary flex-fill" type="submit" :disabled="isSaving">
          {{ isEditMode ? 'Cập nhật thông tin' : 'Tiếp tục' }}
        </button>
        <router-link to="/admin/products" class="btn btn-secondary flex-fill">Hủy</router-link>
      </div>
    </form>

    <div class="mt-5" v-if="isProductCreated">
      <h2 class="mb-4">Danh sách biến thể</h2>

      <div v-if="Object.keys(groupedVariantsByColor).length > 0">
        <div v-for="(variants, colorName) in groupedVariantsByColor" :key="colorName" class="mb-5">
          <h5 class="mb-3 d-flex align-items-center gap-2">
            <span :style="{
              backgroundColor: variants[0].color.hexCode,
              display: 'inline-block',
              width: '20px',
              height: '20px',
              border: '1px solid #ccc',
              borderRadius: '3px'
            }"></span>
            {{ colorName }}
          </h5>

          <table class="table table-hover table-bordered align-middle">
            <thead class="table-dark">
            <tr>
              <th style="text-align: center; vertical-align: middle;">#</th>
              <th style="text-align: center; vertical-align: middle;">Kích thước</th>
              <th style="text-align: center; vertical-align: middle;">Số lượng</th>
              <th style="text-align: center; vertical-align: middle;">Giá nhập</th>
              <th style="text-align: center; vertical-align: middle;">Giá bán</th>
              <th style="text-align: center; vertical-align: middle;">Hành động</th>
              <th style="text-align: center; vertical-align: middle;">Ảnh</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="(variant, index) in variants" :key="variant.id" style="cursor: pointer;">
              <td style="text-align: center; vertical-align: middle;">{{ index + 1 }}</td>
              <td style="text-align: center; vertical-align: middle;">{{ variant.size.value }}</td>
              <td style="text-align: center; vertical-align: middle;">{{ variant.quantity }}</td>
              <td style="text-align: center; vertical-align: middle;">
                {{ (variant.costPrice || 0).toLocaleString() }} đ
              </td>
              <td style="text-align: center; vertical-align: middle;">
                {{ (variant.sellPrice || 0).toLocaleString() }}₫
              </td>
              <td style="text-align: center; vertical-align: middle;">
                <button class="btn btn-sm btn-warning me-2" @click="editVariant(variant)" :disabled="isSaving">
                  <svg width="15" height="15" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
                    <path
                        d="M368.4 18.3L312.7 74.1 437.9 199.3l55.7-55.7c21.9-21.9 21.9-57.3 0-79.2L447.6 18.3c-21.9-21.9-57.3-21.9-79.2 0zM288 94.6l-9.2 2.8L134.7 140.6c-19.9 6-35.7 21.2-42.3 41L3.8 445.8c-3.8 11.3-1 23.9 7.3 32.4L164.7 324.7c-3-6.3-4.7-13.3-4.7-20.7c0-26.5 21.5-48 48-48s48 21.5 48 48s-21.5 48-48 48c-7.4 0-14.4-1.7-20.7-4.7L33.7 500.9c8.6 8.3 21.1 11.2 32.4 7.3l264.3-88.6c19.7-6.6 35-22.4 41-42.3l43.2-144.1 2.7-9.2L288 94.6z"/>
                  </svg>
                </button>
                <button class="btn btn-sm btn-danger" @click="deleteVariant(variant.id)" :disabled="isSaving">
                  <svg width="15" height="15" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512">
                    <path fill="#ffffff"
                          d="M135.2 17.7L128 32 32 32C14.3 32 0 46.3 0 64S14.3 96 32 96l384 0c17.7 0 32-14.3 32-32s-14.3-32-32-32l-96 0-7.2-14.3C307.4 6.8 296.3 0 284.2 0L163.8 0c-12.1 0-23.2 6.8-28.6 17.7zM416 128L32 128 53.2 467c1.6 25.3 22.6 45 47.9 45l245.8 0c25.3 0 46.3-19.7 47.9-45L416 128z"/>
                  </svg>
                </button>
              </td>
              <td v-if="index === 0" :rowspan="variants.length" style="text-align: center; vertical-align: middle;">
                <button class="btn btn-outline-primary btn-sm"
                        @click="enhanced_showImageUploadModal(colorName, variants[0].color)" :disabled="isSaving">
                  <i class="fas fa-camera me-1"></i>
                  Upload ảnh
                </button>
              </td>
            </tr>
            </tbody>
          </table>

          <div v-if="variantImages[colorName] && variantImages[colorName].length > 0" class="mt-3">
            <div class="row g-3">
              <div v-for="image in variantImages[colorName]" :key="image.id" class="col-6 col-sm-4 col-md-3 col-lg-2">
                <div class="position-relative image-container">
                  <img :src="image.imageUrl" :alt="image.imageName" class="img-fluid rounded border w-100 product-image"
                       style="height: 220px; object-fit: cover;"/>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="mt-3">
            <div class="row">
              <div class="col-12">
                <div class="text-center text-muted py-4 border rounded bg-light">
                  <i class="fas fa-image fa-2x mb-2"></i>
                  <p class="mb-0">Không có ảnh</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="text-center text-muted py-4">
        <p>Chưa có biến thể nào. Nhấn "Thêm biến thể" để bắt đầu.</p>
      </div>

      <div class="d-flex justify-content-between align-items-center mt-3">
        <button class="btn btn-outline-primary" @click="showVariantModal" :disabled="isSaving">
          + Thêm biến thể
        </button>
        <button class="btn btn-outline-primary px-4" @click="saveAllData" :disabled="isSaving" v-if="!isEditMode">
          <span v-if="isSaving" class="spinner-border spinner-border-sm me-2" role="status"></span>
          {{ isSaving ? 'Đang lưu...' : 'Lưu sản phẩm' }}
        </button>
      </div>
    </div>

    <!-- Modal Upload Ảnh -->
    <div class="modal fade" id="imageUploadModal" tabindex="-1" ref="imageUploadModalRef">
      <div class="modal-dialog modal-xl">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title d-flex align-items-center gap-2">
              <span>Quản lý ảnh sản phẩm màu {{ currentColorName }}</span>
            </h5>
            <button type="button" class="btn-close" @click="hideImageUploadModal"
                    :disabled="uploadingImages || isUploadingNewFiles"></button>
          </div>

          <div class="modal-body">
            <!-- Phần hiển thị ảnh hiện tại và ảnh đã chọn -->
            <div class="mb-4">
              <div class="d-flex justify-content-between align-items-center mb-3">
                <h6 class="mb-0">
                  Danh sách ảnh của sản phẩm
                </h6>
                <div class="text-muted small">
                  {{ currentColorImages.length + selectedImages.length }}/5 ảnh
                </div>
              </div>

              <div v-if="allDisplayImages.length > 0" class="row g-3">
                <div v-for="image in allDisplayImages" :key="image.id" class="col-6 col-sm-4 col-md-3 col-lg-2">
                  <div class="position-relative">
                    <img :src="image.url" class="img-fluid rounded border w-100"
                         style="height: 150px; object-fit: cover;"/>

                    <!-- Badge cho ảnh hiện tại -->
                    <div v-if="image.isExisting" class="position-absolute top-0 start-0 m-1">
                      <span class="badge bg-info">Hiện tại</span>
                    </div>

                    <!-- Badge cho ảnh đã chọn -->
                    <div v-if="image.selected" class="position-absolute top-0 start-0 m-1">
                      <span class="badge bg-success">Đã chọn</span>
                    </div>

                    <!-- Nút bỏ chọn cho ảnh đã chọn -->
                    <button v-if="image.selected" class="btn btn-danger btn-sm position-absolute top-0 end-0 m-1"
                            @click="toggleImageSelection(image.id)" :disabled="uploadingImages || isUploadingNewFiles"
                            style="width: 24px; height: 24px; padding: 0; display: flex; align-items: center; justify-content: center;">
                      <i class="fas fa-times" style="font-size: 12px;"></i>
                    </button>

                    <!-- Tên ảnh -->
                    <div class="position-absolute bottom-0 start-0 end-0 m-1">
                      <div class="bg-dark bg-opacity-75 text-white px-2 py-1 rounded small text-center"
                           style="font-size: 10px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                        {{ image.name }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-else class="text-center text-muted py-4 border rounded bg-light">
                <i class="fas fa-image fa-2x mb-2"></i>
                <p class="mb-0">Chưa có ảnh nào</p>
              </div>
            </div>

            <hr class="my-4">

            <!-- Phần chọn ảnh từ hệ thống -->
            <div>
              <div class="d-flex justify-content-between align-items-center mb-3">
                <h6 class="mb-0">
                  Danh sách ảnh các sản phẩm màu {{ currentColorName }}
                </h6>
                <div class="d-flex gap-2">
                  <button class="btn btn-outline-primary btn-sm" @click="triggerFileInput"
                          :disabled="uploadingImages || isUploadingNewFiles || (currentColorImages.length + selectedImages.length) >= 5">
                    <span v-if="isUploadingNewFiles" class="spinner-border spinner-border-sm me-1"></span>
                    <i v-else class="fas fa-plus me-1"></i>
                    {{ isUploadingNewFiles ? 'Đang upload...' : 'Tải ảnh mới' }}
                  </button>
                </div>
              </div>

              <!-- Thanh tiến trình upload -->
              <div v-if="isUploadingNewFiles" class="mb-3">
                <div class="progress">
                  <div class="progress-bar progress-bar-striped progress-bar-animated" role="progressbar"
                       style="width: 100%">
                    Đang upload ảnh...
                  </div>
                </div>
              </div>

              <div v-if="filteredSameColorImages.length > 0" class="row g-3">
                <div v-for="image in filteredSameColorImages" :key="image.id" class="col-6 col-sm-4 col-md-3 col-lg-2">
                  <div class="position-relative image-item" @click="toggleImageSelection(image.id)"
                       style="cursor: pointer;" :class="{ 'opacity-50': uploadingImages || isUploadingNewFiles }">
                    <img :src="image.url" :alt="image.name" class="img-fluid rounded border w-100 selectable-image"
                         :class="{
                       'border-primary': image.selected,
                       'border-3': image.selected,
                       'opacity-75': image.selected
                     }" style="height: 150px; object-fit: cover; transition: all 0.2s ease;"/>

                    <!-- Badge ảnh mới -->
                    <div v-if="image.isNew" class="position-absolute top-0 end-0 m-1">
                      <span class="badge bg-warning">Mới</span>
                    </div>

                    <!-- Checkbox -->
                    <div class="position-absolute bottom-0 start-0 m-2">
                      <div class="form-check">
                        <input class="form-check-input" type="checkbox" :checked="image.selected"
                               @click.stop="toggleImageSelection(image.id)"
                               :disabled="uploadingImages || isUploadingNewFiles" style="width: 18px; height: 18px;"/>
                      </div>
                    </div>

                    <!-- Tên ảnh -->
                    <div class="position-absolute bottom-0 end-0 m-2">
                      <div class="bg-dark bg-opacity-75 text-white px-2 py-1 rounded small"
                           style="font-size: 10px; max-width: 80px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                        {{ image.name }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-else class="text-center text-muted py-4 border rounded bg-light">
                <i class="fas fa-images fa-2x mb-2"></i>
                <p class="mb-0">
                  <span v-if="sameColorImages.length === 0">Không có ảnh nào cùng màu trong hệ thống</span>
                  <span v-else>Tất cả ảnh cùng màu đã được sử dụng</span>
                </p>
                <button class="btn btn-outline-primary btn-sm mt-2" @click="triggerFileInput"
                        :disabled="uploadingImages || isUploadingNewFiles || (currentColorImages.length + selectedImages.length) >= 5">
                  <span v-if="isUploadingNewFiles" class="spinner-border spinner-border-sm me-1"></span>
                  <i v-else class="fas fa-plus me-1"></i>
                  {{ isUploadingNewFiles ? 'Đang upload...' : 'Tải ảnh mới' }}
                </button>
              </div>

              <input type="file" ref="fileInputRef" @change="handleFileSelection" multiple accept="image/*"
                     style="display: none;">
            </div>
          </div>

          <div class="modal-footer">
            <div class="d-flex justify-content-between align-items-center w-100">
              <div class="text-muted small">
                <div class="d-flex gap-3">
                  <span>
                    <i class="fas fa-image text-info me-1"></i>
                    {{ currentColorImages.length }} ảnh hiện tại
                  </span>
                  <span v-if="selectedImages.length > 0" class="text-success">
                    <i class="fas fa-check me-1"></i>
                    {{ selectedImages.length }} ảnh đã chọn
                  </span>
                  <span v-if="isUploadingNewFiles" class="text-warning">
                    <i class="fas fa-spinner fa-spin me-1"></i>
                    Đang upload...
                  </span>
                </div>
              </div>
              <div>
                <button type="button" class="btn btn-secondary me-2" @click="hideImageUploadModal"
                        :disabled="uploadingImages || isUploadingNewFiles">
                  Đóng
                </button>
                <button type="button" class="btn btn-primary" @click="saveImageChanges"
                        :disabled="uploadingImages || isUploadingNewFiles || selectedImages.length === 0">
                  <span v-if="uploadingImages" class="spinner-border spinner-border-sm me-2"></span>
                  {{ uploadingImages ? 'Đang lưu...' : 'Lưu thay đổi' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="modal fade" id="variantModal" tabindex="-1" ref="variantModalRef">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Thêm biến thể sản phẩm</h5>
            <button type="button" class="btn-close" @click="hideVariantModal" :disabled="isSaving"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label class="form-label">Màu sắc <span class="text-muted">(Chọn nhiều)</span></label>
              <div class="border rounded p-2" style="max-height: 120px; overflow-y: auto;">
                <div v-for="color in colors" :key="color.id" class="form-check">
                  <input class="form-check-input" type="checkbox" :value="color.id" :id="`color-${color.id}`"
                         v-model="variantForm.color_ids" :disabled="isSaving" @change="errorsVariant.color_ids = ''">
                  <label class="form-check-label d-flex align-items-center gap-2" :for="`color-${color.id}`">
                    <span :style="{
                      backgroundColor: color.hexCode,
                      display: 'inline-block',
                      width: '16px',
                      height: '16px',
                      border: '1px solid #ccc',
                      borderRadius: '2px'
                    }"></span>
                    {{ color.name }}
                  </label>
                </div>
              </div>
              <div class="text-danger mt-1" style="font-size: 0.9rem" v-if="errorsVariant.color_ids">
                {{ errorsVariant.color_ids }}
              </div>
              <small class="text-muted">Đã chọn: {{ variantForm.color_ids.length }} màu</small>
            </div>

            <div class="mb-3">
              <label class="form-label">Kích thước <span class="text-muted">(Chọn nhiều)</span></label>
              <div class="border rounded p-2" style="max-height: 120px; overflow-y: auto;">
                <div v-for="size in sizes" :key="size.id" class="form-check">
                  <input class="form-check-input" type="checkbox" :value="size.id" :id="`size-${size.id}`"
                         v-model="variantForm.size_ids" :disabled="isSaving" @change="errorsVariant.size_ids = ''">
                  <label class="form-check-label" :for="`size-${size.id}`">
                    {{ size.value }}
                  </label>
                </div>
              </div>
              <div class="text-danger mt-1" style="font-size: 0.9rem" v-if="errorsVariant.size_ids">
                {{ errorsVariant.size_ids }}
              </div>
              <small class="text-muted">Đã chọn: {{ variantForm.size_ids.length }} kích thước</small>
            </div>

            <div class="alert alert-info" v-if="variantForm.color_ids.length > 0 && variantForm.size_ids.length > 0">
              <strong>Xem trước:</strong>
              Sẽ tạo {{ variantForm.color_ids.length * variantForm.size_ids.length }} biến thể
              ({{ variantForm.color_ids.length }} màu × {{ variantForm.size_ids.length }} size)
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn btn-secondary" @click="hideVariantModal" :disabled="isSaving">Hủy</button>
            <button class="btn btn-primary" @click="createVariants()" :disabled="isSaving">Thêm biến thể</button>
          </div>
        </div>
      </div>
    </div>

    <div class="modal fade" id="editVariantModal" tabindex="-1" ref="editVariantModalRef">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Chỉnh sửa biến thể sản phẩm</h5>
            <button type="button" class="btn-close" @click="hideEditVariantModal" :disabled="isSaving"></button>
          </div>
          <div class="modal-body">
            <div class="alert alert-info mb-4">
              <div class="d-flex align-items-center gap-2 mb-2">
                <strong>Biến thể:</strong>
                <span :style="{
                  backgroundColor: editVariantForm.color?.hexCode,
                  display: 'inline-block',
                  width: '20px',
                  height: '20px',
                  border: '1px solid #ccc',
                  borderRadius: '3px'
                }"></span>
                <span>{{ editVariantForm.color?.name }} - {{ editVariantForm.size?.value }}</span>
              </div>
            </div>

            <div class="row">
              <div class="col-md-4 mb-3">
                <label class="form-label">Số lượng</label>
                <input type="number" class="form-control" v-model.number="editVariantForm.quantity"
                       :class="{ 'is-invalid': editVariantErrors.quantity }" :disabled="isSaving" min="0"
                       @input="editVariantErrors.quantity = ''" placeholder="Nhập số lượng">
                <div class="invalid-feedback">{{ editVariantErrors.quantity }}</div>
              </div>

              <div class="col-md-4 mb-3">
                <label class="form-label">Giá nhập (VNĐ)</label>
                <input type="number" class="form-control" v-model.number="editVariantForm.costPrice"
                       :class="{ 'is-invalid': editVariantErrors.costPrice }" :disabled="isSaving" min="0"
                       @input="editVariantErrors.costPrice = ''" placeholder="Nhập giá nhập">
                <div class="invalid-feedback">{{ editVariantErrors.costPrice }}</div>
              </div>

              <div class="col-md-4 mb-3">
                <label class="form-label">Giá bán (VNĐ)</label>
                <input type="number" class="form-control" v-model.number="editVariantForm.sellPrice"
                       :class="{ 'is-invalid': editVariantErrors.sellPrice }" :disabled="isSaving" min="0"
                       @input="editVariantErrors.sellPrice = ''" placeholder="Nhập giá bán">
                <div class="invalid-feedback">{{ editVariantErrors.sellPrice }}</div>
              </div>
            </div>

            <div class="alert alert-secondary" v-if="editVariantForm.costPrice && editVariantForm.sellPrice">
              <small>
                <div><strong>Giá nhập hiển thị:</strong> {{ editVariantForm.costPrice.toLocaleString() }}₫</div>
                <div><strong>Giá bán hiển thị:</strong> {{ editVariantForm.sellPrice.toLocaleString() }}₫</div>
                <div v-if="editVariantForm.sellPrice > editVariantForm.costPrice" class="text-success mt-1">
                  <strong>Lợi nhuận:</strong> {{ (editVariantForm.sellPrice - editVariantForm.costPrice).toLocaleString() }}₫
                </div>
              </small>
            </div>

            <div class="alert alert-secondary" v-if="editVariantForm.price">
              <small>
                <strong>Giá hiển thị:</strong> {{ editVariantForm.price.toLocaleString() }}₫
              </small>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn btn-secondary" @click="hideEditVariantModal" :disabled="isSaving">Hủy</button>
            <button class="btn btn-primary" @click="updateVariant" :disabled="isSaving">
              <span v-if="isSaving" class="spinner-border spinner-border-sm me-2" role="status"></span>
              {{ isSaving ? 'Đang cập nhật...' : 'Cập nhật biến thể' }}
            </button>
          </div>
        </div>
      </div>
    </div>
    <ShowToastComponent ref="toastRef"/>
  </div>
</template>

<style scoped>
.btn-primary:active {
  background-color: #202020;
}

.table-hover tbody tr:hover {
  background-color: #f8f9fa;
}

.form-check-input:checked {
  background-color: #0d6efd;
  border-color: #0d6efd;
}

.spinner-border-sm {
  width: 1rem;
  height: 1rem;
}

img {
  transition: transform 0.2s;
}

img:hover {
  transform: scale(1.02);
  cursor: pointer;
}

.image-item {
  position: relative;
}

.selectable-image {
  transition: all 0.3s ease;
  border: 2px solid transparent !important;
}

.selectable-image:hover {
  transform: scale(1.02);
  border-color: #0d6efd !important;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.selectable-image.selected {
  border-color: #198754 !important;
  box-shadow: 0 0 0 2px rgba(25, 135, 84, 0.25);
}

.form-check-input {
  width: 1.2em;
  height: 1.2em;
  background-color: rgba(255, 255, 255, 0.9);
  border: 2px solid #dee2e6;
}

.form-check-input:checked {
  background-color: #198754;
  border-color: #198754;
}

.nav-tabs .nav-link {
  color: #6c757d;
  border: none;
  border-bottom: 2px solid transparent;
  background: none;
}

.nav-tabs .nav-link:hover {
  border-color: transparent;
  border-bottom-color: #dee2e6;
  color: #495057;
}

.nav-tabs .nav-link.active {
  color: #0d6efd;
  border-bottom-color: #0d6efd;
  background: none;
}

.modal-xl {
  max-width: 1200px;
}

@media (max-width: 768px) {
  .modal-xl {
    max-width: 95%;
  }

  .col-6.col-sm-4.col-md-3.col-lg-2 {
    flex: 0 0 50%;
    max-width: 50%;
  }
}
</style>