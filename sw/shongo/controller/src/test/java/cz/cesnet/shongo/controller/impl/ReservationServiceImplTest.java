package cz.cesnet.shongo.controller.impl;

import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.common.xmlrpc.TypeFactory;
import cz.cesnet.shongo.controller.*;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.util.ClientFactory;
import org.joda.time.DateTime;
import org.joda.time.Period;
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

    XmlRpcClient client;

    @Override
    public void before() throws Exception
    {
        super.before();

        // Start controller
        controller = new Controller();
        controller.setEntityManagerFactory(getEntityManagerFactory());
        controller.addService(new ReservationServiceImpl(new Domain("cz.cesnet")));
        controller.start();
        controller.startRpc();

        // Start client
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(String.format("http://%s:%d", controller.getRpcHost(), controller.getRpcPort())));
        client = new XmlRpcClient();
        client.setConfig(config);
        client.setTypeFactory(new TypeFactory(client));
    }

    @Override
    public void after()
    {
        controller.stop();

        super.after();
    }

    @Test
    public void testCreateReservationRequest() throws Exception
    {
        SecurityToken securityToken = new SecurityToken();

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.addSlot(DateTime.parse("2012-06-01T15:00"), Period.parse("PT2H"));
        reservationRequest.addSlot(new PeriodicDateTime(DateTime.parse("2012-07-01T14:00"), Period.parse("P1W")),
                Period.parse("PT2H"));
        Compartment compartment = reservationRequest.addCompartment();
        compartment.addPerson("Martin Srom", "srom@cesnet.cz");
        compartment.addResource(Technology.H323, 2, new Person[]{
                new Person("Ondrej Bouda", "bouda@cesnet.cz"),
                new Person("Petr Holub", "hopet@cesnet.cz")
        });

        ClientFactory factory = new ClientFactory(client);
        ReservationService reservationService = (ReservationService) factory.newInstance(ReservationService.class);

        String identifier = reservationService.createReservationRequest(securityToken, reservationRequest);
        assertEquals("shongo:cz.cesnet:1", identifier);
    }

    @Test
    public void testCreateReservationRequestByRawRpcXml() throws Exception
    {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("type", "NORMAL");
        attributes.put("purpose", "SCIENCE");
        attributes.put("slots", new ArrayList<Object>()
        {{
                add(new HashMap<String, Object>()
                {{
                        put("start", "2012-06-01T15:00");
                        put("duration", "PT2H");
                    }});
                add(new HashMap<String, Object>()
                {{
                        put("start", new HashMap<String, Object>()
                        {{
                                put("start", "2012-07-01T14:00");
                                put("period", "P1W");
                            }});
                        put("duration", "PT2H");
                    }});
            }});
        attributes.put("compartments", new ArrayList<Object>()
        {{
                add(new HashMap<String, Object>()
                {{
                        put("persons", new ArrayList<Object>()
                        {{
                                add(new HashMap<String, Object>()
                                {{
                                        put("name", "Martin Srom");
                                        put("email", "srom@cesnet.cz");
                                    }});
                            }});
                        put("resources", new ArrayList<Object>()
                        {{
                                add(new HashMap<String, Object>()
                                {{
                                        put("technology", "H323");
                                        put("count", 2);
                                        put("persons", new ArrayList<Object>()
                                        {{
                                                add(new HashMap<String, Object>()
                                                {{
                                                        put("name", "Ondrej Bouda");
                                                        put("email", "bouda@cesnet.cz");
                                                    }});
                                                add(new HashMap<String, Object>()
                                                {{
                                                        put("name", "Petr Holub");
                                                        put("email", "hopet@cesnet.cz");
                                                    }});
                                            }});
                                    }});

                            }});
                    }});
            }});

        List<Object> params = new ArrayList<Object>();
        params.add(new HashMap<String, Object>());
        params.add(attributes);

        String identifier = (String) client.execute("Reservation.createReservationRequest", params);
        assertEquals("shongo:cz.cesnet:1", identifier);
    }
}
