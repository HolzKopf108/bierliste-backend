package com.bierliste.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import org.hibernate.annotations.Check;

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
@Check(constraints = "strich_count >= 0")
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
    @Column(nullable = false, length = 30)
    private GroupRole role = GroupRole.MEMBER;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    @PositiveOrZero
    @Column(name = "strich_count", nullable = false, columnDefinition = "integer not null default 0")
    private int strichCount = 0;

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
}
