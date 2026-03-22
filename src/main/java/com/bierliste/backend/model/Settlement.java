package com.bierliste.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(
    name = "settlements",
    indexes = {
        @Index(name = "idx_settlements_group", columnList = "group_id"),
        @Index(name = "idx_settlements_target_user", columnList = "target_user_id"),
        @Index(name = "idx_settlements_actor_user", columnList = "actor_user_id"),
        @Index(name = "idx_settlements_created_at", columnList = "created_at")
    }
)
@Check(constraints = """
    type in ('MONEY', 'STRICHE')
    and (money_amount is null or money_amount > 0)
    and (striche_amount is null or striche_amount >= 1)
    and (
        (type = 'MONEY' and money_amount is not null and striche_amount is null)
        or
        (type = 'STRICHE' and striche_amount is not null and money_amount is null)
    )
    """)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Positive
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @NotNull
    @Positive
    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @NotNull
    @Positive
    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementType type;

    @DecimalMin(value = "0.01")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "money_amount", precision = 10, scale = 2)
    private BigDecimal moneyAmount;

    @Min(1)
    @Column(name = "striche_amount")
    private Integer stricheAmount;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(length = 255)
    private String note;

    public Long getId() {
        return id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public SettlementType getType() {
        return type;
    }

    public void setType(SettlementType type) {
        this.type = type;
    }

    public BigDecimal getMoneyAmount() {
        return moneyAmount;
    }

    public void setMoneyAmount(BigDecimal moneyAmount) {
        this.moneyAmount = moneyAmount;
    }

    public Integer getStricheAmount() {
        return stricheAmount;
    }

    public void setStricheAmount(Integer stricheAmount) {
        this.stricheAmount = stricheAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
