package com.qianniuyun.common.utils;

import java.util.regex.Pattern;

/**
 * 电话号码工具类
 * 作者：深圳市千牛云科技有限公司
 */
public class PhoneUtils {

    private static final Pattern MOBILE_PATTERN =
            Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern LANDLINE_PATTERN =
            Pattern.compile("^(0\\d{2,3})?\\d{7,8}$");

    private PhoneUtils() {}

    public static boolean isMobile(String phone) {
        if (phone == null) return false;
        return MOBILE_PATTERN.matcher(phone.trim()).matches();
    }

    public static boolean isLandline(String phone) {
        if (phone == null) return false;
        return LANDLINE_PATTERN.matcher(phone.trim()).matches();
    }

    public static boolean isValid(String phone) {
        return isMobile(phone) || isLandline(phone);
    }

    /**
     * 脱敏处理：138****8888
     */
    public static String mask(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        int len = phone.length();
        return phone.substring(0, 3) + "****" + phone.substring(len - 4);
    }
}
