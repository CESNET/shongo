package cz.cesnet.shongo.controller.rest.config.security;

import cz.cesnet.shongo.controller.ControllerConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

@Configuration
@EnableWebMvc
@RequiredArgsConstructor
public class WebConfig extends WebMvcConfigurerAdapter {

    private final ControllerConfiguration configuration;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins().toArray(new String[0]))
                .allowedMethods("*")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    private List<String> allowedOrigins()
    {
        return configuration.getRESTApiAllowedOrigins();
    }
}
