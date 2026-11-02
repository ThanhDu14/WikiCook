package com.wikicook.shoppinglist.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class UnitConversionServiceTest {

    private UnitConversionService unitConversionService;

    @BeforeEach
    void setUp() {
        unitConversionService = new UnitConversionService();
    }

    @Test
    @DisplayName("US2 FR-007: converts grams >= 1000 to kilograms with up to 2 decimal places")
    void convertMetric_gramsToKilograms() {
        var res1 = unitConversionService.normalizeQuantityAndUnit(new BigDecimal("1000"), "g");
        assertThat(res1.quantity()).isEqualByComparingTo(new BigDecimal("1"));
        assertThat(res1.unitName()).isEqualTo("kg");

        var res2 = unitConversionService.normalizeQuantityAndUnit(new BigDecimal("1250"), "g");
        assertThat(res2.quantity()).isEqualByComparingTo(new BigDecimal("1.25"));
        assertThat(res2.unitName()).isEqualTo("kg");

        var res3 = unitConversionService.normalizeQuantityAndUnit(new BigDecimal("1333.333"), "g");
        assertThat(res3.quantity()).isEqualByComparingTo(new BigDecimal("1.33"));
        assertThat(res3.unitName()).isEqualTo("kg");
    }

    @Test
    @DisplayName("US2 FR-007: converts kilograms < 1 to grams")
    void convertMetric_kilogramsToGrams() {
        var res1 = unitConversionService.normalizeQuantityAndUnit(new BigDecimal("0.5"), "kg");
        assertThat(res1.quantity()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(res1.unitName()).isEqualTo("g");

        var res2 = unitConversionService.normalizeQuantityAndUnit(new BigDecimal("0.05"), "kg");
        assertThat(res2.quantity()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(res2.unitName()).isEqualTo("g");
    }

    @Test
    @DisplayName("US2 FR-007: converts ml >= 1000 to liters")
    void convertMetric_mlToLiters() {
        var res1 = unitConversionService.normalizeQuantityAndUnit(new BigDecimal("1000"), "ml");
        assertThat(res1.quantity()).isEqualByComparingTo(new BigDecimal("1"));
        assertThat(res1.unitName()).isEqualTo("l");

        var res2 = unitConversionService.normalizeQuantityAndUnit(new BigDecimal("1750"), "ml");
        assertThat(res2.quantity()).isEqualByComparingTo(new BigDecimal("1.75"));
        assertThat(res2.unitName()).isEqualTo("l");
    }

    @Test
    @DisplayName("US2 FR-007: converts liters < 1 to ml")
    void convertMetric_litersToMl() {
        var res1 = unitConversionService.normalizeQuantityAndUnit(new BigDecimal("0.25"), "l");
        assertThat(res1.quantity()).isEqualByComparingTo(new BigDecimal("250"));
        assertThat(res1.unitName()).isEqualTo("ml");
    }

    @Test
    @DisplayName("US2 Scenario 7: rounds up discrete countable units to whole integer using ceiling rule")
    void discreteUnits_roundsUpToCeiling() {
        var res1 = unitConversionService.normalizeQuantityAndUnit(new BigDecimal("0.5"), "quả");
        assertThat(res1.quantity()).isEqualByComparingTo(new BigDecimal("1"));
        assertThat(res1.unitName()).isEqualTo("quả");

        var res2 = unitConversionService.normalizeQuantityAndUnit(new BigDecimal("1.25"), "bắp");
        assertThat(res2.quantity()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(res2.unitName()).isEqualTo("bắp");

        var res3 = unitConversionService.normalizeQuantityAndUnit(new BigDecimal("2.1"), "củ");
        assertThat(res3.quantity()).isEqualByComparingTo(new BigDecimal("3"));

        var res4 = unitConversionService.normalizeQuantityAndUnit(new BigDecimal("0.2"), "trứng");
        assertThat(res4.quantity()).isEqualByComparingTo(new BigDecimal("1"));
    }

    @Test
    @DisplayName("US2 FR-008: detects unit compatibility for mass, volume, and discrete units")
    void areUnitsCompatible() {
        assertThat(unitConversionService.areUnitsCompatible("g", "kg")).isTrue();
        assertThat(unitConversionService.areUnitsCompatible("kg", "g")).isTrue();
        assertThat(unitConversionService.areUnitsCompatible("ml", "l")).isTrue();
        assertThat(unitConversionService.areUnitsCompatible("quả", "quả")).isTrue();

        assertThat(unitConversionService.areUnitsCompatible("g", "ml")).isFalse();
        assertThat(unitConversionService.areUnitsCompatible("quả", "ml")).isFalse();
        assertThat(unitConversionService.areUnitsCompatible("thìa canh", "g")).isFalse();
    }

    @Test
    @DisplayName("US2: sumCompatibleQuantities sums and normalizes compatible quantities")
    void sumCompatibleQuantities() {
        var res = unitConversionService.sumAndNormalize(
                new BigDecimal("600"), "g",
                new BigDecimal("400"), "g"
        );
        assertThat(res.quantity()).isEqualByComparingTo(new BigDecimal("1"));
        assertThat(res.unitName()).isEqualTo("kg");
    }
}
