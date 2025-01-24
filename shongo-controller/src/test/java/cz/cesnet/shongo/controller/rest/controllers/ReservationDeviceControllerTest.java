package cz.cesnet.shongo.controller.rest.controllers;

import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.api.ReservationDevice;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.authorization.ReservationDeviceConfig;
import cz.cesnet.shongo.controller.rest.models.reservationdevice.ReservationDeviceModel;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import static org.junit.Assert.assertEquals;

public class ReservationDeviceControllerTest extends AbstractControllerTest {
    private final AuthorizationService authorizationService = Mockito.mock(AuthorizationService.class);
    private final ReservationDeviceController controller = new ReservationDeviceController(authorizationService);

    @Test
    public void shouldReturnDeviceInfo() {
        String resourceId = createTestResource();
        ReservationDeviceConfig deviceConfig = new ReservationDeviceConfig("test", "test", resourceId);
        getAuthorization().addReservationDevice(deviceConfig);

        SecurityToken deviceToken = new SecurityToken(deviceConfig.getAccessToken());
        ReservationDevice device = new ReservationDevice();
        device.setId(deviceConfig.getDeviceId());
        device.setAccessToken(deviceConfig.getAccessToken());
        device.setResourceId(deviceConfig.getResourceId());
        ReservationDeviceModel model = new ReservationDeviceModel(device);

        Mockito.when(authorizationService.getReservationDevice(deviceToken)).thenReturn(device);

        assertEquals(ResponseEntity.ok(model), controller.getReservationDevice(deviceToken));
    }

    @Test
    public void shouldReturnNotFound() {
        Mockito.when(authorizationService.getReservationDevice(Mockito.any())).thenReturn(null);
        assertEquals(ResponseEntity.notFound().build(), controller.getReservationDevice(new SecurityToken("test")));
    }
}
