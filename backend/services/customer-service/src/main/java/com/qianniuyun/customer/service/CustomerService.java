package com.qianniuyun.customer.service;

import com.qianniuyun.common.exception.BusinessException;
import com.qianniuyun.common.model.PageResult;
import com.qianniuyun.customer.dto.CustomerDTO;
import com.qianniuyun.customer.dto.CustomerQuery;
import com.qianniuyun.customer.entity.Customer;
import com.qianniuyun.customer.entity.Blacklist;
import com.qianniuyun.customer.repository.BlacklistRepository;
import com.qianniuyun.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 客户管理服务
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final BlacklistRepository blacklistRepository;

    /**
     * 根据电话号码查询客户（来电弹屏使用）
     */
    @Cacheable(value = "customer:phone", key = "#phone")
    public Optional<Customer> findByPhone(String phone) {
        return customerRepository.findByPhone(phone);
    }

    /**
     * 创建客户
     */
    @Transactional
    public Customer createCustomer(CustomerDTO dto) {
        if (customerRepository.existsByPhone(dto.getPhone())) {
            throw new BusinessException("该电话号码已存在");
        }

        Customer customer = new Customer();
        customer.setPhone(dto.getPhone());
        customer.setName(dto.getName());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddress());
        customer.setVipLevel(dto.getVipLevel() != null ? dto.getVipLevel() : "NORMAL");
        customer.setNotes(dto.getNotes());
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());

        return customerRepository.save(customer);
    }

    /**
     * 更新客户信息
     */
    @Transactional
    @CacheEvict(value = "customer:phone", key = "#dto.phone")
    public Customer updateCustomer(Long id, CustomerDTO dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("客户不存在"));

        if (dto.getName() != null) customer.setName(dto.getName());
        if (dto.getEmail() != null) customer.setEmail(dto.getEmail());
        if (dto.getAddress() != null) customer.setAddress(dto.getAddress());
        if (dto.getVipLevel() != null) customer.setVipLevel(dto.getVipLevel());
        if (dto.getNotes() != null) customer.setNotes(dto.getNotes());
        customer.setUpdatedAt(LocalDateTime.now());

        return customerRepository.save(customer);
    }

    /**
     * 分页查询客户
     */
    public PageResult<Customer> queryCustomers(CustomerQuery query) {
        Page<Customer> page = customerRepository.findByConditions(
                query.getKeyword(),
                query.getVipLevel(),
                PageRequest.of(query.getPage() - 1, query.getPageSize())
        );
        return PageResult.of(page.getContent(), page.getTotalElements(),
                query.getPage(), query.getPageSize());
    }

    /**
     * 添加黑名单
     */
    @Transactional
    public void addToBlacklist(String phone, String reason, Long operatorId) {
        if (blacklistRepository.existsByPhoneAndRemovedAtIsNull(phone)) {
            throw new BusinessException("该号码已在黑名单中");
        }

        Blacklist blacklist = new Blacklist();
        blacklist.setPhone(phone);
        blacklist.setReason(reason);
        blacklist.setCreatedBy(operatorId);
        blacklist.setCreatedAt(LocalDateTime.now());
        blacklistRepository.save(blacklist);

        log.info("号码 {} 已加入黑名单, 操作人={}", phone, operatorId);
    }

    /**
     * 移除黑名单
     */
    @Transactional
    public void removeFromBlacklist(String phone, Long operatorId) {
        Blacklist blacklist = blacklistRepository.findByPhoneAndRemovedAtIsNull(phone)
                .orElseThrow(() -> new BusinessException("该号码不在黑名单中"));

        blacklist.setRemovedBy(operatorId);
        blacklist.setRemovedAt(LocalDateTime.now());
        blacklistRepository.save(blacklist);

        log.info("号码 {} 已从黑名单移除, 操作人={}", phone, operatorId);
    }

    /**
     * 检查是否在黑名单
     */
    @Cacheable(value = "blacklist", key = "#phone")
    public boolean isBlacklisted(String phone) {
        return blacklistRepository.existsByPhoneAndRemovedAtIsNull(phone);
    }
}
