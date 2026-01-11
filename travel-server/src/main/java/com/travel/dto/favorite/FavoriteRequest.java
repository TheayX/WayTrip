package com.travel.dto.favorite;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 收藏请求
 */
@Data
public class FavoriteRequest {
    @NotNull(message = "景点ID不能为空")
    private Long spotId;
}
