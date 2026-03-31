package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.review.request.AdminReviewListRequest;
import com.travel.dto.review.request.ReviewFeedRequest;
import com.travel.dto.review.request.ReviewRequest;
import com.travel.dto.review.response.ReviewResponse;

/**
 * 评价服务接口。
 * <p>
 * 定义用户端评价提交、查询、删除，以及管理端评价查询和景点评分同步能力。
 */
public interface ReviewService {

    /**
     * 提交或更新当前用户对指定景点的评价。
     *
     * @param userId 当前登录用户 ID
     * @param request 评价提交参数，包含景点 ID、评分和评论内容
     */
    void submitReview(Long userId, ReviewRequest request);

    /**
     * 获取当前用户对指定景点的评价记录。
     *
     * @param userId 当前登录用户 ID
     * @param spotId 景点 ID
     * @return 评价记录；若当前用户尚未评价则返回 {@code null}
     */
    ReviewResponse getUserReview(Long userId, Long spotId);

    /**
     * 分页获取指定景点下的评价列表。
     *
     * @param spotId 景点 ID
     * @param page 当前页码
     * @param pageSize 每页条数
     * @return 景点评价分页结果
     */
    PageResult<ReviewResponse> getSpotReviews(Long spotId, Integer page, Integer pageSize);

    /**
     * 分页获取口碑流。
     *
     * @param request 口碑流查询参数
     * @return 口碑流分页结果
     */
    PageResult<ReviewResponse> getReviewFeed(ReviewFeedRequest request);

    /**
     * 分页获取当前用户发布的评价列表。
     *
     * @param userId 当前登录用户 ID
     * @param page 当前页码
     * @param pageSize 每页条数
     * @return 用户评价分页结果
     */
    PageResult<ReviewResponse> getUserReviews(Long userId, Integer page, Integer pageSize);

    /**
     * 删除当前用户自己的评价记录。
     *
     * @param userId 当前登录用户 ID
     * @param reviewId 评价 ID
     */
    void deleteReview(Long userId, Long reviewId);

    /**
     * 管理员删除指定评价记录。
     *
     * @param reviewId 评价 ID
     */
    void deleteReviewByAdmin(Long reviewId);

    /**
     * 分页获取管理端评价列表。
     *
     * @param request 管理端评价查询参数
     * @return 管理端评价分页结果
     */
    PageResult<ReviewResponse> getAdminReviews(AdminReviewListRequest request);

    /**
     * 获取指定用户的有效评价数量。
     *
     * @param userId 用户 ID
     * @return 有效评价数量
     */
    int getUserReviewCount(Long userId);

    /**
     * 刷新单个景点的评分统计信息。
     *
     * @param spotId 景点 ID
     */
    void refreshSpotRating(Long spotId);

    /**
     * 刷新全部景点的评分统计信息。
     */
    void refreshAllSpotRatings();
}
