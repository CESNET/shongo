package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.DomainResource;
import cz.cesnet.shongo.controller.api.domains.request.CapabilitySpecificationRequest;
import cz.cesnet.shongo.controller.api.domains.response.*;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.ssl.SSLCommunication;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

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
    private Long loopbackDomainId;

    @Override
    public void before() throws Exception
    {
        System.setProperty(ControllerConfiguration.REST_API_HOST, INTERDOMAIN_LOCAL_HOST);
        System.setProperty(ControllerConfiguration.REST_API_PORT, INTERDOMAIN_LOCAL_PORT.toString());
        System.setProperty(ControllerConfiguration.REST_API_SSL_KEY_STORE, "./shongo-controller/src/test/resources/keystore/server.p12");
        System.setProperty(ControllerConfiguration.REST_API_SSL_KEY_STORE_PASSWORD, "shongo");
        System.setProperty(ControllerConfiguration.REST_API_SSL_KEY_STORE_TYPE, "PKCS12");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_PKI_CLIENT_AUTH, "false");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_COMMAND_TIMEOUT, "PT10S");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_BASIC_AUTH_PASSWORD, INTERDOMAIN_LOCAL_PASSWORD);
        System.setProperty(ControllerConfiguration.INTERDOMAIN_CACHE_REFRESH_RATE, "PT5S");

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
        this.loopbackDomainId = ObjectIdentifier.parseLocalId(domainId, ObjectType.DOMAIN);
    }

    @After
    public void tearDown() throws Exception
    {
        System.clearProperty(ControllerConfiguration.REST_API_HOST);
        System.clearProperty(ControllerConfiguration.REST_API_PORT);
        System.clearProperty(ControllerConfiguration.REST_API_SSL_KEY_STORE);
        System.clearProperty(ControllerConfiguration.REST_API_SSL_KEY_STORE_PASSWORD);
        System.clearProperty(ControllerConfiguration.REST_API_SSL_KEY_STORE_TYPE);
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
    public void testBasicAuthLogin() throws ForeignDomainConnectException
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
        DomainCapabilityListRequest listRequest = new DomainCapabilityListRequest(DomainCapability.Type.RESOURCE);
        Map<String, List<DomainCapability>> domainCapabilities = getConnector().listForeignCapabilities(listRequest);
        Assert.assertEquals(1, domainCapabilities.size());
    }

    /**
     * Test of listing foreign resources and capabilities
     */
    @Test
    public void testListForeignCapabilities() throws ForeignDomainConnectException
    {
        DomainResource mcuDomainResource = new DomainResource();
        mcuDomainResource.setPrice(1);
        mcuDomainResource.setLicenseCount(10);
        mcuDomainResource.setPriority(1);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("firstMcu");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setAllocationOrder(2);
        String mcuId = createResource(mcu);

        DeviceResource tcs = new DeviceResource();
        tcs.setName("tcs");
        tcs.addTechnology(Technology.H323);
        tcs.addTechnology(Technology.SIP);
        tcs.addCapability(new RecordingCapability(2));
        tcs.setAllocatable(true);
        tcs.setAllocationOrder(2);
        String tcsId = createResource(tcs);

        Resource meetingRoom = new Resource();
        meetingRoom.setAllocatable(true);
        meetingRoom.setName("meeting-room");
        String meetingRoomId = createResource(meetingRoom);

        DomainResource mrDomainResource = new DomainResource();
        mrDomainResource.setPrice(1);
        mrDomainResource.setLicenseCount(null);
        mrDomainResource.setPriority(1);
        mrDomainResource.setType("mr");

        DomainResource tcsForIdp = new DomainResource();
        tcsForIdp.setPrice(1);
        tcsForIdp.setLicenseCount(2);
        tcsForIdp.setPriority(1);

        getResourceService().addDomainResource(SECURITY_TOKEN_ROOT, mcuDomainResource, loopbackDomain.getId(), mcuId);
        getResourceService().addDomainResource(SECURITY_TOKEN_ROOT, mrDomainResource, loopbackDomain.getId(), meetingRoomId);
        getResourceService().addDomainResource(SECURITY_TOKEN_ROOT, tcsForIdp, loopbackDomain.getId(), tcsId);


        getConnector().login(loopbackDomain);
        DomainCapabilityListRequest listRequest = new DomainCapabilityListRequest(DomainCapability.Type.RESOURCE);
        Map<String, List<DomainCapability>> resources = getConnector().listForeignCapabilities(listRequest);
        Assert.assertEquals(1, resources.get(loopbackDomain.getName()).size());

        DomainCapabilityListRequest listRequest2 = new DomainCapabilityListRequest(DomainCapability.Type.VIRTUAL_ROOM);

        List<Set<Technology>> technologyVariants = new ArrayList<>();
        Set<Technology> technologies = new HashSet<>();
        technologies.add(Technology.H323);
        technologies.add(Technology.SIP);
        technologyVariants.add(technologies);

        listRequest2.getCapabilitySpecificationRequests().get(0).setTechnologyVariants(technologyVariants);
        CapabilitySpecificationRequest capabilitySpecificationRequest = new CapabilitySpecificationRequest(DomainCapability.Type.RECORDING_SERVICE);
        capabilitySpecificationRequest.setTechnologyVariants(technologyVariants);
        capabilitySpecificationRequest.setLicenseCount(1);
        listRequest2.addCapabilityListRequest(capabilitySpecificationRequest);

        Map<String, List<DomainCapability>> resources2 = getConnector().listForeignCapabilities(listRequest2);
        Assert.assertEquals(2, resources2.get(loopbackDomain.getName()).size());

        DomainCapabilityListRequest listRequest3 = new DomainCapabilityListRequest(DomainCapability.Type.VIRTUAL_ROOM);

        List<Set<Technology>> technologyVariantsAC = new ArrayList<>();
        Set<Technology> technologiesAC = new HashSet<>();
        technologiesAC.add(Technology.ADOBE_CONNECT);
        technologyVariantsAC.add(technologiesAC);

        listRequest3.getCapabilitySpecificationRequests().get(0).setTechnologyVariants(technologyVariantsAC);
        Map<String, List<DomainCapability>> resources3 = getConnector().listForeignCapabilities(listRequest3);
        Assert.assertEquals(0, resources3.get(loopbackDomain.getName()).size());
    }

    /**
     * Test of cached domain connector.
     */
    @Test
    public void testCachedDomainResources() throws ForeignDomainConnectException
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
            DomainCapabilityListRequest listRequest = new DomainCapabilityListRequest(DomainCapability.Type.RESOURCE);
            Map<String, List<DomainCapability>> resources = getConnector().listForeignCapabilities(listRequest);
            Assert.assertEquals(2, resources.get(loopbackDomain.getName()).size());

            int i = 0;
            while (!getConnector().checkResourcesCacheInitialized()) {
                Assert.assertTrue("Cache should be initialized by now.", i < 20);
                i++;
                System.out.println("Waiting for cache to be initialized ...");
                Thread.sleep(1000);
            }
            System.out.println("Cache was INITIALIZED");

            getResourceService().removeDomainResource(SECURITY_TOKEN_ROOT, loopbackDomain.getId(), meetingRoom2Id);

            System.out.println("Waiting for cache to be refreshed");
            Thread.sleep(5000);
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

    @Test
    public void testListLocalResources()
    {
        DeviceResource firstMcu = new DeviceResource();
        firstMcu.setName("firstMcu");
        firstMcu.addTechnology(Technology.H323);
        firstMcu.addTechnology(Technology.SIP);
        firstMcu.addCapability(new RoomProviderCapability(10));
        firstMcu.setAllocatable(true);
        firstMcu.setAllocationOrder(2);
        String firstMcuId = createResource(firstMcu);

        DeviceResource secondMcu = new DeviceResource();
        secondMcu.setName("firstMcu");
        secondMcu.addTechnology(Technology.H323);
        secondMcu.addTechnology(Technology.SIP);
        secondMcu.addCapability(new RoomProviderCapability(5));
        secondMcu.setAllocatable(true);
        secondMcu.setAllocationOrder(1);
        createResource(secondMcu);

        DeviceResource ac = new DeviceResource();
        ac.setName("adobe connect");
        ac.addTechnology(Technology.ADOBE_CONNECT);
        ac.addCapability(new RoomProviderCapability(100));
        ac.addCapability(new RecordingCapability());
        ac.setAllocatable(true);
        ac.setAllocationOrder(1);
        String acId = createResource(ac);

        DeviceResource tcs = new DeviceResource();
        tcs.setName("tcs");
        tcs.addTechnology(Technology.H323);
        tcs.addTechnology(Technology.SIP);
        tcs.addCapability(new RecordingCapability(2));
        tcs.setAllocatable(true);
        tcs.setAllocationOrder(2);
        String tcsId = createResource(tcs);

        DomainResource mcuForIdp = new DomainResource();
        mcuForIdp.setPrice(1);
        mcuForIdp.setLicenseCount(3);
        mcuForIdp.setPriority(1);

        DomainResource acForIdp = new DomainResource();
        acForIdp.setPrice(-1);
        acForIdp.setLicenseCount(200);
        acForIdp.setPriority(1);

        DomainResource tcsForIdp = new DomainResource();
        tcsForIdp.setPrice(1);
        tcsForIdp.setLicenseCount(2);
        tcsForIdp.setPriority(1);

        getResourceService().addDomainResource(SECURITY_TOKEN_ROOT, mcuForIdp, loopbackDomain.getId(), firstMcuId);
        getResourceService().addDomainResource(SECURITY_TOKEN_ROOT, acForIdp, loopbackDomain.getId(), acId);
        getResourceService().addDomainResource(SECURITY_TOKEN_ROOT, tcsForIdp, loopbackDomain.getId(), tcsId);


        List<Set<Technology>> technologyVariants = new ArrayList<>();
        Set<Technology> technologies = new HashSet<>();
        technologies.add(Technology.H323);
        technologies.add(Technology.SIP);
        technologyVariants.add(technologies);
        DomainCapability.Type type = DomainCapability.Type.VIRTUAL_ROOM;

        List<DomainCapability> capabilities = getDomainService().listLocalResourcesByDomain(loopbackDomainId, type, null, technologyVariants);
        Assert.assertEquals(1, capabilities.size());
        Assert.assertEquals(firstMcuId, capabilities.get(0).getId());

        Domain unavailableDomain = new Domain();
        unavailableDomain.setName(LocalDomain.getLocalDomainName() + ".unavailable");
        unavailableDomain.setOrganization("CESNET z.s.p.o.");
        unavailableDomain.setAllocatable(true);
        DeviceAddress deviceAddress = new DeviceAddress("none", INTERDOMAIN_LOCAL_PORT);
        unavailableDomain.setDomainAddress(deviceAddress);
        unavailableDomain.setPasswordHash("none");
        String unavailableDomainId = getResourceService().createDomain(SECURITY_TOKEN_ROOT, unavailableDomain);
        unavailableDomain.setId(unavailableDomainId);

        Long persistenceUnavailableDomainId = ObjectIdentifier.parseLocalId(unavailableDomainId, ObjectType.DOMAIN);

        capabilities = getDomainService().listLocalResourcesByDomain(persistenceUnavailableDomainId, type, null, technologyVariants);
        Assert.assertEquals(0, capabilities.size());

        type = DomainCapability.Type.RECORDING_SERVICE;
        capabilities = getDomainService().listLocalResourcesByDomain(loopbackDomainId, type, null, technologyVariants);
        Assert.assertEquals(1, capabilities.size());
        Assert.assertEquals(tcsId, capabilities.get(0).getId());
    }

    /**
     * Test of reservation serialization
     * @throws Exception
     */
    @Test
    public void testGetReservation() throws Exception
    {
        //TODO: fix - add another domain?
//        Resource meetingRoom = new Resource();
//        meetingRoom.setAllocatable(true);
//        meetingRoom.setName("meeting-room");
//        String meetingRoomId = createResource(meetingRoom);
//
//        DomainResource mrDomainResource = new DomainResource();
//        mrDomainResource.setPrice(1);
//        mrDomainResource.setLicenseCount(null);
//        mrDomainResource.setPriority(1);
//        mrDomainResource.setType("mr");
//
//        getResourceService().addDomainResource(SECURITY_TOKEN_ROOT, mrDomainResource, loopbackDomain.getId(), meetingRoomId);
//
//        ReservationRequest reservationRequest = new ReservationRequest();
//        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
//        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
//        reservationRequest.setSpecification(new ResourceSpecification(meetingRoomId));
//        cz.cesnet.shongo.controller.api.Reservation reservation = allocateAndCheck(reservationRequest);
//
//        Reservation reservationResult = getConnector().getReservationByRequest(loopbackDomain, reservation.getReservationRequestId());
//        Assert.assertEquals(true, Temporal.isIntervalEqualed(reservation.getSlot(), reservationResult.getSlot()));
    }

    @Test
    public void test() throws Exception
    {
//        DomainResource mcuDomainResource = new DomainResource();
//        mcuDomainResource.setPrice(1);
//        mcuDomainResource.setLicenseCount(10);
//        mcuDomainResource.setPriority(1);
//
//        DeviceResource mcu = new DeviceResource();
//        mcu.setName("firstMcu");
//        mcu.addTechnology(Technology.H323);
//        mcu.addTechnology(Technology.SIP);
//        mcu.addCapability(new RoomProviderCapability(10));
//        mcu.setAllocatable(true);
//        mcu.setAllocationOrder(2);
//        String mcuId = createResource(mcu);
//
//
//        DomainResource mrDomainResource = new DomainResource();
//        mrDomainResource.setPrice(1);
//        mrDomainResource.setLicenseCount(null);
//        mrDomainResource.setPriority(1);
//        mrDomainResource.setType("mr");
//
//        getResourceService().addDomainResource(SECURITY_TOKEN_ROOT, mcuDomainResource, loopbackDomain.getId(), mcuId);
//
//        getConnector().login(loopbackDomain);
//        DomainCapabilityListRequest listRequest = new DomainCapabilityListRequest(DomainCapabilityListRequest.Type.RESOURCE);
//        Map<String, List<DomainCapability>> resources = getConnector().listForeignCapabilities(listRequest);
//        Assert.assertEquals(1, resources.get(loopbackDomain.getName()).size());

//        DomainCapabilityListRequest listRequest2 = new DomainCapabilityListRequest(DomainCapabilityListRequest.Type.VIRTUAL_ROOM);
//
//        List<Set<Technology>> technologyVariants = new ArrayList<>();
//        Set<Technology> technologies = new HashSet<>();
//        technologies.add(Technology.H323);
//        technologies.add(Technology.SIP);
//        technologyVariants.add(technologies);
//
//        listRequest2.getCapabilityListRequests().get(0).setTechnologyVariants(technologyVariants);
//
//        Map<String, List<DomainCapability>> resources2 = getConnector().listForeignCapabilities(listRequest2);
//        Assert.assertEquals(1, resources2.get(loopbackDomain.getName()).size());
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
