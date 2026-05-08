package com.qianniuyun.ticket.repository;

import com.qianniuyun.common.enums.TicketStatus;
import com.qianniuyun.ticket.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByStatusAndCreatedAtBefore(TicketStatus status, LocalDateTime threshold);

    @Query("SELECT t FROM Ticket t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:priority IS NULL OR t.priority = :priority) AND " +
           "(:assignedTo IS NULL OR t.assignedTo = :assignedTo) AND " +
           "(:customerId IS NULL OR t.customerId = :customerId)")
    Page<Ticket> findByConditions(@Param("status") TicketStatus status,
                                   @Param("priority") String priority,
                                   @Param("assignedTo") Long assignedTo,
                                   @Param("customerId") Long customerId,
                                   Pageable pageable);
}
