package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.GuideSpotRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 攻略与景点关联数据访问接口。
 */
@Mapper
public interface GuideSpotRelationMapper extends BaseMapper<GuideSpotRelation> {
}

