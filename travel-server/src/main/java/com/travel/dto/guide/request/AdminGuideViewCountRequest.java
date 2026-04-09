package com.travel.dto.guide.request;

import lombok.Data;

/**
 * 管理端攻略浏览量调整请求对象。
 * <p>
 * 仅用于后台运营或修数场景下调整攻略浏览量。
 */
@Data
public class AdminGuideViewCountRequest {

    private Integer viewCount;
}
