package cz.cesnet.shongo.controller.rest.config;

import cz.cesnet.shongo.controller.ControllerClient;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ServiceConfig
{

    private final ControllerClient controllerClient;

    @Bean
    public ReservationService reservationService()
    {
        return controllerClient.getService(ReservationService.class);
    }

    @Bean
    public AuthorizationService authorizationService()
    {
        return controllerClient.getService(AuthorizationService.class);
    }

    @Bean
    public ExecutableService executableService()
    {
        return controllerClient.getService(ExecutableService.class);
    }

    @Bean
    public ResourceService resourceService()
    {
        return controllerClient.getService(ResourceService.class);
    }

    @Bean
    public ResourceControlService resourceControlService()
    {
        return controllerClient.getService(ResourceControlService.class);
    }
}
