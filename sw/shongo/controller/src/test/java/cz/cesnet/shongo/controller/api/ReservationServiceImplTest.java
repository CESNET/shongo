package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Technology;
import cz.cesnet.shongo.controller.*;
import org.apache.xmlrpc.XmlRpcException;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.*;

/**
 * Tests for using the implementation of {@link ReservationService} through XML-RPC.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationServiceImplTest extends AbstractDatabaseTest
{
    Controller controller;

    ControllerClient controllerClient;

    ReservationService reservationService;

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
        controllerClient = new ControllerClient(controller.getRpcHost(), controller.getRpcPort());

        // Get reservation service from client
        reservationService = controllerClient.getService(ReservationService.class);
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

        String identifier = reservationService.createReservationRequest(new SecurityToken(), reservationRequest);
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

        String identifier = (String) controllerClient.execute("Reservation.createReservationRequest", params);
        assertEquals("shongo:cz.cesnet:1", identifier);
    }

    @Test
    public void testModifyAndDeleteReservationRequest() throws Exception
    {
        SecurityToken securityToken = new SecurityToken();
        String identifier = null;

        // ---------------------------
        // Create reservation request
        // ---------------------------
        {
            ReservationRequest reservationRequest = new ReservationRequest();
            reservationRequest.setType(ReservationRequestType.NORMAL);
            reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
            reservationRequest.addSlot(DateTime.parse("2012-06-01T15:00"), Period.parse("PT2H"));
            reservationRequest.addSlot(new PeriodicDateTime(DateTime.parse("2012-07-01T14:00"), Period.parse("P1W")),
                    Period.parse("PT2H"));
            Compartment compartment = reservationRequest.addCompartment();
            compartment.addPerson("Martin Srom", "srom@cesnet.cz");

            identifier = reservationService.createReservationRequest(securityToken, reservationRequest);
            assertNotNull(identifier);

            reservationRequest = reservationService.getReservationRequest(securityToken, identifier);
            assertNotNull(reservationRequest);
            assertEquals(ReservationRequestType.NORMAL, reservationRequest.getType());
            assertEquals(ReservationRequestPurpose.SCIENCE, reservationRequest.getPurpose());
            assertEquals(2, reservationRequest.getSlots().size());
            assertEquals(1, reservationRequest.getCompartments().size());
        }

        // ---------------------------
        // Modify reservation request
        // ---------------------------
        {
            ReservationRequest reservationRequest = reservationService.getReservationRequest(securityToken, identifier);
            reservationRequest.setType(ReservationRequestType.PERMANENT);
            reservationRequest.setPurpose(null);
            reservationRequest.removeSlot(reservationRequest.getSlots().iterator().next());
            Compartment compartment = reservationRequest.addCompartment();
            reservationRequest.removeCompartment(compartment);

            reservationService.modifyReservationRequest(securityToken, reservationRequest);

            reservationRequest = reservationService.getReservationRequest(securityToken, identifier);
            assertNotNull(reservationRequest);
            assertEquals(ReservationRequestType.PERMANENT, reservationRequest.getType());
            assertEquals(null, reservationRequest.getPurpose());
            assertEquals(1, reservationRequest.getSlots().size());
            assertEquals(1, reservationRequest.getCompartments().size());
        }

        // ---------------------------
        // Delete reservation request
        // ---------------------------
        {
            ReservationRequest reservationRequest = reservationService.getReservationRequest(securityToken, identifier);
            assertNotNull(reservationRequest);

            reservationService.deleteReservationRequest(securityToken, identifier);

            try {
                reservationRequest = reservationService.getReservationRequest(securityToken, identifier);
                fail("Exception that record doesn't exists should be thrown.");
            } catch (XmlRpcException exception) {
                assertEquals(Fault.Common.RECORD_NOT_EXIST.getCode(), exception.code);
            }
        }
    }

    @Test
    public void testExceptions() throws Exception
    {
        Map<String, Object> reservationRequest = null;

        reservationRequest = new HashMap<String, Object>();
        reservationRequest.put("slots", new ArrayList<Object>()
        {{
                add(new HashMap<String, Object>());
            }});
        try {
            controllerClient.execute("Reservation.createReservationRequest",
                    new Object[]{new HashMap(), reservationRequest});
            fail("Exception that collection cannot contain null should be thrown.");
        }
        catch (XmlRpcException exception) {
            assertEquals(Fault.Common.COLLECTION_ITEM_NULL.getCode(), exception.code);
        }

        reservationRequest = new HashMap<String, Object>();
        reservationRequest.put("slots", new ArrayList<Object>()
        {{
                add(new HashMap<String, Object>()
                {{
                        put("start", "xxx");
                    }});
            }});
        try {
            controllerClient.execute("Reservation.createReservationRequest",
                    new Object[]{new HashMap(), reservationRequest});
            fail("Exception that attribute has wrong type should be thrown.");
        }
        catch (XmlRpcException exception) {
            assertEquals(Fault.Common.CLASS_ATTRIBUTE_TYPE_MISMATCH.getCode(), exception.code);
        }

        reservationRequest = new HashMap<String, Object>();
        reservationRequest.put("requests", new ArrayList<Object>());
        try {
            controllerClient.execute("Reservation.createReservationRequest",
                    new Object[]{new HashMap(), reservationRequest});
            fail("Exception that attribute is read only should be thrown.");
        }
        catch (XmlRpcException exception) {
            assertEquals(Fault.Common.CLASS_ATTRIBUTE_READ_ONLY.getCode(), exception.code);
        }
    }
}
