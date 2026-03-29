package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.auth.ChangePasswordRequest;
import com.travel.dto.auth.UpdateUserInfoRequest;
import com.travel.dto.auth.UserInfoResponse;
import com.travel.entity.SpotCategory;
import com.travel.entity.User;
import com.travel.entity.UserPreference;
import com.travel.mapper.SpotCategoryMapper;
import com.travel.mapper.UserMapper;
import com.travel.mapper.UserPreferenceMapper;
import com.travel.service.RecommendationService;
import com.travel.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户账户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private final UserMapper userMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final SpotCategoryMapper spotCategoryMapper;
    private final RecommendationService recommendationService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        List<UserPreference> preferences = userPreferenceMapper.selectList(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getUserId, userId)
                        .eq(UserPreference::getIsDeleted, 0)
        );
        Map<Long, String> categoryMap = getCategoryMap();
        List<Long> categoryIds = resolvePreferenceCategoryIds(preferences);
        List<String> categoryNames = categoryIds.stream()
                .map(categoryMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return UserInfoResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatarUrl())
                .phone(user.getPhone())
                .hasPassword(StringUtils.hasText(user.getPassword()))
                .preferences(categoryNames)
                .preferenceCategoryIds(categoryIds)
                .preferenceCategoryNames(categoryNames)
                .build();
    }

    @Override
    public void updateUserInfo(Long userId, UpdateUserInfoRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        if (StringUtils.hasText(request.getAvatar())) {
            user.setAvatarUrl(request.getAvatar());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        userMapper.updateById(user);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        if (StringUtils.hasText(user.getPassword())) {
            if (!StringUtils.hasText(request.getOldPassword())) {
                throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
            }
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
            }
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public void deactivateAccount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        user.setIsDeleted(1);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void setPreferences(Long userId, List<Long> categoryIds) {
        validateCategoryIds(categoryIds);
        Set<Long> distinctCategoryIds = categoryIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(categoryIds);

        UserPreference deletedPreference = new UserPreference();
        deletedPreference.setIsDeleted(1);
        userPreferenceMapper.update(
                deletedPreference,
                new LambdaUpdateWrapper<UserPreference>().eq(UserPreference::getUserId, userId)
        );

        if (distinctCategoryIds.isEmpty()) {
            recommendationService.invalidateUserRecommendationCache(userId);
            return;
        }

        Map<String, UserPreference> existingPreferenceMap = userPreferenceMapper.selectList(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getUserId, userId)
                        .in(UserPreference::getTag, distinctCategoryIds.stream().map(String::valueOf).collect(Collectors.toSet()))
        ).stream().collect(Collectors.toMap(UserPreference::getTag, preference -> preference, (left, right) -> left));

        for (Long categoryId : distinctCategoryIds) {
            String tag = String.valueOf(categoryId);
            UserPreference existingPreference = existingPreferenceMap.get(tag);
            if (existingPreference != null) {
                UserPreference restoredPreference = new UserPreference();
                restoredPreference.setId(existingPreference.getId());
                restoredPreference.setIsDeleted(0);
                userPreferenceMapper.updateById(restoredPreference);
                continue;
            }

            UserPreference preference = new UserPreference();
            preference.setUserId(userId);
            preference.setTag(tag);
            preference.setIsDeleted(0);
            userPreferenceMapper.insert(preference);
        }

        recommendationService.invalidateUserRecommendationCache(userId);
    }

    private void validateCategoryIds(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }

        Set<Long> distinctIds = new LinkedHashSet<>(categoryIds);
        List<SpotCategory> categories = spotCategoryMapper.selectBatchIds(distinctIds);
        Set<Long> validIds = categories.stream().map(SpotCategory::getId).collect(Collectors.toSet());

        if (validIds.size() != distinctIds.size()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "存在无效的分类ID");
        }
    }

    private Map<Long, String> getCategoryMap() {
        return spotCategoryMapper.selectList(
                new LambdaQueryWrapper<SpotCategory>().eq(SpotCategory::getIsDeleted, 0)
        ).stream().collect(Collectors.toMap(SpotCategory::getId, SpotCategory::getName));
    }

    private List<Long> resolvePreferenceCategoryIds(List<UserPreference> preferences) {
        return preferences.stream()
                .map(UserPreference::getTag)
                .map(this::parsePreferenceCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Long parsePreferenceCategoryId(String tag) {
        if (!StringUtils.hasText(tag)) {
            return null;
        }

        try {
            return Long.parseLong(tag.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
