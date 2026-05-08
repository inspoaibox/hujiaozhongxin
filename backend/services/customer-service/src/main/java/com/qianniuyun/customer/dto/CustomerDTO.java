package com.qianniuyun.customer.dto;

import lombok.Data;

@Data
public class CustomerDTO {
    private String phone;
    private String name;
    private String email;
    private String address;
    private String vipLevel;
    private String notes;
}
