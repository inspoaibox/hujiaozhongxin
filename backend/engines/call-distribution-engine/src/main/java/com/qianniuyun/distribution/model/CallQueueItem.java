package com.qianniuyun.distribution.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CallQueueItem {
    private String callId;
    private String skillGroupCode;
    private long waitSeconds;
}
