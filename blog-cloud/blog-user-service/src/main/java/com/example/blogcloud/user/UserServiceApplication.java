package com.example.blogcloud.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.blogcloud.common.pojo.dataObject.UserInfo;
import com.example.blogcloud.common.utils.SecurityUtil;
import com.example.blogcloud.user.mapper.UserInfoMapper;
import jakarta.annotation.Resource;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import java.time.LocalDate;

/**
 * 用户服务启动类
 * <p>
 * scanBasePackages 扫描整个 com.example.blogcloud, 让 blog-common 的 JwtConfig、
 * blog-web-common 的全局响应包装/异常处理随服务生效。
 * 注意: @MapperScan 只扫 mapper 子包 —— 若扫整个 com.example.blogcloud,
 * UserService 等业务接口也会被注册成 Mapper 代理, 导致注入错 bean。
 */
@SpringBootApplication(scanBasePackages = "com.example.blogcloud")
@MapperScan("com.example.blogcloud.user.mapper")
@EnableFeignClients(basePackages = "com.example.blogcloud.user.client")
public class UserServiceApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @Resource
    private UserInfoMapper userInfoMapper;

    /** 首次启动自动创建默认管理员(与单体工程行为一致) */
    @Override
    public void run(String... args) {
        QueryWrapper<UserInfo> qw = new QueryWrapper<>();
        qw.lambda().eq(UserInfo::getUserName, "admin");
        if (userInfoMapper.selectCount(qw) == 0) {
            UserInfo admin = new UserInfo();
            admin.setUserName("admin");
            admin.setPassword(SecurityUtil.encrypt("admin123"));
            admin.setGithubUrl("https://github.com");
            admin.setBlogCount(0);
            admin.setDeleteFlag(0);
            admin.setCreateTime(LocalDate.now());
            admin.setUpdateTime(LocalDate.now());
            userInfoMapper.insert(admin);
            System.out.println("=== 默认管理员已创建: admin / admin123 ===");
        }
    }
}
