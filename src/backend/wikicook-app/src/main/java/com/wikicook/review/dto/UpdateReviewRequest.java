package com.wikicook.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateReviewRequest(
    @NotNull(message = "Điểm đánh giá không được để trống")
    @Min(value = 1, message = "Điểm đánh giá tối thiểu là 1 sao")
    @Max(value = 5, message = "Điểm đánh giá tối đa là 5 sao")
    Short rating,

    @NotBlank(message = "Nội dung nhận xét không được để trống")
    @Size(min = 10, max = 2000, message = "Nhận xét phải có từ 10 đến 2000 ký tự")
    String comment,

    @Size(max = 500, message = "Mẹo thay thế nguyên liệu tối đa 500 ký tự")
    String substitutionTip,

    @Size(max = 3, message = "Tối đa 3 ảnh Cooksnap")
    List<Long> removeCooksnapIds
) {}

