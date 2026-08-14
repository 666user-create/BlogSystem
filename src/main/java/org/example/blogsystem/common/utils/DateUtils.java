package org.example.blogsystem.common.utils;

import lombok.Data;

import java.text.SimpleDateFormat;

/**
 * 日期时间工具类
 * <p>
 * 提供日期时间的格式化方法。
 */
public class DateUtils {
    public static String formatLocalDateTime(Data date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        return sdf.format(date);
    }
}
