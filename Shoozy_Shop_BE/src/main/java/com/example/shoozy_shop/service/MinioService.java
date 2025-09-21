package com.example.shoozy_shop.service;

import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.avatar-bucket-name}")
    private String avatarBucketName;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.return-bucket-name}")
    private String returnBucketName;

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    public String getAvatarBucketName() {
        return this.avatarBucketName;
    }

    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif");
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB/ảnh
    private static final int MAX_IMAGES_PER_UPLOAD = 5;

    private boolean isValidImage(String contentType, String originalName) {
        if (contentType == null || originalName == null)
            return false;
        String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase()
                : "";
        return ALLOWED_MIME.contains(contentType.toLowerCase()) && ALLOWED_EXT.contains(ext);
    }

    @SneakyThrows
    private void setBucketPublicPolicy(String bucketName) {
        final var policyJson = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {
                        "AWS": ["*"]
                      },
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucketName);

        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policyJson)
                        .build());
    }

    @SneakyThrows
    private void checkBucketExists(String bucketName) {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("✅ Created new bucket: {}", bucketName);
        } else {
            log.info("ℹ️ Bucket already exists: {}", bucketName);
        }

        // 🔹 Luôn set lại policy public, kể cả bucket đã tồn tại
        setBucketPublicPolicy(bucketName);
    }

    public String uploadProductImages(MultipartFile file) {
        try {
            if (!isValidImageType(file.getContentType())) {
                throw new RuntimeException("Invalid file type. Only images are allowed for product.");
            }
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            checkBucketExists(bucketName);
            // Upload ảnh sản phẩm
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            return this.buildObjectUrl(bucketName, fileName);
        } catch (Exception e) {
            throw new RuntimeException("Error uploading product image to MinIO: " + e.getMessage());
        }
    }

    public String uploadUserAvatar(MultipartFile file) {
        try {
            // Validate file type for avatar
            if (!isValidImageType(file.getContentType())) {
                throw new RuntimeException("Invalid file type. Only images are allowed for avatar.");
            }
            String fileName = "avatar_" + UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            checkBucketExists(avatarBucketName);
            // Upload avatar
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(avatarBucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            return this.buildObjectUrl(avatarBucketName, fileName);
        } catch (Exception e) {
            throw new RuntimeException("Error uploading avatar to MinIO: " + e.getMessage());
        }
    }

    public String uploadUserAvatarSafe(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File avatar không được để trống");
        }

        // Kiểm tra định dạng MIME
        String contentType = file.getContentType();
        if (contentType == null
                || !(contentType.equals("image/jpeg")
                        || contentType.equals("image/png")
                        || contentType.equals("image/jpg"))) {
            throw new IllegalArgumentException("Định dạng ảnh không hợp lệ. Chỉ chấp nhận JPG hoặc PNG");
        }

        try {
            return uploadUserAvatar(file);
        } catch (Exception e) {
            log.warn("Không thể upload avatar: {}", e.getMessage(), e);
            throw new RuntimeException("Upload avatar thất bại, vui lòng thử lại sau");
        }
    }

    public String buildObjectUrl(String bucketName, String fileName) {
        return String.format("%s/%s/%s", minioUrl, bucketName, fileName);
    }

    private boolean isValidImageType(String contentType) {
        return contentType != null && (contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/webp"));
    }

    // Method để xóa avatar cũ khi user upload avatar mới
    public void deleteFile(String bucketName, String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Error deleting file from MinIO: " + e.getMessage());
        }
    }

    // Helper method để extract fileName từ URL
    public String extractFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }

    @Component
    public class MinioStartupCheck {

        @Autowired
        private MinioClient minioClient;

        @Value("${minio.avatar-bucket-name}")
        private String bucketName;

        @PostConstruct
        public void checkMinioConnection() {
            try {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
                if (!exists) {
                    System.err.println("⚠️ MinIO bucket '" + bucketName + "' chưa tồn tại!");
                } else {
                    System.out.println("✅ Kết nối MinIO thành công.");
                }
            } catch (Exception e) {
                System.err.println("❌ Không thể kết nối MinIO: " + e.getMessage());
            }
        }
    }

    // ====== THÊM: validator cho ảnh trả hàng ======
    private void validateReturnImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được để trống.");
        }
        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename();

        if (contentType == null || originalName == null) {
            throw new IllegalArgumentException("Thiếu thông tin file (loại hoặc tên file).");
        }
        String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase()
                : "";

        if (!ALLOWED_MIME.contains(contentType.toLowerCase()) || !ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("Định dạng ảnh không hợp lệ. Chỉ chấp nhận: JPG, JPEG, PNG, WEBP, GIF.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Kích thước ảnh tối đa 5MB.");
        }
    }

    // ====== SỬA: hàm đơn (1 ảnh) để dùng validator mới ======
    public String uploadReturnImage(MultipartFile file) {
        validateReturnImage(file); // <-- dùng validate chặt chẽ

        String safeOriginal = file.getOriginalFilename() == null ? "unknown"
                : file.getOriginalFilename().replace("\\", "_").replace("/", "_");
        String fileName = "return_" + UUID.randomUUID() + "_" + safeOriginal;

        checkBucketExists(returnBucketName);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(returnBucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            return buildObjectUrl(returnBucketName, fileName);
        } catch (Exception e) {
            throw new RuntimeException("Error uploading return image", e);
        }
    }

    // ====== THÊM: hàm upload nhiều ảnh (giới hạn 5) ======
    public List<String> uploadReturnImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 ảnh.");
        }
        if (files.size() > 5) {
            throw new IllegalArgumentException("Chỉ cho phép tối đa 5 ảnh.");
        }

        checkBucketExists(returnBucketName);

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Có tệp rỗng. Vui lòng chọn lại.");
            }
            String contentType = file.getContentType();
            String original = file.getOriginalFilename();

            if (!isValidImageType(contentType)) {
                throw new IllegalArgumentException(
                        "Định dạng ảnh không hợp lệ. Chỉ chấp nhận JPG, JPEG, PNG, GIF, WEBP.");
            }
            if (file.getSize() > 5L * 1024 * 1024) {
                throw new IllegalArgumentException("Kích thước mỗi ảnh tối đa 5MB.");
            }

            String fileName = "return_" + UUID.randomUUID() + "_" + (original == null ? "image" : original);
            try {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(returnBucketName)
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(contentType)
                        .build());
                urls.add(buildObjectUrl(returnBucketName, fileName));
            } catch (Exception e) {
                throw new RuntimeException("Không thể upload ảnh: " + (original == null ? "" : original), e);
            }
        }
        return urls;
    }

}
