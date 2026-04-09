package com.travel.service.impl;

import com.travel.dto.category.request.AdminCategoryRequest;
import com.travel.entity.Spot;
import com.travel.entity.SpotCategory;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.SpotCategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 分类排序服务测试
 * 重点覆盖插入冲突顺延和同级重排两个核心场景。
 */
@ExtendWith(MockitoExtension.class)
class SpotCategoryServiceImplTest {

    @Mock
    private SpotCategoryMapper spotCategoryMapper;

    @Mock
    private SpotMapper spotMapper;

    private SpotCategoryServiceImpl spotCategoryService;

    /**
     * 将 MyBatis Mapper 注入到 ServiceImpl 父类字段，便于直接做单元测试。
     */
    @BeforeEach
    void setUp() {
        spotCategoryService = new SpotCategoryServiceImpl(spotMapper);
        ReflectionTestUtils.setField(spotCategoryService, "baseMapper", spotCategoryMapper);
    }

    @Test
    void createCategory_shouldShiftFollowingSiblings_whenRequestedSortAlreadyExists() {
        SpotCategory first = buildCategory(1L, 0L, 1);
        SpotCategory second = buildCategory(2L, 0L, 2);

        when(spotCategoryMapper.selectList(any())).thenReturn(List.of(first, second));
        when(spotCategoryMapper.selectById(0L)).thenReturn(buildCategory(0L, null, 1));
        when(spotCategoryMapper.updateById(any())).thenReturn(1);
        when(spotCategoryMapper.insert(any())).thenReturn(1);

        AdminCategoryRequest request = new AdminCategoryRequest();
        request.setParentId(0L);
        request.setName("新分类");
        request.setSortOrder(2);

        spotCategoryService.createCategory(request);

        verify(spotCategoryMapper).updateById(second);
        assertEquals(3, second.getSortOrder());

        ArgumentCaptor<SpotCategory> insertCaptor = ArgumentCaptor.forClass(SpotCategory.class);
        verify(spotCategoryMapper).insert(insertCaptor.capture());
        assertEquals(2, insertCaptor.getValue().getSortOrder());
        assertEquals(0, insertCaptor.getValue().getIsDeleted());
    }

    @Test
    void updateCategory_shouldShiftRange_whenMovingUpWithinSameParent() {
        SpotCategory current = buildCategory(3L, 0L, 3);
        SpotCategory first = buildCategory(1L, 0L, 1);
        SpotCategory second = buildCategory(2L, 0L, 2);
        SpotCategory fourth = buildCategory(4L, 0L, 4);

        when(spotCategoryMapper.selectById(3L)).thenReturn(current);
        when(spotCategoryMapper.selectById(0L)).thenReturn(buildCategory(0L, null, 1));
        when(spotCategoryMapper.selectList(any())).thenReturn(List.of(first, second, current, fourth));
        when(spotCategoryMapper.updateById(any())).thenReturn(1);

        AdminCategoryRequest request = new AdminCategoryRequest();
        request.setParentId(0L);
        request.setName("当前分类");
        request.setSortOrder(2);

        spotCategoryService.updateCategory(3L, request);

        verify(spotCategoryMapper, times(2)).updateById(any());
        assertEquals(3, second.getSortOrder());
        assertEquals(2, current.getSortOrder());
    }

    @Test
    void deleteCategory_shouldCompactFollowingSiblings_whenDeleteSucceeds() {
        SpotCategory current = buildCategory(2L, 0L, 2);
        SpotCategory following = buildCategory(3L, 0L, 3);

        when(spotCategoryMapper.selectById(2L)).thenReturn(current);
        when(spotCategoryMapper.selectCount(any())).thenReturn(0L);
        when(spotMapper.selectCount(any())).thenReturn(0L);
        when(spotCategoryMapper.updateById(any())).thenReturn(1);
        when(spotCategoryMapper.selectList(any())).thenReturn(List.of(current, following));

        spotCategoryService.deleteCategory(2L);

        verify(spotCategoryMapper).updateById(argThat(item -> item.getId().equals(2L) && item.getIsDeleted() == 1));
        verify(spotCategoryMapper).updateById(argThat(item -> item.getId().equals(3L) && item.getSortOrder() == 2));
        assertEquals(2, following.getSortOrder());
    }

    @Test
    void deleteCategory_rejectsWhenActiveSpotStillReferencesCategory() {
        SpotCategory current = buildCategory(2L, 0L, 2);

        when(spotCategoryMapper.selectById(2L)).thenReturn(current);
        when(spotCategoryMapper.selectCount(any())).thenReturn(0L);
        when(spotMapper.selectCount(any())).thenReturn(1L);

        RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
            () -> spotCategoryService.deleteCategory(2L));

        assertEquals("该分类下仍有关联景点，请先调整景点分类", ex.getMessage());
    }

    /**
     * 按给定父节点和排序构造有效分类夹具。
     */
    private SpotCategory buildCategory(Long id, Long parentId, Integer sortOrder) {
        SpotCategory category = new SpotCategory();
        category.setId(id);
        category.setParentId(parentId);
        category.setName("分类" + id);
        category.setSortOrder(sortOrder);
        category.setIsDeleted(0);
        return category;
    }
}
