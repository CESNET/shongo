package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.DummyAuthorization;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestReusement;
import cz.cesnet.shongo.controller.api.ReservationRequest;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.ResourceSpecification;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.authorization.ReservationDeviceConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReservationServiceImplTest extends AbstractControllerTest {
    @Test
    public void shouldListReservationsForResourceManagedByDevice() throws Exception {
        ReservationService reservationService = getReservationService();

        String resourceId = createTestResource();
        String reservationRequestId = createTestReservationRequest(resourceId);

        // Create reservation device that manages test resource.
        ReservationDeviceConfig deviceConfig = new ReservationDeviceConfig("test", "asd15asd19e1fe5wf9e51f", resourceId);
        DummyAuthorization.addReservationDevice(deviceConfig);
        SecurityToken deviceToken = new SecurityToken(deviceConfig.getAccessToken());
        deviceToken.setUserInformation(deviceToken.getUserInformation());

        // List resources with device's security token.
        ReservationRequestListRequest listRequest = new ReservationRequestListRequest(deviceToken);
        ListResponse<ReservationRequestSummary> response = reservationService.listReservationRequests(listRequest);
        ReservationRequestSummary firstItem = response.getItem(0);

        // Assert that there is only the test reservation request in response.
        assertEquals(response.getCount(), 1);
        assertEquals(reservationRequestId, firstItem.getId());
    }

    @Test
    public void shouldNotListReservationsForResourceNotManagedByDevice() throws Exception {
        ReservationService reservationService = getReservationService();

        String resourceId = createTestResource();
        createTestReservationRequest(resourceId);

        // Create security token for device that doesn't manage the test resource.
        SecurityToken deviceToken = new SecurityToken(RESERVATION_DEVICE_CONFIG1.getAccessToken());
        deviceToken.setUserInformation(deviceToken.getUserInformation());

        // List resources with device's security token.
        ReservationRequestListRequest listRequest = new ReservationRequestListRequest(deviceToken);
        ListResponse<ReservationRequestSummary> response = reservationService.listReservationRequests(listRequest);

        // Assert that there is only the test reservation request in response.
        assertEquals(response.getCount(), 0);
    }

    private String createTestResource() {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        return createResource(resource);
    }

    private String createTestReservationRequest(String resourceId) throws Exception {
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setSpecification(new ResourceSpecification(resourceId));
        reservationRequest.setReusement(ReservationRequestReusement.OWNED);
        reservationRequest.setPurpose(ReservationRequestPurpose.USER);
        return allocate(reservationRequest);
    }
}