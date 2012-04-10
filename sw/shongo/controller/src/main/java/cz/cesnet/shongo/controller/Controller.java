package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.common.xmlrpc.WebServer;

import java.io.IOException;

/**
 * Controller class
 *
 * @author Martin Srom
 */
public class Controller
{
    /**
     * Server port
     */
    public static final int port = 8008;

    /**
     * Main controller method
     *
     * @param args
     */
    public static void main(String[] args)
    {
        try {
            WebServer webServer = new WebServer(port, "xmlrpc.properties");
            webServer.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
