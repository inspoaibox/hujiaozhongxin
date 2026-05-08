package com.qianniuyun.common.model;

import lombok.Data;
import java.util.List;

/**
 * 分页响应模型
 * 作者：深圳市千牛云科技有限公司
 */
@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private int page;
    private int pageSize;
    private int totalPages;

    public static <T> PageResult<T> of(List<T> records, long total, int page, int pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setTotalPages((int) Math.ceil((double) total / pageSize));
        return result;
    }
}
