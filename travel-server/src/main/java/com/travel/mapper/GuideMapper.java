package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.Guide;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 攻略数据访问接口。
 */
@Mapper
public interface GuideMapper extends BaseMapper<Guide> {
    
    /**
     * 查询所有已启用攻略中不重复的分类名称。
     */
    @Select("SELECT DISTINCT category FROM guide WHERE is_deleted = 0 AND category IS NOT NULL AND category != '' ORDER BY category")
    List<String> selectDistinctCategories();
}
