package com.travel.service.support.storage;

import lombok.Builder;
import lombok.Getter;

/**
 * 已落盘文件信息，供上传服务组装响应使用。
 */
@Getter
@Builder
public class StoredFileInfo {

    private final String filename;
    private final String url;
    private final String absolutePath;
}
