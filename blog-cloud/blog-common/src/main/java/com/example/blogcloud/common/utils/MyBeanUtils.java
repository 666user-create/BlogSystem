package com.example.blogcloud.common.utils;

import com.example.blogcloud.common.pojo.dataObject.BlogInfo;
import com.example.blogcloud.common.pojo.dataObject.UserInfo;
import com.example.blogcloud.common.pojo.response.BlogInfoResponse;
import com.example.blogcloud.common.pojo.response.UserInfoResponse;
import org.springframework.beans.BeanUtils;

/**
 * Bean 转换工具: DO(数据库实体) <-> Response(对外响应)
 */
public class MyBeanUtils {
    public static BlogInfoResponse transBlogInfo(BlogInfo blogInfo) {
        if (blogInfo == null) {
            return null;
        }
        BlogInfoResponse response = new BlogInfoResponse();
        BeanUtils.copyProperties(blogInfo, response);
        return response;
    }

    public static UserInfoResponse transUserInfo(UserInfo userInfo) {
        if (userInfo == null) {
            return null;
        }
        UserInfoResponse response = new UserInfoResponse();
        BeanUtils.copyProperties(userInfo, response);
        return response;
    }
}
