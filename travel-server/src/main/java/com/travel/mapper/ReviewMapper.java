package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.dto.review.SpotRatingStats;
import com.travel.dto.spot.SpotDetailResponse;
import com.travel.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 景点评价数据访问接口。
 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    /**
     * 分页查询指定景点下的评价记录。
     */
    IPage<Review> selectReviewPage(Page<Review> page, @Param("spotId") Long spotId);

    /**
     * 分页查询指定用户发布的评价记录。
     */
    IPage<Review> selectUserReviewPage(Page<Review> page, @Param("userId") Long userId);

    /**
     * 分页查询管理端评价列表。
     */
    IPage<Review> selectAdminReviewPage(Page<Review> page,
                                        @Param("nickname") String nickname,
                                        @Param("spotName") String spotName);

    /**
     * 查询景点详情页展示的最新评论列表。
     */
    List<SpotDetailResponse.CommentItem> selectLatestComments(@Param("spotId") Long spotId, @Param("limit") int limit);

    /**
     * 统计指定景点的评分汇总信息。
     */
    SpotRatingStats selectSpotRatingStats(@Param("spotId") Long spotId);
}
