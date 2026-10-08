# TEST CASES SPECIFICATION: WIKICOOK

## DOCUMENT METADATA & RESPONSIBILITY MATRIX

| Section | Title | Performed by (Author) | Reviewed by | Edited by |
| :---: | :--- | :---: | :---: | :---: |
| **Section 1** | Test Suite Overview & Traceability | **Trần Nguyễn Công Chung (itzchugnn)** | **Mai Văn Hiển (MaiHien3507)** | **Lê Quốc Hưng (LqHung06)** |
| **Section 2** | Core Test Cases (Auth & RBAC) | **Trần Nguyễn Công Chung (itzchugnn)** | **Mai Văn Hiển (MaiHien3507)** | **Lê Quốc Hưng (LqHung06)** |
| **Section 3** | Traceability Matrix & Execution Gates | **Trần Nguyễn Công Chung (itzchugnn)** | **Mai Văn Hiển (MaiHien3507)** | **Lê Quốc Hưng (LqHung06)** |

---

## 1. Test Suite Overview & Traceability
*Performed by: Trần Nguyễn Công Chung (itzchugnn) | Reviewed by: Mai Văn Hiển (MaiHien3507) | Edited by: Lê Quốc Hưng (LqHung06)*

This test specification defines the acceptance test suite for **WikiCook Module 1: User Authentication & Role-Based Access Control (RBAC)**, implementing the testing requirements formalized in:
1. **WikiCook Constitution** (`.specify/memory/constitution.md` - Principle II: Test-First NON-NEGOTIABLE).
2. **Master Test Plan** (`docs/test/test-plan.md`).
3. **Feature Specification** (`specs/user-authentication-roles/spec.md`).

All test cases are written following a strict Given-When-Then behavioral contract and serve as the baseline for automated backend integration tests (via Spring Boot Test & Testcontainers) and frontend unit/E2E tests (Vitest & Playwright).

---

## 2. Core Test Cases: Module 1 (Authentication & Roles)
*Performed by: Trần Nguyễn Công Chung (itzchugnn) | Reviewed by: Mai Văn Hiển (MaiHien3507) | Edited by: Lê Quốc Hưng (LqHung06)*

| Test ID | Module / Feature | Test Objective & Scenario | Preconditions & Inputs | Expected Result | Traceability | Test Level | Status |
| :---: | :--- | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC-001** | Registration | **Valid Email/Password Registration**<br>Verify that a guest can register an account with valid credentials. | **Preconditions:** Email is not in system.<br>**Input:**<br>- Name: `Nguyen Van A`<br>- Email: `vana@gmail.com`<br>- Password: `Password123` | 1. Account created with `Member` role.<br>2. Account status set to `UNVERIFIED`.<br>3. Verification email sent with single-use token valid for 24h. | US2, FR-001, FR-003, FR-004 | Integration (API) | Ready |
| **TC-002** | Registration | **Duplicate Email Registration Prevention**<br>Verify registration rejection when using an existing email. | **Preconditions:** `vana@gmail.com` already registered.<br>**Input:** Submit registration with same email (even with mixed case/spaces: ` VaNa@gmail.com `). | 1. Request rejected with HTTP 409 Conflict.<br>2. User prompt suggests signing in or resetting password.<br>3. No duplicate account created. | US2, FR-002 | Integration (API) | Ready |
| **TC-003** | Email Verification | **Account Activation Flow**<br>Verify account verification via token unlocks full member privileges. | **Preconditions:** Unverified account exists.<br>**Action:** User opens activation link with valid token within 24h. | 1. Account status updated to `VERIFIED`.<br>2. Verification token invalidated.<br>3. Publishing permissions (recipes, reviews, cooksnaps) unlocked. | US2, FR-005, FR-006 | Integration (API) | Ready |
| **TC-004** | Sign-In | **Standard Authentication**<br>Verify registered user can sign in with valid email and password. | **Preconditions:** Active, verified account exists.<br>**Input:** Valid email & password. | 1. HTTP 200 OK returned with valid JWT session cookie.<br>2. User authenticated and redirected to requested URL or home feed.<br>3. Last login timestamp updated. | US1, FR-007, FR-011 | Integration / E2E | Ready |
| **TC-005** | Sign-In | **Invalid Credentials & Privacy**<br>Verify failed sign-in does not reveal whether the email exists. | **Input:** Unregistered email OR registered email with wrong password. | 1. HTTP 401 Unauthorized returned.<br>2. Generic error displayed: *"Email or password is incorrect"*.<br>3. Password field cleared, no account enumeration exposed. | US1, FR-008, NFR-003 | Unit / Integration | Ready |
| **TC-006** | Session Management | **"Remember Me" Lifespan Validation**<br>Verify session expiration logic with and without "Remember me". | **Scenario A:** "Remember me" checked.<br>**Scenario B:** "Remember me" unchecked. | 1. Scenario A: Session persists across browser restarts for up to 30 days.<br>2. Scenario B: Session expires after 24h of inactivity or on browser session close. | US1, FR-009, FR-010 | Integration / E2E | Ready |
| **TC-007** | Security / Lockout | **Brute-Force Account Lockout**<br>Verify account is temporarily locked after repeated failures. | **Action:** Submit 5 consecutive incorrect password attempts for same account within 15 minutes. | 1. Account status set to `LOCKED` for 15 minutes.<br>2. Error states retry cooldown period.<br>3. Security notification sent to account owner's email. | US1, FR-012, Edge Case | Integration (API) | Ready |
| **TC-008** | Password Recovery | **Password Reset via Single-Use Token**<br>Verify user can reset forgotten password securely. | **Action:** Request reset link -> receive 30m token -> submit new valid password. | 1. Password hash updated in database.<br>2. Reset token immediately invalidated.<br>3. All active user sessions/tokens invalidated.<br>4. User signs in successfully with new password. | US4, FR-014, FR-015, FR-016 | Integration (API) | Ready |
| **TC-009** | OAuth 2.0 | **Google OAuth Sign-In & Linking**<br>Verify single sign-on with Google accounts. | **Scenario A:** New Google account.<br>**Scenario B:** Existing email account. | 1. Scenario A: Auto-creates verified `Member` account.<br>2. Scenario B: Safely links Google identity to existing WikiCook account without duplication. | US5, FR-018, FR-019 | Integration / E2E | Ready |
| **TC-010** | RBAC | **Role-Based Privilege Enforcement**<br>Verify authorization boundaries across Guest, Member, Moderator, Administrator. | **Test Actions:**<br>- Guest access to private planner.<br>- Member access to moderation queue.<br>- Moderator access to role management.<br>- Admin performing role change. | 1. Guest -> 401 Unauthorized / Login redirect.<br>2. Member -> 403 Forbidden for moderation.<br>3. Moderator -> 403 Forbidden for role change.<br>4. Admin -> 200 OK, role change logged in audit log.<br>5. Demoting last Admin blocked (FR-023). | US3, FR-020, FR-021, FR-022, FR-023 | Integration (API) | Ready |
| **TC-011** | Password Recovery / Security | **Outbound Email Rate Limiting**<br>Verify anti-abuse rate limiter prevents spamming of verification and password reset emails. | **Preconditions:** Active registered user exists.<br>**Action:** Submit >3 password reset or verification email requests within a 15-minute window for the same account. | 1. First 3 requests succeed with HTTP 200.<br>2. 4th request within 15 minutes is rejected with HTTP 429 Too Many Requests.<br>3. Response header includes `Retry-After`.<br>4. No excess email is dispatched. | US4, FR-017, NFR-001 | Integration (API) | Ready |

---

## 3. Traceability Matrix & Execution Gates
*Performed by: Trần Nguyễn Công Chung (itzchugnn) | Reviewed by: Mai Văn Hiển (MaiHien3507) | Edited by: Lê Quốc Hưng (LqHung06)*

### 3.1. Requirement to Test Case Mapping

| Specification Requirement | Covered by Test Cases | Target Automation Level |
| :--- | :--- | :--- |
| **FR-001 to FR-006** (Registration & Verification) | TC-001, TC-002, TC-003 | Spring Boot Integration Test (`AuthServiceTest`, `AuthControllerTest`) |
| **FR-007 to FR-013** (Sign-In, Session, Lockout) | TC-004, TC-005, TC-006, TC-007 | Spring Security Integration Test + Playwright E2E |
| **FR-014 to FR-017** (Password Reset & Rate Limiting) | TC-008, TC-011 | Spring Boot Integration Test (`PasswordResetServiceTest`, Redis RateLimiter) |
| **FR-018 to FR-019** (Google OAuth) | TC-009 | Spring Security OAuth2 Mock Tests |
| **FR-020 to FR-025** (RBAC & Audit Logging) | TC-010 | Method Security (`@PreAuthorize`) & Controller Integration Tests |
| **NFR-001 to NFR-003** (Security & Privacy) | TC-005, TC-007, TC-008, TC-011 | OWASP Zap Automated Scan & Security Unit Tests |

### 3.2. Execution Quality Gate
- Before merging branch `feature/SCRUM-27-user-authentication-roles`, all test cases TC-001 through TC-011 must execute in CI pipeline with **100% pass rate**.
- Code coverage for `com.wikicook.auth` package must satisfy **≥ 70% line & branch coverage** measured via JaCoCo.
