package cz.cesnet.shongo.controller.rest.controllers;

import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.api.ReservationDevice;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
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
        SecurityToken deviceToken = new SecurityToken(RESERVATION_DEVICE_CONFIG1.getAccessToken());
        ReservationDevice device = new ReservationDevice();
        device.setId(RESERVATION_DEVICE_CONFIG1.getDeviceId());
        device.setAccessToken(RESERVATION_DEVICE_CONFIG1.getAccessToken());
        device.setResourceId(RESERVATION_DEVICE_CONFIG1.getResourceId());
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