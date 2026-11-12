# Quickstart: Community Reviews, Cooksnaps & Interactive Ratings – 070

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **API**: [contracts/openapi.yaml](contracts/openapi.yaml) | **Tasks**: [tasks.md](tasks.md)

This document provides a comprehensive step-by-step developer guide for starting, testing, and verifying the **070-review-cooksnap** module in a local development environment using Docker Compose, Spring Boot 3.3, and React 18.

---

## 1. Environment Requirements

Ensure the following tools are installed on your workstation:

- **Java Development Kit (JDK)**: OpenJDK 21 LTS (`java -version` $\ge 21$)
- **Node.js**: v20.x or v22.x LTS with `npm` v10+ (`node -v`, `npm -v`)
- **Docker & Docker Compose**: Docker Engine 24+ (`docker compose version`)
- **Maven**: 3.9+ (`mvn -v`)
- **EXIF Verification Tool**: `exiftool` (used to verify Constitution Principle IV Privacy Safeguards)
  * *macOS*: `brew install exiftool`
  * *Ubuntu/Debian*: `sudo apt-get install libimage-exiftool-perl`
  * *Windows*: `winget install OliverBetz.ExifTool` or download from [exiftool.org](https://exiftool.org/)

---

## 2. Start the Local Database & Flyway Migration

The database infrastructure uses PostgreSQL 16 managed via `src/backend/docker-compose.yml`. Flyway automatically executes migrations from `wikicook-db`, including `V501__create_reviews_and_cooksnaps_schema.sql`.

```bash
# Navigate to the backend directory
cd src/backend

# Copy environment variables template if not present
cp .env.example .env

# Start PostgreSQL 16 container in background
docker compose up -d db

# Run Flyway migrations up to V501
docker compose run --rm migrate
```

Verify that all three tables belonging to module 070 were successfully created in PostgreSQL:

```bash
docker compose exec db psql -U wikicook -d wikicook -c "\dt (reviews|cooksnaps|review_helpful_votes)"
```

**Expected Output:**
```text
                  List of relations
 Schema |         Name         | Type  |  Owner   
--------+----------------------+-------+----------
 public | cooksnaps            | table | wikicook
 public | review_helpful_votes | table | wikicook
 public | reviews              | table | wikicook
(3 rows)
```

Verify table indexes and constraints:
```bash
docker compose exec db psql -U wikicook -d wikicook -c "\d reviews"
```

You should see:
- Partial unique index: `uq_reviews_recipe_user UNIQUE (recipe_id, user_id) WHERE user_id IS NOT NULL`
- Performance indexes: `idx_reviews_recipe_lookup`, `idx_reviews_recipe_rating`, `idx_reviews_recipe_helpful`

---

## 3. Run Automated Test Suites

### 3.1. Backend Unit & Integration Tests (Testcontainers)

The integration tests spin up an isolated, temporary PostgreSQL 16 container automatically via Testcontainers:

```bash
cd src/backend
mvn -pl wikicook-app -am verify -Dtest="com.wikicook.review.**" -Dsurefire.failIfNoSpecifiedTests=false
```

Verify **JaCoCo Code Coverage** meets the $\ge 70\%$ threshold required by **Constitution Principle II**:
```bash
# Open the generated HTML coverage report in your browser:
# On Windows:
start wikicook-app/target/site/jacoco/com.wikicook.review.service/index.html
# On macOS:
open wikicook-app/target/site/jacoco/com.wikicook.review.service/index.html
# On Linux:
xdg-open wikicook-app/target/site/jacoco/com.wikicook.review.service/index.html
```

### 3.2. Frontend Tests & Lint

Frontend component tests run using Vitest and React Testing Library:

```bash
cd src/frontend
npm ci
npm run lint
npm test -- run reviews
```

### 3.3. Run Full-Stack Local App

To run the whole stack concurrently:

```bash
# 1. Run Spring Boot Backend (Port 8080)
cd src/backend
mvn -pl wikicook-app spring-boot:run

# 2. In another terminal, run Vite React Frontend (Port 3000 / 5173)
cd src/frontend
npm run dev
```

- Backend API Base: `http://localhost:8080/api/v1`
- Swagger UI / OpenAPI Docs: `http://localhost:8080/swagger-ui.html`
- Frontend SPA: `http://localhost:5173`

---

## 4. End-to-End cURL API Verification Suite

Set up shell variables for testing with mock JWT Bearer tokens (from spec 010):

```bash
BASE_URL="http://localhost:8080/api/v1"
RECIPE_ID="105"

# Mock tokens (signed JWT with role and userId claims)
TOKEN_MEMBER_A="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0MiIsImVtYWlsIjoiYWxpY2VAd2lraWNvb2suY29tIiwicm9sZSI6Ik1FTUJFUiIsImlhdCI6MTY5OTk5OTk5OX0.MOCK_SIG_A"
TOKEN_MEMBER_B="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0MyIsImVtYWlsIjoiYm9iQHdpa2ljb29rLmNvbSIsInJvbGUiOiJNRU1CRVIiLCJpYXQiOjE2OTk5OTk5OTl9.MOCK_SIG_B"
TOKEN_AUTHOR="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI5OSIsImVtYWlsIjoiYXV0aG9yQHdpa2ljb29rLmNvbSIsInJvbGUiOiJNRU1CRVIiLCJpYXQiOjE2OTk5OTk5OTl9.MOCK_SIG_AUTHOR"
```

---

### 4.1. Happy Path Scenarios (Standard Success Workflows)

#### Step 1: Create a Review with Cooksnap Upload (Multipart Form)
As User A (`TOKEN_MEMBER_A`), submit a 5-star review with a comment, substitution tip, and a photo:

```bash
# Create a temporary dummy JPEG image with EXIF tag for testing
echo "fake image content" > sample_cooksnap.jpg

curl -i -X POST "$BASE_URL/recipes/$RECIPE_ID/reviews" \
  -H "Authorization: Bearer $TOKEN_MEMBER_A" \
  -F "rating=5" \
  -F "comment=Món phở bò này nấu theo công thức nước dùng rất thanh và thơm mùi quế hồi! Cả nhà đều khen ngon." \
  -F "substitutionTip=Nếu không có sá sùng, có thể nướng thêm 1 củ hành tây để ngọt nước dùng." \
  -F "cooksnapFiles=@sample_cooksnap.jpg;type=image/jpeg"
```

**Expected Response**: `HTTP/1.1 201 Created`
```json
{
  "id": 789,
  "recipeId": 105,
  "author": {
    "id": 42,
    "displayName": "Alice Nguyen",
    "avatarUrl": "https://images.wikicook.com/avatars/42.jpg",
    "role": "MEMBER",
    "isDeleted": false
  },
  "rating": 5,
  "comment": "Món phở bò này nấu theo công thức nước dùng rất thanh và thơm mùi quế hồi! Cả nhà đều khen ngon.",
  "substitutionTip": "Nếu không có sá sùng, có thể nướng thêm 1 củ hành tây để ngọt nước dùng.",
  "cooksnaps": [
    {
      "id": 1201,
      "storageKey": "cs_105_789_a1b2c3d4e5f6.webp",
      "url": "/api/v1/media/cooksnaps/cs_105_789_a1b2c3d4e5f6.webp",
      "thumbnailUrl": "/api/v1/media/cooksnaps/cs_105_789_a1b2c3d4e5f6.webp?thumbnail=true",
      "sizeBytes": 345678,
      "contentType": "image/webp"
    }
  ],
  "helpfulCount": 0,
  "hasVotedHelpful": false,
  "createdAt": "2026-11-12T10:00:00Z",
  "updatedAt": "2026-11-12T10:00:00Z",
  "isOwner": true
}
```

> [!NOTE]
> In the database, `recipes.rating_avg` and `recipes.review_count` are recalculated synchronously in this transaction. **Notice that `recipes.version` was NOT incremented**, preserving lock freedom for recipe authors and editors.

---

#### Step 2: Browse Reviews & Rating Summary Breakdown (Public / Guest)
Guests and authenticated members can retrieve paginated reviews sorted by newest first:

```bash
curl -s -X GET "$BASE_URL/recipes/$RECIPE_ID/reviews?page=0&size=10&sort=NEWEST" \
  -H "Accept: application/json" | jq .
```

**Expected Response**: `HTTP/1.1 200 OK`
```json
{
  "items": [
    {
      "id": 789,
      "recipeId": 105,
      "author": { "displayName": "Alice Nguyen" },
      "rating": 5,
      "comment": "Món phở bò này nấu theo công thức nước dùng...",
      "helpfulCount": 0
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 10,
  "ratingSummary": {
    "averageRating": 5.0,
    "totalReviews": 1,
    "ratingDistribution": {
      "1": 0,
      "2": 0,
      "3": 0,
      "4": 0,
      "5": 1
    }
  }
}
```

---

#### Step 3: Fetch Static Cooksnap Image & Thumbnail (Public Media Stream)
Download the processed image or thumbnail variant:

```bash
STORAGE_KEY="cs_105_789_a1b2c3d4e5f6.webp"

# Download full-resolution image
curl -I "$BASE_URL/media/cooksnaps/$STORAGE_KEY"

# Download 400x400 thumbnail variant
curl -I "$BASE_URL/media/cooksnaps/$STORAGE_KEY?thumbnail=true"
```

**Expected Response Headers**: `HTTP/1.1 200 OK`
```text
HTTP/1.1 200 OK
Content-Type: image/webp
Cache-Control: public, max-age=31536000, immutable
Content-Length: 125430
```

---

#### Step 4: Edit Own Review (Update Rating & Comment)
As User A, update your review rating from 5 to 4 stars:

```bash
REVIEW_ID="789"

curl -i -X PUT "$BASE_URL/reviews/$REVIEW_ID" \
  -H "Authorization: Bearer $TOKEN_MEMBER_A" \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 4,
    "comment": "Nấu lần 2 giảm bớt chút gừng thì vị nước dùng thanh hơn hẳn. Rất khuyến khích thử!",
    "substitutionTip": "Dùng hành tây nướng thay cho hành tím cũng rất dậy mùi."
  }'
```

**Expected Response**: `HTTP/1.1 200 OK` (with `rating: 4` and updated timestamp).

---

#### Step 5: Toggle Helpful Vote (Upvote & Cancel Upvote)
As User B (`TOKEN_MEMBER_B`), vote "Helpful" on User A's review:

```bash
# 1st Call: Cast helpful vote
curl -s -X POST "$BASE_URL/reviews/$REVIEW_ID/helpful" \
  -H "Authorization: Bearer $TOKEN_MEMBER_B" | jq .

# Expected Response:
# { "reviewId": 789, "helpfulCount": 1, "hasVoted": true }

# 2nd Call: Cancel helpful vote (toggle)
curl -s -X POST "$BASE_URL/reviews/$REVIEW_ID/helpful" \
  -H "Authorization: Bearer $TOKEN_MEMBER_B" | jq .

# Expected Response:
# { "reviewId": 789, "helpfulCount": 0, "hasVoted": false }
```

---

#### Step 6: Delete Own Review (Soft-Delete)
As User A, delete your review:

```bash
curl -i -X DELETE "$BASE_URL/reviews/$REVIEW_ID" \
  -H "Authorization: Bearer $TOKEN_MEMBER_A"
```

**Expected Response**: `HTTP/1.1 204 No Content`
- Review row is marked `status = 'HIDDEN'`.
- Associated Cooksnaps are marked `status = 'HIDDEN'`.
- Recipe's `rating_avg` and `review_count` are recalculated synchronously.

---

### 4.2. Edge Cases & Security Safeguards Verification

#### Test 1: Author Self-Review Guard (HTTP 403 Forbidden)
Author of Recipe #105 (`TOKEN_AUTHOR`) attempts to review their own recipe:

```bash
curl -i -X POST "$BASE_URL/recipes/$RECIPE_ID/reviews" \
  -H "Authorization: Bearer $TOKEN_AUTHOR" \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 5,
    "comment": "Công thức của tôi nấu chuẩn vị nhất quả đất!"
  }'
```

**Expected Response**: `HTTP/1.1 403 Forbidden`
```json
{
  "type": "https://wikicook.com/errors/self-review-forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Bạn là tác giả công thức này — bạn có thể xem đánh giá, nhưng không thể tự chấm điểm.",
  "instance": "/api/v1/recipes/105/reviews"
}
```

---

#### Test 2: Duplicate Review Guard (HTTP 409 Conflict)
User A attempts to submit a second active review on Recipe #105 without deleting the first:

```bash
curl -i -X POST "$BASE_URL/recipes/$RECIPE_ID/reviews" \
  -H "Authorization: Bearer $TOKEN_MEMBER_A" \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 5,
    "comment": "Gửi thêm bài đánh giá nữa để tăng điểm..."
  }'
```

**Expected Response**: `HTTP/1.1 409 Conflict`
```json
{
  "type": "https://wikicook.com/errors/review-already-exists",
  "title": "Conflict",
  "status": 409,
  "detail": "Bạn đã đánh giá công thức này rồi. Vui lòng chỉnh sửa đánh giá hiện có.",
  "instance": "/api/v1/recipes/105/reviews"
}
```

---

#### Test 3: High-Frequency Rate Limiting (HTTP 429 Too Many Requests)
Execute 6 review creation requests rapidly within 10 minutes from the same client IP:

```bash
for i in {1..6}; do
  echo "--- Request $i ---"
  curl -s -X POST "$BASE_URL/recipes/20$i/reviews" \
    -H "Authorization: Bearer $TOKEN_MEMBER_A" \
    -H "Content-Type: application/json" \
    -d '{ "rating": 5, "comment": "Bình luận spam thử nghiệm rate limit." }' \
    | jq -r '.status, .detail'
done
```

**Expected Result on 6th Request**: `HTTP/1.1 429 Too Many Requests`
```json
{
  "type": "https://wikicook.com/errors/rate-limit-exceeded",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Bạn gửi đánh giá quá nhanh. Vui lòng thử lại sau 10 phút.",
  "instance": "/api/v1/recipes/206/reviews"
}
```

---

#### Test 4: Self-Helpful Vote Guard (HTTP 403 Forbidden)
User A attempts to upvote their own review:

```bash
curl -i -X POST "$BASE_URL/reviews/789/helpful" \
  -H "Authorization: Bearer $TOKEN_MEMBER_A"
```

**Expected Response**: `HTTP/1.1 403 Forbidden`
```json
{
  "type": "https://wikicook.com/errors/self-vote-forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Bạn không thể tự bình chọn hữu ích cho bài đánh giá của chính mình.",
  "instance": "/api/v1/reviews/789/helpful"
}
```

---

#### Test 5: Input Validation Errors (HTTP 400 Bad Request)
Submit an invalid review with a comment under 10 characters:

```bash
curl -i -X POST "$BASE_URL/recipes/$RECIPE_ID/reviews" \
  -H "Authorization: Bearer $TOKEN_MEMBER_B" \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 5,
    "comment": "Ngon"
  }'
```

**Expected Response**: `HTTP/1.1 400 Bad Request`
```json
{
  "type": "https://wikicook.com/errors/validation-error",
  "title": "Bad Request",
  "status": 400,
  "detail": "Dữ liệu đánh giá không hợp lệ. Vui lòng kiểm tra lại các trường thông tin.",
  "instance": "/api/v1/recipes/105/reviews",
  "invalidParams": [
    {
      "name": "comment",
      "reason": "Nhận xét phải có ít nhất 10 ký tự"
    }
  ]
}
```

---

## 5. Manual Privacy Verification with `exiftool` (Constitution IV)

Execute this manual verification procedure to confirm that user home locations are completely protected:

1. **Capture a Test Photo**: Take a photo of cooked food with your smartphone camera with **Location Tags / GPS** toggled **ON**.
2. **Inspect Metadata Before Upload**:
   ```bash
   exiftool phone_photo.jpg | grep -iE 'GPS|Latitude|Longitude|Model|Make'
   ```
   *Output should show camera model (e.g., iPhone 15 / Galaxy S24) and exact GPS Coordinates.*
3. **Upload via Cooksnap API**:
   ```bash
   curl -X POST "$BASE_URL/recipes/105/reviews" \
     -H "Authorization: Bearer $TOKEN_MEMBER_A" \
     -F "rating=5" \
     -F "comment=Kiểm tra bảo mật quyền riêng tư ảnh Cooksnap." \
     -F "cooksnapFiles=@phone_photo.jpg;type=image/jpeg"
   ```
4. **Download Stored Image from Media Endpoint**:
   ```bash
   curl -o downloaded_cooksnap.webp "$BASE_URL/media/cooksnaps/{storageKey}"
   ```
5. **Inspect Metadata After Upload**:
   ```bash
   exiftool downloaded_cooksnap.webp | grep -iE 'GPS|Latitude|Longitude|Model|Make|Owner'
   ```
6. **Pass Condition**: `exiftool` returns **absolutely zero output**. All EXIF tags, GPS latitude/longitude, and camera serial numbers have been completely stripped by the TwelveMonkeys image processing pipeline.

