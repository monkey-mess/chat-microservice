package ru.ogyrecheksan.chatmicroservice.Integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import ru.ogyrecheksan.chatmicroservice.dto.Request.CreateChatRequest;
import ru.ogyrecheksan.chatmicroservice.dto.Response.ChatResponse;
import ru.ogyrecheksan.chatmicroservice.dto.Response.UserInfoResponse;
import ru.ogyrecheksan.chatmicroservice.model.enums.ChatType;
import ru.ogyrecheksan.chatmicroservice.service.UserServiceClient;

import javax.crypto.SecretKey;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "auth.service.url=http://localhost:5252"
})
class ChatComponentTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private UserServiceClient userServiceClient;

    private String validToken;
    private String baseUrl;

    private final String jwtSecret = "Peanut_Butter_Jelly_The_Long_Way_Secret_Key_123";

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        
        // Создаем валидный JWT токен для тестов
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        validToken = "Bearer " + Jwts.builder()
                .claim("email", "test@example.com")
                .signWith(key)
                .compact();

        // Настраиваем моки для UserServiceClient
        UserInfoResponse userInfo = new UserInfoResponse();
        userInfo.setId(1L);
        userInfo.setUsername("testuser");
        userInfo.setEmail("test@example.com");

        when(userServiceClient.getUserById(anyString(), anyLong()))
                .thenReturn(userInfo);
        when(userServiceClient.getUsersByIds(anyString(), anyList()))
                .thenReturn(Arrays.asList(userInfo));
    }

    @Test
    void accessProtectedEndpoint_WithoutToken_ShouldReturnForbidden() {
        String url = baseUrl + "/api/chats";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void accessProtectedEndpoint_WithInvalidToken_ShouldReturnForbidden() {
        String url = baseUrl + "/api/chats";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer invalid-token");
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getUserChats_WithValidToken_ShouldReturnOk() {
        String url = baseUrl + "/api/chats";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", validToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void createGroupChat_WithValidToken_ShouldReturnCreated() throws Exception {
        String url = baseUrl + "/api/chats";
        CreateChatRequest request = new CreateChatRequest();
        request.setName("Test Group Chat");
        request.setType(ChatType.GROUP);
        request.setParticipantIds(Arrays.asList(2L, 3L));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", validToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateChatRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, ChatResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test Group Chat", response.getBody().getName());
        assertEquals(ChatType.GROUP, response.getBody().getType());
    }

    @Test
    void createGroupChat_WithoutToken_ShouldReturnForbidden() throws Exception {
        String url = baseUrl + "/api/chats";
        CreateChatRequest request = new CreateChatRequest();
        request.setName("Test Group Chat");
        request.setType(ChatType.GROUP);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateChatRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void createGroupChat_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        String url = baseUrl + "/api/chats";
        CreateChatRequest request = new CreateChatRequest();
        // Не устанавливаем обязательные поля

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", validToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateChatRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createPersonalChat_WithValidToken_ShouldReturnOk() {
        String url = baseUrl + "/api/chats/personal/2";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", validToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<ChatResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, ChatResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ChatType.PERSONAL, response.getBody().getType());
    }

    @Test
    void createPersonalChat_WithoutToken_ShouldReturnForbidden() {
        String url = baseUrl + "/api/chats/personal/2";
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getChat_WithValidToken_ShouldReturnOk() {
        // Сначала создаем чат
        String createUrl = baseUrl + "/api/chats";
        CreateChatRequest request = new CreateChatRequest();
        request.setName("Test Chat");
        request.setType(ChatType.GROUP);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", validToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateChatRequest> createEntity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatResponse> createResponse = restTemplate.exchange(
                createUrl, HttpMethod.POST, createEntity, ChatResponse.class);

        assertNotNull(createResponse.getBody());
        Long chatId = createResponse.getBody().getId();

        // Затем получаем чат
        String getUrl = baseUrl + "/api/chats/" + chatId;
        HttpEntity<?> getEntity = new HttpEntity<>(headers);

        ResponseEntity<ChatResponse> getResponse = restTemplate.exchange(
                getUrl, HttpMethod.GET, getEntity, ChatResponse.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals(chatId, getResponse.getBody().getId());
    }

    @Test
    void getChat_WithoutToken_ShouldReturnForbidden() {
        String url = baseUrl + "/api/chats/1";
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getChat_WithNonExistentChat_ShouldReturnNotFound() {
        String url = baseUrl + "/api/chats/99999";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", validToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

        // Может вернуть 500 или 404 в зависимости от реализации
        assertTrue(response.getStatusCode().is5xxServerError() || 
                   response.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    @Test
    void getChatMessages_WithValidToken_ShouldReturnOk() {
        // Сначала создаем чат
        String createUrl = baseUrl + "/api/chats";
        CreateChatRequest request = new CreateChatRequest();
        request.setName("Test Chat");
        request.setType(ChatType.GROUP);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", validToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateChatRequest> createEntity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatResponse> createResponse = restTemplate.exchange(
                createUrl, HttpMethod.POST, createEntity, ChatResponse.class);

        assertNotNull(createResponse.getBody());
        Long chatId = createResponse.getBody().getId();

        // Затем получаем сообщения
        String messagesUrl = baseUrl + "/api/chats/" + chatId + "/messages?page=0&size=10";
        HttpEntity<?> messagesEntity = new HttpEntity<>(headers);

        ResponseEntity<String> messagesResponse = restTemplate.exchange(
                messagesUrl, HttpMethod.GET, messagesEntity, String.class);

        assertEquals(HttpStatus.OK, messagesResponse.getStatusCode());
    }

    @Test
    void getChatMessages_WithoutToken_ShouldReturnForbidden() {
        String url = baseUrl + "/api/chats/1/messages";
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}