package org.example.server;

import com.dianping.HmDianPingApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = HmDianPingApplication.class,
        properties = "security.jwt.secret=dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LXdpdGgtMzItYnl0ZXM=")
class ServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
