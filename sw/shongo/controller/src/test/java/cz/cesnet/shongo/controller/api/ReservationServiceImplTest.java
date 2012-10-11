package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.EntityNotFoundException;
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
    private cz.cesnet.shongo.controller.Controller controller;

    private ControllerClient controllerClient;

    private ReservationService reservationService;

    private static final SecurityToken TESTING_SECURITY_TOKEN =
            new SecurityToken("18eea565098d4620d398494b111cb87067a3b6b9");

    @Override
    public void before() throws Exception
    {
        super.before();

        // Change XML-RPC port
        System.setProperty(Configuration.RPC_PORT, "8484");

        // Start controller
        controller = new cz.cesnet.shongo.controller.Controller();
        controller.setDomain("cz.cesnet", "CESNET, z.s.p.o.");
        controller.setEntityManagerFactory(getEntityManagerFactory());
        controller.addService(new ReservationServiceImpl());
        controller.start();
        controller.startRpc();
        controller.getAuthorization().setTestingAccessToken(TESTING_SECURITY_TOKEN.getAccessToken());

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
        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setType(ReservationRequestType.NORMAL);
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.addSlot(DateTime.parse("2012-06-01T15:00"), Period.parse("PT2H"));
        reservationRequestSet.addSlot(new PeriodicDateTime(DateTime.parse("2012-07-01T14:00"), Period.parse("P1W")),
                Period.parse("PT2H"));
        CompartmentSpecification compartment = reservationRequestSet.addSpecification(new CompartmentSpecification());
        compartment.addSpecification(new PersonSpecification("Martin Srom", "srom@cesnet.cz"));
        compartment.addSpecification(new ExternalEndpointSpecification(Technology.H323, 2));

        String identifier = reservationService.createReservationRequest(TESTING_SECURITY_TOKEN, reservationRequestSet);
        assertEquals("shongo:cz.cesnet:1", identifier);
    }

    @Test
    public void testCreateReservationRequestByRawRpcXml() throws Exception
    {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("class", "ReservationRequestSet");
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
        attributes.put("specifications", new ArrayList<Object>()
        {{
                add(new HashMap<String, Object>()
                {{
                        put("class", "CompartmentSpecification");
                        put("specifications", new ArrayList<Object>()
                        {{
                                add(new HashMap<String, Object>()
                                {{
                                        put("class", "ExternalEndpointSpecification");
                                        put("technology", "H323");
                                        put("count", 2);
                                    }});
                                add(new HashMap<String, Object>()
                                {{
                                        put("class", "PersonSpecification");
                                        put("person", new HashMap<String, Object>()
                                        {{
                                                put("name", "Martin Srom");
                                                put("email", "srom@cesnet.cz");
                                            }});
                                    }});

                            }});
                    }});
            }});

        List<Object> params = new ArrayList<Object>();
        params.add(TESTING_SECURITY_TOKEN.getAccessToken());
        params.add(attributes);

        String identifier = (String) controllerClient.execute("Reservation.createReservationRequest", params);
        assertEquals("shongo:cz.cesnet:1", identifier);
    }

    @Test
    public void testModifyAndDeleteReservationRequest() throws Exception
    {
        String identifier = null;

        // ---------------------------
        // Create reservation request
        // ---------------------------
        {
            ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
            reservationRequestSet.setType(ReservationRequestType.NORMAL);
            reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
            reservationRequestSet.addSlot(DateTime.parse("2012-06-01T15:00"), Period.parse("PT2H"));
            reservationRequestSet.addSlot(new PeriodicDateTime(DateTime.parse("2012-07-01T14:00"), Period.parse("P1W")),
                    Period.parse("PT2H"));
            CompartmentSpecification compartmentSpecification =
                    reservationRequestSet.addSpecification(new CompartmentSpecification());
            compartmentSpecification.addSpecification(new PersonSpecification("Martin Srom", "srom@cesnet.cz"));

            identifier = reservationService.createReservationRequest(TESTING_SECURITY_TOKEN, reservationRequestSet);
            assertNotNull(identifier);

            reservationRequestSet = (ReservationRequestSet) reservationService.getReservationRequest(
                    TESTING_SECURITY_TOKEN, identifier);
            assertNotNull(reservationRequestSet);
            assertEquals(ReservationRequestType.NORMAL, reservationRequestSet.getType());
            assertEquals(ReservationRequestPurpose.SCIENCE, reservationRequestSet.getPurpose());
            assertEquals(2, reservationRequestSet.getSlots().size());
            assertEquals(1, reservationRequestSet.getSpecifications().size());
        }

        // ---------------------------
        // Modify reservation request
        // ---------------------------
        {
            ReservationRequestSet reservationRequestSet =
                    (ReservationRequestSet) reservationService.getReservationRequest(TESTING_SECURITY_TOKEN,
                            identifier);
            reservationRequestSet.setType(ReservationRequestType.PERMANENT);
            reservationRequestSet.setPurpose(null);
            reservationRequestSet.removeSlot(reservationRequestSet.getSlots().iterator().next());
            CompartmentSpecification compartmentSpecification =
                    reservationRequestSet.addSpecification(new CompartmentSpecification());
            reservationRequestSet.removeSpecification(compartmentSpecification);

            reservationService.modifyReservationRequest(TESTING_SECURITY_TOKEN, reservationRequestSet);

            reservationRequestSet = (ReservationRequestSet) reservationService.getReservationRequest(
                    TESTING_SECURITY_TOKEN, identifier);
            assertNotNull(reservationRequestSet);
            assertEquals(ReservationRequestType.PERMANENT, reservationRequestSet.getType());
            assertEquals(null, reservationRequestSet.getPurpose());
            assertEquals(1, reservationRequestSet.getSlots().size());
            assertEquals(1, reservationRequestSet.getSpecifications().size());
        }

        // ---------------------------
        // Delete reservation request
        // ---------------------------
        {
            ReservationRequestSet reservationRequestSet =
                    (ReservationRequestSet) reservationService.getReservationRequest(TESTING_SECURITY_TOKEN,
                            identifier);
            assertNotNull(reservationRequestSet);

            reservationService.deleteReservationRequest(TESTING_SECURITY_TOKEN, identifier);

            try {
                reservationRequestSet = (ReservationRequestSet) reservationService.getReservationRequest(
                        TESTING_SECURITY_TOKEN, identifier);
                fail("Exception that record doesn't exists should be thrown.");
            }
            catch (EntityNotFoundException exception) {
            }
        }
    }

    @Test
    public void testExceptions() throws Exception
    {
        Map<String, Object> reservationRequest = null;

        reservationRequest = new HashMap<String, Object>();
        reservationRequest.put("class", "ReservationRequestSet");
        reservationRequest.put("slots", new ArrayList<Object>()
        {{
                add(new HashMap<String, Object>());
            }});
        try {
            controllerClient.execute("Reservation.createReservationRequest",
                    new Object[]{TESTING_SECURITY_TOKEN.getAccessToken(), reservationRequest});
            fail("Exception that collection cannot contain null should be thrown.");
        }
        catch (XmlRpcException exception) {
            assertEquals(CommonFault.COLLECTION_ITEM_NULL.getCode(), exception.code);
        }

        reservationRequest = new HashMap<String, Object>();
        reservationRequest.put("class", "ReservationRequestSet");
        reservationRequest.put("slots", new ArrayList<Object>()
        {{
                add(new HashMap<String, Object>()
                {{
                        put("start", "xxx");
                    }});
            }});
        try {
            controllerClient.execute("Reservation.createReservationRequest",
                    new Object[]{TESTING_SECURITY_TOKEN.getAccessToken(), reservationRequest});
            fail("Exception that attribute has wrong type should be thrown.");
        }
        catch (XmlRpcException exception) {
            assertEquals(CommonFault.CLASS_ATTRIBUTE_TYPE_MISMATCH.getCode(), exception.code);
        }

        reservationRequest = new HashMap<String, Object>();
        reservationRequest.put("class", "ReservationRequestSet");
        reservationRequest.put("reservationRequests", new ArrayList<Object>());
        try {
            controllerClient.execute("Reservation.createReservationRequest",
                    new Object[]{TESTING_SECURITY_TOKEN.getAccessToken(), reservationRequest});
            fail("Exception that attribute is read only should be thrown.");
        }
        catch (XmlRpcException exception) {
            assertEquals(CommonFault.CLASS_ATTRIBUTE_READ_ONLY.getCode(), exception.code);
        }
    }
}
