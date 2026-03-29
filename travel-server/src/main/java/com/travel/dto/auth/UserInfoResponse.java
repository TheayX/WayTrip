package com.travel.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 用户资料响应对象。
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
