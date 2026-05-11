package com.qianniuyun.customer.controller;

import com.qianniuyun.common.model.PageResult;
import com.qianniuyun.common.model.Result;
import com.qianniuyun.customer.dto.BlacklistRequest;
import com.qianniuyun.customer.dto.CustomerDTO;
import com.qianniuyun.customer.dto.CustomerQuery;
import com.qianniuyun.customer.entity.Customer;
import com.qianniuyun.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public Result<PageResult<Customer>> queryCustomers(CustomerQuery query) {
        return Result.success(customerService.queryCustomers(query));
    }

    @GetMapping("/by-phone/{phone}")
    public Result<Customer> findByPhone(@PathVariable String phone) {
        return Result.success(customerService.findByPhone(phone).orElse(null));
    }

    @PostMapping
    public Result<Customer> createCustomer(@RequestBody CustomerDTO request) {
        return Result.success(customerService.createCustomer(request));
    }

    @PutMapping("/{customerId}")
    public Result<Customer> updateCustomer(@PathVariable Long customerId,
                                           @RequestBody CustomerDTO request) {
        return Result.success(customerService.updateCustomer(customerId, request));
    }

    @PostMapping("/blacklist")
    public Result<Void> addToBlacklist(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                       @RequestBody BlacklistRequest request) {
        customerService.addToBlacklist(request.getPhone(), request.getReason(), userId != null ? userId : 0L);
        return Result.success();
    }

    @DeleteMapping("/blacklist/{phone}")
    public Result<Void> removeFromBlacklist(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                            @PathVariable String phone) {
        customerService.removeFromBlacklist(phone, userId != null ? userId : 0L);
        return Result.success();
    }

    @GetMapping("/blacklist/{phone}")
    public Result<Map<String, Object>> isBlacklisted(@PathVariable String phone) {
        return Result.success(Map.of("phone", phone, "blacklisted", customerService.isBlacklisted(phone)));
    }
}
