package cz.cesnet.shongo.controller.rest.config.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Configures spring security for REST api server.
 *
 * @author Filip Karnis
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter
{

    @Override
    public void configure(WebSecurity web)
    {
        web.ignoring()
                .antMatchers("/domain/**")
                .antMatchers("/v3/api-docs")
                .antMatchers("/swagger-ui/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception
    {
        http.cors().and().csrf().disable();
        AuthFilter authFilter = new AuthFilter();
        http.addFilterAt(authFilter, BasicAuthenticationFilter.class);
    }
}
