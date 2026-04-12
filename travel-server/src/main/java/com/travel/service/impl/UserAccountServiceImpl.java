package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.auth.request.ChangePasswordRequest;
import com.travel.dto.auth.request.UpdateUserInfoRequest;
import com.travel.dto.user.response.UserInfoResponse;
import com.travel.entity.SpotCategory;
import com.travel.entity.User;
import com.travel.entity.UserPreference;
import com.travel.entity.UserSpotFavorite;
import com.travel.mapper.SpotCategoryMapper;
import com.travel.mapper.UserMapper;
import com.travel.mapper.UserPreferenceMapper;
import com.travel.mapper.UserSpotFavoriteMapper;
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
 * 用户账户服务实现，负责资料维护、密码修改、偏好设置与账户状态变更。
 */
@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    // 持久层与服务依赖
    private final UserMapper userMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final UserSpotFavoriteMapper userSpotFavoriteMapper;
    private final SpotCategoryMapper spotCategoryMapper;
    private final RecommendationService recommendationService;
    private final BCryptPasswordEncoder passwordEncoder;

    // 账户资料与安全设置

    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        User user = getActiveUser(userId);

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
        User user = getActiveUser(userId);

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
        User user = getActiveUser(userId);

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

    // 账户状态与偏好维护

    @Override
    @Transactional
    public void deactivateAccount(Long userId) {
        User user = getActiveUser(userId);
        deactivateUser(user);
    }

    @Override
    @Transactional
    public void deactivateAccountByAdmin(Long userId) {
        User user = getManagedUser(userId);
        if (user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户已被封禁");
        }
        deactivateUser(user);
    }

    @Override
    @Transactional
    public void reactivateAccountByAdmin(Long userId) {
        User user = getManagedUser(userId);
        if (user.getIsDeleted() == null || user.getIsDeleted() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户当前无需解封");
        }
        reactivateUser(user);
    }

    @Override
    @Transactional
    public void setPreferences(Long userId, List<Long> categoryIds) {
        getActiveUser(userId);
        validateCategoryIds(categoryIds);
        Set<Long> distinctCategoryIds = categoryIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(categoryIds);

        // 先整体标记为删除，再按最新选择恢复或补建，便于处理取消偏好的情况。
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

    // 偏好校验与转换方法

    /**
     * 当前登录用户相关操作都要求账号仍然有效，统一收口 token 对应用户校验。
     */
    private User getActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        return user;
    }

    /**
     * 管理员封禁和用户主动注销都属于软删收口，统一复用同一套清理逻辑。
     */
    private User getManagedUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户不存在");
        }
        return user;
    }

    /**
     * 账号软删时统一清理当前状态型数据，避免不同入口出现不一致的封禁后效果。
     */
    private void deactivateUser(User user) {
        Long userId = user.getId();
        // 注销后收起当前偏好和收藏状态，只保留订单、评价、浏览等强历史数据。
        softDeleteUserPreferences(userId);
        softDeleteUserFavorites(userId);
        user.setIsDeleted(1);
        userMapper.updateById(user);
        recommendationService.invalidateUserRecommendationCache(userId);
    }

    /**
     * 当前封禁复用软删语义，因此解封时也要同步恢复偏好和收藏状态，避免账号恢复后数据残缺。
     */
    private void reactivateUser(User user) {
        Long userId = user.getId();
        restoreUserPreferences(userId);
        restoreUserFavorites(userId);
        user.setIsDeleted(0);
        userMapper.updateById(user);
        recommendationService.invalidateUserRecommendationCache(userId);
    }

    private void validateCategoryIds(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }

        Set<Long> distinctIds = new LinkedHashSet<>(categoryIds);
        List<SpotCategory> categories = spotCategoryMapper.selectList(
                new LambdaQueryWrapper<SpotCategory>()
                        .in(SpotCategory::getId, distinctIds)
                        .eq(SpotCategory::getIsDeleted, 0)
        );
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

    /**
     * 注销后偏好画像不再视为当前有效状态，但保留历史记录供后续审计或恢复使用。
     */
    private void softDeleteUserPreferences(Long userId) {
        UserPreference deletedPreference = new UserPreference();
        deletedPreference.setIsDeleted(1);
        userPreferenceMapper.update(
                deletedPreference,
                new LambdaUpdateWrapper<UserPreference>().eq(UserPreference::getUserId, userId)
        );
    }

    /**
     * 收藏属于当前偏好状态，账号注销后统一软删，避免继续参与前台状态展示。
     */
    private void softDeleteUserFavorites(Long userId) {
        UserSpotFavorite deletedFavorite = new UserSpotFavorite();
        deletedFavorite.setIsDeleted(1);
        userSpotFavoriteMapper.update(
                deletedFavorite,
                new LambdaUpdateWrapper<UserSpotFavorite>().eq(UserSpotFavorite::getUserId, userId)
        );
    }

    /**
     * 解封账号时恢复历史偏好，保持“封禁仅限制使用、解封即可继续使用”的管理语义。
     */
    private void restoreUserPreferences(Long userId) {
        UserPreference restoredPreference = new UserPreference();
        restoredPreference.setIsDeleted(0);
        userPreferenceMapper.update(
                restoredPreference,
                new LambdaUpdateWrapper<UserPreference>().eq(UserPreference::getUserId, userId)
        );
    }

    /**
     * 解封后恢复用户收藏，避免后台封禁临时管控造成收藏状态永久丢失。
     */
    private void restoreUserFavorites(Long userId) {
        UserSpotFavorite restoredFavorite = new UserSpotFavorite();
        restoredFavorite.setIsDeleted(0);
        userSpotFavoriteMapper.update(
                restoredFavorite,
                new LambdaUpdateWrapper<UserSpotFavorite>().eq(UserSpotFavorite::getUserId, userId)
        );
    }
}
