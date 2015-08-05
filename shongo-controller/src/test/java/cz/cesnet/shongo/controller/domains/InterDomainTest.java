package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.DomainResource;
import cz.cesnet.shongo.controller.api.domains.response.*;
import cz.cesnet.shongo.controller.api.domains.response.Reservation;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import cz.cesnet.shongo.ssl.SSLCommunication;
import org.codehaus.jackson.map.Module;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.ext.JodaSerializers;
import org.codehaus.jackson.map.module.SimpleModule;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.datetime.joda.DateTimeFormatterFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

/**
 * Tests for Inter Domain Protocol.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class InterDomainTest extends AbstractControllerTest
{
    private static final String INTERDOMAIN_LOCAL_HOST = "localhost";
    private static final Integer INTERDOMAIN_LOCAL_PORT = 8443;
    private static final String INTERDOMAIN_LOCAL_PASSWORD = "shongo_test";
    private static final String INTERDOMAIN_LOCAL_PASSWORD_HASH = SSLCommunication.hashPassword(INTERDOMAIN_LOCAL_PASSWORD.getBytes());
    private static final String TEST_CERT_PATH = "./shongo-controller/src/test/resources/keystore/server.crt";

    private Domain loopbackDomain;

    @Override
    public void before() throws Exception
    {
        System.setProperty(ControllerConfiguration.INTERDOMAIN_HOST, INTERDOMAIN_LOCAL_HOST);
        System.setProperty(ControllerConfiguration.INTERDOMAIN_PORT, INTERDOMAIN_LOCAL_PORT.toString());
        System.setProperty(ControllerConfiguration.INTERDOMAIN_SSL_KEY_STORE, "./shongo-controller/src/test/resources/keystore/server.p12");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_SSL_KEY_STORE_PASSWORD, "shongo");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_SSL_KEY_STORE_TYPE, "PKCS12");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_PKI_CLIENT_AUTH, "false");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_COMMAND_TIMEOUT, "PT10S");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_BASIC_AUTH_PASSWORD, INTERDOMAIN_LOCAL_PASSWORD);
        System.setProperty(ControllerConfiguration.INTERDOMAIN_CACHE_REFRESH_RATE, "PT10S");

        super.before();

        loopbackDomain = new Domain();
        loopbackDomain.setName(LocalDomain.getLocalDomainName());
        loopbackDomain.setOrganization("CESNET z.s.p.o.");
        loopbackDomain.setAllocatable(true);
        loopbackDomain.setCertificatePath(TEST_CERT_PATH);
        DeviceAddress deviceAddress = new DeviceAddress(INTERDOMAIN_LOCAL_HOST, INTERDOMAIN_LOCAL_PORT);
        loopbackDomain.setDomainAddress(deviceAddress);
        loopbackDomain.setPasswordHash(INTERDOMAIN_LOCAL_PASSWORD_HASH);
        String domainId = getResourceService().createDomain(SECURITY_TOKEN_ROOT, loopbackDomain);
        loopbackDomain.setId(domainId);
    }

    @After
    public void tearDown() throws Exception
    {
        System.clearProperty(ControllerConfiguration.INTERDOMAIN_HOST);
        System.clearProperty(ControllerConfiguration.INTERDOMAIN_PORT);
        System.clearProperty(ControllerConfiguration.INTERDOMAIN_SSL_KEY_STORE);
        System.clearProperty(ControllerConfiguration.INTERDOMAIN_SSL_KEY_STORE_PASSWORD);
        System.clearProperty(ControllerConfiguration.INTERDOMAIN_SSL_KEY_STORE_TYPE);
        System.clearProperty(ControllerConfiguration.INTERDOMAIN_PKI_CLIENT_AUTH);
        System.clearProperty(ControllerConfiguration.INTERDOMAIN_COMMAND_TIMEOUT);
        System.clearProperty(ControllerConfiguration.INTERDOMAIN_BASIC_AUTH_PASSWORD);
        System.clearProperty(ControllerConfiguration.INTERDOMAIN_CACHE_REFRESH_RATE);

        super.after();
    }

    /**
     * Test of basic authentication on loopback domain
     */
    @Test
    public void testBasicAuthLogin()
    {
        getConnector().login(loopbackDomain);
        List<Domain> domains = getConnector().getForeignDomainsStatuses();
        Assert.assertEquals(Domain.Status.AVAILABLE, domains.get(0).getStatus());
    }

    /**
     * Test of 2 domains of which one is not accessible.
     */
    @Test
    public void testUnavailableDomain()
    {
        Domain unavailableDomain = new Domain();
        unavailableDomain.setName(LocalDomain.getLocalDomainName()+".unavailable");
        unavailableDomain.setOrganization("CESNET z.s.p.o.");
        unavailableDomain.setAllocatable(true);
        DeviceAddress deviceAddress = new DeviceAddress("none", INTERDOMAIN_LOCAL_PORT);
        unavailableDomain.setDomainAddress(deviceAddress);
        unavailableDomain.setPasswordHash("none");
        getResourceService().createDomain(SECURITY_TOKEN_ROOT, unavailableDomain);

        // Should return 2 domain statuses
        List<Domain> domains = getConnector().getForeignDomainsStatuses();
        Assert.assertEquals(2, domains.size());

        // Should return only one empty list of domain capabilities
        DomainCapabilityListRequest listRequest = new DomainCapabilityListRequest(DomainCapabilityListRequest.Type.RESOURCE);
        Map<String, List<DomainCapability>> domainCapabilities = getConnector().listForeignCapabilities(listRequest);
        Assert.assertEquals(1, domainCapabilities.size());
    }

    /**
     * Test of action {@link cz.cesnet.shongo.controller.api.domains.InterDomainAction#DOMAIN_RESOURCES_LIST}
     * for some types of resources/capabilities
     */
    @Test
    public void testListForeignCapabilities()
    {
        DomainResource mcuDomainResource = new DomainResource();
        mcuDomainResource.setPrice(1);
        mcuDomainResource.setLicenseCount(10);
        mcuDomainResource.setPriority(1);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("firstMcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setAllocationOrder(2);
        String mcuId = createResource(mcu);

        Resource meetingRoom = new Resource();
        meetingRoom.setAllocatable(true);
        meetingRoom.setName("meeting-room");
        String meetingRoomId = createResource(meetingRoom);

        DomainResource mrDomainResource = new DomainResource();
        mrDomainResource.setPrice(1);
        mrDomainResource.setLicenseCount(null);
        mrDomainResource.setPriority(1);
        mrDomainResource.setType("mr");

        getResourceService().addDomainResource(SECURITY_TOKEN_ROOT, mcuDomainResource, loopbackDomain.getId(), mcuId);
        getResourceService().addDomainResource(SECURITY_TOKEN_ROOT, mrDomainResource, loopbackDomain.getId(), meetingRoomId);

        getConnector().login(loopbackDomain);
        DomainCapabilityListRequest listRequest = new DomainCapabilityListRequest(DomainCapabilityListRequest.Type.RESOURCE);
        Map<String, List<DomainCapability>> resources = getConnector().listForeignCapabilities(listRequest);
        Assert.assertEquals(1, resources.get(loopbackDomain.getName()).size());

        DomainCapabilityListRequest listRequest2 = new DomainCapabilityListRequest(DomainCapabilityListRequest.Type.VIRTUAL_ROOM);
        listRequest2.setTechnology(Technology.H323);
        Map<String, List<DomainCapability>> resources2 = getConnector().listForeignCapabilities(listRequest2);
        Assert.assertEquals(1, resources2.get(loopbackDomain.getName()).size());

        DomainCapabilityListRequest listRequest3 = new DomainCapabilityListRequest(DomainCapabilityListRequest.Type.VIRTUAL_ROOM);
        listRequest3.setTechnology(Technology.ADOBE_CONNECT);
        Map<String, List<DomainCapability>> resources3 = getConnector().listForeignCapabilities(listRequest3);
        Assert.assertEquals(0, resources3.get(loopbackDomain.getName()).size());
    }

    /**
     * Test of cached domain connector.
     */
    @Test
    public void testCachedDomainResources()
    {
        try {
            Resource meetingRoom = new Resource();
            meetingRoom.setAllocatable(true);
            meetingRoom.setName("meeting-room");
            String meetingRoomId = createResource(meetingRoom);

            Resource meetingRoom2 = new Resource();
            meetingRoom2.setAllocatable(true);
            meetingRoom2.setName("meeting-room");
            String meetingRoom2Id = createResource(meetingRoom2);

            DomainResource mrDomainResource = new DomainResource();
            mrDomainResource.setPrice(1);
            mrDomainResource.setLicenseCount(null);
            mrDomainResource.setPriority(1);
            mrDomainResource.setType("mr");

            getResourceService().addDomainResource(SECURITY_TOKEN_ROOT, mrDomainResource, loopbackDomain.getId(), meetingRoomId);
            getResourceService().addDomainResource(SECURITY_TOKEN_ROOT, mrDomainResource, loopbackDomain.getId(), meetingRoom2Id);

            Domain unavailableDomain = new Domain();
            unavailableDomain.setName(LocalDomain.getLocalDomainName() + ".unavailable");
            unavailableDomain.setOrganization("CESNET z.s.p.o.");
            unavailableDomain.setAllocatable(true);
            DeviceAddress deviceAddress = new DeviceAddress("none", INTERDOMAIN_LOCAL_PORT);
            unavailableDomain.setDomainAddress(deviceAddress);
            unavailableDomain.setPasswordHash("none");
            String unavailableDomainId = getResourceService().createDomain(SECURITY_TOKEN_ROOT, unavailableDomain);

            getResourceService().addDomainResource(SECURITY_TOKEN_ROOT, mrDomainResource, unavailableDomainId, meetingRoomId);

            // Uninitialized cache should return 2 resources for loopback domain, but it could be slow
            getConnector().login(loopbackDomain);
            DomainCapabilityListRequest listRequest = new DomainCapabilityListRequest(DomainCapabilityListRequest.Type.RESOURCE);
            Map<String, List<DomainCapability>> resources = getConnector().listForeignCapabilities(listRequest);
            Assert.assertEquals(2, resources.get(loopbackDomain.getName()).size());

            int i = 0;
            while (!getConnector().checkResourcesCacheInitialized()) {
                Assert.assertTrue("Cache should be initialized by now.", i < 20);
                i++;
                System.out.println("Waiting for cache to be initialized ...");
                Thread.sleep(2000);
            }
            System.out.println("Cache was INITIALIZED");

            getResourceService().removeDomainResource(SECURITY_TOKEN_ROOT, loopbackDomain.getId(), meetingRoom2Id);

            System.out.println("Waiting for cache to be refreshed");
            Thread.sleep(15000);
            System.out.println("Cached shoud be refreshed");

            // After cache refresh, it should return only 1 resource for loopback domain
            resources = getConnector().listForeignCapabilities(listRequest);
            Assert.assertEquals(1, resources.get(loopbackDomain.getName()).size());

            getResourceService().removeDomainResource(SECURITY_TOKEN_ROOT, unavailableDomainId, meetingRoomId);
            getResourceService().removeDomainResource(SECURITY_TOKEN_ROOT, loopbackDomain.getId(), meetingRoomId);
            getResourceService().deleteDomain(SECURITY_TOKEN_ROOT, unavailableDomainId);
            getResourceService().deleteDomain(SECURITY_TOKEN_ROOT, loopbackDomain.getId());

            // Deleting of foreign domain should be recognized immediately
            resources = getConnector().listForeignCapabilities(listRequest);
            Assert.assertEquals(0, resources.size());
        } catch (InterruptedException e) {
            // Should not have happened
            e.printStackTrace();
        }
    }

    /**
     * Test of reservation serialization
     * @throws Exception
     */
    @Test
    public void testGetReservation() throws Exception
    {
        Resource meetingRoom = new Resource();
        meetingRoom.setAllocatable(true);
        meetingRoom.setName("meeting-room");
        String meetingRoomId = createResource(meetingRoom);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(meetingRoomId));
        cz.cesnet.shongo.controller.api.Reservation reservation = allocateAndCheck(reservationRequest);


        Reservation reservationResult = getConnector().getReservationByRequest(loopbackDomain, reservation.getReservationRequestId());
        Assert.assertEquals(true, Temporal.isIntervalEqualed(reservation.getSlot(), reservationResult.getSlot()));
    }

    @Test
    public void test() throws Exception
    {
    }

    protected CachedDomainsConnector getConnector()
    {
        return InterDomainAgent.getInstance().getConnector();
    }

    protected DomainService getDomainService()
    {
        return InterDomainAgent.getInstance().getDomainService();
    }
}
