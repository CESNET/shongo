package cz.cesnet.shongo.controller.rest.config;

import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.controller.ControllerClient;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ControllerConfig
{

    @Bean
    public Controller controller()
    {
        return Controller.getInstance();
    }

    @Bean
    public ControllerConfiguration configuration()
    {
        return controller().getConfiguration();
    }

    @Bean
    public ControllerClient controllerClient() throws Exception
    {
        return new ControllerClient(configuration().getRpcUrl());
    }
}
