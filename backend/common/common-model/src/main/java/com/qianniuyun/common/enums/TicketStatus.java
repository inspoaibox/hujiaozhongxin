package com.qianniuyun.common.enums;

/**
 * 工单状态枚举
 * 作者：深圳市千牛云科技有限公司
 */
public enum TicketStatus {
    PENDING("待处理"),
    IN_PROGRESS("处理中"),
    RESOLVED("已解决"),
    CLOSED("已关闭");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
