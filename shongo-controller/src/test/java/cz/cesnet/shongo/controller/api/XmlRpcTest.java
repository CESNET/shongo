package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Tests for using the implementation of {@link cz.cesnet.shongo.controller.api.rpc.ReservationService} through XML-RPC.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class XmlRpcTest extends AbstractControllerTest
{
    @Test
    public void testCreateReservationRequest() throws Exception
    {
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSlot("2012-06-01T15:00", "PT2H");
        reservationRequest.setSpecification(new RoomSpecification(5, Technology.H323));

        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:1", id);
        reservationRequest = getReservationRequest(id, ReservationRequest.class);
        Assert.assertEquals(ReservationRequestPurpose.SCIENCE, reservationRequest.getPurpose());
        RoomSpecification roomSpecification = (RoomSpecification) reservationRequest.getSpecification();
        Assert.assertEquals(new HashSet<Technology>()
        {{
                add(Technology.H323);
            }}, roomSpecification.getEstablishment().getTechnologies());

        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.addSlot("2012-06-01T15:00", "PT2H");
        reservationRequestSet.addSlot(new PeriodicDateTimeSlot("2012-07-01T14:00", "PT2H", "P1W"));
        CompartmentSpecification compartment = reservationRequestSet.setSpecification(new CompartmentSpecification());
        compartment.addParticipant(new InvitedPersonParticipant("Martin Srom", "srom@cesnet.cz"));
        compartment.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 2));

        id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequestSet);
        Assert.assertEquals("shongo:cz.cesnet:req:2", id);
    }

    @Test
    public void testCreateReservationRequestByRawXmlRpc() throws Exception
    {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("class", "ReservationRequest");
        attributes.put("purpose", "SCIENCE");
        attributes.put("slot", "2012-06-01T15:00/2012-06-01T17:00");
        attributes.put("specification", new HashMap<String, Object>()
        {{
                put("class", "RoomSpecification");
                put("establishment", new HashMap<String, Object>()
                {{
                    put("technologies", new ArrayList<Object>()
                    {{
                            add("H323");
                        }});
                    }});
                put("availability", new HashMap<String, Object>()
                {{
                        put("participantCount", 5);
                    }});
            }});
        List<Object> params = new ArrayList<Object>();
        params.add(SECURITY_TOKEN.getAccessToken());
        params.add(attributes);

        String id = (String) getControllerClient().execute("Reservation.createReservationRequest", params);
        Assert.assertEquals("shongo:cz.cesnet:req:1", id);
        ReservationRequest reservationRequest = getReservationRequest(id, ReservationRequest.class);
        Assert.assertEquals(ReservationRequestPurpose.SCIENCE, reservationRequest.getPurpose());
        RoomSpecification roomSpecification = (RoomSpecification) reservationRequest.getSpecification();
        Assert.assertEquals(new HashSet<Technology>()
        {{
                add(Technology.H323);
            }}, roomSpecification.getEstablishment().getTechnologies());

        attributes = new HashMap<String, Object>();
        attributes.put("class", "ReservationRequestSet");
        attributes.put("purpose", "SCIENCE");
        attributes.put("slots", new ArrayList<Object>()
        {{
                add("2012-06-01T15:00/2012-06-01T17:00");
                add(new HashMap<String, Object>()
                {{
                        put("start", "2012-07-01T14:00");
                        put("period", "P1W");
                        put("duration", "PT2H");
                    }});
            }});
        attributes.put("specification", new HashMap<String, Object>()
        {{
                put("class", "CompartmentSpecification");
                put("participants", new ArrayList<Object>()
                {{
                        add(new HashMap<String, Object>()
                        {{
                                put("class", "ExternalEndpointSetParticipant");
                                put("technologies", new ArrayList<Object>()
                                {{
                                        add("H323");
                                    }});
                                put("count", 2);
                            }});
                        add(new HashMap<String, Object>()
                        {{
                                put("class", "InvitedPersonParticipant");
                                put("person", new HashMap<String, Object>()
                                {{
                                        put("class", "AnonymousPerson");
                                        put("name", "Martin Srom");
                                        put("email", "srom@cesnet.cz");
                                    }});
                            }});

                    }});
            }});
        params = new ArrayList<Object>();
        params.add(SECURITY_TOKEN.getAccessToken());
        params.add(attributes);

        id = (String) getControllerClient().execute("Reservation.createReservationRequest", params);
        Assert.assertEquals("shongo:cz.cesnet:req:2", id);
    }

    @Test
    public void testModifyAndDeleteReservationRequest() throws Exception
    {
        String id;

        // ---------------------------
        // Create reservation request
        // ---------------------------
        {
            ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
            reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
            reservationRequestSet.addSlot("2012-06-01T15:00", "PT2H");
            reservationRequestSet.addSlot(new PeriodicDateTimeSlot("2012-07-01T14:00", "PT2H", "P1W"));
            CompartmentSpecification compartmentSpecification =
                    reservationRequestSet.setSpecification(new CompartmentSpecification());
            compartmentSpecification.addParticipant(new InvitedPersonParticipant("Martin Srom", "srom@cesnet.cz"));

            id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequestSet);
            Assert.assertNotNull(id);

            reservationRequestSet = getReservationRequest(id, ReservationRequestSet.class);
            Assert.assertNotNull(reservationRequestSet);
            Assert.assertEquals(ReservationRequestPurpose.SCIENCE, reservationRequestSet.getPurpose());
            Assert.assertEquals(2, reservationRequestSet.getSlots().size());
        }

        // ---------------------------
        // Modify reservation request
        // ---------------------------
        {
            ReservationRequestSet reservationRequestSet = getReservationRequest(id, ReservationRequestSet.class);
            reservationRequestSet.setPurpose(ReservationRequestPurpose.EDUCATION);
            reservationRequestSet.removeSlot(reservationRequestSet.getSlots().iterator().next());

            id = getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequestSet);

            reservationRequestSet = getReservationRequest(id, ReservationRequestSet.class);
            Assert.assertNotNull(reservationRequestSet);
            Assert.assertEquals(ReservationRequestPurpose.EDUCATION, reservationRequestSet.getPurpose());
            Assert.assertEquals(1, reservationRequestSet.getSlots().size());
        }

        // ---------------------------
        // Delete reservation request
        // ---------------------------
        {
            ReservationRequestSet reservationRequestSet = getReservationRequest(id, ReservationRequestSet.class);
            Assert.assertNotNull(reservationRequestSet);

            getReservationService().deleteReservationRequest(SECURITY_TOKEN, id);

            /*try {
                getReservationService().getReservationRequest(SECURITY_TOKEN, id);
                Assert.fail("Exception that record doesn't exists should be thrown.");
            }
            catch (ControllerReportSet.ReservationRequestDeletedException exception) {
                Assert.assertEquals(id, exception.getId());
            }*/
        }
    }

    @Test
    public void testExceptionSerializing() throws Exception
    {
        ResourceService resourceService = getResourceService();
        try {
            resourceService.getResource(SECURITY_TOKEN, "1");
            Assert.fail("Exception should be thrown.");
        }
        catch (CommonReportSet.ObjectNotExistsException exception) {
            Assert.assertEquals("shongo:cz.cesnet:res:1", exception.getObjectId());
        }
    }

    @Test
    public void testExceptionByRawXmlRpc() throws Exception
    {
        Map<String, Object> reservationRequest = null;

        reservationRequest = new HashMap<String, Object>();
        reservationRequest.put("class", "ReservationRequestSet");
        reservationRequest.put("purpose", "SCIENCE");
        reservationRequest.put("specification", new HashMap<String, Object>()
        {{
                put("class", "AliasSpecification");
                put("value", "1");
            }});
        reservationRequest.put("slots", new ArrayList<Object>()
        {{
                add(new HashMap<String, Object>());
            }});
        try {
            getControllerClient().execute("Reservation.createReservationRequest",
                    new Object[]{SECURITY_TOKEN.getAccessToken(), reservationRequest});
            Assert.fail("Exception that collection cannot contain null should be thrown.");
        }
        catch (XmlRpcException exception) {
            Assert.assertEquals(CommonReportSet.COLLECTION_ITEM_NULL_CODE, exception.code);
        }

        reservationRequest = new HashMap<String, Object>();
        reservationRequest.put("class", "ReservationRequestSet");
        reservationRequest.put("purpose", "SCIENCE");
        reservationRequest.put("specification", new HashMap<String, Object>()
        {{
                put("class", "AliasSpecification");
                put("value", "1");
            }});
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
            Assert.fail("Exception that attribute has wrong type should be thrown.");
        }
        catch (XmlRpcException exception) {
            Assert.assertEquals(CommonReportSet.CLASS_ATTRIBUTE_TYPE_MISMATCH_CODE, exception.code);
        }
    }
}
