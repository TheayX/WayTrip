package com.travel.dto.favorite.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 收藏操作请求对象。
 * <p>
 * 仅承接收藏动作所需的景点标识，用户身份由登录上下文补足。
 */
@Data
public class FavoriteRequest {
    @NotNull(message = "景点ID不能为空")
    private Long spotId;
}
