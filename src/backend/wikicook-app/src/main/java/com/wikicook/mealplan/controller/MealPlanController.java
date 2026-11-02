package com.wikicook.mealplan.controller;

import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.dto.AddItemRequest;
import com.wikicook.mealplan.dto.MoveItemRequest;
import com.wikicook.mealplan.dto.MoveResult;
import com.wikicook.mealplan.dto.PatchServingsRequest;
import com.wikicook.mealplan.dto.PlacedItem;
import com.wikicook.mealplan.dto.ReplaceItemRequest;
import com.wikicook.mealplan.dto.WeekPlanResponse;
import com.wikicook.mealplan.service.ItemMoveService;
import com.wikicook.mealplan.service.MealPlanService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Week endpoints of contracts/openapi.yaml 0.2.0; every call works on the caller's own plan (FR-006, NFR-004). */
@RestController
@RequestMapping("/api/v1/meal-plans/{weekStart}")
@Tag(name = "meal-plans")
public class MealPlanController {

    private final CurrentUserId currentUser;
    private final MealPlanService service;
    private final ItemMoveService moves;

    public MealPlanController(CurrentUserId currentUser, MealPlanService service, ItemMoveService moves) {
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

    @Operation(operationId = "addItem", summary = "Thêm món vào cuối một bữa (FR-004, FR-007, FR-008)")
    @PostMapping("/meals/{day}/{meal}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public PlacedItem addItem(
            @PathVariable @MondayDate @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @PathVariable DayOfWeek day, @PathVariable Meal meal, @Valid @RequestBody AddItemRequest request) {
        return service.addItem(currentUser.get(), weekStart, day, meal, request);
    }

    @Operation(operationId = "replaceItem", summary = "Thay món bằng công thức khác, giữ servings và vị trí (FR-016)")
    @PutMapping("/items/{itemId}")
    public PlacedItem replaceItem(
            @PathVariable @MondayDate @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @PathVariable long itemId, @Valid @RequestBody ReplaceItemRequest request) {
        return service.replaceItem(currentUser.get(), weekStart, itemId, request);
    }

    @Operation(operationId = "deleteItem", summary = "Xóa một món; các món sau nó trong bữa dồn lên (FR-016)")
    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
            @PathVariable @MondayDate @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @PathVariable long itemId, @RequestParam int version) {
        service.removeItem(currentUser.get(), weekStart, itemId, version);
    }

    @Operation(operationId = "patchServings", summary = "Đổi khẩu phần của một món (FR-011)")
    @PatchMapping("/items/{itemId}/servings")
    public PlacedItem patchServings(
            @PathVariable @MondayDate @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @PathVariable long itemId, @Valid @RequestBody PatchServingsRequest request) {
        return service.patchServings(currentUser.get(), weekStart, itemId, request);
    }

    @Operation(operationId = "moveItem", summary = "Chuyển món sang bữa khác cùng tuần hoặc đổi thứ tự trong bữa (FR-014, FR-015)")
    @PostMapping("/items/{itemId}/move")
    public MoveResult moveItem(
            @PathVariable @MondayDate @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @PathVariable long itemId, @Valid @RequestBody MoveItemRequest request) {
        return moves.move(currentUser.get(), weekStart, itemId, request);
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
