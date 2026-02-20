package com.travel.dto.spot;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端景点创建/更新请求
 */
@Data
public class AdminSpotRequest {
    
    @NotBlank(message = "景点名称不能为空")
    private String name;
    
    private String description;
    
    @NotNull(message = "门票价格不能为空")
    private BigDecimal price;
    
    private String openTime;
    
    @NotBlank(message = "详细地址不能为空")
    private String address;
    
    @NotNull(message = "纬度不能为空")
    private BigDecimal latitude;
    
    @NotNull(message = "经度不能为空")
    private BigDecimal longitude;
    
    private String coverImage;
    
    private List<String> images;
    
    private Long regionId;
    
    private Long categoryId;
    
    private Boolean published = false;

    private BigDecimal avgRating;

    private Integer ratingCount;

    private Integer heatScore;
}
