package com.travel.config.persistence;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充配置。
 * <p>
 * 统一处理创建时间和更新时间字段的自动写入。
 */
@Component
public class MetaObjectHandlerConfig implements MetaObjectHandler {

    /**
     * 新增数据时自动填充创建时间和更新时间。
     * <p>
     * 创建和更新时同时写入当前时间，可以避免业务层每次保存实体都重复关心公共审计字段。
     *
     * @param metaObject MyBatis 元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新数据时自动刷新更新时间。
     * <p>
     * 更新场景只覆盖更新时间，避免误改创建时间。
     *
     * @param metaObject MyBatis 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("updatedAt", LocalDateTime.now(), metaObject);
    }
}
