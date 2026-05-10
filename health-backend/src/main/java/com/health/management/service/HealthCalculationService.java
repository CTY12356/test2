package com.health.management.service;

import com.health.management.entity.HealthProfile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class HealthCalculationService {
    private static final Map<String, BigDecimal> ACTIVITY_FACTORS = Map.of(
            "SEDENTARY", new BigDecimal("1.2"),
            "LIGHT", new BigDecimal("1.375"),
            "MODERATE", new BigDecimal("1.55"),
            "HIGH", new BigDecimal("1.725"),
            "EXTREME", new BigDecimal("1.9")
    );

    public void fillCalculatedFields(HealthProfile profile) {
        profile.setBmi(calculateBmi(profile.getWeightKg(), profile.getHeightCm()));
        profile.setBmr(calculateBmr(profile));
        profile.setRecommendedCalories(calculateTdee(profile));
    }

    public BigDecimal calculateTdee(HealthProfile profile) {
        BigDecimal factor = ACTIVITY_FACTORS.getOrDefault(profile.getActivityLevel(), ACTIVITY_FACTORS.get("SEDENTARY"));
        return profile.getBmr().multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateBmi(BigDecimal weightKg, BigDecimal heightCm) {
        BigDecimal heightM = heightCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return weightKg.divide(heightM.multiply(heightM), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateBmr(HealthProfile profile) {
        BigDecimal base = profile.getWeightKg().multiply(new BigDecimal("10"))
                .add(profile.getHeightCm().multiply(new BigDecimal("6.25")))
                .subtract(new BigDecimal(profile.getAge()).multiply(new BigDecimal("5")));
        if ("MALE".equalsIgnoreCase(profile.getGender())) {
            return base.add(new BigDecimal("5")).setScale(2, RoundingMode.HALF_UP);
        }
        return base.subtract(new BigDecimal("161")).setScale(2, RoundingMode.HALF_UP);
    }
}
