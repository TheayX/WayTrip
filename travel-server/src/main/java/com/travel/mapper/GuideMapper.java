package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.Guide;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GuideMapper extends BaseMapper<Guide> {
    
    /**
     * 获取所有不重复的分类
     */
    @Select("SELECT DISTINCT category FROM guide WHERE is_deleted = 0 AND category IS NOT NULL AND category != '' ORDER BY category")
    List<String> selectDistinctCategories();
}
