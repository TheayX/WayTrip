package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.GuideSpotRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 攻略与景点关联数据访问接口。
 * <p>
 * 主要用于维护攻略与景点的排序关联，业务规则仍由攻略服务层控制。
 */
@Mapper
public interface GuideSpotRelationMapper extends BaseMapper<GuideSpotRelation> {
}

