package cz.cesnet.shongo.controller.impl;

import cz.cesnet.shongo.common.api.SecurityToken;
import cz.cesnet.shongo.common.xmlrpc.TypeFactory;
import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.api.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.ReservationRequestType;
import cz.cesnet.shongo.controller.api.ReservationService;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.junit.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for using the implementation of {@link ReservationService} through XML-RPC.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationServiceImplTest extends AbstractDatabaseTest
{
    Controller controller;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        controller = new Controller();
        controller.setEntityManagerFactory(getEntityManagerFactory());
        controller.addService(new ReservationServiceImpl(new Domain("cz.cesnet")));
        controller.start();
        controller.startRpc();
    }

    @Override
    public void tearDown()
    {
        super.tearDown();

        controller.stop();
    }

    @Test
    public void testCreateReservationRequest() throws Exception
    {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(String.format("http://%s:%d", controller.getRpcHost(), controller.getRpcPort())));

        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        client.setTypeFactory(new TypeFactory(client));
        
        Map attributes = new HashMap();
        attributes.put("purpose", ReservationRequestPurpose.SCIENCE);

        List params = new ArrayList();
        params.add(new SecurityToken());
        params.add(ReservationRequestType.NORMAL);
        params.add(attributes);

        String identifier = (String) client.execute("Reservation.createReservationRequest", params);
        assertEquals("shongo:cz.cesnet:1", identifier);
    }
}
