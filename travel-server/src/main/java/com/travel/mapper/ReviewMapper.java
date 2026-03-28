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

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    IPage<Review> selectReviewPage(Page<Review> page, @Param("spotId") Long spotId);

    IPage<Review> selectUserReviewPage(Page<Review> page, @Param("userId") Long userId);

    IPage<Review> selectAdminReviewPage(Page<Review> page,
                                        @Param("nickname") String nickname,
                                        @Param("spotName") String spotName);

    List<SpotDetailResponse.CommentItem> selectLatestComments(@Param("spotId") Long spotId, @Param("limit") int limit);

    SpotRatingStats selectSpotRatingStats(@Param("spotId") Long spotId);
}
