package com.qianniuyun.customer.dto;

import lombok.Data;

@Data
public class BlacklistRequest {
    private String phone;
    private String reason;
}
