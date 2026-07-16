/**
 * © Nowina Solutions, 2015-2015
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package lu.nowina.nexu.server.config;

import static java.net.URLDecoder.decode;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.apache.cxf.transport.servlet.CXFServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.DispatcherServlet;
//import org.springframework.ws.config.annotation.EnableWs;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;


/**
 * Configuration of web application with Servlet 3.0 APIs.
 * 
 * Static Content
 * 
 * By default, Spring Boot serves static content from a directory called /static (or /public or /resources or /META-INF/resources) in the classpath or from 
 * the root of the ServletContext. It uses the ResourceHttpRequestHandler from Spring MVC so that you can modify that behavior by adding your own WebMvcConfigurer
 * and overriding the addResourceHandlers method.
 *  
 * By default, resources are mapped on /** and located on /static directory. But you can customize the static loactions programmatically inside our web 
 * context configuration class.
 *
 * @href https://stackoverflow.com/questions/42393211/how-can-i-serve-static-html-from-spring-boot
 * @href https://www.programmergate.com/serve-static-resources-with-spring-boot/
 */
@Configuration
//@ComponentScan(basePackages = { "lu.nowina.nexu.server" })
//@EnableWebMvc
//@EnableWs
@ImportResource({ "classpath:META-INF/cxf/cxf.xml", "classpath:META-INF/cxf/cxf-servlet.xml" })
//@AutoConfigureAfter(DispatcherServletAutoConfiguration.class)
public class WebConfigurer  implements ServletContextInitializer {//, WebServerFactoryCustomizer<WebServerFactory> { //implements ServletContextInitializer, WebMvcConfigurer, WebServerFactoryCustomizer<WebServerFactory> {

    private final Logger log = LoggerFactory.getLogger(WebConfigurer.class);

    private final Environment env;

    public WebConfigurer(Environment env) {
        this.env = env;
      
    }
    
	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
        if (env.getActiveProfiles().length != 0) {
            log.info("Web application configuration, using profiles: {}", (Object[]) env.getActiveProfiles());
        }

        log.info("Web application fully configured");
        
		WebApplicationContext context = getContext();
		
		servletContext.addListener(new ContextLoaderListener(context));
		ServletRegistration.Dynamic dispatcher = servletContext.addServlet("DispatcherServlet",
				new DispatcherServlet(context));
		dispatcher.setLoadOnStartup(1);
		dispatcher.addMapping("/*");

		ServletRegistration.Dynamic cxf = servletContext.addServlet("CXFServlet", new CXFServlet());
		cxf.setLoadOnStartup(1);
		cxf.addMapping("/api/v1/*");
	}

	private AnnotationConfigWebApplicationContext getContext() {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setConfigLocation(WebConfigurer.class.getPackage().getName());
		return context;
	}
    
    // SPRING BOOT 2 E ALFRESCO 6

//    /**
//     * Customize the Servlet engine: Mime types, the document root, the cache.
//     */
//    @Override
//    public void customize(WebServerFactory server) {
//        setMimeMappings(server);
//        // When running in an IDE or with ./mvnw spring-boot:run, set location of the static web assets.
//        setLocationForStaticAssets(server);
//    }
//
//    private void setMimeMappings(WebServerFactory server) {
//        if (server instanceof ConfigurableServletWebServerFactory) {
//            MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
//            // IE issue, see https://github.com/jhipster/generator-jhipster/pull/711
//            mappings.add("html", MediaType.TEXT_HTML_VALUE + ";charset=" + StandardCharsets.UTF_8.name().toLowerCase());
//            // CloudFoundry issue, see https://github.com/cloudfoundry/gorouter/issues/64
//            mappings.add("json", MediaType.TEXT_HTML_VALUE + ";charset=" + StandardCharsets.UTF_8.name().toLowerCase());
//            ConfigurableServletWebServerFactory servletWebServer = (ConfigurableServletWebServerFactory) server;
//            servletWebServer.setMimeMappings(mappings);
//        }
//    }
//
//    private void setLocationForStaticAssets(WebServerFactory server) {
//        if (server instanceof ConfigurableServletWebServerFactory) {
//            ConfigurableServletWebServerFactory servletWebServer = (ConfigurableServletWebServerFactory) server;
//            File root;
//            String prefixPath = resolvePathPrefix();
//            root = new File(prefixPath + "target/classes/static/");
//            if (root.exists() && root.isDirectory()) {
//                servletWebServer.setDocumentRoot(root);
//            }
//        }
//    }

    /**
     * Resolve path prefix to static resources.
     */
    private String resolvePathPrefix() {
        String fullExecutablePath;
        try {
            fullExecutablePath = decode(this.getClass().getResource("").getPath(), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            /* try without decoding if this ever happens */
            fullExecutablePath = this.getClass().getResource("").getPath();
        }
        String rootPath = Paths.get(".").toUri().normalize().getPath();
        String extractedPath = fullExecutablePath.replace(rootPath, "");
        int extractionEndIndex = extractedPath.indexOf("target/");
        if (extractionEndIndex <= 0) {
            return "";
        }
        return extractedPath.substring(0, extractionEndIndex);
    }


    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();//.getCors();
        config.setAllowedOrigins(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("*"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setExposedHeaders(Arrays.asList("Authorization","Link","X-Total-Count"));
        config.setAllowCredentials(true);
        config.setMaxAge(1880L);
        if (config.getAllowedOrigins() != null && !config.getAllowedOrigins().isEmpty()) {
            log.debug("Registering CORS filter");
            //source.registerCorsConfiguration("/api/**", config);
            //source.registerCorsConfiguration("/management/**", config);
            //source.registerCorsConfiguration("/v2/api-docs", config);
        }
        return new CorsFilter(source);
    }
    
//    /**
//     * CorsFilter
//     * @https://www.baeldung.com/spring-cors
//     */
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")     
//	        // response.setHeader("Access-Control-Allow-Origin", "*");
//	        .allowedOrigins("*")
//        	// response.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, PATCH, HEAD, OPTIONS");
//        	.allowedMethods("*")
//	        // response.addHeader("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers, Authorization, Content-Length");
//        	.allowedHeaders("Origin", "Accept", "X-Requested-With", "Content-Type", "Access-Control-Request-Method", "Access-Control-Request-Headers", "Authorization", "Content-Length")
//	        // response.addHeader("Access-Control-Expose-Headers", "Access-Control-Allow-Origin, Access-Control-Allow-Credentials");
//	        .exposedHeaders("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials")
//        	// response.addHeader("Access-Control-Allow-Credentials", "true");
//	        .allowCredentials(true)
//	        // response.setHeader("Access-Control-Max-Age", "3600");
//            .maxAge(3600);
//    }
	
	@Bean
	public ReloadableResourceBundleMessageSource messageSource() {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		String[] resources= {"classpath:messages"};
		messageSource.setBasenames(resources);
		messageSource.setFallbackToSystemLocale(false);
		return messageSource;
	}

//	@Bean
//	public ServletContextTemplateResolver defaultTemplateResolver() {
//		ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver();
//		templateResolver.setPrefix("/WEB-INF/html/");
//		templateResolver.setSuffix(".html");
//		templateResolver.setCacheable(false);
//		return templateResolver;
//	}
//
//	@Bean
//	public SpringTemplateEngine templateEngine() {
//		SpringTemplateEngine templateEngine = new SpringTemplateEngine();
//		templateEngine.setTemplateResolver(defaultTemplateResolver());
//		return templateEngine;
//	}
//
//	@Bean
//	public ThymeleafViewResolver viewResolver() {
//		ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
//		viewResolver.setTemplateEngine(templateEngine());
//		return viewResolver;
//	}
	
//	@Override
//	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
//		configurer.enable();
//	}
}