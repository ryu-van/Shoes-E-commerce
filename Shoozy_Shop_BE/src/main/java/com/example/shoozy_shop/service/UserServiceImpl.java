package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.UpdateUserRequest;
import com.example.shoozy_shop.dto.response.UserDto;
import com.example.shoozy_shop.exception.CustomException;
import com.example.shoozy_shop.exception.DuplicateResourceException;
import com.example.shoozy_shop.model.Role;
import com.example.shoozy_shop.model.Transaction;
import com.example.shoozy_shop.model.User;
import com.example.shoozy_shop.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.shoozy_shop.dto.request.AdminCreateUserRequest;
import com.example.shoozy_shop.dto.request.AdminUpdateUserRequest;
import com.example.shoozy_shop.dto.request.ChangePasswordRequest;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private VnPayTransactionDetailRepository vnPayTransactionDetailRepository;

    UserServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Autowired
    private MinioService minioService;

    // mapping User → UserDto
    private UserDto mapToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setFullname(user.getFullname());
        dto.setEmail(user.getEmail());
        dto.setGender(user.getGender());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAddress(user.getAddress());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setRoleName(user.getRole().getName());
        dto.setIsActive(user.getIsActive());
        dto.setAvatar(user.getAvatar());
        return dto;
    }

    @Override
    public UserDto getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        return mapToDto(user);
    }

    @Override
    public UserDto updateProfile(String email, UpdateUserRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        user.setAvatar(request.getAvatar());
        user.setFullname(request.getFullname());
        user.setEmail(request.getEmail());
        user.setGender(request.getGender());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setUpdatedAt(LocalDate.now());

        return mapToDto(userRepository.save(user));
    }

    // ===== AVATAR METHODS ====
    @Override
    public String updateUserAvatarById(Long userId, MultipartFile avatarFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        // Upload avatar mới
        String newAvatarUrl = minioService.uploadUserAvatar(avatarFile);
        // Cập nhật database
        user.setAvatar(newAvatarUrl);
        user.setUpdatedAt(LocalDate.now());
        userRepository.save(user);

        return newAvatarUrl;
    }

    // ===== EXISTING METHODS =====

    @Override
    public Page<UserDto> searchUsers(String roleName, Boolean status, String keyword, Pageable pageable) {
        Page<User> page;

        if (status == null && (keyword == null || keyword.isBlank())) {
            page = userRepository.findByRole_Name(roleName, pageable);
        } else if (status != null && (keyword == null || keyword.isBlank())) {
            page = userRepository.findByRole_NameAndIsActive(roleName, status, pageable);
        } else if (status == null && keyword != null && !keyword.isBlank()) {
            page = userRepository.findByRole_NameAndFullnameContainingIgnoreCase(roleName, keyword, pageable);
        } else {
            page = userRepository.findByRole_NameAndIsActiveAndFullnameContainingIgnoreCase(roleName, status, keyword,
                    pageable);
        }

        return page.map(this::mapToDto);
    }

    @Override
    public UserDto changeUserStatus(Long userId, Boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        user.setIsActive(isActive);
        user.setUpdatedAt(LocalDate.now());
        return mapToDto(userRepository.save(user));
    }

    @Override
    public UserDto changeUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
        user.setRole(role);
        user.setUpdatedAt(LocalDate.now());
        return mapToDto(userRepository.save(user));
    }

    @Override
    public List<UserDto> getUsersByRole(String roleName) {
        // kiểm existence của role trước
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
        // lọc user theo role
        return userRepository.findAll().stream()
                .filter(u -> u.getRole().getName().equals(role.getName()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        return mapToDto(user);
    }

    @Override
    public UserDto changePassword(String email, ChangePasswordRequest request) {
        // 1. Lấy user theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User không tồn tại", 404));

        // 2. Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new CustomException("Mật khẩu cũ không đúng", 400);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new CustomException("Mật khẩu mới phải khác mật khẩu cũ", 400);
        }

        // 3. Mã hóa và gán mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDate.now());

        // 4. Lưu xuống DB
        User updated = userRepository.save(user);

        // 5. Trả về DTO (có thể không cần, hoặc trả thông tin cơ bản)
        return mapToDto(updated);
    }

    @Override
    public UserDto createUser(AdminCreateUserRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email đã tồn tại");
        }

        if (userRepository.existsByPhoneNumber(req.getPhoneNumber())) {
            throw new DuplicateResourceException("Số điện thoại đã tồn tại");
        }

        Role role = roleRepository.findByName(req.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));

        User user = new User();
        user.setFullname(req.getFullname());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setGender(req.getGender());
        user.setPhoneNumber(req.getPhoneNumber());
        user.setAddress(req.getAddress());
        user.setDateOfBirth(req.getDateOfBirth());
        user.setRole(role);
        user.setIsActive(true); // ✅ Luôn set true tại đây
        user.setCreatedAt(LocalDate.now());
        user.setUpdatedAt(LocalDate.now());

        // Xử lý avatar nếu có
        MultipartFile avatarFile = req.getAvatar();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String avatarUrl = minioService.uploadUserAvatarSafe(avatarFile);
            user.setAvatar(avatarUrl);
        }

        return mapToDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto updateUser(Long userId, AdminUpdateUserRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        String newEmail = normalizeEmail(req.getEmail());
        String oldEmail = normalizeEmail(user.getEmail());

        String newPhone = normalizePhone(req.getPhoneNumber());
        String oldPhone = normalizePhone(user.getPhoneNumber());

        // Email
        if (!Objects.equals(oldEmail, newEmail)) {
            if (userRepository.existsByEmailAndIdNot(newEmail, userId)) {
                throw new DuplicateResourceException("EMAIL_EXISTS", "Email đã tồn tại");
            }
            user.setEmail(newEmail);
        }

        // Phone
        if (!Objects.equals(oldPhone, newPhone)) {
            if (userRepository.existsByPhoneNumberAndIdNot(newPhone, userId)) {
                throw new DuplicateResourceException("PHONE_EXISTS", "Số điện thoại đã tồn tại");
            }
            user.setPhoneNumber(newPhone);
        }

        // Role
        Role role = roleRepository.findByName(req.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));

        // Fields khác
        user.setFullname(req.getFullname());
        if (req.getGender() != null)
            user.setGender(req.getGender());
        user.setAddress(req.getAddress());
        user.setDateOfBirth(req.getDateOfBirth());
        user.setRole(role);
        user.setIsActive(Boolean.TRUE.equals(req.getIsActive()));
        user.setUpdatedAt(LocalDate.now());

        // Avatar
        MultipartFile avatarFile = req.getAvatar();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String oldAvatarUrl = user.getAvatar();
            if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
                String fileName = minioService.extractFileNameFromUrl(oldAvatarUrl);
                String bucketName = minioService.getAvatarBucketName();
                minioService.deleteFile(bucketName, fileName);
            }
            String avatarUrl = minioService.uploadUserAvatarSafe(avatarFile);
            user.setAvatar(avatarUrl);
        }

        try {
            return mapToDto(userRepository.saveAndFlush(user));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Nếu DB vẫn báo unique (do dữ liệu cũ hoặc race condition) => convert sang 409
            String low = (ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage())
                    .toLowerCase();
            if (low.contains("phone"))
                throw new DuplicateResourceException("PHONE_EXISTS", "Số điện thoại đã tồn tại");
            if (low.contains("email"))
                throw new DuplicateResourceException("EMAIL_EXISTS", "Email đã tồn tại");
            throw ex;
        }
    }

    private String normalizeEmail(String v) {
        return v == null ? null : v.trim().toLowerCase();
    }

    private String normalizePhone(String v) {
        if (v == null || v.isBlank())
            return null;
        String d = v.replaceAll("[^0-9]", "");

        if (d.startsWith("840")) {
            d = d.substring(2); // 840xxxxxxxxx -> 0xxxxxxxxx
        } else if (d.startsWith("84")) {
            d = "0" + d.substring(2); // 84xxxxxxxxx -> 0xxxxxxxxx
        }
        if (!d.startsWith("0"))
            throw new IllegalArgumentException("Số điện thoại phải bắt đầu bằng 0.");
        if (d.length() != 10)
            throw new IllegalArgumentException("Số điện thoại phải có đúng 10 chữ số.");
        return d;
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Chuyển trạng thái isActive thành false (xoá mềm)
        user.setIsActive(false);
        user.setUpdatedAt(LocalDate.now());
        userRepository.save(user);
    }

}
