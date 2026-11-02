package com.wikicook.mealplan.dto;

import java.util.List;

/** openapi {@code PickerPage}. */
public record PickerPageDto(List<PickerResultDto> content, int page, int size, boolean hasNext) {
}
