package com.travel.dto.guide;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

/**
 * 管理端攻略创建/更新请求
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

    // 编辑回显用：包含已下架/已删除的景点信息
    private List<SpotOption> spotOptions;

    @Data
    public static class SpotOption {
        private Long id;
        private String name;
        private Integer published;
        private Integer isDeleted;
    }
}
