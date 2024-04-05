package cz.cesnet.shongo.controller.rest.controllers;

import cz.cesnet.shongo.controller.api.ReservationDevice;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.rest.RestApiPath;
import cz.cesnet.shongo.controller.rest.models.reservationdevice.ReservationDeviceModel;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cz.cesnet.shongo.controller.rest.config.security.AuthFilter.TOKEN;

/**
 * Rest controller for endpoints related to reservation devices.
 *
 * @author Michal Drobňák
 */
@RestController
@RequestMapping(RestApiPath.RESERVATION_DEVICE)
@RequiredArgsConstructor
@Slf4j
public class ReservationDeviceController {

    private final AuthorizationService authorizationService;

    @Operation(summary = "Get reservation device associated with Bearer token.")
    @GetMapping
    ResponseEntity<ReservationDeviceModel> getReservationDevice(
            @RequestAttribute(TOKEN) SecurityToken securityToken
    ) {
        ReservationDevice device = authorizationService.getReservationDevice(securityToken);

        if (device != null) {
            ReservationDeviceModel model = new ReservationDeviceModel(device);
            log.info("Get reservation device: {}", model);
            return ResponseEntity.ok().body(model);
        }
        log.info("Device not found");

        return ResponseEntity.notFound().build();
    }
}
