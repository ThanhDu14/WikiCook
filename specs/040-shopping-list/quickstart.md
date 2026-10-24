# Quickstart: Smart Shopping List & Ingredient Aggregation – 040

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **API**: [openapi.yaml](contracts/openapi.yaml)

Tài liệu này hướng dẫn lập trình viên khởi chạy, kiểm thử và xác thực module **040-shopping-list** trong môi trường cục bộ (Local Development) bằng Docker Compose, Spring Boot 3 và React Vite.

---

## 1. Yêu cầu môi trường

- **Java Development Kit (JDK)**: OpenJDK 21 (LTS)
- **Node.js**: v20.x hoặc v22.x LTS, kèm `npm` v10+
- **Docker & Docker Compose**: Docker Engine 24+ để chạy PostgreSQL 16
- **Maven**: 3.9+ (hoặc dùng `./mvnw` đi kèm backend)

---

## 2. Khởi động CSDL Cục bộ

Sử dụng Docker Compose để khởi chạy PostgreSQL 16 với migration Flyway:

```bash
# Khởi động PostgreSQL 16 container
docker run -d --name wikicook-postgres \
  -e POSTGRES_DB=wikicook \
  -e POSTGRES_USER=wikicook_user \
  -e POSTGRES_PASSWORD=wikicook_secret \
  -p 5432:5432 \
  postgres:16-alpine
```

Kiểm tra kết nối và chạy Flyway migration:
```bash
cd src/backend
./mvnw flyway:migrate
```

Xác nhận 4 bảng của module 040 đã được tạo thành công:
```bash
docker exec -it wikicook-postgres psql -U wikicook_user -d wikicook -c "\dt shopping*"
```
Kết quả mong đợi:
```text
               List of relations
 Schema |          Name          | Type  |     Owner      
--------+------------------------+-------+----------------
 public | shopping_lists         | table | wikicook_user
 public | shopping_list_items    | table | wikicook_user
 public | shopping_list_item_sources | table | wikicook_user
 public | user_aisle_preferences| table | wikicook_user
```

---

## 3. Chạy Kiểm thử Tự động (Automated Tests)

### 3.1. Backend Unit & Integration Tests (Testcontainers)
Hệ thống tự động khởi chạy container PostgreSQL 16 thật thông qua Testcontainers:

```bash
cd src/backend
./mvnw clean test -Dtest="com.wikicook.shoppinglist.**"
```

Kiểm tra độ phủ JaCoCo ($\ge 70\%$ theo Constitution II):
```bash
./mvnw jacoco:report
# Mở báo cáo tại: target/site/jacoco/com.wikicook.shoppinglist.service/index.html
```

### 3.2. Frontend Tests & Lint
```bash
cd src/frontend
npm test -- src/features/shopping-list/
```

---

## 4. Kiểm thử API bằng cURL (Manual Verification Flow)

Giả định bạn đã có JWT Bearer token hợp lệ (`$JWT_TOKEN`):

### Bước 1: Lấy danh sách mua sắm hiện hành (tự động khởi tạo nếu chưa có)
```bash
curl -X GET "http://localhost:8080/api/v1/shopping-lists/active" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Accept: application/json"
```

### Bước 2: Thêm món thủ công ngoài chợ (Custom Item)
```bash
curl -X POST "http://localhost:8080/api/v1/shopping-lists/items" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientUuid": "c71a3642-1234-4b56-89ab-123456789abc",
    "customName": "Nước rửa chén Sunlight",
    "quantity": 1,
    "unitName": "chai",
    "aisleCategoryId": 6
  }'
```

### Bước 3: Gộp nguyên liệu từ công thức nấu ăn lẻ (Scale khẩu phần 4 người)
```bash
curl -X POST "http://localhost:8080/api/v1/shopping-lists/import/recipe" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Idempotency-Key: a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "recipeId": 101,
    "targetServings": 4.0,
    "excludedIngredientIds": [1, 2]
  }'
```

### Bước 4: Thao tác đi siêu thị ngoại tuyến & Đồng bộ theo lô (Offline Batch Sync)
```bash
curl -X POST "http://localhost:8080/api/v1/shopping-lists/active/sync" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "mutations": [
      {
        "clientUuid": "c71a3642-1234-4b56-89ab-123456789abc",
        "action": "TOGGLE_CHECK",
        "expectedVersion": 0,
        "isChecked": true,
        "clientTimestamp": "2026-10-24T19:30:00Z"
      }
    ]
  }'
```

### Bước 5: Dọn dẹp món đã mua (Soft-Clear) & Hoàn tác (Undo)
```bash
# Dọn dẹp các món đã tick
CLEAR_RESP=$(curl -s -X POST "http://localhost:8080/api/v1/shopping-lists/active/clear-purchased" \
  -H "Authorization: Bearer $JWT_TOKEN")
echo $CLEAR_RESP

# Lấy clearBatchId và hoàn tác trong vòng 5 giây
BATCH_ID=$(echo $CLEAR_RESP | jq -r '.clearBatchId')
curl -X POST "http://localhost:8080/api/v1/shopping-lists/active/undo-clear" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"clearBatchId\": \"$BATCH_ID\"}"
```

### Bước 6: Lưu trữ danh sách vào lịch sử (Archive)
```bash
curl -X POST "http://localhost:8080/api/v1/shopping-lists/active/archive" \
  -H "Authorization: Bearer $JWT_TOKEN"
```
