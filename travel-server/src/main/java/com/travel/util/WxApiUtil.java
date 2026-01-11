package com.travel.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 微信 API 工具类
 */
@Slf4j
@Component
public class WxApiUtil {

    @Value("${wx.appid}")
    private String appid;

    @Value("${wx.secret}")
    private String secret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    /**
     * 通过 code 获取 openid
     */
    public String getOpenid(String code) {
        try {
            String url = String.format(WX_LOGIN_URL, appid, secret, code);
            log.info("调用微信登录API, code={}", code);
            
            String response = restTemplate.getForObject(url, String.class);
            log.info("微信API响应: {}", response);
            
            if (response == null) {
                log.error("微信API返回空响应");
                return null;
            }
            
            JsonNode jsonNode = objectMapper.readTree(response);
            
            // 检查错误码
            if (jsonNode.has("errcode") && jsonNode.get("errcode").asInt() != 0) {
                int errcode = jsonNode.get("errcode").asInt();
                String errmsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : "未知错误";
                log.error("微信登录失败: errcode={}, errmsg={}", errcode, errmsg);
                return null;
            }
            
            // 安全获取 openid
            if (!jsonNode.has("openid") || jsonNode.get("openid").isNull()) {
                log.error("微信API响应中没有openid字段: {}", response);
                return null;
            }
            
            return jsonNode.get("openid").asText();
        } catch (Exception e) {
            log.error("调用微信API异常: {}", e.getMessage(), e);
            return null;
        }
    }
}
