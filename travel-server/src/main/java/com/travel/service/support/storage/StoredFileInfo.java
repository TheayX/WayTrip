package com.travel.service.support.storage;

import lombok.Builder;
import lombok.Getter;

/**
 * 已落盘文件信息，供上传服务组装响应使用。
 * <p>
 * 同时保留 URL 和绝对路径，方便业务层既能回前端，也能在日志里定位真实落盘位置。
 */
@Getter
@Builder
public class StoredFileInfo {

    /**
     * 落盘后的文件名。
     */
    private final String filename;
    /**
     * 可供前端访问的相对 URL。
     */
    private final String url;
    /**
     * 服务端本地绝对路径。
     */
    private final String absolutePath;
}
