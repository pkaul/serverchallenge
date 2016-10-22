package de.girino.serverchallenge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.AbstractFileResolvingResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.*;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * A pseudo {@link ResourceResolver} that returns a HTML page Resource in case that the resource path denotes a directory.
 * The page contains links to directory's files.
 */
@Slf4j
class DirectoryListingResourceResolver implements ResourceResolver {

    @Override
    public Resource resolveResource(HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

        // check other resolvers of chain first
        Resource resource = chain.resolveResource(request, requestPath, locations);
        if (resource != null) {
            // found a resource -> return it.
            return resource;
        }

        // couldn't be resolved yet. Check whether requestPath denotes a directory
        for (Resource location : locations) {
            try {
                resource = location.createRelative(requestPath);
                if (isDirectory(resource)) {
                    // it's a directory! Return a pseudo resource that represents the directory listing.
                    return new DirectoryListingResource(resource);
                }
            } catch (IOException e) {
                // Nothing more to do here. ignore.
                log.debug("Error resolving resource to a directory", e);
            }
        }

        // can't resolve at all. give up.
        return null;
    }

    @Override
    public String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolverChain chain) {
        // not supported (but also not called)
        throw new UnsupportedOperationException();
    }

    // =============

    /**
     * Checks whether a given resource represents a directory
     */
    private boolean isDirectory(Resource resource) throws IOException {
        return resource instanceof AbstractFileResolvingResource && resource.getFile() != null && resource.getFile().isDirectory();
    }

    /**
     * Building HTML for a given directory.
     */
    private static String buildHTML(File directory) {

        // using plain string builder here but a more fancy solution might use templating (e.g. Freemarker) here.
        StringBuilder result = new StringBuilder();
        result.append("<html><body>");

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            result.append("<p>empty directory</p>");
        } else {

            result.append("<ul>");
            for (File file : directory.listFiles()) {

                String name = HtmlUtils.htmlEscape(file.getName());
                String trailingSlash = file.isDirectory() ? "/" : "";

                result.append(String.format("<li><a href=\"./%s%s\">%s%s</a></li>", name, trailingSlash, name, trailingSlash));
            }
            result.append("</ul>");
        }

        result.append("</body></html>");
        return result.toString();
    }

    // =============================

    /**
     * Encapsulates an existing directory (represented as {@link Resource} or {@link AbstractFileResolvingResource} respectively)
     * as a HTML Resource containing directory links.
     */
    private static class DirectoryListingResource implements Resource {

        private static final String ENCODING = "UTF-8";
        private final Resource delegate;

        public DirectoryListingResource(Resource resource) {
            this.delegate = resource;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(buildHTML(delegate.getFile()).getBytes(ENCODING));
        }

        @Override
        public long contentLength() throws IOException {
            return -1;  // unknown length
        }

        @Override
        public String getFilename() {
            // tricky: providing the directory name with an *.html suffix so that content type resolver chooses text/html
            return String.format("%s.html", delegate.getFilename());
        }

        @Override
        public boolean exists() {
            return delegate.exists();
        }

        @Override
        public boolean isReadable() {
            return delegate.isReadable();
        }

        @Override
        public boolean isOpen() {
            return delegate.isOpen();
        }

        @Override
        public URL getURL() throws IOException {
            return delegate.getURL();
        }

        @Override
        public URI getURI() throws IOException {
            return delegate.getURI();
        }

        @Override
        public File getFile() throws IOException {
            return delegate.getFile();
        }

        @Override
        public long lastModified() throws IOException {
            return delegate.lastModified();
        }

        @Override
        public Resource createRelative(String relativePath) throws IOException {
            return delegate.createRelative(relativePath);
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }


    }


}
