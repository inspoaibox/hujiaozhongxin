package com.qianniuyun.ticket.dto;

import lombok.Data;

@Data
public class TicketStatusRequest {
    private String status;
    private String comment;
}
