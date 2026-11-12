# Data Model: Community Reviews, Cooksnaps & Interactive Ratings – 070

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

This document specifies the Phase 1 Data Model for the **070-review-cooksnap** module, including the relational database schema (PostgreSQL 16), complete Flyway migration DDL script (`V501__create_reviews_and_cooksnaps_schema.sql`), JPA entities (Java 21), Java Record DTOs (API Transfer Contracts), and client-side TypeScript interfaces.

---

## 1. Relational Database Schema (PostgreSQL 16)

The design strictly complies with [Section 4.5 & Section 4.9 of `docs/analysis-and-design/database/full-schema.md`](../../docs/analysis-and-design/database/full-schema.md#49-danh-gia--cooksnap-070--e) and [Constitution Principles III & IV](../../.specify/memory/constitution.md). It is initialized through Flyway migration `V501__create_reviews_and_cooksnaps_schema.sql`.

```mermaid
erDiagram
    users ||--o{ reviews : "writes"
    users ||--o{ cooksnaps : "uploads"
    users ||--o{ review_helpful_votes : "votes"
    recipes ||--o{ reviews : "receives"
    recipes ||--o{ cooksnaps : "displays"
    reviews ||--o{ cooksnaps : "contains"
    reviews ||--o{ review_helpful_votes : "accumulates"

    recipes {
        bigint id PK
        varchar title
        numeric rating_avg "NUMERIC(3,2) NOT NULL DEFAULT 0.00"
        integer review_count "INTEGER NOT NULL DEFAULT 0"
        integer version "Optimistic lock (recipe content only)"
    }

    users {
        bigint id PK
        varchar display_name
        varchar avatar_url
        varchar role
        varchar status
    }

    reviews {
        bigint id PK
        bigint recipe_id FK "CASCADE"
        bigint user_id FK "SET NULL"
        smallint rating "CHECK BETWEEN 1 AND 5"
        varchar comment "VARCHAR(2000) NULL"
        varchar substitution_tip "VARCHAR(500) NULL"
        varchar status "CHECK IN ('VISIBLE','HIDDEN')"
        integer helpful_count "NOT NULL DEFAULT 0"
        timestamptz created_at "NOT NULL DEFAULT NOW()"
        timestamptz updated_at "NOT NULL DEFAULT NOW()"
    }

    cooksnaps {
        bigint id PK
        bigint recipe_id FK "CASCADE"
        bigint review_id FK "CASCADE"
        bigint user_id FK "SET NULL"
        varchar storage_key "VARCHAR(255) NOT NULL"
        varchar content_type "VARCHAR(50) NOT NULL"
        integer size_bytes "NOT NULL, <= 5MB"
        varchar caption "VARCHAR(300) NULL"
        varchar status "CHECK IN ('VISIBLE','HIDDEN')"
        timestamptz created_at "NOT NULL DEFAULT NOW()"
    }

    review_helpful_votes {
        bigint id PK
        bigint review_id FK "CASCADE"
        bigint user_id FK "CASCADE"
        timestamptz created_at "NOT NULL DEFAULT NOW()"
    }
```

---

### 1.1. `reviews` Table

Stores community culinary reviews, star ratings, and optional ingredient substitution tips.

| Column | Data Type | Nullable | Constraint / Default | Business Meaning |
|---|---|---|---|---|
| `id` | `BIGINT` | NOT NULL | `PRIMARY KEY GENERATED ALWAYS AS IDENTITY` | Unique primary key of the review |
| `recipe_id` | `BIGINT` | NOT NULL | `REFERENCES recipes(id) ON DELETE CASCADE` | Recipe being reviewed |
| `user_id` | `BIGINT` | NULL | `REFERENCES users(id) ON DELETE SET NULL` | Review author (NULL indicates deleted account) |
| `rating` | `SMALLINT` | NOT NULL | `CHECK (rating BETWEEN 1 AND 5)` | Star rating score (1 to 5 stars) |
| `comment` | `VARCHAR(2000)` | NULL | Default: `NULL` | Feedback text and cooking experience |
| `substitution_tip` | `VARCHAR(500)` | NULL | Default: `NULL` | Practical ingredient substitute advice |
| `status` | `VARCHAR(10)` | NOT NULL | `DEFAULT 'VISIBLE' CHECK (status IN ('VISIBLE','HIDDEN'))` | Visibility status (`VISIBLE` or `HIDDEN` by moderation) |
| `helpful_count` | `INTEGER` | NOT NULL | `DEFAULT 0 CHECK (helpful_count >= 0)` | Denormalized count of helpful community votes |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `DEFAULT NOW()` | Submission timestamp |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | `DEFAULT NOW()` | Last update timestamp |

**Indexes & Uniqueness Constraints:**
- `CREATE UNIQUE INDEX uq_reviews_recipe_user ON reviews(recipe_id, user_id) WHERE user_id IS NOT NULL;`
  $\rightarrow$ Strictly enforces that each registered user can submit **at most one active review** per recipe (anti-spam constraint).
- `CREATE INDEX idx_reviews_recipe_lookup ON reviews(recipe_id, created_at DESC) WHERE status = 'VISIBLE';`
  $\rightarrow$ Fast paginated retrieval of latest visible reviews for a recipe (NFR-001 $< 500\text{ms}$).
- `CREATE INDEX idx_reviews_recipe_rating ON reviews(recipe_id, rating DESC, created_at DESC) WHERE status = 'VISIBLE';`
  $\rightarrow$ Accelerates sorting by highest rating.
- `CREATE INDEX idx_reviews_recipe_helpful ON reviews(recipe_id, helpful_count DESC, created_at DESC) WHERE status = 'VISIBLE';`
  $\rightarrow$ Accelerates sorting by most helpful reviews.
- `CREATE INDEX idx_reviews_user_lookup ON reviews(user_id, created_at DESC) WHERE user_id IS NOT NULL;`
  $\rightarrow$ Enables fast lookup of reviews written by a specific user (profile activity).

---

### 1.2. `cooksnaps` Table

Stores user-submitted proof-of-cooking photos attached to reviews, with EXIF stripped and stored securely.

| Column | Data Type | Nullable | Constraint / Default | Business Meaning |
|---|---|---|---|---|
| `id` | `BIGINT` | NOT NULL | `PRIMARY KEY GENERATED ALWAYS AS IDENTITY` | Unique primary key of the Cooksnap photo |
| `recipe_id` | `BIGINT` | NOT NULL | `REFERENCES recipes(id) ON DELETE CASCADE` | Recipe associated with the photo |
| `review_id` | `BIGINT` | NOT NULL | `REFERENCES reviews(id) ON DELETE CASCADE` | Review associated with this photo |
| `user_id` | `BIGINT` | NULL | `REFERENCES users(id) ON DELETE SET NULL` | Uploader (NULL indicates deleted account) |
| `storage_key` | `VARCHAR(255)` | NOT NULL | | Relative storage key (e.g., `cs_105_789_uuid.webp`) |
| `content_type` | `VARCHAR(50)` | NOT NULL | `CHECK (content_type IN ('image/jpeg','image/png','image/webp'))` | MIME type of processed image |
| `size_bytes` | `INTEGER` | NOT NULL | `CHECK (size_bytes > 0 AND size_bytes <= 5242880)` | File size in bytes ($\le 5\text{MB}$) |
| `caption` | `VARCHAR(300)` | NULL | Default: `NULL` | Optional caption describing the photo |
| `status` | `VARCHAR(10)` | NOT NULL | `DEFAULT 'VISIBLE' CHECK (status IN ('VISIBLE','HIDDEN'))` | Visibility status (`VISIBLE` or `HIDDEN`) |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `DEFAULT NOW()` | Upload timestamp |

**Indexes & Foreign Key Constraints:**
- `CREATE INDEX idx_cooksnaps_review ON cooksnaps(review_id) WHERE status = 'VISIBLE';`
  $\rightarrow$ Instant loading of Cooksnaps belonging to a specific review.
- `CREATE INDEX idx_cooksnaps_recipe ON cooksnaps(recipe_id, created_at DESC) WHERE status = 'VISIBLE';`
  $\rightarrow$ Fast retrieval of Cooksnap photo galleries on recipe detail pages.
- `CREATE INDEX idx_cooksnaps_user ON cooksnaps(user_id) WHERE user_id IS NOT NULL;`
  $\rightarrow$ Displays user's uploaded Cooksnaps in their personal profile gallery.

**Privacy & Storage Standards (Constitution IV & Research R1):**
- Images are strictly stripped of all EXIF/GPS metadata before writing to storage.
- Storage keys follow the naming convention: `cs_{recipeId}_{reviewId}_{uuid}.webp`.
- Images are served via application endpoint `/api/v1/media/cooksnaps/{storage_key}`. No direct filesystem paths or absolute external URLs are stored in the database.

---

### 1.3. `review_helpful_votes` Table

Tracks user votes marking reviews as "Helpful" (`helpful_count`), preventing duplicate votes from the same account.

| Column | Data Type | Nullable | Constraint / Default | Business Meaning |
|---|---|---|---|---|
| `id` | `BIGINT` | NOT NULL | `PRIMARY KEY GENERATED ALWAYS AS IDENTITY` | Unique primary key of the vote |
| `review_id` | `BIGINT` | NOT NULL | `REFERENCES reviews(id) ON DELETE CASCADE` | Review receiving the helpful vote |
| `user_id` | `BIGINT` | NOT NULL | `REFERENCES users(id) ON DELETE CASCADE` | Authenticated user casting the vote |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `DEFAULT NOW()` | Vote timestamp |

**Indexes & Uniqueness Constraints:**
- `CONSTRAINT uq_review_helpful_votes UNIQUE (review_id, user_id)`
  $\rightarrow$ Prevents a user from voting "Helpful" more than once on the same review.
- `CREATE INDEX idx_review_helpful_votes_user ON review_helpful_votes(user_id, review_id);`
  $\rightarrow$ Rapid lookup of whether the current user has already upvoted a given set of reviews.

---

### 1.4. Summary Columns on `recipes` Table

The `recipes` table ([Section 4.5 of `full-schema.md`](../../docs/analysis-and-design/database/full-schema.md#45-cong-thuc-060-020--b)) contains denormalized rating metrics:

| Column | Data Type | Nullable | Default | Description |
|---|---|---|---|---|
| `rating_avg` | `NUMERIC(3,2)` | NOT NULL | `0.00` | Current arithmetic mean of all visible reviews (1.00 to 5.00) |
| `review_count` | `INTEGER` | NOT NULL | `0` | Total number of visible community reviews |

**Critical Architectural Invariant (Research R2):**
When calculating and updating `rating_avg` and `review_count` upon review submission, update, or status change, the query:
```sql
WITH stats AS (
    SELECT
        COALESCE(ROUND(AVG(rating)::numeric, 2), 0.00) AS calc_avg,
        COUNT(*)::integer AS calc_count
    FROM reviews
    WHERE recipe_id = :recipeId AND status = 'VISIBLE'
)
UPDATE recipes
SET rating_avg = stats.calc_avg,
    review_count = stats.calc_count
FROM stats
WHERE recipes.id = :recipeId;
```
> [!IMPORTANT]
> **DO NOT increment `version = r.version + 1`** in the `UPDATE recipes` query for rating calculation. Bumping the recipe's optimistic locking version during background community reviews causes false `409 Conflict` errors for recipe authors or moderators who are concurrently editing the recipe content.

---

## 2. Flyway Migration DDL (`V501__create_reviews_and_cooksnaps_schema.sql`)

```sql
-- ============================================================================
-- Migration: V501__create_reviews_and_cooksnaps_schema.sql
-- Module: 070-review-cooksnap
-- Standards: PostgreSQL 16, full-schema.md §4.9, Constitution III & IV
-- ============================================================================

-- 1. Table: reviews
CREATE TABLE reviews (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipe_id        BIGINT       NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    user_id          BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,
    rating           SMALLINT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment          VARCHAR(2000) NULL,
    substitution_tip VARCHAR(500)  NULL,
    status           VARCHAR(10)  NOT NULL DEFAULT 'VISIBLE' CHECK (status IN ('VISIBLE', 'HIDDEN')),
    helpful_count    INTEGER      NOT NULL DEFAULT 0 CHECK (helpful_count >= 0),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Partial Unique Index: 1 active review per user per recipe
CREATE UNIQUE INDEX uq_reviews_recipe_user 
    ON reviews(recipe_id, user_id) 
    WHERE user_id IS NOT NULL;

-- Performance Indexes for filtering, pagination, and multi-criteria sorting
CREATE INDEX idx_reviews_recipe_lookup 
    ON reviews(recipe_id, created_at DESC) 
    WHERE status = 'VISIBLE';

CREATE INDEX idx_reviews_recipe_rating 
    ON reviews(recipe_id, rating DESC, created_at DESC) 
    WHERE status = 'VISIBLE';

CREATE INDEX idx_reviews_recipe_helpful 
    ON reviews(recipe_id, helpful_count DESC, created_at DESC) 
    WHERE status = 'VISIBLE';

CREATE INDEX idx_reviews_user_lookup 
    ON reviews(user_id, created_at DESC) 
    WHERE user_id IS NOT NULL;

-- 2. Table: cooksnaps
CREATE TABLE cooksnaps (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipe_id    BIGINT       NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    review_id    BIGINT       NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    user_id      BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,
    storage_key  VARCHAR(255) NOT NULL,
    content_type VARCHAR(50)  NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    size_bytes   INTEGER      NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 5242880),
    caption      VARCHAR(300) NULL,
    status       VARCHAR(10)  NOT NULL DEFAULT 'VISIBLE' CHECK (status IN ('VISIBLE', 'HIDDEN')),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cooksnaps_review 
    ON cooksnaps(review_id) 
    WHERE status = 'VISIBLE';

CREATE INDEX idx_cooksnaps_recipe 
    ON cooksnaps(recipe_id, created_at DESC) 
    WHERE status = 'VISIBLE';

CREATE INDEX idx_cooksnaps_user 
    ON cooksnaps(user_id) 
    WHERE user_id IS NOT NULL;

-- 3. Table: review_helpful_votes
CREATE TABLE review_helpful_votes (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    review_id  BIGINT      NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_review_helpful_votes UNIQUE (review_id, user_id)
);

CREATE INDEX idx_review_helpful_votes_user 
    ON review_helpful_votes(user_id, review_id);

-- 4. Trigger for automatic updated_at timestamp on reviews
CREATE OR REPLACE FUNCTION trg_update_reviews_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reviews_updated_at
BEFORE UPDATE ON reviews
FOR EACH ROW
EXECUTE FUNCTION trg_update_reviews_updated_at();

-- 5. Comments on tables and columns for self-documenting schema
COMMENT ON TABLE reviews IS 'Community reviews and star ratings on recipes';
COMMENT ON COLUMN reviews.status IS 'Visibility flag: VISIBLE or HIDDEN by moderator';
COMMENT ON COLUMN reviews.substitution_tip IS 'Optional tip explaining ingredient replacement';
COMMENT ON TABLE cooksnaps IS 'User-uploaded proof-of-cooking images attached to reviews';
COMMENT ON COLUMN cooksnaps.storage_key IS 'Relative file storage key (e.g. cs_12_45_uuid.webp)';
COMMENT ON TABLE review_helpful_votes IS 'Helpful votes cast by authenticated users on reviews';
```

---

## 3. JPA Entities (Backend Entities - Java 21)

Located in package `com.wikicook.review.domain`. Entities follow **Constitution Principle III** (Layered architecture; entities are never directly exposed through web controllers).

### 3.1. `ReviewStatus` Enum
```java
package com.wikicook.review.domain;

public enum ReviewStatus {
    VISIBLE,
    HIDDEN
}
```

### 3.2. `Review` Entity
```java
package com.wikicook.review.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "user_id")
    private Long userId;

    @NotNull
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Short rating;

    @Size(max = 2000)
    @Column(length = 2000)
    private String comment;

    @Size(max = 500)
    @Column(name = "substitution_tip", length = 500)
    private String substitutionTip;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.VISIBLE;

    @Min(0)
    @Column(name = "helpful_count", nullable = false)
    @Builder.Default
    private Integer helpfulCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Cooksnap> cooksnaps = new ArrayList<>();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewHelpfulVote> helpfulVotes = new ArrayList<>();

    // Domain helper methods
    public void addCooksnap(Cooksnap cooksnap) {
        cooksnaps.add(cooksnap);
        cooksnap.setReview(this);
    }

    public void removeCooksnap(Cooksnap cooksnap) {
        cooksnaps.remove(cooksnap);
        cooksnap.setReview(null);
    }

    public void incrementHelpfulCount() {
        this.helpfulCount = (this.helpfulCount == null ? 0 : this.helpfulCount) + 1;
    }

    public void decrementHelpfulCount() {
        if (this.helpfulCount != null && this.helpfulCount > 0) {
            this.helpfulCount--;
        }
    }

    public void hide() {
        this.status = ReviewStatus.HIDDEN;
    }

    public void restore() {
        this.status = ReviewStatus.VISIBLE;
    }
}
```

### 3.3. `Cooksnap` Entity
```java
package com.wikicook.review.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "cooksnaps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cooksnap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "user_id")
    private Long userId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @NotBlank
    @Size(max = 50)
    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @NotNull
    @Positive
    @Max(5242880) // 5 MB limit
    @Column(name = "size_bytes", nullable = false)
    private Integer sizeBytes;

    @Size(max = 300)
    @Column(length = 300)
    private String caption;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.VISIBLE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public void hide() {
        this.status = ReviewStatus.HIDDEN;
    }

    public void restore() {
        this.status = ReviewStatus.VISIBLE;
    }
}
```

### 3.4. `ReviewHelpfulVote` Entity
```java
package com.wikicook.review.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "review_helpful_votes", uniqueConstraints = {
    @UniqueConstraint(name = "uq_review_helpful_votes", columnNames = {"review_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewHelpfulVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
```

---

## 4. Java Record DTOs (API Transfer Contracts)

Located in package `com.wikicook.review.dto`. Java 21 Records guarantee immutability, thread-safety, and complete separation between persistence entities and web clients (**Constitution Principle III**).

### 4.1. Request DTOs

#### `CreateReviewRequest`
```java
package com.wikicook.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
    @NotNull(message = "Điểm đánh giá không được để trống")
    @Min(value = 1, message = "Điểm đánh giá tối thiểu là 1 sao")
    @Max(value = 5, message = "Điểm đánh giá tối đa là 5 sao")
    Short rating,

    @Size(max = 2000, message = "Nội dung nhận xét tối đa 2000 ký tự")
    String comment,

    @Size(max = 500, message = "Mẹo thay thế nguyên liệu tối đa 500 ký tự")
    String substitutionTip
) {}
```

#### `UpdateReviewRequest`
```java
package com.wikicook.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateReviewRequest(
    @NotNull(message = "Điểm đánh giá không được để trống")
    @Min(value = 1, message = "Điểm đánh giá tối thiểu là 1 sao")
    @Max(value = 5, message = "Điểm đánh giá tối đa là 5 sao")
    Short rating,

    @Size(max = 2000, message = "Nội dung nhận xét tối đa 2000 ký tự")
    String comment,

    @Size(max = 500, message = "Mẹo thay thế nguyên liệu tối đa 500 ký tự")
    String substitutionTip
) {}
```

---

### 4.2. Response DTOs

#### `CooksnapDto`
```java
package com.wikicook.review.dto;

public record CooksnapDto(
    Long id,
    String storageKey,
    String url,
    String thumbnailUrl,
    String caption,
    Integer sizeBytes,
    String contentType
) {}
```

#### `ReviewAuthorDto`
```java
package com.wikicook.review.dto;

public record ReviewAuthorDto(
    Long id,
    String displayName,
    String avatarUrl,
    String role,
    boolean isDeleted
) {}
```

#### `ReviewResponse`
```java
package com.wikicook.review.dto;

import java.time.Instant;
import java.util.List;

public record ReviewResponse(
    Long id,
    Long recipeId,
    ReviewAuthorDto author,
    short rating,
    String comment,
    String substitutionTip,
    List<CooksnapDto> cooksnaps,
    int helpfulCount,
    boolean hasVotedHelpful,
    Instant createdAt,
    Instant updatedAt,
    boolean isOwner
) {}
```

#### `HelpfulVoteResponse`
```java
package com.wikicook.review.dto;

public record HelpfulVoteResponse(
    Long reviewId,
    int helpfulCount,
    boolean hasVoted
) {}
```

#### `RatingSummaryDto`
```java
package com.wikicook.review.dto;

import java.math.BigDecimal;
import java.util.Map;

public record RatingSummaryDto(
    BigDecimal averageRating,
    int totalReviews,
    Map<Integer, Long> ratingDistribution // 1 -> count, 2 -> count, ..., 5 -> count
) {}
```

#### `ReviewListResponse`
```java
package com.wikicook.review.dto;

import java.util.List;

public record ReviewListResponse(
    List<ReviewResponse> items,
    long totalElements,
    int totalPages,
    int currentPage,
    int pageSize,
    RatingSummaryDto ratingSummary
) {}
```

---

### 4.3. RFC 7807 Problem Detail (Error Responses)

Standardized API error response format returned by `ReviewExceptionHandler`:

```json
{
  "type": "https://wikicook.com/errors/self-review-forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Bạn là tác giả công thức này — bạn có thể xem đánh giá, nhưng không thể tự chấm điểm.",
  "instance": "/api/v1/recipes/105/reviews"
}
```

```json
{
  "type": "https://wikicook.com/errors/duplicate-review",
  "title": "Conflict",
  "status": 409,
  "detail": "Bạn đã đánh giá công thức này rồi. Vui lòng chỉnh sửa đánh giá hiện có.",
  "instance": "/api/v1/recipes/105/reviews"
}
```

```json
{
  "type": "https://wikicook.com/errors/rate-limit-exceeded",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Bạn gửi đánh giá quá nhanh. Vui lòng thử lại sau 10 phút.",
  "instance": "/api/v1/recipes/105/reviews"
}
```

---

## 5. TypeScript Interfaces (Client-Side)

Located in `src/features/reviews/types/review.ts` for use across the React SPA:

```typescript
/**
 * Types & Interfaces for Feature 070: Community Reviews & Cooksnaps
 */

export type ReviewStatus = 'VISIBLE' | 'HIDDEN';

export type ReviewSortOption = 
  | 'NEWEST' 
  | 'RATING_DESC' 
  | 'RATING_ASC' 
  | 'HELPFUL_DESC' 
  | 'WITH_COOKSNAP';

export interface Cooksnap {
  id: number;
  storageKey: string;
  url: string;
  thumbnailUrl: string;
  caption?: string | null;
  sizeBytes: number;
  contentType: string;
}

export interface ReviewAuthor {
  id: number | null;
  displayName: string;
  avatarUrl?: string | null;
  role?: string;
  isDeleted: boolean;
}

export interface Review {
  id: number;
  recipeId: number;
  author: ReviewAuthor;
  rating: number; // 1 to 5
  comment?: string | null;
  substitutionTip?: string | null;
  cooksnaps: Cooksnap[];
  helpfulCount: number;
  hasVotedHelpful: boolean;
  createdAt: string; // ISO 8601 string
  updatedAt: string; // ISO 8601 string
  isOwner: boolean;
}

export interface RatingDistribution {
  1: number;
  2: number;
  3: number;
  4: number;
  5: number;
}

export interface RatingSummary {
  averageRating: number;
  totalReviews: number;
  ratingDistribution: RatingDistribution;
}

export interface ReviewListResponse {
  items: Review[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  ratingSummary: RatingSummary;
}

export interface CreateReviewPayload {
  rating: number;
  comment?: string;
  substitutionTip?: string;
  cooksnapFiles?: File[];
}

export interface UpdateReviewPayload {
  rating: number;
  comment?: string;
  substitutionTip?: string;
}

export interface HelpfulVoteResult {
  reviewId: number;
  helpfulCount: number;
  hasVoted: boolean;
}

export interface ReviewFilterCriteria {
  recipeId: number;
  page?: number;
  size?: number;
  sort?: ReviewSortOption;
  rating?: number;
  hasCooksnapOnly?: boolean;
}

export interface ApiProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  invalidParams?: Array<{
    name: string;
    reason: string;
  }>;
}
```

