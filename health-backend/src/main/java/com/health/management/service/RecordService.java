package com.health.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.management.dto.HealthDtos.DailySummaryResponse;
import com.health.management.dto.RecordDtos.DietRecordRequest;
import com.health.management.dto.RecordDtos.ExerciseRecordRequest;
import com.health.management.entity.DietRecord;
import com.health.management.entity.ExerciseRecord;
import com.health.management.entity.HealthProfile;
import com.health.management.mapper.DietRecordMapper;
import com.health.management.mapper.ExerciseRecordMapper;
import com.health.management.mapper.HealthProfileMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecordService {
    private final DietRecordMapper dietRecordMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final HealthProfileMapper healthProfileMapper;
    private final HealthCalculationService calculationService;

    public RecordService(DietRecordMapper dietRecordMapper,
                         ExerciseRecordMapper exerciseRecordMapper,
                         HealthProfileMapper healthProfileMapper,
                         HealthCalculationService calculationService) {
        this.dietRecordMapper = dietRecordMapper;
        this.exerciseRecordMapper = exerciseRecordMapper;
        this.healthProfileMapper = healthProfileMapper;
        this.calculationService = calculationService;
    }

    // ---- Diet Records ----

    public List<DietRecord> listDietRecords(Long userId, LocalDate date) {
        LambdaQueryWrapper<DietRecord> query = new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getUserId, userId)
                .orderByDesc(DietRecord::getRecordDate);
        if (date != null) {
            query.eq(DietRecord::getRecordDate, date);
        }
        return dietRecordMapper.selectList(query);
    }

    public DietRecord createDietRecord(Long userId, DietRecordRequest request) {
        DietRecord record = new DietRecord();
        record.setUserId(userId);
        applyDiet(record, request);
        dietRecordMapper.insert(record);
        return record;
    }

    public DietRecord updateDietRecord(Long userId, Long id, DietRecordRequest request) {
        DietRecord record = dietRecordMapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("记录不存在");
        }
        applyDiet(record, request);
        dietRecordMapper.updateById(record);
        return record;
    }

    public void deleteDietRecord(Long userId, Long id) {
        DietRecord record = dietRecordMapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("记录不存在");
        }
        dietRecordMapper.deleteById(id);
    }

    // ---- Exercise Records ----

    public List<ExerciseRecord> listExerciseRecords(Long userId, LocalDate date) {
        LambdaQueryWrapper<ExerciseRecord> query = new LambdaQueryWrapper<ExerciseRecord>()
                .eq(ExerciseRecord::getUserId, userId)
                .orderByDesc(ExerciseRecord::getRecordDate);
        if (date != null) {
            query.eq(ExerciseRecord::getRecordDate, date);
        }
        return exerciseRecordMapper.selectList(query);
    }

    public ExerciseRecord createExerciseRecord(Long userId, ExerciseRecordRequest request) {
        ExerciseRecord record = new ExerciseRecord();
        record.setUserId(userId);
        applyExercise(record, request);
        exerciseRecordMapper.insert(record);
        return record;
    }

    public ExerciseRecord updateExerciseRecord(Long userId, Long id, ExerciseRecordRequest request) {
        ExerciseRecord record = exerciseRecordMapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("记录不存在");
        }
        applyExercise(record, request);
        exerciseRecordMapper.updateById(record);
        return record;
    }

    public void deleteExerciseRecord(Long userId, Long id) {
        ExerciseRecord record = exerciseRecordMapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("记录不存在");
        }
        exerciseRecordMapper.deleteById(id);
    }

    // ---- Summary ----

    public DailySummaryResponse getDailySummary(Long userId, LocalDate date) {
        HealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<HealthProfile>().eq(HealthProfile::getUserId, userId));
        if (profile == null) {
            throw new IllegalArgumentException("请先创建健康档案");
        }
        return computeSummary(userId, date, profile);
    }

    public List<DailySummaryResponse> getWeeklyTrend(Long userId) {
        HealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<HealthProfile>().eq(HealthProfile::getUserId, userId));
        if (profile == null) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        List<DailySummaryResponse> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            trend.add(computeSummary(userId, today.minusDays(i), profile));
        }
        return trend;
    }

    // ---- Private helpers ----

    private DailySummaryResponse computeSummary(Long userId, LocalDate date, HealthProfile profile) {
        BigDecimal intake = dietRecordMapper
                .selectList(new LambdaQueryWrapper<DietRecord>()
                        .eq(DietRecord::getUserId, userId)
                        .eq(DietRecord::getRecordDate, date))
                .stream().map(DietRecord::getCalories).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal exercise = exerciseRecordMapper
                .selectList(new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, userId)
                        .eq(ExerciseRecord::getRecordDate, date))
                .stream().map(ExerciseRecord::getCaloriesBurned).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal activity = calculationService.calculateTdee(profile);
        BigDecimal totalBurned = activity.add(exercise);
        BigDecimal balance = totalBurned.subtract(intake);
        String status = balance.signum() > 0 ? "消耗大于摄入" : balance.signum() == 0 ? "摄入与消耗平衡" : "摄入大于消耗";

        return new DailySummaryResponse(date, intake, profile.getBmr(), activity, exercise, totalBurned, balance, status);
    }

    private void applyDiet(DietRecord record, DietRecordRequest request) {
        record.setRecordDate(request.getRecordDate());
        record.setMealType(request.getMealType());
        record.setFoodName(request.getFoodName());
        record.setAmount(request.getAmount());
        record.setCalories(request.getCalories());
        record.setImageUrl(request.getImageUrl());
        record.setRemark(request.getRemark());
    }

    private void applyExercise(ExerciseRecord record, ExerciseRecordRequest request) {
        record.setExerciseType(request.getExerciseType());
        record.setRecordDate(request.getRecordDate());
        record.setDurationMinutes(request.getDurationMinutes());
        record.setIntensity(request.getIntensity());
        record.setCaloriesBurned(request.getCaloriesBurned());
        record.setImageUrl(request.getImageUrl());
        record.setRemark(request.getRemark());
    }
}
