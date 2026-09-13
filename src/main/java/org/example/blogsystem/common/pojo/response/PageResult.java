package org.example.blogsystem.common.pojo.response;

import lombok.Data;

import java.util.List;

/**
 * 分页响应体
 * <p>
 * 列表类接口统一返回该结构，避免"同一接口返回数组或对象两种结构"引起的前端解析问题。
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> list;

    /** 总条数 */
    private long total;

    /** 当前页码（从 1 开始） */
    private long pageNum;

    /** 每页条数 */
    private long pageSize;

    /** 总页数（无数据时为 0） */
    private long pages;

    public static <T> PageResult<T> of(List<T> list, long total, long pageNum, long pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        // 向上取整计算总页数；pageSize 非法时置 0，避免除零
        result.setPages(pageSize > 0 ? (total + pageSize - 1) / pageSize : 0);
        return result;
    }
}
