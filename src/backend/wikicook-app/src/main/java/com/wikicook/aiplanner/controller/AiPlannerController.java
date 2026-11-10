package com.wikicook.aiplanner.controller;

import com.wikicook.aiplanner.dto.ConfirmRequest;
import com.wikicook.aiplanner.dto.ConfirmResult;
import com.wikicook.aiplanner.dto.DraftResponse;
import com.wikicook.aiplanner.dto.GenerateRequest;
import com.wikicook.aiplanner.dto.GenerationOptionsResponse;
import com.wikicook.aiplanner.service.ConfirmService;
import com.wikicook.aiplanner.service.GenerationOptionsService;
import com.wikicook.aiplanner.service.GenerationService;
import com.wikicook.common.security.CurrentUser;
import com.wikicook.common.security.UnauthenticatedException;
import com.wikicook.mealplan.controller.MondayDate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** US1/US2 endpoints of contracts/openapi.yaml; every call works on the caller's own week. */
@RestController
@RequestMapping("/api/v1/ai-planner")
@Tag(name = "ai-planner")
public class AiPlannerController {

    private final ObjectProvider<CurrentUser> currentUser;
    private final GenerationOptionsService options;
    private final GenerationService generation;
    private final ConfirmService confirmation;

    /** {@link CurrentUser} comes from spec 010 (or the dev stubs); while neither exists every call is a 401. */
    public AiPlannerController(ObjectProvider<CurrentUser> currentUser, GenerationOptionsService options,
                               GenerationService generation, ConfirmService confirmation) {
        this.currentUser = currentUser;
        this.options = options;
        this.generation = generation;
        this.confirmation = confirmation;
    }

    @Operation(operationId = "getGenerationOptions",
            summary = "Dữ liệu cho dialog ràng buộc (giá trị mặc định từ hồ sơ, ngày chọn được, lượt còn lại)")
    @GetMapping("/weeks/{weekStart}/options")
    public GenerationOptionsResponse options(
            @PathVariable @MondayDate @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return options.options(userId(), weekStart);
    }

    @Operation(operationId = "generateDraft",
            summary = "Tạo bản nháp cho các ô trống được chọn (đồng bộ, tối đa 60 s)")
    @PostMapping("/weeks/{weekStart}/generations")
    public DraftResponse generate(
            @PathVariable @MondayDate @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @Valid @RequestBody GenerateRequest request) {
        return generation.generate(userId(), weekStart, request);
    }

    @Operation(operationId = "confirmDraft", summary = "Lưu bản nháp vào kế hoạch tuần (FR-018, FR-019)")
    @PostMapping("/weeks/{weekStart}/confirm")
    public ConfirmResult confirm(
            @PathVariable @MondayDate @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @Valid @RequestBody ConfirmRequest request) {
        return confirmation.confirm(userId(), weekStart, request);
    }

    private long userId() {
        CurrentUser user = currentUser.getIfAvailable();
        if (user == null) {
            throw new UnauthenticatedException();
        }
        return user.id();
    }
}
