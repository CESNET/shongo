package cz.cesnet.shongo.controller.rest.config.security;

import cz.cesnet.shongo.controller.ControllerConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configures spring security for REST api server.
 *
 * @author Filip Karnis
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter
{

    private final ControllerConfiguration configuration;

    @Override
    public void configure(WebSecurity web)
    {
        web.ignoring()
                .antMatchers("/domain/**")
                .antMatchers("/v3/api-docs")
                .antMatchers("/swagger-ui/**")
                .antMatchers("/**/report");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception
    {
        http.cors().and().csrf().disable();
        AuthFilter authFilter = new AuthFilter();
        http.addFilterAt(authFilter, BasicAuthenticationFilter.class)
                .authorizeRequests()
                .antMatchers("/domain/**").permitAll()
                .antMatchers("/v3/api-docs").permitAll()
                .antMatchers("/swagger-ui/**").permitAll()
                .antMatchers("/**/report").permitAll();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource()
    {
        CorsConfiguration configuration = new CorsConfiguration().applyPermitDefaultValues();
        configuration.setAllowedMethods(List.of(CorsConfiguration.ALL));
        configuration.setAllowedOrigins(allowedOrigins());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> allowedOrigins()
    {
        return configuration.getRESTApiAllowedOrigins();
    }
}
