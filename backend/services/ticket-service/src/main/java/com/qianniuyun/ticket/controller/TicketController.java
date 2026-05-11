package com.qianniuyun.ticket.controller;

import com.qianniuyun.common.enums.TicketStatus;
import com.qianniuyun.common.model.PageResult;
import com.qianniuyun.common.model.Result;
import com.qianniuyun.ticket.dto.AssignTicketRequest;
import com.qianniuyun.ticket.dto.CreateTicketDTO;
import com.qianniuyun.ticket.dto.TicketQuery;
import com.qianniuyun.ticket.dto.TicketStatusRequest;
import com.qianniuyun.ticket.entity.Ticket;
import com.qianniuyun.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public Result<Ticket> createTicket(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                       @RequestBody CreateTicketDTO request) {
        return Result.success(ticketService.createTicket(request, userId != null ? userId : 0L));
    }

    @GetMapping
    public Result<PageResult<Ticket>> queryTickets(TicketQuery query) {
        return Result.success(ticketService.queryTickets(query));
    }

    @PutMapping("/{ticketId}/status")
    public Result<Ticket> updateStatus(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                       @PathVariable Long ticketId,
                                       @RequestBody TicketStatusRequest request) {
        Ticket ticket = ticketService.updateStatus(
                ticketId,
                TicketStatus.valueOf(request.getStatus()),
                request.getComment(),
                userId != null ? userId : 0L
        );
        return Result.success(ticket);
    }

    @PutMapping("/{ticketId}/assign")
    public Result<Void> assignTicket(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                     @PathVariable Long ticketId,
                                     @RequestBody AssignTicketRequest request) {
        ticketService.assignTicket(ticketId, request.getAssigneeId(), userId != null ? userId : 0L);
        return Result.success();
    }
}
