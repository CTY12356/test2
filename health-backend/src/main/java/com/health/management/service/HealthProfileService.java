package com.health.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.management.dto.HealthDtos.HealthProfileRequest;
import com.health.management.dto.HealthDtos.WeightRecordRequest;
import com.health.management.entity.HealthProfile;
import com.health.management.entity.WeightRecord;
import com.health.management.mapper.HealthProfileMapper;
import com.health.management.mapper.WeightRecordMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HealthProfileService {
    private final HealthProfileMapper healthProfileMapper;
    private final WeightRecordMapper weightRecordMapper;
    private final HealthCalculationService calculationService;

    public HealthProfileService(HealthProfileMapper healthProfileMapper,
                                WeightRecordMapper weightRecordMapper,
                                HealthCalculationService calculationService) {
        this.healthProfileMapper = healthProfileMapper;
        this.weightRecordMapper = weightRecordMapper;
        this.calculationService = calculationService;
    }

    public HealthProfile getProfile(Long userId) {
        return healthProfileMapper.selectOne(
                new LambdaQueryWrapper<HealthProfile>().eq(HealthProfile::getUserId, userId));
    }

    public HealthProfile saveProfile(Long userId, HealthProfileRequest request) {
        HealthProfile profile = getProfile(userId);
        if (profile == null) {
            profile = new HealthProfile();
            profile.setUserId(userId);
        }
        profile.setGender(request.getGender());
        profile.setAge(request.getAge());
        profile.setHeightCm(request.getHeightCm());
        profile.setWeightKg(request.getWeightKg());
        profile.setTargetWeightKg(request.getTargetWeightKg());
        profile.setActivityLevel(request.getActivityLevel());
        profile.setGoal(request.getGoal());
        calculationService.fillCalculatedFields(profile);
        if (profile.getId() == null) {
            healthProfileMapper.insert(profile);
        } else {
            healthProfileMapper.updateById(profile);
        }
        return profile;
    }

    public WeightRecord addWeight(Long userId, WeightRecordRequest request) {
        // 不用 selectOne：同日若仍有历史重复行会抛 TooManyResultsException，导致前端「记录失败」
        List<WeightRecord> sameDay = weightRecordMapper.selectList(
                new LambdaQueryWrapper<WeightRecord>()
                        .eq(WeightRecord::getUserId, userId)
                        .eq(WeightRecord::getRecordDate, request.getRecordDate())
                        .orderByDesc(WeightRecord::getId));
        if (!sameDay.isEmpty()) {
            WeightRecord keep = sameDay.get(0);
            keep.setWeightKg(request.getWeightKg());
            keep.setRemark(request.getRemark());
            weightRecordMapper.updateById(keep);
            if (sameDay.size() > 1) {
                List<Long> redundantIds = sameDay.stream().skip(1).map(WeightRecord::getId).toList();
                weightRecordMapper.delete(
                        new LambdaQueryWrapper<WeightRecord>().in(WeightRecord::getId, redundantIds));
            }
            return keep;
        }
        WeightRecord record = new WeightRecord();
        record.setUserId(userId);
        record.setWeightKg(request.getWeightKg());
        record.setRecordDate(request.getRecordDate());
        record.setRemark(request.getRemark());
        weightRecordMapper.insert(record);
        return record;
    }

    public List<WeightRecord> listWeights(Long userId) {
        return weightRecordMapper.selectList(
                new LambdaQueryWrapper<WeightRecord>()
                        .eq(WeightRecord::getUserId, userId)
                        .orderByAsc(WeightRecord::getRecordDate));
    }
}
