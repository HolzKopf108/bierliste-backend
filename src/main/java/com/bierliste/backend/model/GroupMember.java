package com.bierliste.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(
    name = "group_members",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_group_members_user_group", columnNames = {"user_id", "group_id"})
    },
    indexes = {
        @Index(name = "idx_group_members_group", columnList = "group_id"),
        @Index(name = "idx_group_members_user", columnList = "user_id")
    }
)
@Check(constraints = """
    strich_count >= 0
    and role in ('MEMBER', 'ADMIN')
    and (
        (active = true and left_at is null)
        or
        (active = false and left_at is not null)
    )
    """)
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'MEMBER'")
    @Column(nullable = false, length = 30)
    private GroupRole role = GroupRole.MEMBER;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    @PositiveOrZero
    @ColumnDefault("0")
    @Column(name = "strich_count", nullable = false)
    private int strichCount = 0;

    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "left_at")
    private Instant leftAt;

    public Long getId() { return id; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public GroupRole getRole() { return role; }
    public void setRole(GroupRole role) { this.role = role; }
    public Instant getJoinedAt() { return joinedAt; }
    public int getStrichCount() { return strichCount; }
    public void setStrichCount(int strichCount) { this.strichCount = strichCount; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getLeftAt() { return leftAt; }
    public void setLeftAt(Instant leftAt) { this.leftAt = leftAt; }
}
