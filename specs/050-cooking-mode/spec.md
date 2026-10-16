# Feature Specification: Hands-free Cooking Mode & Concurrent Timers

**Feature Branch**: `feature/050-cooking-mode` *(Sprint 02 Backlog / Sprint 03 Candidate)*

**Created**: 2026-10-16

**Status**: Draft (Ready for Review)

**Input**: User description: "Tạo đặc tả yêu cầu chức năng cho tính năng 050-cooking-mode: Hands-free Cooking Mode & Concurrent Timers (Chế độ nấu ăn rảnh tay và Đếm giờ song song) của dự án WikiCook theo đúng cấu trúc spec-template.md."

## Summary

When cooking in an active home kitchen, users frequently encounter physical friction with standard mobile interfaces: hands are coated with water, oil, flour, or food ingredients, or occupied holding utensils. Standard mobile device screens automatically dim and lock after 30 to 60 seconds of inactivity, forcing the cook to repeatedly clean their hands or touch the screen with dirty fingers. Furthermore, conventional recipe layouts display small text intended for close-up reading rather than glanceable viewing from across a countertop or stove. Multi-stage culinary preparation also requires tracking distinct, overlapping cooking durations (e.g., boiling pasta for 9 minutes while simultaneously searing salmon for 4 minutes).

**WikiCook Cooking Mode** delivers a dedicated, distraction-free kitchen execution environment:
1. An immersive, high-contrast, full-screen step-by-step viewer that maximizes readability from a distance and attempts to keep the screen illuminated throughout preparation.
2. Oversized touch navigation targets and keyboard shortcuts enabling interaction via elbows, wrists, or remote keyboards without requiring precise touch.
3. Multiple parallel countdown timers automatically suggested from recipe instruction text or created manually, accompanied by clear audio and visual alerts.
4. An optional online voice-assistant layer supporting localized Vietnamese commands for zero-touch step progression and timer control.
5. Robust local session preservation that safeguards the cooking state and active timers against accidental page refreshes, tab closures, or network interruptions.

**In scope**:
- Full-screen distraction-free cooking viewer with enlarged typography and high-contrast toggle.
- Best-effort display wakefulness retention while active in Cooking Mode.
- Step-by-step navigation (forward, backward, jump to step) with persistent step-level ingredient checklists.
- Multi-timer concurrent countdown engine with custom duration input, pause, resume, and repeating audio alarms.
- Push-to-talk voice navigation in Vietnamese with conservative acoustic confidence filtering.
- Client-side cooking session persistence with a 4-hour expiration window and overdue timer calculations.
- Keyboard navigation (Space, Arrow keys) for desktop and tablet users.

**Out of scope** (handled in separate specifications):
- Recipe creation and step editing (`060-recipe-editor`).
- Nutritional estimation calculations (`090-nutrition-analyzer`).
- Video streaming or live camera-based hand gesture recognition.
- Voice-synthesized conversational AI cooking dialogue (deferred to post-MVP roadmap).

---

## Clarifications

### Session 2026-10-16
- Q: Khi bộ đếm giờ (timer) chạm mốc 00:00, nếu người dùng không thao tác tắt thì âm thanh chuông báo sẽ reo liên tục vô thời hạn hay tự động ngắt sau một khoảng thời gian nhất định? → A: Reo tối đa 60 giây rồi tự động ngắt âm thanh, chuyển sang hiển thị cảnh báo đỏ "Đã quá hạn X phút" (Overdue) nhấp nháy trên màn hình cho đến khi người dùng tắt.
- Q: Khi người dùng đang nấu dở một công thức (trong thời hạn 4 tiếng) mà mở tiếp một công thức khác và bấm "Bắt đầu nấu", hệ thống sẽ xử lý phiên nấu ăn như thế nào? → A: Lưu phiên độc lập theo từng mã công thức (recipe_id) trong LocalStorage (key: `cooking_session_{recipe_id}`), cho phép người dùng mở nhiều tab trên trình duyệt để nấu song song các món khác nhau mà không sợ xung đột hay mất dấu timer.
- Q: Khi người dùng nói khẩu lệnh "Đọc lại", hệ thống sẽ phản hồi bằng giọng nói (Text-to-Speech đọc nội dung bước) hay chỉ phản hồi bằng tín hiệu hình ảnh và âm báo ngắn? → A: Đọc to toàn bộ nội dung hướng dẫn của bước hiện tại bằng Web Speech Synthesis (Text-to-Speech) tiếng Việt (`vi-VN`). Nếu thiết bị/trình duyệt không hỗ trợ gói giọng tiếng Việt, hệ thống tự động kích hoạt phương án dự phòng (fallback): phát âm báo ngắn (earcon), chớp sáng viền nội dung bước và hiển thị thông báo nhẹ.
- Q: Khi một bộ đếm giờ (timer) đang chạy đếm ngược, người dùng có thể điều chỉnh nhanh thời gian (như cộng thêm 1 phút, 5 phút) trực tiếp trên thẻ timer hay không? → A: Cung cấp các nút chip bấm nhanh "+1 phút", "+5 phút" trực tiếp trên mỗi thẻ timer đang chạy để người dùng gia hạn thời gian tức thì chỉ với 1 lần chạm mà không cần tạm dừng hay tạo lại timer mới.
- Q: Khi người dùng bấm "Hoàn thành nấu ăn" hoặc "Thoát" khỏi chế độ nấu, nếu vẫn còn bộ đếm giờ (timer) đang chạy đếm ngược thì hệ thống sẽ xử lý như thế nào? → A: Hiển thị hộp thoại cảnh báo kèm số lượng timer đang chạy, cung cấp hai tùy chọn: "Tiếp tục chạy bộ đếm giờ" (chuyển sang widget đếm giờ nổi trên trang công thức) hoặc "Hủy tất cả và thoát" (xóa sạch dữ liệu phiên và dừng tất cả timer).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Full-Screen & Always-On Cooking View (Priority: P1 - MVP)

As a **home cook preparing a dish**, I want to view recipe instructions in an enlarged, full-screen interface that prevents my device screen from turning off, so that I can easily read directions from across the kitchen counter without touching my phone with messy hands.

**Why this priority**: Solves the fundamental pain point of screen timeout and poor legibility in physical cooking environments. This forms the essential visual foundation of Cooking Mode.

**Independent Test**: From any published recipe, tap "Start Cooking". Verify the interface transitions into full-screen mode displaying only the current step in high-contrast text, and verify the device screen remains illuminated without locking for at least 5 minutes of user inactivity.

**Acceptance Scenarios**:

1. **Given** a published recipe with multiple preparation steps, **When** the user taps "Bắt đầu nấu" (Start Cooking), **Then** the system opens a dedicated full-screen view focused exclusively on Step 1, displaying an oversized step number, bold instruction text readable from 1.5 meters, and a prominent exit button.
2. **Given** the user enters Cooking Mode, **When** the device and browser support screen wake retention, **Then** the system activates wake lock so the display remains continuously illuminated without dimming or locking.
3. **Given** the device or browser does not support automatic screen wake retention (or battery saver is engaged), **When** Cooking Mode initializes, **Then** the system displays a non-intrusive notification banner: "Thiết bị không hỗ trợ giữ sáng tự động. Vui lòng kiểm tra cài đặt màn hình của bạn." (Device does not support auto-wake. Please check your screen timeout settings).
4. **Given** the user is in Cooking Mode, **When** they toggle the High-Contrast mode icon, **Then** the color palette switches between a clean light theme and an ultra-high-contrast dark theme (pure black background `#000000` with high-luminance white and amber accents).
5. **Given** the user is on the final step, **When** they tap "Hoàn thành nấu ăn" (Finish Cooking), **Then** if no timers are active, the screen wake lock is released, full-screen mode is exited, and the user returns to the recipe overview page with a completion banner; if active timers remain, a confirmation dialog appears allowing the user to either keep timers running in a floating drawer or terminate all.

---

### User Story 2 - Oversized Step Navigation & Keyboard Controls (Priority: P1 - MVP)

As a **cook with busy or dirty hands**, I want large, easily tappable navigation targets and keyboard shortcuts to move between steps, so that I can advance instructions using my wrist, forearm, or keyboard without precision tapping.

**Why this priority**: Essential physical usability. Touch targets must accommodate imprecise contact in wet, hurried kitchen conditions.

**Independent Test**: Load a 5-step recipe in Cooking Mode. Using forearm taps on the oversized bottom buttons, navigate through all steps. Connect a keyboard and confirm pressing the Spacebar advances steps and the Left Arrow returns to previous steps.

**Acceptance Scenarios**:

1. **Given** the user is viewing Step $N$ of a recipe with $M$ steps ($N < M$), **When** they tap the primary "Bước tiếp theo" (Next Step) touch target (minimum dimension $64 \times 64$ pixels located at the bottom of the viewport), **Then** the view transitions smoothly to Step $N+1$, updating the progress indicator (e.g., "Bước 2 / 5").
2. **Given** the user is viewing Step $N$ ($N > 1$), **When** they tap the secondary "Bước trước" (Previous Step) touch target, **Then** the view returns to Step $N-1$.
3. **Given** the user has a keyboard attached to their tablet, laptop, or kitchen screen, **When** they press the `Spacebar` or `Right Arrow` key, **Then** the system advances to the next step.
4. **Given** the user presses the `Left Arrow` key, **When** they are on Step $N > 1$, **Then** the system returns to the previous step.
5. **Given** a recipe step has associated ingredients, **When** that step is displayed, **Then** the system displays an embedded subsection listing only the ingredients and measured quantities required for that specific step.
6. **Given** a recipe step lacks granular step-level ingredient mappings, **When** the step is displayed, **Then** the system provides a floating quick-access button "Xem nguyên liệu" (View Ingredients) that opens a sliding sheet showing the complete recipe ingredient checklist.

---

### User Story 3 - Multi-Timer Concurrent Countdown & Alarms (Priority: P2)

As a **home cook managing multi-phase dishes**, I want step-specific timers that can run simultaneously and alert me loudly when time expires, so that I never burn food or miss critical boiling/baking thresholds.

**Why this priority**: Timing failures ruin dishes. Integrating parallel countdown timers directly into recipe steps prevents mental overload and eliminates the need for separate kitchen gadgets.

**Independent Test**: On a step with "Đun sôi trong 8 phút", verify an 8-minute timer button is automatically provided. Tap to start, add a second manual 2-minute timer, confirm both countdown concurrently without drift, and verify a loud repeating alarm sounds when the first timer reaches 00:00.

**Acceptance Scenarios**:

1. **Given** an instruction step contains standard duration expressions (e.g., "nướng 15 phút", "luộc 8 phút", "hầm 1 giờ 30 phút"), **When** the step is rendered, **Then** the system parses the text and displays a pre-configured quick-start timer button with the detected duration.
2. **Given** an instruction step specifies a duration range (e.g., "xào trong 3-5 phút"), **When** the quick-start timer is generated, **Then** the system sets the timer to the upper bound (5 minutes) and displays a secondary button for the lower bound (3 minutes).
3. **Given** any step in Cooking Mode, **When** the user taps "+ Thêm hẹn giờ" (Add Custom Timer), **Then** a numeric duration picker opens allowing the user to label the timer (e.g., "Trứng luộc lòng đào") and set hours, minutes, and seconds.
4. **Given** multiple timers have been initiated across different steps, **When** the user navigates between steps, **Then** all active timers continue counting down concurrently in a persistent top/bottom drawer showing real-time remaining minutes and seconds.
5. **Given** an active timer reaches `00:00:00`, **When** the active cooking tab is visible, **Then** the system plays a continuous, repeating acoustic alarm sound for a maximum of 60 seconds (automatically muting audio after 60 seconds if unacknowledged), flashes the screen perimeter in pulsing red/amber, triggers device vibration on supported mobile devices, and displays a persistent pulsing red badge "Đã quá hạn X phút" (Overdue by X minutes) until dismissed.
6. **Given** an alarm is sounding, **When** the user taps anywhere on the screen or presses the `Spacebar`, **Then** the alarm sound and vibration immediately silence, and the timer card transitions to a "Hoàn thành" (Completed) state.
7. **Given** two timers expire at the exact same moment, **When** both reach zero, **Then** the system plays the alarm continuously (up to 60 seconds) and highlights both expired timer cards simultaneously.
8. **Given** a timer is actively counting down, **When** the user taps a quick-add chip ("+1 phút" or "+5 phút") on the active timer card, **Then** the system immediately extends the timer's remaining duration and target end timestamp by 60 or 300 seconds respectively without interrupting the countdown or resetting state.

---

### User Story 4 - Voice-Assisted Hands-Free Navigation (Priority: P3 - Enhancement)

As a **cook whose hands are completely covered in food**, I want to speak simple Vietnamese commands into my device to advance steps and manage timers, so that I never have to touch the screen during mess-intensive tasks.

**Why this priority**: High-value accessibility enhancement for advanced kitchen scenarios. Designated as P3 because it acts as an online enhancement; core cooking mode remains 100% usable via touch and keyboard if voice is inactive or unavailable.

**Independent Test**: Enable microphone permissions and tap the Push-to-Talk microphone button. Speak "Tiếp theo" in a normal tone; verify the system advances to the next step. Speak background chatter or kitchen noise; verify the system ignores ambiguous inputs without false transitions.

**Acceptance Scenarios**:

1. **Given** an active Internet connection and granted microphone permissions, **When** the user taps the microphone icon to activate voice listening, **Then** the system displays a glowing listening state with a subtle audio prompt.
2. **Given** voice listening is active, **When** the user clearly utters one of the supported standard command phrases ("Tiếp theo", "Quay lại", "Đọc lại", "Hẹn giờ", "Tắt chuông"), **Then** the system executes the corresponding action within 1.0 second and provides visual confirmation (e.g., green border pulse). For "Đọc lại", the system synthesizes and speaks the current step's instruction text aloud using the browser's Vietnamese Text-to-Speech voice (`vi-VN`).
3. **Given** voice listening is active, **When** background kitchen noise (sizzling pan, range hood, running faucet) produces ambiguous audio with acoustic recognition confidence $< 70\%$, **Then** the system ignores the sound, displays a brief amber notice "Không rõ khẩu lệnh", and strictly refrains from advancing or altering steps.
4. **Given** the device loses Internet connectivity while Cooking Mode is active, **When** the user attempts to use voice commands, **Then** the system gracefully disables the voice listening mode, displays a message "Tính năng giọng nói tạm ngừng do mất kết nối mạng", while keeping all step navigation and active timers functioning locally.
5. **Given** the user has not interacted with voice for 30 seconds after completing a command, **When** the idle timeout expires, **Then** the microphone automatically deactivates to conserve battery and preserve user privacy.
6. **Given** the user triggers the "Đọc lại" command, **When** the device or browser lacks an installed Vietnamese SpeechSynthesis voice (`vi-VN`), **Then** the system plays an earcon chime, flashes the step content border, and displays an informational toast "Thiết bị chưa hỗ trợ giọng đọc tiếng Việt" without failing or disrupting the session.

---

### User Story 5 - Session Recovery & Interruption Protection (Priority: P3)

As a **cook who accidentally refreshed the browser or closed the tab**, I want my cooking session and timer countdowns restored when I return, so that I do not lose my place in the recipe or ruin my meal timing.

**Why this priority**: Web browsers are susceptible to accidental reloads, swipe-to-back gestures, or memory reclamation by mobile operating systems. Session recovery protects users from catastrophic timing loss.

**Independent Test**: Start cooking a recipe, advance to Step 3, start a 5-minute timer. Force-refresh the browser tab (F5). Upon reload, verify the system prompts to resume Step 3 and resumes the timer with the elapsed time deducted accurately.

**Acceptance Scenarios**:

1. **Given** an active cooking session on recipe $R$, **When** the user advances steps or starts/modifies timers, **Then** the system continuously persists the session state (`recipe_id`, `current_step_index`, `active_timers`, `session_start_timestamp`) in browser local storage.
2. **Given** a cooking session was active within the past 4 hours, **When** the user re-opens the recipe page or refreshes the browser, **Then** the system presents a prominent modal dialog: "Tiếp tục phiên nấu ăn? Bạn đang nấu dở ở Bước 3." with buttons "Tiếp tục" (Resume) and "Bắt đầu lại" (Start Over).
3. **Given** a timer was running when the browser was closed, **When** the session is resumed before the timer expiration timestamp, **Then** the system recalculates `remaining_seconds = planned_end_time - current_time` and continues the countdown accurately.
4. **Given** a timer expired while the browser was closed, **When** the session is resumed, **Then** the system immediately displays the timer card in a high-priority red alert state showing "Đã quá hạn X phút" (Overdue by X minutes).
5. **Given** a stored cooking session exceeds the 4-hour expiration window (TTL), **When** the recipe page is opened, **Then** the expired session data is automatically purged, and the recipe loads in its default initial state.
6. **Given** the user explicitly taps "Hoàn thành nấu ăn" hoặc "Hủy phiên nấu", **When** active countdown timers are still running, **Then** the system presents a confirmation dialog warning that active timers remain, offering choices to keep them running in a background floating widget or discard all timers and wipe the recipe session.
7. **Given** an active cooking session exists for Recipe A, **When** the user opens Recipe B in another browser tab and starts Cooking Mode, **Then** the system maintains an isolated cooking session (`cooking_session_{recipe_id}`) for Recipe B without overwriting, pausing, or corrupting Recipe A's progress and timers.

---

### Edge Cases

1. **Audio Autoplay Policy Restrictions**: Web browsers block unsolicited audio playback until the user has performed an explicit gesture. Because Cooking Mode is initiated by the user tapping "Bắt đầu nấu" (Start Cooking), this gesture is captured to prime the Web Audio context, ensuring timer alarm sounds play unhindered even if the user does not touch the screen again until timer completion.
2. **Tab Visibility Changes & CPU Throttling**: When a mobile browser tab is placed in the background or the screen locks, browser engines throttle JavaScript timers (`setInterval`). The system MUST calculate countdowns using absolute hardware clock differentials (`Date.now()` / timestamp target) rather than tick counters, ensuring zero cumulative drift when the tab regains focus.
3. **Multi-Recipe & Multi-Device Session Isolation**: Cooking sessions are isolated locally by discrete keys (`cooking_session_{recipe_id}`). A user can cook multiple recipes concurrently across separate browser tabs without state collision or timer corruption. Likewise, cross-device cooking sessions remain local to each physical client, avoiding multi-user overwrites.
4. **Microphone Permission Denial**: If the user permanently denies microphone access, the voice toggle button is disabled with a tooltip explaining that microphone permission is required in browser settings, while the rest of Cooking Mode operates normally.
5. **Simultaneous Timer Expirations**: When two or more timers expire within seconds of each other, the system sounds a single coordinated alarm loop and displays a consolidated badge (e.g., "2 bộ đếm giờ đã hoàn thành!"). Silencing the alarm silences both timers while leaving their completion cards visible until dismissed individually.
6. **Unattended Expired Timers**: If a timer expires and the user does not dismiss the alarm immediately, the repeating audio alarm will automatically mute after 60 seconds to prevent battery depletion and prolonged kitchen noise, while leaving the screen in a persistent pulsing red badge indicating "Đã quá hạn X phút" (Overdue by X minutes) until acknowledged.
7. **Vietnamese Text-to-Speech (TTS) Voice Availability**: Availability of `vi-VN` speech voices in `window.speechSynthesis` varies across client operating systems. If no Vietnamese voice is discovered upon initializing Cooking Mode, the "Đọc lại" command automatically falls back to visual step border highlighting and earcon chime feedback without throwing unhandled exceptions.
8. **Active Timers on Mode Exit**: If a user exits Cooking Mode or marks a recipe finished while one or more timers are still actively running, the system does not silently kill the countdowns. It prompts with a warning modal offering either to keep timers active via a compact floating notification widget on the recipe detail page, or to confirm discarding all timers and purging the session.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a dedicated "Bắt đầu nấu" (Start Cooking) entry point on every published recipe detail view.
- **FR-002**: System MUST transition into an immersive full-screen viewport layout that hides default website navigation headers, footers, and sidebars.
- **FR-003**: System MUST request and maintain Screen Wake Lock on platforms supporting the capability to prevent display sleep during active cooking.
- **FR-004**: System MUST display a non-blocking informational notice when Screen Wake Lock is unsupported or denied by the device operating system.
- **FR-005**: System MUST provide a high-contrast display mode toggle supporting both an illuminated light theme and a pure dark theme (`#000000`).
- **FR-006**: System MUST present exactly one instruction step per view, rendered with prominent step numbering and scalable font sizing.
- **FR-007**: System MUST provide navigation controls (Next and Previous) with minimum interactive touch target dimensions of $64 \times 64$ pixels.
- **FR-008**: System MUST support keyboard navigation shortcuts (`Spacebar` / `Right Arrow` for Next; `Left Arrow` for Previous).
- **FR-009**: System MUST display the specific ingredients and measurements associated with the current step, or provide a single-tap collapsible drawer displaying the complete recipe ingredient checklist if step-level mappings are absent.
- **FR-010**: System MUST parse instruction text for standard Vietnamese duration patterns (`X phút`, `X giờ`, `X giây`, `X-Y phút`) and render corresponding quick-start timer buttons.
- **FR-011**: System MUST allow users to manually create, label, pause, resume, extend duration in-flight via quick-add chips (`+1 phút`, `+5 phút`), and delete custom countdown timers at any point during Cooking Mode.
- **FR-012**: System MUST support the simultaneous execution of multiple independent countdown timers without clock drift.
- **FR-013**: System MUST emit an audible, repeating alarm for a maximum duration of 60 seconds when any timer reaches zero, auto-silencing the audio after 60 seconds while maintaining a persistent pulsing red "Đã quá hạn X phút" (Overdue) visual badge and device vibration where supported.
- **FR-014**: System MUST allow silencing active alarms through a single tap anywhere on the screen or via the `Spacebar` key.
- **FR-015**: System MUST support an optional Push-to-Talk voice recognition mode capable of interpreting the core Vietnamese command set ("Tiếp theo", "Quay lại", "Đọc lại", "Hẹn giờ", "Tắt chuông"). For "Đọc lại", the system MUST speak the current step's full instruction text aloud using Web Speech Synthesis (`vi-VN`), gracefully falling back to earcon audio and visual highlighting if a Vietnamese voice package is unavailable.
- **FR-016**: System MUST ignore acoustic inputs with recognition confidence $< 70\%$ without advancing steps or triggering timer operations.
- **FR-017**: System MUST persist session state (`recipe_id`, `step_index`, `timers`, `timestamp`) independently for each recipe using discrete keys (`cooking_session_{recipe_id}`) in browser local storage, supporting multiple concurrent cooking sessions across separate tabs within a 4-hour validity window.
- **FR-018**: System MUST recalculate remaining countdown time for timers that continued running while the cooking view was closed or refreshed, marking timers that ended while offline as "Overdue".
- **FR-019**: System MUST prompt for user confirmation before clearing session state if active countdown timers are still running when the user attempts to exit or complete Cooking Mode, offering the option to maintain timers in a background widget or terminate all.

### Non-Functional Requirements

- **NFR-001 (Usability & Readability)**: Primary instruction text MUST maintain a minimum visual contrast ratio of $7:1$ (WCAG 2.1 AAA) in High-Contrast Mode and be readable from a distance of 1.5 meters on a standard 6-inch mobile viewport.
- **NFR-002 (Performance & Latency)**: Step-to-step visual transition latency MUST execute in under 100 milliseconds upon touch, keyboard, or voice trigger.
- **NFR-003 (Timer Precision)**: Active countdown timers MUST exhibit a cumulative drift of less than 1.0 second over any 60-minute interval when the browser tab is open.
- **NFR-004 (Offline Resilience)**: All step navigation, ingredient viewing, and timer countdown capabilities MUST operate 100% offline without active network connectivity once the recipe has loaded.
- **NFR-005 (Graceful Degradation)**: Platform-specific constraints (lack of vibration on iOS Safari, absence of Screen Wake Lock on legacy browsers, cloud voice recognition failure during network dropouts) MUST fail gracefully with visual alternatives and without crashing the application.
- **NFR-006 (Responsiveness)**: The Cooking Mode interface MUST adapt seamlessly across mobile screens ($\ge 360$px width), tablets, and desktop displays.

---

### Key Entities *(data involved)*

- **`CookingSession`**: Represents a user's active cooking instance for a specific recipe.
  - Attributes: `sessionId`, `recipeId`, `currentStepIndex`, `totalSteps`, `startedAt`, `lastActiveAt`, `isCompleted`.
  - Relationship: Belongs to a `Recipe`; contains zero or more `KitchenTimer` instances.
- **`InstructionStep`**: Represents an individual culinary step within the recipe.
  - Attributes: `stepNumber`, `instructionText`, `detectedDurationSeconds`, `ingredientsUsed` (list of ingredient names and quantities).
- **`KitchenTimer`**: Represents an active or completed countdown clock linked to a cooking session.
  - Attributes: `timerId`, `sessionId`, `stepNumber`, `label`, `totalDurationSeconds`, `remainingDurationSeconds`, `targetEndTimestamp`, `status` (`IDLE`, `RUNNING`, `PAUSED`, `EXPIRED`, `DISMISSED`).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001 (Touch Accessibility - Core MVP)**: Users can navigate all recipe steps using touch target regions that are at least 300% larger in surface area ($64 \times 64$px minimum) compared to standard web navigation buttons.
- **SC-002 (Display Wakefulness)**: On devices supporting screen wake management, the display maintains 100% continuous illumination throughout the cooking session without unintentional screen timeouts.
- **SC-003 (Voice Recognition Accuracy - Online Enhancement)**: When voice recognition is actively enabled in a standard kitchen acoustic environment (ambient noise $\le 60$ dB), the system achieves $\ge 85\%$ successful command execution on the 5 standard Vietnamese phrases without misnavigating on background chatter.
- **SC-004 (Timer Precision & Alert Timeliness)**: Countdown alarms trigger within 1.0 second of reaching `00:00:00` in active viewport conditions, and overdue timers report expired duration accurate to the nearest second upon session restoration.
- **SC-005 (Session Recovery Reliability)**: 100% of session progress (exact step index and active timer elapsed durations) is accurately restored upon page reload when reopened within the 4-hour expiration threshold.

---

## Assumptions & Dependencies

### Technical & Platform Assumptions
- **User Audio Gesture**: It is assumed that tapping "Bắt đầu nấu" satisfies the browser's user gesture requirement for audio playback, allowing timer alarms to play without further manual permission prompts.
- **Platform Vibration Support**: Device vibration (`navigator.vibrate`) is assumed to be an optional physical enhancement supported primarily on Android browsers; iOS Safari does not support web vibration, and the application must rely solely on audio and visual cues on Apple devices.
- **Online Voice & Speech Synthesis Dependency**: Vietnamese Speech-to-Text via the browser's speech recognition engine requires an active Internet connection. Furthermore, Vietnamese Text-to-Speech (TTS) for the "Đọc lại" command depends on OS/browser `vi-VN` speech synthesis availability. Both voice input and audio synthesis are treated strictly as optional progressive enhancements with non-blocking fallbacks when unavailable.

### Data Model Dependencies
- **Recipe Step Granularity**: It is assumed that recipes in WikiCook maintain a sequential list of steps (`InstructionStep`). If the existing data schema does not support per-step ingredient linkages (`recipe_step_ingredients`), the interface falls back to providing a full recipe ingredient drawer accessible via a single tap.
- **Storage Availability**: Browser LocalStorage is assumed to have at least 50 KB of available storage to preserve session state and active timer timestamps.
