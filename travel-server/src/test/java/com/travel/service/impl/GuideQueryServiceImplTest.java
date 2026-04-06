package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.guide.request.GuideListRequest;
import com.travel.entity.Guide;
import com.travel.entity.GuideSpotRelation;
import com.travel.entity.Spot;
import com.travel.mapper.GuideMapper;
import com.travel.mapper.GuideSpotRelationMapper;
import com.travel.mapper.SpotMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 攻略查询服务测试。
 * 聚焦发布状态过滤，避免下架内容在用户端泄露。
 */
@ExtendWith(MockitoExtension.class)
class GuideQueryServiceImplTest {

    /**
     * 初始化 MyBatis-Plus Lambda 缓存，避免 Lambda 条件在单测阶段缺少元数据。
     */
    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        assistant.setCurrentNamespace("test");
        TableInfoHelper.initTableInfo(assistant, Guide.class);
        TableInfoHelper.initTableInfo(assistant, GuideSpotRelation.class);
    }

    @Mock
    private GuideMapper guideMapper;

    @Mock
    private GuideSpotRelationMapper guideSpotRelationMapper;

    @Mock
    private SpotMapper spotMapper;

    private GuideQueryServiceImpl guideQueryService;

    @BeforeEach
    void setUp() {
        guideQueryService = new GuideQueryServiceImpl(guideMapper, guideSpotRelationMapper, spotMapper);
    }

    @Test
    void getGuideList_returnsPagedRecords() {
        Guide guide = buildGuide(1L, 1);
        Page<Guide> page = new Page<>(1, 10);
        page.setRecords(List.of(guide));
        page.setTotal(1L);
        when(guideMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        var response = guideQueryService.getGuideList(new GuideListRequest());

        verify(guideMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        assertEquals(1, response.getList().size());
        assertEquals(1L, response.getList().get(0).getId());
    }

    @Test
    void getGuideDetail_rejectsOfflineGuide() {
        Guide offline = buildGuide(2L, 0);
        when(guideMapper.selectById(2L)).thenReturn(offline);

        BusinessException ex = assertThrows(BusinessException.class, () -> guideQueryService.getGuideDetail(2L));

        assertEquals(ResultCode.GUIDE_OFFLINE.getCode(), ex.getCode());
        assertEquals(ResultCode.GUIDE_OFFLINE.getMessage(), ex.getMessage());
        verifyNoInteractions(guideSpotRelationMapper, spotMapper);
    }

    @Test
    void getGuideDetail_filtersOfflineRelatedSpots() {
        Guide guide = buildGuide(3L, 1);
        GuideSpotRelation relation1 = new GuideSpotRelation();
        relation1.setGuideId(3L);
        relation1.setSpotId(11L);
        relation1.setSortOrder(1);
        relation1.setIsDeleted(0);
        GuideSpotRelation relation2 = new GuideSpotRelation();
        relation2.setGuideId(3L);
        relation2.setSpotId(12L);
        relation2.setSortOrder(2);
        relation2.setIsDeleted(0);

        Spot onlineSpot = buildSpot(11L, 1, 0);
        Spot offlineSpot = buildSpot(12L, 0, 0);

        when(guideMapper.selectById(3L)).thenReturn(guide);
        when(guideMapper.update(eq(null), any())).thenReturn(1);
        when(guideSpotRelationMapper.selectList(any())).thenReturn(List.of(relation1, relation2));
        when(spotMapper.selectBatchIds(List.of(11L, 12L))).thenReturn(List.of(onlineSpot, offlineSpot));

        var response = guideQueryService.getGuideDetail(3L);

        assertEquals(1, response.getRelatedSpots().size());
        assertEquals(11L, response.getRelatedSpots().get(0).getId());
        assertFalse(response.getRelatedSpots().stream().anyMatch(item -> item.getId().equals(12L)));
        assertTrue(response.getRelatedSpots().get(0).getPrice().startsWith("￥"));
    }

    private Guide buildGuide(Long id, Integer isPublished) {
        Guide guide = new Guide();
        guide.setId(id);
        guide.setTitle("测试攻略");
        guide.setContent("<p>攻略内容</p>");
        guide.setCategory("亲子");
        guide.setViewCount(9);
        guide.setIsPublished(isPublished);
        guide.setIsDeleted(0);
        guide.setCreatedAt(LocalDateTime.of(2026, 4, 1, 8, 30));
        return guide;
    }

    private Spot buildSpot(Long id, Integer isPublished, Integer isDeleted) {
        Spot spot = new Spot();
        spot.setId(id);
        spot.setName("关联景点" + id);
        spot.setPrice(BigDecimal.valueOf(99));
        spot.setIsPublished(isPublished);
        spot.setIsDeleted(isDeleted);
        return spot;
    }
}


