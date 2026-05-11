package com.qianniuyun.call.dto;

import lombok.Data;

@Data
public class OutboundCallRequest {
    private Long agentId;
    private String phone;
}
