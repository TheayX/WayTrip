package com.travel.dto.guide;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

/**
 * 管理端攻略新增或编辑请求参数对象。
 */
@Data
public class AdminGuideRequest {
    @NotBlank(message = "标题不能为空")
    private String title;
    
    private String coverImage;
    
    private String category;
    
    @NotBlank(message = "内容不能为空")
    private String content;
    
    private Boolean published = false;
    
    private List<Long> spotIds;

    /**
     * 编辑回显使用的景点选项，允许包含已下架或已删除的景点。
     */
    private List<SpotOption> spotOptions;

    /**
     * 攻略编辑回显中的景点选项对象。
     */
    @Data
    public static class SpotOption {
        private Long id;
        private String name;
        private Integer published;
        private Integer isDeleted;
    }
}
