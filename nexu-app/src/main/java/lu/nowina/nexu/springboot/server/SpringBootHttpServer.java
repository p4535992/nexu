package lu.nowina.nexu.springboot.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import lu.nowina.nexu.HttpServer;
import lu.nowina.nexu.api.NexuAPI;

/**
 * Spring Boot implementation of the legacy NexU {@link HttpServer} contract.
 *
 * <p>The implementation deliberately keeps the existing NexU engine and plugin
 * model untouched. It only replaces the embedded HTTP container.</p>
 */
public final class SpringBootHttpServer implements HttpServer {

    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    private NexuAPI api;
    private ConfigurableApplicationContext applicationContext;

    @Override
    public void setConfig(final NexuAPI api) {
        this.api = Objects.requireNonNull(api, "api");
    }

    @Override
    public synchronized void start() {
        if (applicationContext != null) {
            return;
        }
        if (api == null) {
            throw new IllegalStateException("NexuAPI must be configured before starting the HTTP server");
        }

        final List<Integer> ports = api.getAppConfig().getBindingPorts();
        if (ports == null || ports.isEmpty()) {
            throw new IllegalStateException("At least one HTTP binding port must be configured");
        }

        final Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("server.address", api.getAppConfig().getBindingIP());
        defaults.put("server.port", ports.get(0));
        defaults.put("server.shutdown", "graceful");
        defaults.put("spring.application.name", api.getAppConfig().getApplicationName());
        defaults.put("spring.main.banner-mode", "off");
        defaults.put("spring.jmx.enabled", "false");

        final SpringApplication application = new SpringApplication(NexuSpringBootConfiguration.class);
        application.setWebApplicationType(WebApplicationType.SERVLET);
        application.setRegisterShutdownHook(false);
        application.setDefaultProperties(defaults);
        application.addInitializers(context ->
                context.getBeanFactory().registerSingleton("nexuApi", api));

        applicationContext = application.run();
    }

    @Override
    public synchronized void stop() {
        if (applicationContext != null) {
            applicationContext.close();
            applicationContext = null;
        }
        shutdownLatch.countDown();
    }

    @Override
    public void join() throws InterruptedException {
        shutdownLatch.await();
    }
}
