# Quickstart: Smart Shopping List & Ingredient Aggregation – 040

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **API**: [openapi.yaml](contracts/openapi.yaml)

This document guides developers through running, testing, and verifying the **040-shopping-list** module in a local development environment using Docker Compose, Spring Boot 3, and React Vite.

---

## 1. Environment Requirements

- **Java Development Kit (JDK)**: OpenJDK 21 (LTS)
- **Node.js**: v20.x or v22.x LTS, with `npm` v10+
- **Docker & Docker Compose**: Docker Engine 24+ to run PostgreSQL 16
- **Maven**: 3.9+ (or use the `./mvnw` bundled with the backend)

---

## 2. Start the Local Database

Use Docker Compose to start PostgreSQL 16 with Flyway migrations:

```bash
# Start the PostgreSQL 16 container
docker run -d --name wikicook-postgres \
  -e POSTGRES_DB=wikicook \
  -e POSTGRES_USER=wikicook_user \
  -e POSTGRES_PASSWORD=wikicook_secret \
  -p 5432:5432 \
  postgres:16-alpine
```

Check connectivity and run the Flyway migrations:
```bash
cd src/backend
./mvnw flyway:migrate
```

Confirm that the four tables of module 040 have been created successfully:
```bash
docker exec -it wikicook-postgres psql -U wikicook_user -d wikicook -c "\dt shopping*"
```
Expected output:
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

## 3. Run Automated Tests

### 3.1. Backend Unit & Integration Tests (Testcontainers)
The system automatically starts a real PostgreSQL 16 container through Testcontainers:

```bash
cd src/backend
./mvnw clean test -Dtest="com.wikicook.shoppinglist.**"
```

Check JaCoCo coverage ($\ge 70\%$ as required by Constitution II):
```bash
./mvnw jacoco:report
# Open the report at: target/site/jacoco/com.wikicook.shoppinglist.service/index.html
```

### 3.2. Frontend Tests & Lint
```bash
cd src/frontend
npm test -- src/features/shopping-list/
```

---

## 4. Test the API with cURL (Manual Verification Flow)

Assuming you already have a valid JWT Bearer token (`$JWT_TOKEN`):

### Step 1: Retrieve the Current Shopping List (Automatically Created If None Exists)
```bash
curl -X GET "http://localhost:8080/api/v1/shopping-lists/active" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Accept: application/json"
```

### Step 2: Manually Add a Shopping Item (Custom Item)
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

### Step 3: Aggregate Ingredients from a Single Recipe (Scale to Four Servings)
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

### Step 4: Offline Shopping Operations & Batch Synchronization (Offline Batch Sync)
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

### Step 5: Clear Purchased Items (Soft-Clear) & Undo
```bash
# Clear checked items
CLEAR_RESP=$(curl -s -X POST "http://localhost:8080/api/v1/shopping-lists/active/clear-purchased" \
  -H "Authorization: Bearer $JWT_TOKEN")
echo $CLEAR_RESP

# Retrieve clearBatchId and undo within 5 seconds
BATCH_ID=$(echo $CLEAR_RESP | jq -r '.clearBatchId')
curl -X POST "http://localhost:8080/api/v1/shopping-lists/active/undo-clear" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"clearBatchId\": \"$BATCH_ID\"}"
```

### Step 6: Move the List to History (Archive)
```bash
curl -X POST "http://localhost:8080/api/v1/shopping-lists/active/archive" \
  -H "Authorization: Bearer $JWT_TOKEN"
```
