package com.travel.dto.favorite.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 收藏操作请求对象。
 */
@Data
public class FavoriteRequest {
    @NotNull(message = "景点ID不能为空")
    private Long spotId;
}
