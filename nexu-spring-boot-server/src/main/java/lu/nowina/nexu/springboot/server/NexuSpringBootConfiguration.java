package lu.nowina.nexu.springboot.server;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import({
        NexuHttpController.class,
        NexuModernController.class,
        NexuLoopbackCorsFilter.class
})
public class NexuSpringBootConfiguration {
}
