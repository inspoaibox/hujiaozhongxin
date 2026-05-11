package com.qianniuyun.ivr.service;

import com.qianniuyun.ivr.model.IVRFlow;
import com.qianniuyun.ivr.model.IVRNode;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class IVRConfigService {
    public IVRFlow getFlowByCode(String code) {
        return defaultFlow();
    }

    public IVRFlow getFlowById(Long id) {
        return defaultFlow();
    }

    private IVRFlow defaultFlow() {
        IVRNode welcome = new IVRNode();
        welcome.setNodeId("welcome");
        welcome.setType("PLAY_TTS");
        welcome.setText("欢迎致电千牛云客服中心");
        welcome.setNextNodeId("main-menu");

        IVRNode menu = new IVRNode();
        menu.setNodeId("main-menu");
        menu.setType("GET_DIGITS");
        menu.setPromptText("业务咨询请按 1，技术支持请按 2，投诉建议请按 3");
        menu.setTimeout(10);
        menu.setDefaultRoute("transfer-general");
        menu.setRoutes(Map.of(
                "1", "transfer-general",
                "2", "transfer-tech",
                "3", "transfer-complaint"
        ));

        IVRNode general = transferNode("transfer-general", "GENERAL");
        IVRNode tech = transferNode("transfer-tech", "TECH");
        IVRNode complaint = transferNode("transfer-complaint", "COMPLAINT");

        IVRFlow flow = new IVRFlow();
        flow.setId(1L);
        flow.setCode("DEFAULT");
        flow.setStartNodeId("welcome");
        flow.setNodes(Map.of(
                welcome.getNodeId(), welcome,
                menu.getNodeId(), menu,
                general.getNodeId(), general,
                tech.getNodeId(), tech,
                complaint.getNodeId(), complaint
        ));
        return flow;
    }

    private IVRNode transferNode(String nodeId, String skillGroup) {
        IVRNode node = new IVRNode();
        node.setNodeId(nodeId);
        node.setType("TRANSFER");
        node.setSkillGroup(skillGroup);
        return node;
    }
}
