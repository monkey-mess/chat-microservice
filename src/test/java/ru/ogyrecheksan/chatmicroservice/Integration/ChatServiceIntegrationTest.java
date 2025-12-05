package ru.ogyrecheksan.chatmicroservice.Integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import ru.ogyrecheksan.chatmicroservice.service.ChatService;
import ru.ogyrecheksan.chatmicroservice.service.UserServiceClient;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "auth.service.url=http://localhost:5252"
})
class ChatServiceIntegrationTest {

    @Test
    void contextLoads() {
        // Проверяем, что контекст Spring поднимается
    }
}