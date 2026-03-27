package com.bierliste.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.Settlement;
import com.bierliste.backend.model.SettlementType;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.GroupMemberRepository;
import com.bierliste.backend.repository.GroupRepository;
import com.bierliste.backend.repository.SettlementRepository;
import com.bierliste.backend.repository.UserRepository;
import com.bierliste.backend.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SettlementControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void createMoneySettlementReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(post("/api/v1/groups/1/members/2/settlements/money")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 1.00))))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void createMoneySettlementReturnsBadRequestWhenArbitraryAmountsAreDisabledAndAmountIsNotMultipleOfPricePerStrich() throws Exception {
        User admin = createUser("money-invalid-multiple-admin@example.com", "MoneyAdmin");
        User target = createUser("money-invalid-multiple-target@example.com", "MoneyTarget");
        Group group = createGroup("Money Invalid Multiple", admin);
        group.setPricePerStrich(new BigDecimal("1.50"));
        groupRepository.save(group);

        createMembership(group, admin, GroupRole.ADMIN);
        GroupMember targetMembership = createMembership(group, target, GroupRole.MEMBER);
        targetMembership.setStrichCount(3);
        groupMemberRepository.save(targetMembership);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/settlements/money")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 1.00))))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Betrag muss ein exaktes positives Vielfaches von pricePerStrich sein"));

        GroupMember unchangedMembership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), target.getId()).orElseThrow();
        assertThat(unchangedMembership.getStrichCount()).isEqualTo(3);
        assertThat(settlementRepository.count()).isZero();
    }

    @Test
    void createMoneySettlementAllowsExactMultipleWhenArbitraryAmountsAreDisabled() throws Exception {
        User admin = createUser("money-exact-admin@example.com", "MoneyAdmin");
        User target = createUser("money-exact-target@example.com", "MoneyTarget");
        Group group = createGroup("Money Exact Gruppe", admin);
        group.setPricePerStrich(new BigDecimal("1.50"));
        groupRepository.save(group);

        createMembership(group, admin, GroupRole.ADMIN);
        GroupMember targetMembership = createMembership(group, target, GroupRole.MEMBER);
        targetMembership.setStrichCount(5);
        groupMemberRepository.save(targetMembership);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/settlements/money")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 3.00))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.userId").value(target.getId()))
            .andExpect(jsonPath("$.username").value("MoneyTarget"))
            .andExpect(jsonPath("$.role").value("MEMBER"))
            .andExpect(jsonPath("$.joinedAt").isNotEmpty())
            .andExpect(jsonPath("$.strichCount").value(3));

        GroupMember updatedMembership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), target.getId()).orElseThrow();
        assertThat(updatedMembership.getStrichCount()).isEqualTo(3);

        assertThat(settlementRepository.count()).isEqualTo(1);
        Settlement settlement = settlementRepository.findAll().getFirst();
        assertThat(settlement.getGroupId()).isEqualTo(group.getId());
        assertThat(settlement.getTargetUserId()).isEqualTo(target.getId());
        assertThat(settlement.getActorUserId()).isEqualTo(admin.getId());
        assertThat(settlement.getType()).isEqualTo(SettlementType.MONEY);
        assertThat(settlement.getMoneyAmount()).isEqualByComparingTo("3.00");
        assertThat(settlement.getStricheAmount()).isNull();
    }

    @Test
    void createMoneySettlementAllowsPartialAmountWhenArbitraryAmountsAreEnabledAndPersistsFullPayment() throws Exception {
        User admin = createUser("money-arbitrary-admin@example.com", "MoneyAdmin");
        User target = createUser("money-arbitrary-target@example.com", "MoneyTarget");
        Group group = createGroup("Money Arbitrary Gruppe", admin);
        group.setPricePerStrich(new BigDecimal("1.50"));
        group.setAllowArbitraryMoneySettlements(true);
        groupRepository.save(group);

        createMembership(group, admin, GroupRole.ADMIN);
        GroupMember targetMembership = createMembership(group, target, GroupRole.MEMBER);
        targetMembership.setStrichCount(1);
        groupMemberRepository.save(targetMembership);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/settlements/money")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 1.00))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.strichCount").value(1));

        GroupMember updatedMembership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), target.getId()).orElseThrow();
        assertThat(updatedMembership.getStrichCount()).isEqualTo(1);
        assertThat(settlementRepository.count()).isEqualTo(1);

        Settlement settlement = settlementRepository.findAll().getFirst();
        assertThat(settlement.getType()).isEqualTo(SettlementType.MONEY);
        assertThat(settlement.getMoneyAmount()).isEqualByComparingTo("1.00");
    }

    @Test
    void createMoneySettlementRoundsDownToFullStricheAndKeepsRemainingAmountAsLost() throws Exception {
        User admin = createUser("money-floor-admin@example.com", "MoneyAdmin");
        User target = createUser("money-floor-target@example.com", "MoneyTarget");
        Group group = createGroup("Money Floor Gruppe", admin);
        group.setPricePerStrich(new BigDecimal("1.50"));
        group.setAllowArbitraryMoneySettlements(true);
        groupRepository.save(group);

        createMembership(group, admin, GroupRole.ADMIN);
        GroupMember targetMembership = createMembership(group, target, GroupRole.MEMBER);
        targetMembership.setStrichCount(5);
        groupMemberRepository.save(targetMembership);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/settlements/money")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 2.99))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.userId").value(target.getId()))
            .andExpect(jsonPath("$.username").value("MoneyTarget"))
            .andExpect(jsonPath("$.role").value("MEMBER"))
            .andExpect(jsonPath("$.joinedAt").isNotEmpty())
            .andExpect(jsonPath("$.strichCount").value(4));

        GroupMember updatedMembership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), target.getId()).orElseThrow();
        assertThat(updatedMembership.getStrichCount()).isEqualTo(4);

        assertThat(settlementRepository.count()).isEqualTo(1);
        Settlement settlement = settlementRepository.findAll().getFirst();
        assertThat(settlement.getType()).isEqualTo(SettlementType.MONEY);
        assertThat(settlement.getMoneyAmount()).isEqualByComparingTo("2.99");
    }

    @Test
    void createMoneySettlementAllowsCreditWhenPaymentExceedsDebt() throws Exception {
        User admin = createUser("money-overpay-admin@example.com", "MoneyOverpayAdmin");
        User target = createUser("money-overpay-target@example.com", "MoneyOverpayTarget");
        Group group = createGroup("Money Overpay Gruppe", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        GroupMember targetMembership = createMembership(group, target, GroupRole.MEMBER);
        targetMembership.setStrichCount(2);
        groupMemberRepository.save(targetMembership);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/settlements/money")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 10.00))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.strichCount").value(-8));

        GroupMember updatedMembership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), target.getId()).orElseThrow();
        assertThat(updatedMembership.getStrichCount()).isEqualTo(-8);
    }

    @Test
    void createMoneySettlementCreatesNegativeCreditFromZeroWhenAmountExceedsFullStricheThreshold() throws Exception {
        User admin = createUser("money-credit-admin@example.com", "MoneyCreditAdmin");
        User target = createUser("money-credit-target@example.com", "MoneyCreditTarget");
        Group group = createGroup("Money Credit Gruppe", admin);
        group.setPricePerStrich(new BigDecimal("1.50"));
        group.setAllowArbitraryMoneySettlements(true);
        groupRepository.save(group);

        createMembership(group, admin, GroupRole.ADMIN);
        GroupMember targetMembership = createMembership(group, target, GroupRole.MEMBER);
        targetMembership.setStrichCount(0);
        groupMemberRepository.save(targetMembership);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/settlements/money")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 4.00))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.strichCount").value(-2));

        GroupMember updatedMembership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), target.getId()).orElseThrow();
        assertThat(updatedMembership.getStrichCount()).isEqualTo(-2);

        Settlement settlement = settlementRepository.findAll().getFirst();
        assertThat(settlement.getMoneyAmount()).isEqualByComparingTo("4.00");
    }

    @Test
    void createMoneySettlementReturnsForbiddenWhenCallerIsNotAdmin() throws Exception {
        User admin = createUser("money-rights-admin@example.com", "MoneyAdmin");
        User caller = createUser("money-rights-member@example.com", "MoneyMember");
        User target = createUser("money-rights-target@example.com", "MoneyTarget");
        Group group = createGroup("Money Rechte", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, caller, GroupRole.MEMBER);
        GroupMember targetMembership = createMembership(group, target, GroupRole.MEMBER);
        targetMembership.setStrichCount(4);
        groupMemberRepository.save(targetMembership);

        String token = jwtTokenProvider.createAccessToken(caller);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/settlements/money")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 1.00))))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Wart-Rechte erforderlich"));

        GroupMember unchangedMembership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), target.getId()).orElseThrow();
        assertThat(unchangedMembership.getStrichCount()).isEqualTo(4);
        assertThat(settlementRepository.count()).isZero();
    }

    @Test
    void createMoneySettlementReturnsBadRequestWhenAmountIsZero() throws Exception {
        User admin = createUser("money-invalid-admin@example.com", "MoneyAdmin");
        User target = createUser("money-invalid-target@example.com", "MoneyTarget");
        Group group = createGroup("Money Invalid", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, target, GroupRole.MEMBER);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/settlements/money")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 0.00))))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.amount").exists());

        assertThat(settlementRepository.count()).isZero();
    }

    @Test
    void createMoneySettlementReturnsBadRequestWhenPricePerStrichIsZero() throws Exception {
        User admin = createUser("money-zero-price-admin@example.com", "MoneyAdmin");
        User target = createUser("money-zero-price-target@example.com", "MoneyTarget");
        Group group = createGroup("Money Zero Price", admin);
        group.setPricePerStrich(BigDecimal.ZERO);
        groupRepository.save(group);

        createMembership(group, admin, GroupRole.ADMIN);
        GroupMember targetMembership = createMembership(group, target, GroupRole.MEMBER);
        targetMembership.setStrichCount(4);
        groupMemberRepository.save(targetMembership);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/settlements/money")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 1.00))))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("pricePerStrich muss groesser als 0 sein"));

        GroupMember unchangedMembership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), target.getId()).orElseThrow();
        assertThat(unchangedMembership.getStrichCount()).isEqualTo(4);
        assertThat(settlementRepository.count()).isZero();
    }

    @Test
    void createMoneySettlementReturnsNotFoundWhenTargetIsNotMember() throws Exception {
        User admin = createUser("money-target-admin@example.com", "MoneyAdmin");
        User outsider = createUser("money-target-outsider@example.com", "MoneyOutsider");
        Group group = createGroup("Money Missing Member", admin);

        createMembership(group, admin, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + outsider.getId() + "/settlements/money")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 1.00))))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Gruppenmitglied nicht gefunden"));

        assertThat(settlementRepository.count()).isZero();
    }

    @Test
    void createMoneySettlementReturnsNotFoundWhenGroupDoesNotExist() throws Exception {
        User user = createUser("money-missing-group@example.com", "MoneyMissingGroup");
        String token = jwtTokenProvider.createAccessToken(user);

        mockMvc.perform(post("/api/v1/groups/999999/members/123/settlements/money")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 1.00))))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Gruppe nicht gefunden"));
    }

    @Test
    void createStricheSettlementReducesStricheAndPersistsSettlement() throws Exception {
        User admin = createUser("striche-admin@example.com", "StricheAdmin");
        User target = createUser("striche-target@example.com", "StricheTarget");
        Group group = createGroup("Striche Gruppe", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        GroupMember targetMembership = createMembership(group, target, GroupRole.MEMBER);
        targetMembership.setStrichCount(7);
        groupMemberRepository.save(targetMembership);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/settlements/striche")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 3))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.userId").value(target.getId()))
            .andExpect(jsonPath("$.username").value("StricheTarget"))
            .andExpect(jsonPath("$.role").value("MEMBER"))
            .andExpect(jsonPath("$.joinedAt").isNotEmpty())
            .andExpect(jsonPath("$.strichCount").value(4));

        GroupMember updatedMembership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), target.getId()).orElseThrow();
        assertThat(updatedMembership.getStrichCount()).isEqualTo(4);

        assertThat(settlementRepository.count()).isEqualTo(1);
        Settlement settlement = settlementRepository.findAll().getFirst();
        assertThat(settlement.getGroupId()).isEqualTo(group.getId());
        assertThat(settlement.getTargetUserId()).isEqualTo(target.getId());
        assertThat(settlement.getActorUserId()).isEqualTo(admin.getId());
        assertThat(settlement.getType()).isEqualTo(SettlementType.STRICHE);
        assertThat(settlement.getStricheAmount()).isEqualTo(3);
        assertThat(settlement.getMoneyAmount()).isNull();
    }

    @Test
    void createStricheSettlementAllowsCreditWhenMoreStricheAreSettledThanCurrentlyOwed() throws Exception {
        User admin = createUser("striche-negative-admin@example.com", "StricheAdmin");
        User target = createUser("striche-negative-target@example.com", "StricheTarget");
        Group group = createGroup("Striche Zero", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        GroupMember targetMembership = createMembership(group, target, GroupRole.MEMBER);
        targetMembership.setStrichCount(2);
        groupMemberRepository.save(targetMembership);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/settlements/striche")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 5))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.strichCount").value(-3));

        GroupMember updatedMembership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), target.getId()).orElseThrow();
        assertThat(updatedMembership.getStrichCount()).isEqualTo(-3);
    }

    @Test
    void createStricheSettlementReturnsForbiddenWhenCallerIsNotAdmin() throws Exception {
        User admin = createUser("striche-rights-admin@example.com", "StricheAdmin");
        User caller = createUser("striche-rights-member@example.com", "StricheMember");
        User target = createUser("striche-rights-target@example.com", "StricheTarget");
        Group group = createGroup("Striche Rechte", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, caller, GroupRole.MEMBER);
        GroupMember targetMembership = createMembership(group, target, GroupRole.MEMBER);
        targetMembership.setStrichCount(6);
        groupMemberRepository.save(targetMembership);

        String token = jwtTokenProvider.createAccessToken(caller);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/settlements/striche")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 2))))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Wart-Rechte erforderlich"));

        GroupMember unchangedMembership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), target.getId()).orElseThrow();
        assertThat(unchangedMembership.getStrichCount()).isEqualTo(6);
        assertThat(settlementRepository.count()).isZero();
    }

    @Test
    void createStricheSettlementReturnsBadRequestWhenAmountIsZero() throws Exception {
        User admin = createUser("striche-invalid-admin@example.com", "StricheAdmin");
        User target = createUser("striche-invalid-target@example.com", "StricheTarget");
        Group group = createGroup("Striche Invalid", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, target, GroupRole.MEMBER);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/settlements/striche")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 0))))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.amount").exists());

        assertThat(settlementRepository.count()).isZero();
    }

    @Test
    void createStricheSettlementReturnsNotFoundWhenTargetIsNotMember() throws Exception {
        User admin = createUser("striche-target-admin@example.com", "StricheAdmin");
        User outsider = createUser("striche-target-outsider@example.com", "StricheOutsider");
        Group group = createGroup("Striche Missing Member", admin);

        createMembership(group, admin, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + outsider.getId() + "/settlements/striche")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 2))))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Gruppenmitglied nicht gefunden"));

        assertThat(settlementRepository.count()).isZero();
    }

    private User createUser(String email, String username) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash("hashed");
        return userRepository.save(user);
    }

    private Group createGroup(String name, User createdByUser) {
        Group group = new Group();
        group.setName(name);
        group.setCreatedByUser(createdByUser);
        return groupRepository.save(group);
    }

    private GroupMember createMembership(Group group, User user, GroupRole role) {
        GroupMember groupMember = new GroupMember();
        groupMember.setGroup(group);
        groupMember.setUser(user);
        groupMember.setRole(role);
        return groupMemberRepository.save(groupMember);
    }
}
