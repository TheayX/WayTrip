package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.dto.category.request.AdminCategoryRequest;
import com.travel.dto.category.response.AdminCategoryResponse;
import com.travel.service.SpotCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 管理端景点分类控制器，负责分类树查询与维护接口。
 * <p>
 * 分类树维护独立成域，避免景点编辑页直接承担树结构增删改的复杂度。
 */
@Tag(name = "管理端-景点分类", description = "管理端景点分类管理相关接口")
@RestController
@RequestMapping("/api/admin/v1/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final SpotCategoryService spotCategoryService;

    @Operation(summary = "获取分类列表(根据父ID)")
    @GetMapping
    public ApiResponse<List<AdminCategoryResponse>> getCategories(@RequestParam(value = "parentId", required = false) Long parentId) {
        return ApiResponse.success(spotCategoryService.getAdminCategories(parentId));
    }

    @Operation(summary = "创建分类")
    @PostMapping
    public ApiResponse<Void> createCategory(@Valid @RequestBody AdminCategoryRequest request) {
        spotCategoryService.createCategory(request);
        return ApiResponse.success();
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public ApiResponse<Void> updateCategory(@PathVariable("id") Long id, @Valid @RequestBody AdminCategoryRequest request) {
        spotCategoryService.updateCategory(id, request);
        return ApiResponse.success();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable("id") Long id) {
        spotCategoryService.deleteCategory(id);
        return ApiResponse.success();
    }
}
