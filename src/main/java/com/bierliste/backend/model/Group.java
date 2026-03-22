package com.bierliste.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(
    name = "groups",
    indexes = {
        @Index(name = "idx_groups_created_by", columnList = "created_by_user_id")
    }
)
@Check(constraints = "price_per_strich >= 0")
public class Group {

    public static final BigDecimal DEFAULT_PRICE_PER_STRICH = new BigDecimal("1.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 8, fraction = 2)
    @ColumnDefault("1.00")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerStrich = DEFAULT_PRICE_PER_STRICH;

    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean onlyWartsCanBookForOthers = true;

    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean allowArbitraryMoneySettlements = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
    private User createdByUser;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GroupMember> members = new HashSet<>();

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Instant getCreatedAt() { return createdAt; }
    public BigDecimal getPricePerStrich() { return pricePerStrich; }
    public void setPricePerStrich(BigDecimal pricePerStrich) { this.pricePerStrich = pricePerStrich; }
    public boolean isOnlyWartsCanBookForOthers() { return onlyWartsCanBookForOthers; }
    public void setOnlyWartsCanBookForOthers(boolean onlyWartsCanBookForOthers) { this.onlyWartsCanBookForOthers = onlyWartsCanBookForOthers; }
    public boolean isAllowArbitraryMoneySettlements() { return allowArbitraryMoneySettlements; }
    public void setAllowArbitraryMoneySettlements(boolean allowArbitraryMoneySettlements) { this.allowArbitraryMoneySettlements = allowArbitraryMoneySettlements; }
    public Long getCreatedByUserId() { return createdByUser != null ? createdByUser.getId() : null; }
    public User getCreatedByUser() { return createdByUser; }
    public void setCreatedByUser(User createdByUser) { this.createdByUser = createdByUser; }
    public Set<GroupMember> getMembers() { return members; }
}
