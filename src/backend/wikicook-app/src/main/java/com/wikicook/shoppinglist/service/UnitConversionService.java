package com.wikicook.shoppinglist.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;

/**
 * Service handling metric conversions and discrete unit ceiling rounding (T042, US2 FR-006, FR-007).
 */
@Service
public class UnitConversionService {

    public record ConvertedQuantity(BigDecimal quantity, String unitName) {}

    private static final Set<String> MASS_UNITS_G = Set.of("g", "gram", "gr");
    private static final Set<String> MASS_UNITS_KG = Set.of("kg", "kilogram");
    private static final Set<String> VOLUME_UNITS_ML = Set.of("ml", "mililit", "milliliter");
    private static final Set<String> VOLUME_UNITS_L = Set.of("l", "lit", "lít", "liter");

    private static final Set<String> DISCRETE_UNITS = Set.of(
            "quả", "trứng", "củ", "lon", "gói", "bó", "hộp", "miếng",
            "trái", "cây", "lát", "bắp", "tép", "nhánh"
    );

    public ConvertedQuantity normalizeQuantityAndUnit(BigDecimal quantity, String rawUnit) {
        if (quantity == null) {
            return new ConvertedQuantity(null, rawUnit);
        }
        if (rawUnit == null || rawUnit.isBlank()) {
            return new ConvertedQuantity(formatDecimal(quantity), rawUnit);
        }

        String unitLower = rawUnit.trim().toLowerCase(Locale.ROOT);

        if (DISCRETE_UNITS.contains(unitLower)) {
            BigDecimal rounded = quantity.setScale(0, RoundingMode.CEILING);
            if (rounded.compareTo(BigDecimal.ONE) < 0 && quantity.compareTo(BigDecimal.ZERO) > 0) {
                rounded = BigDecimal.ONE;
            }
            return new ConvertedQuantity(rounded, rawUnit);
        }

        if (MASS_UNITS_G.contains(unitLower)) {
            if (quantity.compareTo(new BigDecimal("1000")) >= 0) {
                BigDecimal kg = quantity.divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
                return new ConvertedQuantity(formatDecimal(kg), "kg");
            }
            return new ConvertedQuantity(formatDecimal(quantity), "g");
        }

        if (MASS_UNITS_KG.contains(unitLower)) {
            if (quantity.compareTo(BigDecimal.ONE) < 0 && quantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal g = quantity.multiply(new BigDecimal("1000"));
                return new ConvertedQuantity(formatDecimal(g), "g");
            }
            return new ConvertedQuantity(formatDecimal(quantity), "kg");
        }

        if (VOLUME_UNITS_ML.contains(unitLower)) {
            if (quantity.compareTo(new BigDecimal("1000")) >= 0) {
                BigDecimal l = quantity.divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
                return new ConvertedQuantity(formatDecimal(l), "l");
            }
            return new ConvertedQuantity(formatDecimal(quantity), "ml");
        }

        if (VOLUME_UNITS_L.contains(unitLower)) {
            if (quantity.compareTo(BigDecimal.ONE) < 0 && quantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ml = quantity.multiply(new BigDecimal("1000"));
                return new ConvertedQuantity(formatDecimal(ml), "ml");
            }
            return new ConvertedQuantity(formatDecimal(quantity), "l");
        }

        return new ConvertedQuantity(formatDecimal(quantity), rawUnit);
    }

    public boolean areUnitsCompatible(String unitA, String unitB) {
        if (unitA == null || unitB == null) {
            return false;
        }
        String a = unitA.trim().toLowerCase(Locale.ROOT);
        String b = unitB.trim().toLowerCase(Locale.ROOT);

        if (a.equals(b)) {
            return true;
        }

        boolean aIsMass = isMassUnit(a);
        boolean bIsMass = isMassUnit(b);
        if (aIsMass && bIsMass) {
            return true;
        }

        boolean aIsVol = isVolumeUnit(a);
        boolean bIsVol = isVolumeUnit(b);
        return aIsVol && bIsVol;
    }

    public ConvertedQuantity sumAndNormalize(BigDecimal q1, String u1, BigDecimal q2, String u2) {
        if (!areUnitsCompatible(u1, u2)) {
            throw new IllegalArgumentException("Cannot sum incompatible units: " + u1 + " and " + u2);
        }

        BigDecimal qty1 = q1 != null ? q1 : BigDecimal.ZERO;
        BigDecimal qty2 = q2 != null ? q2 : BigDecimal.ZERO;

        String a = u1.trim().toLowerCase(Locale.ROOT);
        String b = u2.trim().toLowerCase(Locale.ROOT);

        if (isMassUnit(a)) {
            BigDecimal inGrams1 = toGrams(qty1, a);
            BigDecimal inGrams2 = toGrams(qty2, b);
            return normalizeQuantityAndUnit(inGrams1.add(inGrams2), "g");
        }

        if (isVolumeUnit(a)) {
            BigDecimal inMl1 = toMl(qty1, a);
            BigDecimal inMl2 = toMl(qty2, b);
            return normalizeQuantityAndUnit(inMl1.add(inMl2), "ml");
        }

        return normalizeQuantityAndUnit(qty1.add(qty2), u1);
    }

    private boolean isMassUnit(String u) {
        return MASS_UNITS_G.contains(u) || MASS_UNITS_KG.contains(u);
    }

    private boolean isVolumeUnit(String u) {
        return VOLUME_UNITS_ML.contains(u) || VOLUME_UNITS_L.contains(u);
    }

    private BigDecimal toGrams(BigDecimal q, String unit) {
        if (MASS_UNITS_KG.contains(unit)) {
            return q.multiply(new BigDecimal("1000"));
        }
        return q;
    }

    private BigDecimal toMl(BigDecimal q, String unit) {
        if (VOLUME_UNITS_L.contains(unit)) {
            return q.multiply(new BigDecimal("1000"));
        }
        return q;
    }

    private BigDecimal formatDecimal(BigDecimal val) {
        return val.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
    }
}
