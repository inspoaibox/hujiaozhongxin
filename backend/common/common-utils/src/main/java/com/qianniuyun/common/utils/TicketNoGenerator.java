package com.qianniuyun.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 工单编号生成器
 * 格式：TK + yyyyMMddHHmmss + 4位序号
 * 作者：深圳市千牛云科技有限公司
 */
public class TicketNoGenerator {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    private TicketNoGenerator() {}

    public static String generate() {
        int seq = SEQUENCE.incrementAndGet() % 10000;
        return "TK" + LocalDateTime.now().format(FORMATTER) + String.format("%04d", seq);
    }
}
