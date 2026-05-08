package com.qianniuyun.customer.repository;

import com.qianniuyun.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhone(String phone);
    boolean existsByPhone(String phone);

    @Query("SELECT c FROM Customer c WHERE " +
           "(:keyword IS NULL OR c.name LIKE %:keyword% OR c.phone LIKE %:keyword%) AND " +
           "(:vipLevel IS NULL OR c.vipLevel = :vipLevel)")
    Page<Customer> findByConditions(@Param("keyword") String keyword,
                                    @Param("vipLevel") String vipLevel,
                                    Pageable pageable);
}
