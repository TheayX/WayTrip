package com.travel.dto.auth;

import lombok.Data;

/**
 * 更新用户信息请求
 */
@Data
public class UpdateUserInfoRequest {
    private String nickname;
    private String avatar;
    private String phone;
}
