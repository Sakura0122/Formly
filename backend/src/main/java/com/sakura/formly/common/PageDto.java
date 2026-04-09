package com.sakura.formly.common;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageDto {
    @Schema(description = "当前页数")
    @Min(value = 1, message = "当前页数不能小于1")
    private int currentPage = 1;

    @Schema(description = "每页显示条目个数")
    @Min(value = 1, message = "每页显示条目个数不能小于1")
    private int pageSize = 10;

    @Schema(description = "排序字段")
    private String sortField;

    @Schema(description = "是否升序")
    private Boolean isAsc = true;

    public <T> Page<T> toMpPage(OrderItem... orders) {
        // 1.分页条件
        Page<T> p = Page.of(currentPage, pageSize);
        // 2.排序条件
        // 2.1.先看前端有没有传排序字段
        if (sortField != null) {
            p.addOrder(new OrderItem().setColumn(sortField).setAsc(isAsc));
            return p;
        }
        // 2.2.再看有没有手动指定排序字段
        if (orders != null) {
            p.addOrder(orders);
        }
        return p;
    }

    public <T> Page<T> toMpPage(String defaultSortBy, boolean isAsc){
        return this.toMpPage(new OrderItem().setColumn(defaultSortBy).setAsc(isAsc));
    }

    public <T> Page<T> toMpPageDefaultSortByCreateTimeDesc() {
        return toMpPage("create_time", true);
    }

    public <T> Page<T> toMpPageDefaultSortByUpdateTimeDesc() {
        return toMpPage("update_time", false);
    }
}