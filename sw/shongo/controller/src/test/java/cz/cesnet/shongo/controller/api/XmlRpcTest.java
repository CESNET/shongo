package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestType;
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
public class XmlRpcTest extends AbstractControllerTest
{
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
        compartment.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 2));

        String identifier = getReservationService().createReservationRequest(SECURITY_TOKEN,
                reservationRequestSet);
        assertEquals("shongo:cz.cesnet:1", identifier);
    }

    @Test
    public void testCreateReservationRequestByRawXmlRpc() throws Exception
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
                                        put("class", "ExternalEndpointSetSpecification");
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
        params.add(SECURITY_TOKEN.getAccessToken());
        params.add(attributes);

        String identifier = (String) getControllerClient().execute("Reservation.createReservationRequest", params);
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

            identifier = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequestSet);
            assertNotNull(identifier);

            reservationRequestSet = (ReservationRequestSet) getReservationService().getReservationRequest(
                    SECURITY_TOKEN, identifier);
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
                    (ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN,
                            identifier);
            reservationRequestSet.setType(ReservationRequestType.PERMANENT);
            reservationRequestSet.setPurpose(null);
            reservationRequestSet.removeSlot(reservationRequestSet.getSlots().iterator().next());
            CompartmentSpecification compartmentSpecification =
                    reservationRequestSet.addSpecification(new CompartmentSpecification());
            reservationRequestSet.removeSpecification(compartmentSpecification);

            getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequestSet);

            reservationRequestSet = (ReservationRequestSet) getReservationService().getReservationRequest(
                    SECURITY_TOKEN, identifier);
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
                    (ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN,
                            identifier);
            assertNotNull(reservationRequestSet);

            getReservationService().deleteReservationRequest(SECURITY_TOKEN, identifier);

            try {
                reservationRequestSet = (ReservationRequestSet) getReservationService().getReservationRequest(
                        SECURITY_TOKEN, identifier);
                fail("Exception that record doesn't exists should be thrown.");
            }
            catch (EntityNotFoundException exception) {
            }
        }
    }

    @Test
    public void testExceptionSerializing() throws Exception
    {
        ResourceService resourceService = getResourceService();
        try {
            resourceService.getResource(SECURITY_TOKEN, "1");
            fail(EntityNotFoundException.class.getSimpleName() + " should be thrown.");
        }
        catch (EntityNotFoundException exception) {
            assertEquals("1", exception.getEntityIdentifier());
            assertEquals(Resource.class, exception.getEntityType());
        }
    }

    @Test
    public void testExceptionByRawXmlRpc() throws Exception
    {
        Map<String, Object> reservationRequest = null;

        reservationRequest = new HashMap<String, Object>();
        reservationRequest.put("class", "ReservationRequestSet");
        reservationRequest.put("slots", new ArrayList<Object>()
        {{
                add(new HashMap<String, Object>());
            }});
        try {
            getControllerClient().execute("Reservation.createReservationRequest",
                    new Object[]{SECURITY_TOKEN.getAccessToken(), reservationRequest});
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
            getControllerClient().execute("Reservation.createReservationRequest",
                    new Object[]{SECURITY_TOKEN.getAccessToken(), reservationRequest});
            fail("Exception that attribute has wrong type should be thrown.");
        }
        catch (XmlRpcException exception) {
            assertEquals(CommonFault.CLASS_ATTRIBUTE_TYPE_MISMATCH.getCode(), exception.code);
        }

        reservationRequest = new HashMap<String, Object>();
        reservationRequest.put("class", "ReservationRequestSet");
        reservationRequest.put("reservationRequests", new ArrayList<Object>());
        try {
            getControllerClient().execute("Reservation.createReservationRequest",
                    new Object[]{SECURITY_TOKEN.getAccessToken(), reservationRequest});
            fail("Exception that attribute is read only should be thrown.");
        }
        catch (XmlRpcException exception) {
            assertEquals(CommonFault.CLASS_ATTRIBUTE_READ_ONLY.getCode(), exception.code);
        }
    }
}
