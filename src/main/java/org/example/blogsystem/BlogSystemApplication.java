package org.example.blogsystem;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.example.blogsystem.common.pojo.dataObject.UserInfo;
import org.example.blogsystem.common.utils.SecurityUtil;
import org.example.blogsystem.mapper.UserInfoMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDate;

@SpringBootApplication
public class BlogSystemApplication implements CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication.run(BlogSystemApplication.class, args);
    }

    @Resource
    private UserInfoMapper userInfoMapper;

    @Override
    public void run(String... args) {
        QueryWrapper<UserInfo> qw = new QueryWrapper<>();
        qw.lambda().eq(UserInfo::getUserName, "admin");
        if (userInfoMapper.selectCount(qw) == 0) {
            UserInfo admin = new UserInfo();
            admin.setUserName("admin");
            admin.setPassword(SecurityUtil.encrypt("admin123"));
            admin.setGithubUrl("https://github.com");
            admin.setDeleteFlag(0);
            admin.setCreateTime(LocalDate.now());
            admin.setUpdateTime(LocalDate.now());
            userInfoMapper.insert(admin);
            System.out.println("=== 默认管理员已创建: admin / admin123 ===");
        }
    }
}