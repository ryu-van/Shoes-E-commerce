# Hướng dẫn cài đặt Ollama cho Shoozy Shop

## Vấn đề hiện tại
- Model `llama3.2:3b` quá nhỏ (3B parameters) 
- Không đủ khả năng xử lý prompt phức tạp
- Gây ra lỗi timeout và response kém chất lượng

## Giải pháp: Sử dụng Qwen3:8b (Đã có sẵn!)

### 1. Model đã có sẵn
```bash
# Kiểm tra model có sẵn
ollama list

# Model khuyến nghị: qwen3:8b (đã có sẵn)
# Không cần cài đặt thêm!
```

### 2. Nếu muốn cài thêm model khác
```bash
# Model 8B (khuyến nghị)
ollama pull qwen3:8b

# Model 4B nếu máy yếu
ollama pull qwen3:4b

# Model 30B nếu máy mạnh
ollama pull qwen3:30b
```

### 2. Kiểm tra model đã cài
```bash
ollama list
```

### 3. Test model
```bash
ollama run qwen3:8b
# Gõ: "Xin chào, bạn có thể tư vấn giày không?"
# Nhấn Ctrl+D để thoát
```

### 4. Cập nhật application.yml
Đã sửa trong file:
```yaml
spring:
  ai:
    ollama:
      chat:
        options:
          model: qwen3:8b  # Sử dụng Qwen3:8b thay vì llama3.2:3b
```

## Yêu cầu hệ thống

### Model 8B (Khuyến nghị)
- RAM: Tối thiểu 8GB
- CPU: 4 cores trở lên
- GPU: Không bắt buộc (chạy CPU)

### Model 7B (Nếu máy yếu)
- RAM: Tối thiểu 6GB
- CPU: 2 cores trở lên

### Model 13B (Nếu máy mạnh)
- RAM: Tối thiểu 16GB
- CPU: 8 cores trở lên
- GPU: Khuyến nghị có

## Khởi động lại ứng dụng
```bash
# Restart Spring Boot application
# Hoặc restart Docker nếu dùng Docker
```

## Kiểm tra hoạt động
1. Gọi API health check: `GET /api/v1/chat/health`
2. Test chat: `POST /api/v1/chat/message`
3. Kiểm tra logs để đảm bảo không có lỗi

## Troubleshooting

### Lỗi "model not found"
```bash
# Kiểm tra model đã cài
ollama list

# Nếu chưa có, cài lại
ollama pull llama3.2:8b
```

### Lỗi timeout (ĐÃ SỬA)
- ✅ **Đã đồng bộ tất cả timeout**: AI (60s), HTTP (60s), Controller (55s)
- ✅ **Đã thêm retry mechanism**: 2 lần retry với delay 2s
- ✅ **Đã thêm health check**: Kiểm tra Ollama service
- ✅ **Đã tối ưu prompt**: Giảm độ phức tạp

### Lỗi connection refused
- Kiểm tra Ollama service có chạy không
- Kiểm tra port 11434 có mở không
- Restart Ollama service

### Kiểm tra sức khỏe hệ thống
```bash
# Health check chatbot
GET /api/v1/chat/health

# Kiểm tra Ollama
curl http://localhost:11434/api/tags
``` 