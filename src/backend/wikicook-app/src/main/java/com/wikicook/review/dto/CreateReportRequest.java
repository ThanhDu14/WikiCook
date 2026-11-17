package com.wikicook.review.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
    @NotNull(message = "Vui lòng chọn lý do báo cáo")
    ReportReason reason,

    @Size(max = 1000, message = "Mô tả chi tiết tối đa 1000 ký tự")
    String details
) {}

