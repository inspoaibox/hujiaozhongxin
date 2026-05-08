package com.qianniuyun.ticket.dto;

import com.qianniuyun.common.enums.TicketStatus;
import lombok.Data;

@Data
public class TicketQuery {
    private TicketStatus status;
    private String priority;
    private Long assignedTo;
    private Long customerId;
    private int page = 1;
    private int pageSize = 20;
}
