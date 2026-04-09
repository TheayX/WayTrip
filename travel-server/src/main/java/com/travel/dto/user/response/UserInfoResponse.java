package com.travel.dto.user.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 用户资料响应对象。
 * <p>
 * 面向“我的”页和资料设置页，返回当前用户的基础资料与偏好信息。
 */
@Data
@Builder
public class UserInfoResponse {
    private Long id;
    private String nickname;
    private String avatar;
    private String phone;
    private Boolean hasPassword;
    private List<String> preferences;
    private List<Long> preferenceCategoryIds;
    private List<String> preferenceCategoryNames;
}
