package com.example.blogcloud.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blogcloud.common.pojo.dataObject.BlogInfo;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface BlogInfoMapper extends BaseMapper<BlogInfo> {
}
