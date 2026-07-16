package lu.nowina.nexu.server;

import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
//import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * This is a helper Java class that provides an alternative to creating a {@code web.xml}.
 * This will be invoked only when the application is deployed to a Servlet container like Tomcat, JBoss etc.
 */
public class ApplicationWebXml extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SpringBootNexuProxyWebApplication.class);
    }
    
//    /**
//     * @href https://stackoverflow.com/questions/31791587/spring-boot-remove-jsessionid-from-url
//     * Metodo di utility fa l'iverride delle configurazioni di tomcat equivale alle proprieta':
//     * server.servlet.session.cookie.http-only=true
//     * server.servlet.session.tracking-modes=cookie
//     */
//    @Override
//    public void onStartup(ServletContext servletContext) throws ServletException {
//        // This can be done here or as the last step in the method
//        // Doing it in this order will initialize the Spring
//        // Framework first, doing it as last step will initialize
//        // the Spring Framework after the Servlet configuration is 
//        // established
//        super.onStartup(servletContext);
//
//        // This will set to use COOKIE only
//        servletContext
//            .setSessionTrackingModes(
//                Collections.singleton(SessionTrackingMode.COOKIE)
//        );
//        // This will prevent any JS on the page from accessing the
//        // cookie - it will only be used/accessed by the HTTP transport
//        // mechanism in use
//        SessionCookieConfig sessionCookieConfig=
//                servletContext.getSessionCookieConfig();
//        sessionCookieConfig.setHttpOnly(true);
//    }
   
}
