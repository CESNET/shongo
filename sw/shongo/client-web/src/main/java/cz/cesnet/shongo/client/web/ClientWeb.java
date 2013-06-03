package cz.cesnet.shongo.client.web;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.net.URL;
import java.security.ProtectionDomain;

/**
 * Shongo web client application.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ClientWeb
{
    public static void main(final String[] arguments) throws Exception
    {
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setDescriptor("WEB-INF/web.xml");
        webAppContext.setContextPath("/");
        webAppContext.setParentLoaderPriority(true);
        if(arguments.length > 0 && new File(arguments[0] + "/WEB-INF/web.xml").exists()) {
            webAppContext.setResourceBase(arguments[0]);
        }
        else {
            webAppContext.setResourceBase(new ClassPathResource(".").getURI().toString());
        }

        final Server server = new Server(9000);
        server.setHandler(webAppContext);
        server.start();
    }
}
