package org.example.blogsystem.common.limiter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录尝试频率限制（BUG-08）
 * <p>
 * 目标：限制"同一用户名在时间窗口内的连续失败次数"，防暴力破解。
 * <ul>
 *   <li>只统计**失败**次数；成功登录会清零，避免正常用户被误伤</li>
 *   <li>固定窗口 60 秒、窗口内最多 5 次失败</li>
 *   <li>单机内存实现（ConcurrentHashMap）；生产环境应替换为 Redis + 滑动窗口，
 *       否则多实例部署时各实例计数独立、重启后计数清零</li>
 * </ul>
 */
@Slf4j
@Component
public class LoginAttemptLimiter {

    /** 时间窗口：60 秒 */
    private static final long WINDOW_MILLIS = 60_000L;

    /** 窗口内允许的最大失败次数 */
    private static final int MAX_FAILURES = 5;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * 返回该用户名还需等待多少秒才能再次尝试登录；返回 0 表示未被限制。
     */
    public long blockedSeconds(String userName) {
        String key = normalize(userName);
        Window window = windows.get(key);
        if (window == null) {
            return 0;
        }
        synchronized (window) {
            long elapsed = System.currentTimeMillis() - window.start;
            if (elapsed >= WINDOW_MILLIS) {
                windows.remove(key);
                return 0;
            }
            if (window.failures < MAX_FAILURES) {
                return 0;
            }
            return (WINDOW_MILLIS - elapsed + 999) / 1000;
        }
    }

    /** 记录一次登录失败 */
    public void recordFailure(String userName) {
        long now = System.currentTimeMillis();
        windows.compute(normalize(userName), (key, window) -> {
            if (window == null || now - window.start >= WINDOW_MILLIS) {
                Window fresh = new Window(now);
                fresh.failures = 1;
                return fresh;
            }
            synchronized (window) {
                window.failures++;
            }
            return window;
        });
    }

    /** 登录成功：清空该用户名的失败计数 */
    public void reset(String userName) {
        windows.remove(normalize(userName));
    }

    private String normalize(String userName) {
        return userName == null ? "" : userName.trim().toLowerCase();
    }

    private static class Window {
        private long start;
        private int failures;

        private Window(long start) {
            this.start = start;
        }
    }
}
