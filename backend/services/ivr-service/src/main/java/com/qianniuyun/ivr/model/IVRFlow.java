package com.qianniuyun.ivr.model;

import lombok.Data;
import java.util.Map;

@Data
public class IVRFlow {
    private Long id;
    private String code;
    private String startNodeId;
    private Map<String, IVRNode> nodes;

    public IVRNode getStartNode() {
        return nodes != null ? nodes.get(startNodeId) : null;
    }

    public IVRNode getNode(String nodeId) {
        return nodes != null ? nodes.get(nodeId) : null;
    }
}
