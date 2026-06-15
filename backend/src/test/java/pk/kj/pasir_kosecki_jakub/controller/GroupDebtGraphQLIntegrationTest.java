package pk.kj.pasir_kosecki_jakub.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pk.kj.pasir_kosecki_jakub.dto.LoginDto;
import pk.kj.pasir_kosecki_jakub.dto.UserDTO;
import pk.kj.pasir_kosecki_jakub.model.Debt;
import pk.kj.pasir_kosecki_jakub.model.Membership;
import pk.kj.pasir_kosecki_jakub.model.User;
import pk.kj.pasir_kosecki_jakub.repository.DebtRepository;
import pk.kj.pasir_kosecki_jakub.repository.GroupRepository;
import pk.kj.pasir_kosecki_jakub.repository.MembershipRepository;
import pk.kj.pasir_kosecki_jakub.repository.TransactionRepository;
import pk.kj.pasir_kosecki_jakub.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GroupDebtGraphQLIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private DebtRepository debtRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PASSWORD = "TestPassword123";

    private record TestUser(Long id, String username, String email, String token) {
    }

    @BeforeEach
    void setUp() {
        debtRepository.deleteAll();
        membershipRepository.deleteAll();
        groupRepository.deleteAll();
        transactionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Utworzenie grupy dodaje właściciela jako członka i zwraca ją w myGroups")
    void createGroupAddsOwnerAsMemberAndReturnsInMyGroups() throws Exception {
        TestUser owner = createUser("owner");

        Long groupId = createGroup(owner.token(), "Grupa testowa");

        List<Membership> memberships = membershipRepository.findByGroup_Id(groupId);
        assertEquals(1, memberships.size());
        assertEquals(owner.id(), memberships.get(0).getUser().getId());

        JsonNode myGroups = graphQl(
                owner.token(),
                """
                query {
                  myGroups {
                    id
                    name
                    ownerId
                  }
                }
                """,
                Map.of()
        );

        assertNoErrors(myGroups);
        assertEquals(groupId.toString(), myGroups.at("/data/myGroups/0/id").asText());
    }

    @Test
    @DisplayName("Tylko właściciel grupy może dodawać członków")
    void onlyOwnerCanAddMembers() throws Exception {
        TestUser owner = createUser("owner");
        TestUser member = createUser("member");
        TestUser third = createUser("third");

        Long groupId = createGroup(owner.token(), "Grupa");
        addMember(owner.token(), groupId, member.email());

        JsonNode result = addMemberRaw(member.token(), groupId, third.email());

        assertHasErrors(result);
    }

    @Test
    @DisplayName("groupMembers zwraca członków grupy tylko członkowi tej grupy")
    void groupMembersOnlyForGroupMember() throws Exception {
        TestUser owner = createUser("owner");
        TestUser member = createUser("member");
        TestUser outsider = createUser("outsider");

        Long groupId = createGroup(owner.token(), "Grupa");
        addMember(owner.token(), groupId, member.email());

        JsonNode memberResult = groupMembers(member.token(), groupId);
        assertNoErrors(memberResult);
        assertEquals(2, memberResult.at("/data/groupMembers").size());

        JsonNode outsiderResult = groupMembers(outsider.token(), groupId);
        assertHasErrors(outsiderResult);
    }

    @Test
    @DisplayName("groupDebts zwraca długi grupy tylko członkowi tej grupy")
    void groupDebtsOnlyForGroupMember() throws Exception {
        TestUser owner = createUser("owner");
        TestUser member = createUser("member");
        TestUser outsider = createUser("outsider");

        Long groupId = createGroup(owner.token(), "Grupa");
        addMember(owner.token(), groupId, member.email());
        addGroupTransaction(owner.token(), groupId, 100.0, "EXPENSE", "Pizza");

        JsonNode memberResult = groupDebts(member.token(), groupId);
        assertNoErrors(memberResult);
        assertEquals(1, memberResult.at("/data/groupDebts").size());

        JsonNode outsiderResult = groupDebts(outsider.token(), groupId);
        assertHasErrors(outsiderResult);
    }

    @Test
    @DisplayName("Nowy członek dostaje tylko długi z transakcji dodanych po dołączeniu")
    void newMemberGetsOnlyDebtsFromTransactionsAddedAfterJoining() throws Exception {
        TestUser owner = createUser("owner");
        TestUser member1 = createUser("member1");
        TestUser member2 = createUser("member2");

        Long groupId = createGroup(owner.token(), "Grupa");
        addMember(owner.token(), groupId, member1.email());

        addGroupTransaction(owner.token(), groupId, 100.0, "EXPENSE", "Pierwsza transakcja");

        addMember(owner.token(), groupId, member2.email());

        addGroupTransaction(owner.token(), groupId, 90.0, "EXPENSE", "Druga transakcja");

        List<Debt> debts = debtRepository.findByGroup_Id(groupId);

        List<Debt> member1Debts = debts.stream()
                .filter(debt -> debt.getDebtor().getId().equals(member1.id()))
                .toList();

        List<Debt> member2Debts = debts.stream()
                .filter(debt -> debt.getDebtor().getId().equals(member2.id()))
                .toList();

        assertEquals(2, member1Debts.size());
        assertEquals(1, member2Debts.size());
        assertEquals(30.0, member2Debts.get(0).getAmount(), 0.01);
    }

    @Test
    @DisplayName("Transakcja grupowa typu INCOME tworzy długi od aktualnego użytkownika do pozostałych członków")
    void incomeGroupTransactionCreatesDebtsFromCurrentUserToOtherMembers() throws Exception {
        TestUser owner = createUser("owner");
        TestUser member1 = createUser("member1");
        TestUser member2 = createUser("member2");

        Long groupId = createGroup(owner.token(), "Grupa");
        addMember(owner.token(), groupId, member1.email());
        addMember(owner.token(), groupId, member2.email());

        addGroupTransaction(owner.token(), groupId, 90.0, "INCOME", "Zwrot");

        List<Debt> debts = debtRepository.findByGroup_Id(groupId);

        assertEquals(2, debts.size());

        for (Debt debt : debts) {
            assertEquals(owner.id(), debt.getDebtor().getId());
            assertTrue(
                    debt.getCreditor().getId().equals(member1.id())
                            || debt.getCreditor().getId().equals(member2.id())
            );
            assertEquals(30.0, debt.getAmount(), 0.01);
        }
    }

    @Test
    @DisplayName("Usunięcie członka nie usuwa jego historycznych długów")
    void removingMemberDoesNotDeleteHistoricalDebts() throws Exception {
        TestUser owner = createUser("owner");
        TestUser member = createUser("member");

        Long groupId = createGroup(owner.token(), "Grupa");
        Long membershipId = addMember(owner.token(), groupId, member.email());

        addGroupTransaction(owner.token(), groupId, 100.0, "EXPENSE", "Pizza");

        assertEquals(1, debtRepository.findByGroup_Id(groupId).size());

        removeMember(owner.token(), membershipId);

        assertEquals(1, debtRepository.findByGroup_Id(groupId).size());
        assertFalse(membershipRepository.existsByGroup_IdAndUser_Id(groupId, member.id()));
    }

    @Test
    @DisplayName("Nie można usunąć właściciela z jego grupy przez removeMember")
    void cannotRemoveOwnerFromOwnGroup() throws Exception {
        TestUser owner = createUser("owner");

        Long groupId = createGroup(owner.token(), "Grupa");

        Long ownerMembershipId = membershipRepository.findByGroup_Id(groupId)
                .stream()
                .filter(membership -> membership.getUser().getId().equals(owner.id()))
                .findFirst()
                .orElseThrow()
                .getId();

        JsonNode result = removeMemberRaw(owner.token(), ownerMembershipId);

        assertHasErrors(result);
        assertTrue(membershipRepository.existsByGroup_IdAndUser_Id(groupId, owner.id()));
    }

    @Test
    @DisplayName("Członek grupy niebędący właścicielem nie może usunąć grupy")
    void nonOwnerMemberCannotDeleteGroup() throws Exception {
        TestUser owner = createUser("owner");
        TestUser member = createUser("member");

        Long groupId = createGroup(owner.token(), "Grupa");
        addMember(owner.token(), groupId, member.email());

        JsonNode result = deleteGroupRaw(member.token(), groupId);

        assertHasErrors(result);
        assertTrue(groupRepository.existsById(groupId));
    }

    @Test
    @DisplayName("createDebt tworzy ręczny dług tylko między członkami tej samej grupy")
    void createDebtCreatesManualDebtOnlyBetweenMembersOfSameGroup() throws Exception {
        TestUser owner = createUser("owner");
        TestUser member = createUser("member");

        Long groupId = createGroup(owner.token(), "Grupa");
        addMember(owner.token(), groupId, member.email());

        Long debtId = createDebt(owner.token(), groupId, member.id(), owner.id(), 25.0, "Ręczny dług");

        Debt debt = debtRepository.findById(debtId).orElseThrow();

        assertEquals(groupId, debt.getGroup().getId());
        assertEquals(member.id(), debt.getDebtor().getId());
        assertEquals(owner.id(), debt.getCreditor().getId());
        assertEquals(25.0, debt.getAmount(), 0.01);
    }

    @Test
    @DisplayName("createDebt odrzuca użytkownika spoza grupy i dług do samego siebie")
    void createDebtRejectsOutsiderAndSelfDebt() throws Exception {
        TestUser owner = createUser("owner");
        TestUser member = createUser("member");
        TestUser outsider = createUser("outsider");

        Long groupId = createGroup(owner.token(), "Grupa");
        addMember(owner.token(), groupId, member.email());

        JsonNode outsiderResult = createDebtRaw(
                owner.token(),
                groupId,
                outsider.id(),
                owner.id(),
                50.0,
                "Spoza grupy"
        );

        assertHasErrors(outsiderResult);

        JsonNode selfDebtResult = createDebtRaw(
                owner.token(),
                groupId,
                owner.id(),
                owner.id(),
                50.0,
                "Sam do siebie"
        );

        assertHasErrors(selfDebtResult);
    }

    @Test
    @DisplayName("Właściciel grupy może utworzyć dług między innymi członkami grupy")
    void ownerCanCreateDebtBetweenOtherGroupMembers() throws Exception {
        TestUser owner = createUser("owner");
        TestUser member1 = createUser("member1");
        TestUser member2 = createUser("member2");

        Long groupId = createGroup(owner.token(), "Grupa");
        addMember(owner.token(), groupId, member1.email());
        addMember(owner.token(), groupId, member2.email());

        Long debtId = createDebt(owner.token(), groupId, member1.id(), member2.id(), 70.0, "Między członkami");

        Debt debt = debtRepository.findById(debtId).orElseThrow();

        assertEquals(member1.id(), debt.getDebtor().getId());
        assertEquals(member2.id(), debt.getCreditor().getId());
    }

    @Test
    @DisplayName("Członek grupy może utworzyć dług tylko gdy jest jego uczestnikiem")
    void memberCanCreateDebtOnlyWhenParticipant() throws Exception {
        TestUser owner = createUser("owner");
        TestUser member1 = createUser("member1");
        TestUser member2 = createUser("member2");

        Long groupId = createGroup(owner.token(), "Grupa");
        addMember(owner.token(), groupId, member1.email());
        addMember(owner.token(), groupId, member2.email());

        JsonNode invalidResult = createDebtRaw(
                member1.token(),
                groupId,
                owner.id(),
                member2.id(),
                40.0,
                "Bez udziału member1"
        );

        assertHasErrors(invalidResult);

        JsonNode validResult = createDebtRaw(
                member1.token(),
                groupId,
                member1.id(),
                owner.id(),
                40.0,
                "Z udziałem member1"
        );

        assertNoErrors(validResult);
    }

    @Test
    @DisplayName("deleteDebt usuwa dług dostępny dla uczestnika długu")
    void deleteDebtDeletesDebtAccessibleForParticipant() throws Exception {
        TestUser owner = createUser("owner");
        TestUser member = createUser("member");

        Long groupId = createGroup(owner.token(), "Grupa");
        addMember(owner.token(), groupId, member.email());

        Long debtId = createDebt(owner.token(), groupId, member.id(), owner.id(), 30.0, "Dług");

        JsonNode result = deleteDebtRaw(member.token(), debtId);

        assertNoErrors(result);
        assertFalse(debtRepository.existsById(debtId));
    }

    @Test
    @DisplayName("deleteDebt odrzuca członka grupy, który nie jest właścicielem ani uczestnikiem długu")
    void deleteDebtRejectsGroupMemberWhoIsNotOwnerOrParticipant() throws Exception {
        TestUser owner = createUser("owner");
        TestUser member1 = createUser("member1");
        TestUser member2 = createUser("member2");

        Long groupId = createGroup(owner.token(), "Grupa");
        addMember(owner.token(), groupId, member1.email());
        addMember(owner.token(), groupId, member2.email());

        Long debtId = createDebt(owner.token(), groupId, owner.id(), member1.id(), 80.0, "Dług");

        JsonNode result = deleteDebtRaw(member2.token(), debtId);

        assertHasErrors(result);
        assertTrue(debtRepository.existsById(debtId));
    }

    @Test
    @DisplayName("Właściciel grupy może usunąć dług, którego nie jest uczestnikiem")
    void ownerCanDeleteDebtWhenNotParticipant() throws Exception {
        TestUser owner = createUser("owner");
        TestUser member1 = createUser("member1");
        TestUser member2 = createUser("member2");

        Long groupId = createGroup(owner.token(), "Grupa");
        addMember(owner.token(), groupId, member1.email());
        addMember(owner.token(), groupId, member2.email());

        Long debtId = createDebt(owner.token(), groupId, member1.id(), member2.id(), 80.0, "Dług");

        JsonNode result = deleteDebtRaw(owner.token(), debtId);

        assertNoErrors(result);
        assertFalse(debtRepository.existsById(debtId));
    }

    @Test
    @DisplayName("Walidacje danych wejściowych GraphQL odrzucają puste lub niepoprawne wartości")
    void graphqlValidationRejectsInvalidValues() throws Exception {
        TestUser owner = createUser("owner");

        JsonNode blankGroup = graphQl(
                owner.token(),
                """
                mutation CreateGroup($groupDTO: GroupInput!) {
                  createGroup(groupDTO: $groupDTO) {
                    id
                  }
                }
                """,
                Map.of("groupDTO", Map.of("name", ""))
        );

        assertHasErrors(blankGroup);

        Long groupId = createGroup(owner.token(), "Grupa");

        JsonNode invalidTransaction = graphQl(
                owner.token(),
                """
                mutation AddGroupTransaction($groupTransactionDTO: GroupTransactionInput!) {
                  addGroupTransaction(groupTransactionDTO: $groupTransactionDTO)
                }
                """,
                Map.of("groupTransactionDTO", Map.of(
                        "groupId", groupId,
                        "amount", -10.0,
                        "type", "BAD_TYPE",
                        "title", ""
                ))
        );

        assertHasErrors(invalidTransaction);
    }

    @Test
    @DisplayName("Usunięcie grupy przez właściciela usuwa powiązane długi i grupę")
    void ownerDeletingGroupDeletesRelatedDebtsAndGroup() throws Exception {
        TestUser owner = createUser("owner");
        TestUser member = createUser("member");

        Long groupId = createGroup(owner.token(), "Grupa");
        addMember(owner.token(), groupId, member.email());
        createDebt(owner.token(), groupId, member.id(), owner.id(), 20.0, "Dług");

        assertEquals(1, debtRepository.findByGroup_Id(groupId).size());
        assertFalse(membershipRepository.findByGroup_Id(groupId).isEmpty());

        JsonNode result = deleteGroupRaw(owner.token(), groupId);

        assertNoErrors(result);
        assertFalse(groupRepository.existsById(groupId));
        assertTrue(debtRepository.findByGroup_Id(groupId).isEmpty());
        assertTrue(membershipRepository.findByGroup_Id(groupId).isEmpty());
    }

    private TestUser createUser(String usernamePrefix) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = usernamePrefix + "_" + suffix;
        String email = username + "@pk.pl";

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(username);
        userDTO.setEmail(email);
        userDTO.setPassword(PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andReturn();

        User savedUser = userRepository.findByEmail(email).orElseThrow();

        LoginDto loginDto = new LoginDto();
        loginDto.setEmail(email);
        loginDto.setPassword(PASSWORD);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String token = loginJson.get("token").asText();

        return new TestUser(savedUser.getId(), username, email, token);
    }

    private Long createGroup(String token, String name) throws Exception {
        JsonNode result = graphQl(
                token,
                """
                mutation CreateGroup($groupDTO: GroupInput!) {
                  createGroup(groupDTO: $groupDTO) {
                    id
                    name
                    ownerId
                  }
                }
                """,
                Map.of("groupDTO", Map.of("name", name))
        );

        assertNoErrors(result);

        return Long.valueOf(result.at("/data/createGroup/id").asText());
    }

    private Long addMember(String token, Long groupId, String userEmail) throws Exception {
        JsonNode result = addMemberRaw(token, groupId, userEmail);

        assertNoErrors(result);

        return Long.valueOf(result.at("/data/addMember/id").asText());
    }

    private JsonNode addMemberRaw(String token, Long groupId, String userEmail) throws Exception {
        return graphQl(
                token,
                """
                mutation AddMember($membershipDTO: MembershipInput!) {
                  addMember(membershipDTO: $membershipDTO) {
                    id
                    userId
                    groupId
                    userEmail
                  }
                }
                """,
                Map.of("membershipDTO", Map.of(
                        "groupId", groupId,
                        "userEmail", userEmail
                ))
        );
    }

    private JsonNode removeMemberRaw(String token, Long membershipId) throws Exception {
        return graphQl(
                token,
                """
                mutation RemoveMember($membershipId: ID!) {
                  removeMember(membershipId: $membershipId)
                }
                """,
                Map.of("membershipId", membershipId)
        );
    }

    private void removeMember(String token, Long membershipId) throws Exception {
        JsonNode result = removeMemberRaw(token, membershipId);
        assertNoErrors(result);
    }

    private void addGroupTransaction(
            String token,
            Long groupId,
            Double amount,
            String type,
            String title
    ) throws Exception {
        JsonNode result = graphQl(
                token,
                """
                mutation AddGroupTransaction($groupTransactionDTO: GroupTransactionInput!) {
                  addGroupTransaction(groupTransactionDTO: $groupTransactionDTO)
                }
                """,
                Map.of("groupTransactionDTO", Map.of(
                        "groupId", groupId,
                        "amount", amount,
                        "type", type,
                        "title", title
                ))
        );

        assertNoErrors(result);
    }

    private Long createDebt(
            String token,
            Long groupId,
            Long debtorId,
            Long creditorId,
            Double amount,
            String title
    ) throws Exception {
        JsonNode result = createDebtRaw(token, groupId, debtorId, creditorId, amount, title);

        assertNoErrors(result);

        return Long.valueOf(result.at("/data/createDebt/id").asText());
    }

    private JsonNode createDebtRaw(
            String token,
            Long groupId,
            Long debtorId,
            Long creditorId,
            Double amount,
            String title
    ) throws Exception {
        return graphQl(
                token,
                """
                mutation CreateDebt($debtDTO: DebtInput!) {
                  createDebt(debtDTO: $debtDTO) {
                    id
                    debtorId
                    creditorId
                    groupId
                    amount
                    title
                  }
                }
                """,
                Map.of("debtDTO", Map.of(
                        "groupId", groupId,
                        "debtorId", debtorId,
                        "creditorId", creditorId,
                        "amount", amount,
                        "title", title
                ))
        );
    }

    private JsonNode deleteDebtRaw(String token, Long debtId) throws Exception {
        return graphQl(
                token,
                """
                mutation DeleteDebt($debtId: ID!) {
                  deleteDebt(debtId: $debtId)
                }
                """,
                Map.of("debtId", debtId)
        );
    }

    private JsonNode deleteGroupRaw(String token, Long groupId) throws Exception {
        return graphQl(
                token,
                """
                mutation DeleteGroup($id: ID!) {
                  deleteGroup(id: $id)
                }
                """,
                Map.of("id", groupId)
        );
    }

    private JsonNode groupMembers(String token, Long groupId) throws Exception {
        return graphQl(
                token,
                """
                query GroupMembers($groupId: ID!) {
                  groupMembers(groupId: $groupId) {
                    id
                    userId
                    groupId
                    userEmail
                  }
                }
                """,
                Map.of("groupId", groupId)
        );
    }

    private JsonNode groupDebts(String token, Long groupId) throws Exception {
        return graphQl(
                token,
                """
                query GroupDebts($groupId: ID!) {
                  groupDebts(groupId: $groupId) {
                    id
                    debtorId
                    creditorId
                    groupId
                    amount
                    title
                  }
                }
                """,
                Map.of("groupId", groupId)
        );
    }

    private JsonNode graphQl(String token, String query, Map<String, Object> variables) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "query", query,
                "variables", variables
        ));

        String response = mockMvc.perform(post("/graphql")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private void assertNoErrors(JsonNode result) {
        if (result.has("errors")) {
            fail("GraphQL zwrócił błędy: " + result.get("errors").toPrettyString());
        }
    }

    private void assertHasErrors(JsonNode result) {
        assertTrue(
                result.has("errors"),
                "Oczekiwano błędu GraphQL, ale odpowiedź była: " + result.toPrettyString()
        );
    }
}