package com.travel.common.constant;

/**
 * 统一维护资源失效、主体失效和普通空值的展示文案，避免业务层各自硬编码。
 */
public final class ResourceDisplayText {

    private ResourceDisplayText() {
    }

    public static final class User {
        public static final String DEACTIVATED = "已注销用户";
        public static final String PURGED = "已清除用户";
        public static final String UNKNOWN = "未知用户";

        private User() {
        }
    }

    public static final class Spot {
        public static final String OFFLINE = "已下架景点";
        public static final String DELETED = "已删除景点";
        public static final String PURGED = "已清除景点";
        public static final String UNKNOWN = "未知景点";

        private Spot() {
        }
    }

    public static final class Content {
        public static final String OFFLINE = "已下架内容";
        public static final String DELETED = "已删除内容";
        public static final String PURGED = "已清除内容";
        public static final String UNKNOWN = "未知内容";

        private Content() {
        }
    }

    public static final class Source {
        public static final String UNKNOWN = "未知来源";

        private Source() {
        }
    }

    public static final class Common {
        public static final String EMPTY = "--";
        public static final String UNKNOWN = "未知";

        private Common() {
        }
    }
}
