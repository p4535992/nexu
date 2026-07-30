package lu.nowina.nexu.springboot.server;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import lu.nowina.nexu.HttpServer;
import lu.nowina.nexu.NexuHttpsConfiguration;
import lu.nowina.nexu.NexuHttpsConfiguration.TlsMaterial;
import lu.nowina.nexu.api.NexuAPI;

/**
 * Spring Boot implementation of the legacy NexU {@link HttpServer} contract.
 *
 * <p>The implementation deliberately keeps the existing NexU engine and plugin
 * model untouched. It only replaces the embedded HTTP container.</p>
 */
public final class SpringBootHttpServer implements HttpServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringBootHttpServer.class);

    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    private NexuAPI api;
    private ConfigurableApplicationContext httpApplicationContext;
    private ConfigurableApplicationContext httpsApplicationContext;

    @Override
    public void setConfig(final NexuAPI api) {
        this.api = Objects.requireNonNull(api, "api");
    }

    @Override
    public synchronized void start() {
        if (httpApplicationContext != null) {
            return;
        }
        if (api == null) {
            throw new IllegalStateException("NexuAPI must be configured before starting the HTTP server");
        }

        final List<Integer> ports = api.getAppConfig().getBindingPorts();
        if (ports == null || ports.isEmpty()) {
            throw new IllegalStateException("At least one HTTP binding port must be configured");
        }

        httpApplicationContext = createApplication(defaultProperties(ports.get(0), false, null)).run();
        startHttpsIfConfigured();
    }

    private void startHttpsIfConfigured() {
        final List<Integer> httpsPorts = api.getAppConfig().getBindingPortsHttps();
        if (httpsPorts == null || httpsPorts.isEmpty()) {
            LOGGER.info("NexU HTTPS endpoint is disabled because binding_ports_https is empty");
            return;
        }

        final TlsMaterial tlsMaterial;
        try {
            tlsMaterial = NexuHttpsConfiguration.resolve();
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Cannot resolve the NexU HTTPS configuration directory; HTTPS remains disabled", e);
            return;
        }

        if (!tlsMaterial.isComplete()) {
            LOGGER.error(
                    "NexU HTTPS endpoint is disabled. Place both localhost.cer and localhost.key in {}. Missing files: {}",
                    tlsMaterial.configDirectory(),
                    tlsMaterial.missingFiles());
            return;
        }

        final int httpsPort = httpsPorts.get(0);
        try {
            httpsApplicationContext = createApplication(defaultProperties(httpsPort, true, tlsMaterial)).run();
            LOGGER.info(
                    "NexU HTTPS endpoint enabled at https://localhost:{}/nexu-info using certificate={} and privateKey={}",
                    httpsPort,
                    tlsMaterial.certificate(),
                    tlsMaterial.privateKey());
            if (java.nio.file.Files.isRegularFile(tlsMaterial.optionalPkcs12())) {
                LOGGER.info("Optional PKCS#12 file detected at {} (the HTTPS connector uses the PEM certificate and key)",
                        tlsMaterial.optionalPkcs12());
            }
        } catch (RuntimeException e) {
            httpsApplicationContext = null;
            LOGGER.error(
                    "Cannot start NexU HTTPS at https://localhost:{}/nexu-info with certificate={} and privateKey={}; HTTP remains available",
                    httpsPort,
                    tlsMaterial.certificate(),
                    tlsMaterial.privateKey(),
                    e);
        }
    }

    private SpringApplication createApplication(final Map<String, Object> defaults) {
        final SpringApplication application = new SpringApplication(NexuSpringBootConfiguration.class);
        application.setWebApplicationType(WebApplicationType.SERVLET);

        // SpringApplication defaults to headless=true. NexU is a desktop agent and
        // initializes its notification-area icon after the HTTP server starts, so
        // leaving that default enabled makes both Dorkbox and AWT reject the tray.
        application.setHeadless(false);

        application.setRegisterShutdownHook(false);
        application.setDefaultProperties(defaults);
        application.addInitializers(context ->
                context.getBeanFactory().registerSingleton("nexuApi", api));
        return application;
    }

    private Map<String, Object> defaultProperties(
            final int port,
            final boolean sslEnabled,
            final TlsMaterial tlsMaterial) {

        final Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("server.address", api.getAppConfig().getBindingIP());
        defaults.put("server.port", port);
        defaults.put("server.shutdown", "graceful");
        defaults.put("spring.application.name", api.getAppConfig().getApplicationName());
        defaults.put("spring.main.banner-mode", "off");
        defaults.put("spring.jmx.enabled", "false");

        if (sslEnabled) {
            defaults.put("server.ssl.enabled", true);
            defaults.put("server.ssl.certificate", tlsMaterial.certificate().toUri().toString());
            defaults.put("server.ssl.certificate-private-key", tlsMaterial.privateKey().toUri().toString());
            defaults.put("server.ssl.enabled-protocols", "TLSv1.3,TLSv1.2");
            defaults.put("server.ssl.client-auth", "none");
        }
        return defaults;
    }

    @Override
    public synchronized void stop() {
        if (httpsApplicationContext != null) {
            httpsApplicationContext.close();
            httpsApplicationContext = null;
        }
        if (httpApplicationContext != null) {
            httpApplicationContext.close();
            httpApplicationContext = null;
        }
        shutdownLatch.countDown();
    }

    @Override
    public void join() throws InterruptedException {
        shutdownLatch.await();
    }
}
