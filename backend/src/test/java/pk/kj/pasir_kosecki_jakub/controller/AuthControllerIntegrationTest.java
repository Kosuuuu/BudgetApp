package pk.kj.pasir_kosecki_jakub.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import pk.kj.pasir_kosecki_jakub.dto.LoginDto;
import pk.kj.pasir_kosecki_jakub.dto.UserDTO;
import pk.kj.pasir_kosecki_jakub.model.User;
import pk.kj.pasir_kosecki_jakub.repository.TransactionRepository;
import pk.kj.pasir_kosecki_jakub.repository.UserRepository;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "TestPassword123";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        transactionRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String generateUniqueEmail() {
        return "test_" + UUID.randomUUID().toString().substring(0, 8) + "@pk.pl";
    }

    private UserDTO createUserDTO(String username, String email, String password) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(username);
        userDTO.setEmail(email);
        userDTO.setPassword(password);
        return userDTO;
    }

    private LoginDto createLoginDto(String email, String password) {
        LoginDto loginDto = new LoginDto();
        loginDto.setEmail(email);
        loginDto.setPassword(password);
        return loginDto;
    }

    @Test
    @Order(1)
    @DisplayName("Powinien zarejestrować nowego użytkownika")
    void shouldRegisterNewUser() throws Exception {
        String email = generateUniqueEmail();

        UserDTO userDTO = createUserDTO(
                TEST_USERNAME,
                email,
                TEST_PASSWORD
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.currency").value("PLN"))
                .andExpect(content().string(not(containsString(TEST_PASSWORD))));
    }

    @Test
    @Order(2)
    @DisplayName("Nie powinien zarejestrować użytkownika z istniejącym emailem")
    void shouldNotRegisterUserWithExistingEmail() throws Exception {
        String email = generateUniqueEmail();

        UserDTO userDTO = createUserDTO(
                TEST_USERNAME,
                email,
                TEST_PASSWORD
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    @DisplayName("Powinien zalogować istniejącego użytkownika i zwrócić token JWT")
    void shouldLoginExistingUserAndReturnJwtToken() throws Exception {
        String email = generateUniqueEmail();

        UserDTO userDTO = createUserDTO(
                TEST_USERNAME,
                email,
                TEST_PASSWORD
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk());

        LoginDto loginDto = createLoginDto(email, TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").value(containsString(".")));
    }

    @Test
    @Order(4)
    @DisplayName("Nie powinien zalogować użytkownika z błędnym hasłem")
    void shouldNotLoginWithWrongPassword() throws Exception {
        String email = generateUniqueEmail();

        UserDTO userDTO = createUserDTO(
                TEST_USERNAME,
                email,
                TEST_PASSWORD
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk());

        LoginDto loginDto = createLoginDto(email, "WrongPassword123");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    @DisplayName("Nie powinien zalogować nieistniejącego użytkownika")
    void shouldNotLoginNonExistingUser() throws Exception {
        LoginDto loginDto = createLoginDto(
                generateUniqueEmail(),
                TEST_PASSWORD
        );

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @DisplayName("Nie powinien zarejestrować użytkownika z błędnym emailem")
    void shouldNotRegisterUserWithInvalidEmail() throws Exception {
        UserDTO userDTO = createUserDTO(
                TEST_USERNAME,
                "invalid-email",
                TEST_PASSWORD
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(7)
    @DisplayName("Nie powinien zarejestrować użytkownika z pustymi polami")
    void shouldNotRegisterUserWithBlankFields() throws Exception {
        UserDTO userDTO = createUserDTO(
                "",
                "",
                ""
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(8)
    @DisplayName("Hasło powinno być zahashowane w bazie danych BCrypt")
    void passwordShouldBeHashedInDatabase() throws Exception {
        String email = generateUniqueEmail();

        UserDTO userDTO = createUserDTO(
                TEST_USERNAME + "_hash",
                email,
                TEST_PASSWORD
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(TEST_PASSWORD))));

        User savedUser = userRepository.findByEmail(email).orElseThrow();

        assert !savedUser.getPassword().equals(TEST_PASSWORD);
        assert savedUser.getPassword().startsWith("$2");
    }

    @Test
    @Order(9)
    @DisplayName("Token JWT powinien mieć poprawny format")
    void jwtTokenShouldHaveCorrectFormat() throws Exception {
        String email = generateUniqueEmail();

        UserDTO userDTO = createUserDTO(
                TEST_USERNAME,
                email,
                TEST_PASSWORD
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk());

        LoginDto loginDto = createLoginDto(email, TEST_PASSWORD);

        String response = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        String token = jsonNode.get("token").asText();

        String[] tokenParts = token.split("\\.");

        assert tokenParts.length == 3;
    }

    @Test
    @Order(10)
    @DisplayName("Odpowiedź rejestracji nie powinna zawierać plain text password")
    void registerResponseShouldNotContainPlainTextPassword() throws Exception {
        String email = generateUniqueEmail();

        UserDTO userDTO = createUserDTO(
                TEST_USERNAME,
                email,
                TEST_PASSWORD
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(TEST_PASSWORD))));
    }
}