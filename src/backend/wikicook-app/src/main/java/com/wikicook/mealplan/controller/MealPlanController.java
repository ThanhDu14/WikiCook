package com.wikicook.mealplan.controller;

import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.dto.ExpectedSlotState;
import com.wikicook.mealplan.dto.MoveRequest;
import com.wikicook.mealplan.dto.MoveResult;
import com.wikicook.mealplan.dto.PatchServingsRequest;
import com.wikicook.mealplan.dto.PlanItemDto;
import com.wikicook.mealplan.dto.PutSlotRequest;
import com.wikicook.mealplan.dto.WeekPlanResponse;
import com.wikicook.mealplan.service.MealPlanService;
import com.wikicook.mealplan.service.SlotMoveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Week endpoints of contracts/openapi.yaml; every call works on the caller's own plan (FR-006, NFR-004). */
@RestController
@RequestMapping("/api/v1/meal-plans/{weekStart}")
@Tag(name = "meal-plans")
public class MealPlanController {

    private final CurrentUserId currentUser;
    private final MealPlanService service;
    private final SlotMoveService moves;

    public MealPlanController(CurrentUserId currentUser, MealPlanService service, SlotMoveService moves) {
        this.currentUser = currentUser;
        this.service = service;
        this.moves = moves;
    }

    @Operation(operationId = "getWeekPlan", summary = "Lấy kế hoạch của một tuần (tuần chưa có kế hoạch trả về 7 ngày rỗng)")
    @GetMapping
    public WeekPlanResponse getWeek(
            @PathVariable @MondayDate @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return service.getWeek(currentUser.get(), weekStart);
    }

    @Operation(operationId = "putSlot", summary = "Thêm món vào ô trống hoặc thay món đang có (FR-007, FR-008, FR-016)")
    @PutMapping("/slots/{day}/{meal}")
    public PlanItemDto putSlot(
            @PathVariable @MondayDate @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @PathVariable DayOfWeek day, @PathVariable Meal meal, @Valid @RequestBody PutSlotRequest request) {
        return service.putSlot(currentUser.get(), weekStart, day, meal, request);
    }

    @Operation(operationId = "patchServings", summary = "Đổi khẩu phần của món trong ô (FR-011)")
    @PatchMapping("/slots/{day}/{meal}/servings")
    public PlanItemDto patchServings(
            @PathVariable @MondayDate @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @PathVariable DayOfWeek day, @PathVariable Meal meal, @Valid @RequestBody PatchServingsRequest request) {
        return service.patchServings(currentUser.get(), weekStart, day, meal, request);
    }

    @Operation(operationId = "deleteSlot", summary = "Xóa món khỏi ô (FR-016)")
    @DeleteMapping("/slots/{day}/{meal}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSlot(
            @PathVariable @MondayDate @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @PathVariable DayOfWeek day, @PathVariable Meal meal, @RequestParam long itemId,
            @RequestParam int version) {
        service.removeSlot(currentUser.get(), weekStart, day, meal, new ExpectedSlotState(itemId, version));
    }

    @Operation(operationId = "moveItem", summary = "Chuyển món sang ô khác cùng tuần; ô đích có món thì đổi chỗ (FR-014, FR-015)")
    @PostMapping("/moves")
    public MoveResult move(
            @PathVariable @MondayDate @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @Valid @RequestBody MoveRequest request) {
        return moves.move(currentUser.get(), weekStart, request);
    }

    @Operation(operationId = "clearDay", summary = "Xóa cả ngày (FR-017) – client đã hỏi xác nhận")
    @DeleteMapping("/days/{day}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearDay(
            @PathVariable @MondayDate @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @PathVariable DayOfWeek day) {
        service.clearDay(currentUser.get(), weekStart, day);
    }
}
