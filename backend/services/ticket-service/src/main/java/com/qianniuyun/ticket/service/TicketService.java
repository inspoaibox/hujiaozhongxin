package com.qianniuyun.ticket.service;

import com.qianniuyun.common.enums.TicketStatus;
import com.qianniuyun.common.exception.BusinessException;
import com.qianniuyun.common.model.PageResult;
import com.qianniuyun.common.utils.TicketNoGenerator;
import com.qianniuyun.ticket.dto.CreateTicketDTO;
import com.qianniuyun.ticket.dto.TicketQuery;
import com.qianniuyun.ticket.entity.Ticket;
import com.qianniuyun.ticket.entity.TicketHistory;
import com.qianniuyun.ticket.repository.TicketHistoryRepository;
import com.qianniuyun.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工单服务
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository historyRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 创建工单
     */
    @Transactional
    public Ticket createTicket(CreateTicketDTO dto, Long createdBy) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new BusinessException("工单标题不能为空");
        }

        Ticket ticket = new Ticket();
        ticket.setTicketNo(TicketNoGenerator.generate());
        ticket.setTitle(dto.getTitle());
        ticket.setDescription(dto.getDescription());
        ticket.setPriority(dto.getPriority() != null ? dto.getPriority() : "NORMAL");
        ticket.setCategory(dto.getCategory());
        ticket.setCustomerId(dto.getCustomerId());
        ticket.setCallId(dto.getCallId());
        ticket.setStatus(TicketStatus.PENDING);
        ticket.setCreatedBy(createdBy);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        ticketRepository.save(ticket);

        // 记录创建历史
        recordHistory(ticket.getId(), null, TicketStatus.PENDING, "创建工单", createdBy);

        // 发布工单创建事件（触发自动分配）
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", "TICKET_CREATED");
        event.put("ticketId", ticket.getId());
        event.put("priority", ticket.getPriority());
        event.put("category", ticket.getCategory());
        kafkaTemplate.send("qianniu.ticket.events", ticket.getTicketNo(), event);

        log.info("工单创建: ticketNo={}, priority={}", ticket.getTicketNo(), ticket.getPriority());
        return ticket;
    }

    /**
     * 更新工单状态
     */
    @Transactional
    public Ticket updateStatus(Long ticketId, TicketStatus newStatus,
                                String comment, Long operatedBy) {
        if (newStatus == null) {
            throw new BusinessException("工单状态不能为空");
        }
        Ticket ticket = getTicketOrThrow(ticketId);
        TicketStatus oldStatus = ticket.getStatus();

        // 验证状态转换
        validateStatusTransition(oldStatus, newStatus);

        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());

        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else if (newStatus == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
        }

        ticketRepository.save(ticket);

        // 记录状态变更历史
        recordHistory(ticketId, oldStatus, newStatus, comment, operatedBy);

        // 发布状态变更事件
        kafkaTemplate.send("qianniu.ticket.events", ticket.getTicketNo(),
                Map.of("eventType", "TICKET_STATUS_CHANGED",
                        "ticketId", ticketId,
                        "oldStatus", oldStatus.name(),
                        "newStatus", newStatus.name()));

        log.info("工单状态变更: ticketNo={}, {} -> {}", ticket.getTicketNo(), oldStatus, newStatus);
        return ticket;
    }

    /**
     * 分配工单
     */
    @Transactional
    public void assignTicket(Long ticketId, Long assigneeId, Long operatedBy) {
        if (assigneeId == null) {
            throw new BusinessException("处理人不能为空");
        }
        Ticket ticket = getTicketOrThrow(ticketId);
        ticket.setAssignedTo(assigneeId);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        recordHistory(ticketId, TicketStatus.PENDING, TicketStatus.IN_PROGRESS,
                "分配给处理人", operatedBy);

        // 通知被分配人
        kafkaTemplate.send("qianniu.notification.events", String.valueOf(assigneeId),
                Map.of("type", "TICKET_ASSIGNED",
                        "ticketId", ticketId,
                        "ticketNo", ticket.getTicketNo(),
                        "userId", assigneeId));

        log.info("工单分配: ticketNo={}, assignee={}", ticket.getTicketNo(), assigneeId);
    }

    /**
     * 分页查询工单
     */
    public PageResult<Ticket> queryTickets(TicketQuery query) {
        int currentPage = Math.max(query.getPage(), 1);
        int currentPageSize = Math.max(query.getPageSize(), 1);
        Page<Ticket> page = ticketRepository.findByConditions(
                query.getStatus(),
                query.getPriority(),
                query.getAssignedTo(),
                query.getCustomerId(),
                PageRequest.of(currentPage - 1, currentPageSize)
        );
        return PageResult.of(page.getContent(), page.getTotalElements(),
                currentPage, currentPageSize);
    }

    /**
     * 定时检查超时工单（每小时执行）
     */
    @Scheduled(fixedDelay = 3600000)
    public void checkOverdueTickets() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<Ticket> overdueTickets = ticketRepository
                .findByStatusAndCreatedAtBefore(TicketStatus.PENDING, threshold);

        for (Ticket ticket : overdueTickets) {
            log.warn("工单超时未处理: ticketNo={}", ticket.getTicketNo());
            // 通知管理员
            kafkaTemplate.send("qianniu.notification.events", "ADMIN",
                    Map.of("type", "TICKET_OVERDUE",
                            "ticketId", ticket.getId(),
                            "ticketNo", ticket.getTicketNo()));
        }
    }

    private void recordHistory(Long ticketId, TicketStatus oldStatus,
                                TicketStatus newStatus, String comment, Long operatedBy) {
        TicketHistory history = new TicketHistory();
        history.setTicketId(ticketId);
        history.setOldStatus(oldStatus != null ? oldStatus.name() : null);
        history.setNewStatus(newStatus.name());
        history.setComment(comment);
        history.setOperatedBy(operatedBy);
        history.setCreatedAt(LocalDateTime.now());
        historyRepository.save(history);
    }

    private void validateStatusTransition(TicketStatus from, TicketStatus to) {
        if (from == TicketStatus.CLOSED) {
            throw new BusinessException("已关闭的工单不能再变更状态");
        }
    }

    private Ticket getTicketOrThrow(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException("工单不存在: " + ticketId));
    }
}
