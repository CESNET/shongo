package cz.cesnet.shongo.controller.rest.auth;

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
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers("/domain/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        AuthFilter authFilter = new AuthFilter();
        http.addFilterAt(authFilter, BasicAuthenticationFilter.class)
                .authorizeRequests()
                .antMatchers("/domain/**").permitAll();
    }
}
