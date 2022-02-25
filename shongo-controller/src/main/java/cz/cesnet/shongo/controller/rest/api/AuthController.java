package cz.cesnet.shongo.controller.rest.api;

import cz.cesnet.shongo.controller.SystemPermission;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cz.cesnet.shongo.controller.rest.auth.AuthFilter.TOKEN;

/**
 * Rest controller for user authorization endpoints.
 *
 * @author Filip Karnis
 */
@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthorizationService authorizationService;

    public AuthController(@Autowired AuthorizationService reservationService) {
        this.authorizationService = reservationService;
    }

    @Operation(summary = "Returns user's system permissions.")
    @GetMapping("/permissions")
    public List<SystemPermission> getPermissions(
            @RequestAttribute(TOKEN) SecurityToken securityToken)
    {
        return Stream.of(SystemPermission.values()).filter(permission ->
                authorizationService.hasSystemPermission(securityToken, permission)
        ).collect(Collectors.toList());
    }
}
