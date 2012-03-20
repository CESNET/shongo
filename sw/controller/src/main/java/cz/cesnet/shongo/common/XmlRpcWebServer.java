package cz.cesnet.shongo.common;

import org.apache.xmlrpc.server.XmlRpcStreamServer;

/**
 * XmlRpc WebServer with improved type factory
 *
 * @author Martin Srom
 */
public class XmlRpcWebServer extends org.apache.xmlrpc.webserver.WebServer {
    public XmlRpcWebServer(int pPort) {
        super(pPort);
    }
    protected XmlRpcStreamServer newXmlRpcStreamServer() {
        XmlRpcStreamServer server = super.newXmlRpcStreamServer();
        server.setTypeFactory(new XmlRpcTypeFactory(server));
        return server;
    }
}
