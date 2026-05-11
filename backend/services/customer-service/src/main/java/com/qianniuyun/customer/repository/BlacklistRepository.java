package com.qianniuyun.customer.repository;

import com.qianniuyun.customer.entity.Blacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndRemovedAtIsNull(String phone);
    Optional<Blacklist> findByPhone(String phone);
    Optional<Blacklist> findByPhoneAndRemovedAtIsNull(String phone);
}
