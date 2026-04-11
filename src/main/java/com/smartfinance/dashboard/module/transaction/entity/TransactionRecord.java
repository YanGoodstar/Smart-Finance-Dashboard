package com.smartfinance.dashboard.module.transaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartfinance.dashboard.module.transaction.enums.CategorySource;
import com.smartfinance.dashboard.module.transaction.enums.TransactionDirection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("transaction_record")
public class TransactionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("import_job_id")
    private Long importJobId;

    @TableField("transaction_date")
    private LocalDate transactionDate;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("direction")
    private TransactionDirection direction;

    @TableField("merchant_name")
    private String merchantName;

    @TableField("summary")
    private String summary;

    @TableField("raw_text")
    private String rawText;

    @TableField("auto_category")
    private String autoCategory;

    @TableField("final_category")
    private String finalCategory;

    @TableField("category_source")
    private CategorySource categorySource;

    @TableField("suspected_duplicate")
    private Boolean suspectedDuplicate;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getImportJobId() {
        return importJobId;
    }

    public void setImportJobId(Long importJobId) {
        this.importJobId = importJobId;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionDirection getDirection() {
        return direction;
    }

    public void setDirection(TransactionDirection direction) {
        this.direction = direction;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getAutoCategory() {
        return autoCategory;
    }

    public void setAutoCategory(String autoCategory) {
        this.autoCategory = autoCategory;
    }

    public String getFinalCategory() {
        return finalCategory;
    }

    public void setFinalCategory(String finalCategory) {
        this.finalCategory = finalCategory;
    }

    public CategorySource getCategorySource() {
        return categorySource;
    }

    public void setCategorySource(CategorySource categorySource) {
        this.categorySource = categorySource;
    }

    public Boolean getSuspectedDuplicate() {
        return suspectedDuplicate;
    }

    public void setSuspectedDuplicate(Boolean suspectedDuplicate) {
        this.suspectedDuplicate = suspectedDuplicate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
