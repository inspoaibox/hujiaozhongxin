package com.qianniuyun.ticket.repository;

import com.qianniuyun.ticket.entity.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {
}
