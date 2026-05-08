package com.qianniuyun.notification.service;

import org.springframework.stereotype.Service;

@Service
public class NotificationTemplateService {
    public String getTemplate(String type) {
        return type;
    }
}
