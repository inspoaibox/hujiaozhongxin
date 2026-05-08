package com.qianniuyun.common.enums;

/**
 * 座席状态枚举
 * 作者：深圳市千牛云科技有限公司
 */
public enum AgentStatus {
    IDLE("空闲"),
    TALKING("通话中"),
    DIALING("拨号中"),
    WRAPUP("整理"),
    REST("休息"),
    OFFLINE("离线");

    private final String label;

    AgentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 是否可以接收新呼叫
     */
    public boolean isAvailable() {
        return this == IDLE;
    }
}
