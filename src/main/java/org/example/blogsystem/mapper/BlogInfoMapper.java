package org.example.blogsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.blogsystem.common.pojo.dataObject.BlogInfo;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface BlogInfoMapper extends BaseMapper<BlogInfo> {

}
