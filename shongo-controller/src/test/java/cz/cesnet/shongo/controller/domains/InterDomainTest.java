package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.LocalDomain;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.DomainResource;
import cz.cesnet.shongo.controller.api.domains.response.*;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import cz.cesnet.shongo.ssl.SSLCommunication;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * Tests for Inter Domain Protocol.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class InterDomainTest extends AbstractControllerTest {
    private static final String INTERDOMAIN_LOCAL_HOST = "localhost";
    private static final Integer INTERDOMAIN_LOCAL_PORT = 8443;
    private static final String INTERDOMAIN_LOCAL_PASSWORD = "shongo_test";
    private static final String INTERDOMAIN_LOCAL_PASSWORD_HASH = SSLCommunication.hashPassword(INTERDOMAIN_LOCAL_PASSWORD.getBytes());

    private Domain loopbackDomain;

    @Override
    public void before() throws Exception {
        System.setProperty(ControllerConfiguration.INTERDOMAIN_HOST, INTERDOMAIN_LOCAL_HOST);
        System.setProperty(ControllerConfiguration.INTERDOMAIN_PORT, INTERDOMAIN_LOCAL_PORT.toString());
        System.setProperty(ControllerConfiguration.INTERDOMAIN_SSL_KEY_STORE, "shongo-controller/src/test/resources/keystore/server.p12");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_SSL_KEY_STORE_PASSWORD, "shongo");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_SSL_KEY_STORE_TYPE, "PKCS12");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_PKI_CLIENT_AUTH, "false");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_COMMAND_TIMEOUT, "PT10S");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_BASIC_AUTH_PASSWORD, INTERDOMAIN_LOCAL_PASSWORD);

        super.before();

        loopbackDomain = new Domain();
        loopbackDomain.setName("TEST");
        loopbackDomain.setCode(LocalDomain.getLocalDomainCode());
        loopbackDomain.setOrganization("CESNET z.s.p.o.");
        loopbackDomain.setAllocatable(true);
        loopbackDomain.setCertificatePath("shongo-controller/src/test/resources/keystore/server.crt");
        DeviceAddress deviceAddress = new DeviceAddress(INTERDOMAIN_LOCAL_HOST, INTERDOMAIN_LOCAL_PORT);
        loopbackDomain.setDomainAddress(deviceAddress);
        loopbackDomain.setPasswordHash(INTERDOMAIN_LOCAL_PASSWORD_HASH);
        String domainId = getResourceService().createDomain(SECURITY_TOKEN_ROOT, loopbackDomain);
        loopbackDomain.setId(domainId);
    }

    @Test
    public void testBasicAuthLogin() {
        getConnector().login(loopbackDomain);
        List<Domain> domains = getConnector().getForeignDomainsStatuses();
        Assert.assertEquals(Domain.Status.AVAILABLE, domains.get(0).getStatus());
    }

    @Test
    public void testUnavailableDomain() {
        Domain unavailableDomain = new Domain();
        unavailableDomain.setName("Unavailable");
        unavailableDomain.setCode("none");
        unavailableDomain.setOrganization("CESNET z.s.p.o.");
        unavailableDomain.setAllocatable(true);
//        unavailableDomain.setCertificatePath("shongo-controller/src/test/resources/keystore/server.crt");
        DeviceAddress deviceAddress = new DeviceAddress("none", INTERDOMAIN_LOCAL_PORT);
        unavailableDomain.setDomainAddress(deviceAddress);
        unavailableDomain.setPasswordHash("none");
        getResourceService().createDomain(SECURITY_TOKEN_ROOT, unavailableDomain);

        List<Domain> domains = getConnector().getForeignDomainsStatuses();
        Assert.assertEquals(2, domains.size());

        DomainCapabilityListRequest listRequest = new DomainCapabilityListRequest();
        listRequest.setType(DomainCapabilityListRequest.Type.RESOURCE);
        Map<String, List<DomainCapability>> domainCapabilities = getConnector().listForeignCapabilities(listRequest);
        Assert.assertEquals(1, domainCapabilities.entrySet().size());
    }

    @Test
    public void testListForeignCapabilities() {
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

        DomainResource mrDomainResource = new DomainResource();
        mrDomainResource.setPrice(1);
        mrDomainResource.setLicenseCount(null);
        mrDomainResource.setPriority(1);
        mrDomainResource.setType("mr");

        Resource meetingRoom = new Resource();
        meetingRoom.setAllocatable(true);
        meetingRoom.setName("meeting-room");
        String meetingRoomId = createResource(meetingRoom);

        getResourceService().addDomainResource(SECURITY_TOKEN_ROOT, mcuDomainResource, loopbackDomain.getId(), mcuId);
        getResourceService().addDomainResource(SECURITY_TOKEN_ROOT, mrDomainResource, loopbackDomain.getId(), meetingRoomId);

        getConnector().login(loopbackDomain);
        DomainCapabilityListRequest listRequest = new DomainCapabilityListRequest();
        listRequest.setType(DomainCapabilityListRequest.Type.RESOURCE);
        Map<String, List<DomainCapability>> resources = getConnector().listForeignCapabilities(listRequest);
        Assert.assertEquals(1, resources.get(loopbackDomain.getCode()).size());

        DomainCapabilityListRequest listRequest2 = new DomainCapabilityListRequest();
        listRequest2.setType(DomainCapabilityListRequest.Type.VIRTUAL_ROOM);
        listRequest2.setTechnology(Technology.H323);
        Map<String, List<DomainCapability>> resources2 = getConnector().listForeignCapabilities(listRequest2);
        Assert.assertEquals(1, resources2.get(loopbackDomain.getCode()).size());

        DomainCapabilityListRequest listRequest3 = new DomainCapabilityListRequest();
        listRequest3.setType(DomainCapabilityListRequest.Type.VIRTUAL_ROOM);
        listRequest3.setTechnology(Technology.ADOBE_CONNECT);
        Map<String, List<DomainCapability>> resources3 = getConnector().listForeignCapabilities(listRequest3);
        Assert.assertEquals(0, resources3.get(loopbackDomain.getCode()).size());
    }

    @Test
    public void test() throws Exception {
    }

    protected DomainsConnector getConnector() {
        return InterDomainAgent.getInstance().getConnector();
    }

    protected DomainService getDomainService() {
        return InterDomainAgent.getInstance().getDomainService();
    }
}
