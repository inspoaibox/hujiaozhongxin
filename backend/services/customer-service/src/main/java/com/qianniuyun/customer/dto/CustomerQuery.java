package com.qianniuyun.customer.dto;

import lombok.Data;

@Data
public class CustomerQuery {
    private String keyword;
    private String vipLevel;
    private int page = 1;
    private int pageSize = 20;
}
