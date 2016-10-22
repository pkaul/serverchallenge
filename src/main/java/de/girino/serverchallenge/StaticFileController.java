package de.girino.serverchallenge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.handler.MappedInterceptor;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * A (pseudo-) controller for serving static files from a given base directory. This implementation mainly
 * sets up an {@link org.springframework.web.servlet.resource.ResourceHttpRequestHandler} that already
 * deals with lots of HTTP specific requirements.
 *
 * See https://spring.io/blog/2014/07/24/spring-framework-4-1-handling-static-web-resources for an overview
 */
@Controller
@Slf4j
@Configuration
class StaticFileController extends WebMvcConfigurerAdapter {

    /**
     * URI pattern to be mapped to static resources.
     */
    private static final String URI_PATTERN = "/**";

    /**
   	 * Base dir of static files (will be mapped from application.properties or system property)
   	 */
   	@Value("${documentroot}")
   	String baseDir;


    /**
     * Configures delivery of static resources via {@link org.springframework.web.servlet.resource.ResourceHttpRequestHandler}
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        String directory = normalizeDirectory(baseDir);

        // Register mapping from URI to local directory. Also enable directory listing.
        registry.addResourceHandler(URI_PATTERN).
                addResourceLocations(directory).
                resourceChain(true).addResolver(new DirectoryListingResourceResolver()); // for directory listing

        log.info("*************** Serving files from {} *************", directory);
    }


    /**
     * Enabling ETag (in general).
     * Note that this ETag implementation has some limitations:
     * <ul>
     *     <li>{@link ShallowEtagHeaderFilter only saves bandwidth, not server performance}</li>
     *     <li>
     *         Since content will be loaded in the main memory in order to compute the ETag hash, this implementation will
     *         have issues with large files. See https://jira.spring.io/browse/SPR-10855.
     *     </li>
     * </ul>
     */
    @Bean
    public Filter shallowEtagHeaderFilter() {
      return new ShallowEtagHeaderFilter();
    }


    /**
     * Workaround: File handler's root resource (http://host/) is internally translated into
     * an empty path ("") that is always handled by ResourceHttpRequestHandler with 404. Simply translating this path into
     * a slash ("/") so that it is handled properly
     */
    @Bean
    public MappedInterceptor pathAdjustingInterceptor() {

        return new MappedInterceptor(new String[]{URI_PATTERN}, new HandlerInterceptorAdapter() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

                // if path is empty -> replace by slash
                String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
                if( path != null && path.length() == 0 ) {
                    request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/");
                }
                return true;
            }
        });
    }

    // =================

    /**
     * Replaces OS specific directory separators by slash and adds trailing slash (if not already there). This
     * is necessary for above's ResourceHandlerRegistration to be working properly
     */
    private String normalizeDirectory(String directory) {
        return directory.replace(File.pathSeparatorChar, '/')+(directory.endsWith("/") ? "" : "/");
    }

}
