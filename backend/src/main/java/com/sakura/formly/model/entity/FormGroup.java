package com.sakura.formly.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 表单分组
 * @TableName form_group
 */
@TableName(value ="form_group")
@Data
public class FormGroup {
    /**
     * 分组ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 父级分组ID，根分组为空
     */
    private Long parentId;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 同级排序值，越小越靠前
     */
    private Integer sort;

    /**
     * 创建人标识
     */
    private String createdBy;

    /**
     * 更新人标识
     */
    private String updatedBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除时间
     */
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}
