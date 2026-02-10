package com.travel.dto.auth;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 用户信息响应
 */
@Data
@Builder
public class UserInfoResponse {
    private Long id;
    private String nickname;
    private String avatar;
    private String phone;
    private List<String> preferences;
}
