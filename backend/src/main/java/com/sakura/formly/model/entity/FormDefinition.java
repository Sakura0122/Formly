package com.sakura.formly.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 表单定义
 * @TableName form_definition
 */
@TableName(value ="form_definition")
@Data
public class FormDefinition {
    /**
     * 表单ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属分组ID，允许为空表示根级表单
     */
    private Long groupId;

    /**
     * 表单名称
     */
    private String name;

    /**
     * 表单唯一业务标识
     */
    private String formKey;

    /**
     * 表单描述
     */
    private String description;

    /**
     * 当前草稿Schema
     */
    private String draftSchemaJson;

    /**
     * 当前已发布版本ID
     */
    private Long publishedVersionId;

    /**
     * 同级排序值，越小越靠前
     */
    private Integer sort;

    /**
     * 创建人标识
     */
    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    /**
     * 更新人标识
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
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
