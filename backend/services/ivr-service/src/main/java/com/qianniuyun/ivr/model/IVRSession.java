package com.qianniuyun.ivr.model;

import lombok.Data;

@Data
public class IVRSession {
    private String callId;
    private Long flowId;
    private String currentNodeId;
    private int timeoutRetries;
}
