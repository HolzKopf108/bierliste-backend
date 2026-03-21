package com.bierliste.backend.service;

import com.bierliste.backend.dto.GroupMemberDto;
import com.bierliste.backend.dto.MoneySettlementCreateDto;
import com.bierliste.backend.dto.StricheSettlementCreateDto;
import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.Settlement;
import com.bierliste.backend.model.SettlementType;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.GroupMemberRepository;
import com.bierliste.backend.repository.GroupRepository;
import com.bierliste.backend.repository.SettlementRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SettlementService {

    private static final String GROUP_NOT_FOUND_MESSAGE = "Gruppe nicht gefunden";
    private static final String TARGET_NOT_FOUND_MESSAGE = "Gruppenmitglied nicht gefunden";
    private static final String INVALID_PRICE_PER_STRICH_MESSAGE = "pricePerStrich muss groesser als 0 sein";
    private static final String INVALID_MONEY_MULTIPLE_MESSAGE = "Betrag muss ein exaktes positives Vielfaches von pricePerStrich sein";

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final SettlementRepository settlementRepository;
    private final GroupAuthorizationService groupAuthorizationService;

    public SettlementService(
        GroupRepository groupRepository,
        GroupMemberRepository groupMemberRepository,
        SettlementRepository settlementRepository,
        GroupAuthorizationService groupAuthorizationService
    ) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.settlementRepository = settlementRepository;
        this.groupAuthorizationService = groupAuthorizationService;
    }

    @Transactional
    public GroupMemberDto createMoneySettlement(Long groupId, Long targetUserId, MoneySettlementCreateDto dto, User actor) {
        Group group = requireWartGroup(groupId, actor);
        BigDecimal pricePerStrich = requirePositivePricePerStrich(group);
        GroupMember targetMembership = requireTargetMembership(groupId, targetUserId);

        MoneySettlementCalculation calculation = calculateMoneySettlement(
            targetMembership.getStrichCount(),
            pricePerStrich,
            dto.getAmount(),
            group.isAllowArbitraryMoneySettlements()
        );

        if (calculation.appliedStrichCount() > 0) {
            Settlement settlement = new Settlement();
            settlement.setGroupId(groupId);
            settlement.setTargetUserId(targetUserId);
            settlement.setActorUserId(actor.getId());
            settlement.setType(SettlementType.MONEY);
            settlement.setMoneyAmount(calculation.appliedAmount());
            settlementRepository.save(settlement);
        }

        targetMembership.setStrichCount(calculation.newStrichCount());

        return toGroupMemberDto(targetMembership);
    }

    @Transactional
    public GroupMemberDto createStricheSettlement(Long groupId, Long targetUserId, StricheSettlementCreateDto dto, User actor) {
        requireWartGroup(groupId, actor);
        GroupMember targetMembership = requireTargetMembership(groupId, targetUserId);

        Settlement settlement = new Settlement();
        settlement.setGroupId(groupId);
        settlement.setTargetUserId(targetUserId);
        settlement.setActorUserId(actor.getId());
        settlement.setType(SettlementType.STRICHE);
        settlement.setStricheAmount(dto.getAmount());
        settlementRepository.save(settlement);

        int newStrichCount = Math.max(0, targetMembership.getStrichCount() - dto.getAmount());
        targetMembership.setStrichCount(newStrichCount);

        return toGroupMemberDto(targetMembership);
    }

    private Group requireWartGroup(Long groupId, User actor) {
        groupAuthorizationService.requireWart(groupId, actor);
        return groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, GROUP_NOT_FOUND_MESSAGE));
    }

    private BigDecimal requirePositivePricePerStrich(Group group) {
        BigDecimal pricePerStrich = group.getPricePerStrich();
        if (pricePerStrich == null || pricePerStrich.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_PRICE_PER_STRICH_MESSAGE);
        }
        return pricePerStrich;
    }

    private GroupMember requireTargetMembership(Long groupId, Long targetUserId) {
        return groupMemberRepository.findByGroup_IdAndUser_Id(groupId, targetUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, TARGET_NOT_FOUND_MESSAGE));
    }

    private MoneySettlementCalculation calculateMoneySettlement(
        int currentStrichCount,
        BigDecimal pricePerStrich,
        BigDecimal amount,
        boolean allowArbitraryMoneySettlements
    ) {
        if (!allowArbitraryMoneySettlements && amount.remainder(pricePerStrich).compareTo(BigDecimal.ZERO) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_MONEY_MULTIPLE_MESSAGE);
        }

        if (currentStrichCount <= 0) {
            return new MoneySettlementCalculation(0, 0, BigDecimal.ZERO);
        }

        int requestedStrichCount = amount.divideToIntegralValue(pricePerStrich).intValueExact();
        int appliedStrichCount = Math.min(currentStrichCount, requestedStrichCount);
        int newStrichCount = currentStrichCount - appliedStrichCount;
        BigDecimal appliedAmount = pricePerStrich.multiply(BigDecimal.valueOf(appliedStrichCount));

        return new MoneySettlementCalculation(newStrichCount, appliedStrichCount, appliedAmount);
    }

    private GroupMemberDto toGroupMemberDto(GroupMember membership) {
        return new GroupMemberDto(
            membership.getUser().getId(),
            membership.getUser().getUsername(),
            membership.getJoinedAt(),
            membership.getRole(),
            membership.getStrichCount()
        );
    }

    private record MoneySettlementCalculation(int newStrichCount, int appliedStrichCount, BigDecimal appliedAmount) {
    }
}
