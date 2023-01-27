package example.micronaut;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import io.micronaut.http.*;
import io.micronaut.http.client.*;
import io.micronaut.http.client.annotation.*;
import io.micronaut.test.extensions.junit5.annotation.*;
import jakarta.inject.*;

@MicronautTest // <1>
public class HelloControllerTest {

    @Inject
    @Client("/")  // <2>
    HttpClient client;

    @Test
    public void testHello() {
        HttpRequest<String> request = HttpRequest.GET("/hello");  // <3>
        String body = client.toBlocking().retrieve(request);

        assertNotNull(body);
        assertEquals("Hello World", body);
    }
}