package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.common.xmlrpc.Service;
import cz.cesnet.shongo.common.xmlrpc.WebServer;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;

/**
 * Controller class
 *
 * @author Martin Srom
 */
public class Controller implements ApplicationContextAware
{
    private static Logger logger = Logger.getLogger(Controller.class);

    /**
     * Server port
     */
    public static final int port = 8008;

    /**
     * Init controller
     */
    @PostConstruct
    public void init()
    {
        logger.info("Starting Controller XML-RPC server on port " + port + "...");

        try {
            WebServer webServer = new WebServer(port);

            // Fill services to web server
            Map<String, Service> services = applicationContext.getBeansOfType(Service.class);
            for (Service service : services.values()) {
                webServer.addHandler(service.getServiceName(), service);
            }

            // Start web server
            webServer.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Application context
     */
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    /**
     * Main controller method
     *
     * @param args
     */
    public static void main(String[] args)
    {
        // Run application by spring application context
        new ClassPathXmlApplicationContext("spring-context.xml");

        logger.info("Controller started.");
    }
}
