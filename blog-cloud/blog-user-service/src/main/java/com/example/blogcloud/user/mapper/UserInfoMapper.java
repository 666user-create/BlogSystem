package com.example.blogcloud.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blogcloud.common.pojo.dataObject.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface UserInfoMapper extends BaseMapper<UserInfo> {
}
