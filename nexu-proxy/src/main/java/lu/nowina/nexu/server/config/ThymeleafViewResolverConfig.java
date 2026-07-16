package lu.nowina.nexu.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.spring4.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;
//import org.thymeleaf.spring5.SpringTemplateEngine;
//import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
//import org.thymeleaf.spring5.view.ThymeleafViewResolver;
//import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

/**
 * @href https://o7planning.org/en/11257/using-multiple-viewresolvers-in-spring-boot
 * 
 * In this application, we configure the  Thymeleaf ViewResolver with the highest priority (order = 1).
 * Note: The Thymeleaf ViewResolver will throw an exception when it does not find the required " View Name" (A necessary html file). 
 * It is contrary to your wish that the ViewResolver with succeeding priority will be used. Therefore, you need to set up rules for the " View Names" 
 * to be served by the Thymeleaf ViewResolver. 
 */
@Configuration
public class ThymeleafViewResolverConfig {
	
	// ===============================================
    // THYMELEAF
    // ===============================================
    
    @Bean
    @Description("Thymeleaf view resolver")
    public ThymeleafViewResolver thymeleafViewResolver() {
        ThymeleafViewResolver resolver = new ThymeleafViewResolver();
        resolver.setTemplateEngine(thymeleafTemplateEngine());
        resolver.setCharacterEncoding("UTF-8");
        resolver.setOrder(1);
        // Important!!
        // th_page1.html, th_page2.html, ...
        resolver.setViewNames(new String[] { "th_*" });
        return resolver;
    }
    
    /**
     * Thymeleaf template engine with Spring integration
     * @return
     */
    @Bean
    @Description("Thymeleaf template engine with Spring integration")
    public SpringTemplateEngine thymeleafTemplateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        //engine.setEnableSpringELCompiler(true);
        engine.addTemplateResolver(thymeleafTemplateResolver());
        
        // Resolver for TEXT emails
        //engine.addTemplateResolver(textTemplateResolver());
        // Resolver for HTML emails (except the editable one)
        //engine.addTemplateResolver(htmlTemplateResolver());
        // Resolver for HTML editable emails (which will be treated as a String)
        //engine.addTemplateResolver(stringTemplateResolver());
        // Message source, internationalization specific to emails
        //engine.setTemplateEngineMessageSource(emailMessageSource());

        return engine;
    }
    
    @Bean
    public SpringResourceTemplateResolver springResourceTemplateResolver() {
        return new SpringResourceTemplateResolver();
    }
    
    @Bean
    @Description("Thymeleaf template resolver serving HTML 5")
    public ITemplateResolver thymeleafTemplateResolver() {
        //SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
    	//resolver.setApplicationContext(applicationContext);
    	ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();       
        // Folder containing FreeMarker templates.
        // 1 - "/WEB-INF/views/"
        // 2 - "classpath:/templates"
        //resolver.setPrefix("classpath:/templates/");
    	//resolver.setPrefix("templates/");
    	resolver.setPrefix("templates/thymeleaf/");
        resolver.setSuffix(".html");
        //resolver.setTemplateMode(TemplateMode.HTML);
        //  Template Mode 'HTML5' is deprecated. Using Template Mode 'HTML' instead.
        //resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setTemplateMode("HTML5"); 
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        resolver.setOrder(2);
        return resolver;
    }
    
	@Bean
	public ServletContextTemplateResolver defaultTemplateResolver() {
		ServletContextTemplateResolver resolver = new ServletContextTemplateResolver();
    	resolver.setPrefix("templates/thymeleaf/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setOrder(1);
        resolver.setCacheable(false);
        return resolver;
	}

	@Bean
	public SpringTemplateEngine templateEngine() {
		SpringTemplateEngine templateEngine = new SpringTemplateEngine();
		templateEngine.setTemplateResolver(defaultTemplateResolver());
		return templateEngine;
	}

	@Bean
	public ThymeleafViewResolver viewResolver() {
		ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
		viewResolver.setTemplateEngine(templateEngine());
		return viewResolver;
	}

}
