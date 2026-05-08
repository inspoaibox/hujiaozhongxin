package com.qianniuyun.ivr.model;

import lombok.Data;
import java.util.Map;

@Data
public class IVRNode {
    private String nodeId;
    private String type;
    private String audioFile;
    private String promptAudio;
    private String promptText;
    private String text;
    private String nextNodeId;
    private String defaultRoute;
    private String skillGroup;
    private Integer timeout;
    private Map<String, String> routes;
}
