package org.example.blogsystem.common.utils;

import org.example.blogsystem.common.pojo.dataObject.BlogInfo;
import org.example.blogsystem.common.pojo.dataObject.UserInfo;
import org.example.blogsystem.common.pojo.response.BlogInfoResponse;
import org.example.blogsystem.common.pojo.response.UserInfoResponse;
import org.springframework.beans.BeanUtils;

/**
 * Bean 转换工具
 * <p>
 * 负责 DO（数据库实体）与 Response 对象之间的属性复制。
 */
public class MyBeanUtils {
    /**
     * BlogInfo -> BlogInfoResponse
     */
    public static BlogInfoResponse transBlogInfo(BlogInfo blogInfo){
        if (blogInfo == null) {
            return null; // 或抛自定义异常，前端统一处理
        }
        BlogInfoResponse blogInfoResponse=new BlogInfoResponse();
        BeanUtils.copyProperties(blogInfo,blogInfoResponse);
        return blogInfoResponse;
    }

    /**
     * UserInfo -> UserInfoResponse
     */
    public static UserInfoResponse transUserInfo(UserInfo userInfo){
        if (userInfo == null) {
            return null; // 或抛自定义异常，前端统一处理
        }
        UserInfoResponse userInfoResponse=new UserInfoResponse();
        BeanUtils.copyProperties(userInfo,userInfoResponse);
        return userInfoResponse;
    }
}
