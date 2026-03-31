package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.dto.region.request.AdminRegionRequest;
import com.travel.dto.region.response.AdminRegionResponse;
import com.travel.service.SpotRegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 管理端景点地区控制器，负责地区树查询与维护接口。
 */
@Tag(name = "管理端-景点地区", description = "管理端景点地区管理相关接口")
@RestController
@RequestMapping("/api/admin/v1/regions")
@RequiredArgsConstructor
public class AdminRegionController {

    private final SpotRegionService spotRegionService;

    @Operation(summary = "获取地区列表(根据父ID)")
    @GetMapping
    public ApiResponse<List<AdminRegionResponse>> getRegions(@RequestParam(value = "parentId", required = false) Long parentId) {
        return ApiResponse.success(spotRegionService.getAdminRegions(parentId));
    }

    @Operation(summary = "创建地区")
    @PostMapping
    public ApiResponse<Void> createRegion(@Valid @RequestBody AdminRegionRequest request) {
        spotRegionService.createRegion(request);
        return ApiResponse.success();
    }

    @Operation(summary = "更新地区")
    @PutMapping("/{id}")
    public ApiResponse<Void> updateRegion(@PathVariable("id") Long id, @Valid @RequestBody AdminRegionRequest request) {
        spotRegionService.updateRegion(id, request);
        return ApiResponse.success();
    }

    @Operation(summary = "删除地区")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRegion(@PathVariable("id") Long id) {
        spotRegionService.deleteRegion(id);
        return ApiResponse.success();
    }
}
