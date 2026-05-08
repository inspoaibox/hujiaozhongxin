package com.qianniuyun.common.enums;

/**
 * 呼叫状态枚举
 * 作者：深圳市千牛云科技有限公司
 */
public enum CallStatus {
    INITIATED("已创建"),
    RINGING("振铃中"),
    QUEUED("排队中"),
    ANSWERED("通话中"),
    HOLDING("保持中"),
    TRANSFERRING("转接中"),
    CONFERENCING("三方通话"),
    COMPLETED("已完成"),
    ABANDONED("已放弃"),
    FAILED("失败");

    private final String label;

    CallStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isActive() {
        return this == ANSWERED || this == HOLDING ||
               this == TRANSFERRING || this == CONFERENCING;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == ABANDONED || this == FAILED;
    }
}
