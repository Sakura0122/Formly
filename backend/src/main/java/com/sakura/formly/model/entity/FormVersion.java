package com.sakura.formly.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 表单历史版本
 * @TableName form_version
 */
@TableName(value ="form_version")
@Data
public class FormVersion {
    /**
     * 版本ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属表单ID
     */
    private Long formId;

    /**
     * 递增版本号
     */
    private Integer versionNo;

    /**
     * 版本标签，如V1
     */
    private String versionLabel;

    /**
     * 整份编辑器Schema
     */
    private Object schemaJson;

    /**
     * 是否曾被发布
     */
    private Integer isPublished;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 创建人标识
     */
    private String createdBy;

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
