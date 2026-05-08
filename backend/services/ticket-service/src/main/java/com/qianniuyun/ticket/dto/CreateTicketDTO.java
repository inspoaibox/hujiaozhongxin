package com.qianniuyun.ticket.dto;

import lombok.Data;

@Data
public class CreateTicketDTO {
    private String title;
    private String description;
    private String priority;
    private String category;
    private Long customerId;
    private String callId;
}
