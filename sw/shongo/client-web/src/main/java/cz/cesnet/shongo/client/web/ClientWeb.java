package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Shongo web client application.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ClientWeb
{
    public static void main(final String[] arguments) throws Exception
    {
        // Setup class-path for JAR file
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader instanceof URLClassLoader) {
            URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
            URL[] urls = urlClassLoader.getURLs();
            // Only when current class-path is single JAR file
            if (urls.length == 1 && urls[0].toExternalForm().endsWith(".jar")) {
                // Get directory from single JAR file
                File mainFile = new File(urls[0].toExternalForm());
                String path = mainFile.getParent();

                // Read class-path from manifest
                InputStream manifestStream = classLoader.getResourceAsStream("META-INF/MANIFEST.MF");
                Manifest manifest = new Manifest(manifestStream);
                String manifestClassPath = manifest.getMainAttributes().getValue("Class-Path");

                // Setup new class loader from the manifest class-path
                List<URL> newUrls = new LinkedList<URL>();
                for (String library : manifestClassPath.split(" ")) {
                    URL url = new URL(path + "/" + library);
                    newUrls.add(url);
                }
                urlClassLoader = new URLClassLoader(newUrls.toArray(new URL[newUrls.size()]));
                Thread.currentThread().setContextClassLoader(urlClassLoader);
            }
        }

        ConfiguredSSLContext.getInstance().addTrustedHostMapping("shongo-auth-dev.cesnet.cz", "hroch.cesnet.cz");

        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setDescriptor("WEB-INF/web.xml");
        webAppContext.setContextPath("/");
        webAppContext.setParentLoaderPriority(true);
        if (arguments.length > 0 && new File(arguments[0] + "/WEB-INF/web.xml").exists()) {
            webAppContext.setResourceBase(arguments[0]);
        }
        else {
            URL resourceBaseUrl = ClientWeb.class.getClassLoader().getResource("WEB-INF");
            if (resourceBaseUrl == null) {
                throw new RuntimeException("WEB-INF is not in classpath.");
            }
            String resourceBase = resourceBaseUrl.toExternalForm().replace("/WEB-INF", "/");
            webAppContext.setResourceBase(resourceBase);
        }

        final Server server = new Server(8182);
        server.setHandler(webAppContext);
        server.start();
    }
}
