package org.obiba.mica.config;

import javax.inject.Inject;

import org.obiba.mica.security.AjaxAuthenticationFailureHandler;
import org.obiba.mica.security.AjaxAuthenticationSuccessHandler;
import org.obiba.mica.security.AjaxLogoutSuccessHandler;
import org.obiba.mica.security.Http401UnauthorizedEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.web.authentication.RememberMeServices;

import static org.obiba.mica.security.AuthoritiesConstants.ADMIN;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

  @Inject
  private Environment env;

  @Inject
  private AjaxAuthenticationSuccessHandler ajaxAuthenticationSuccessHandler;

  @Inject
  private AjaxAuthenticationFailureHandler ajaxAuthenticationFailureHandler;

  @Inject
  private AjaxLogoutSuccessHandler ajaxLogoutSuccessHandler;

  @Inject
  private Http401UnauthorizedEntryPoint authenticationEntryPoint;

  @Inject
  private UserDetailsService userDetailsService;

  @Inject
  private RememberMeServices rememberMeServices;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new StandardPasswordEncoder();
  }

  @Inject
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
  }

  @Override
  public void configure(WebSecurity web) throws Exception {
    web.ignoring().antMatchers("/bower_components/**").antMatchers("/fonts/**").antMatchers("/images/**")
        .antMatchers("/scripts/**").antMatchers("/styles/**").antMatchers("/view/**").antMatchers("/console/**");
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.exceptionHandling() //
        .authenticationEntryPoint(authenticationEntryPoint).and() //

        .rememberMe().rememberMeServices(rememberMeServices)
        .key(env.getProperty("jhipster.security.rememberme.key")).and() //

        .formLogin().loginProcessingUrl("/app/authentication").successHandler(ajaxAuthenticationSuccessHandler)
        .failureHandler(ajaxAuthenticationFailureHandler).usernameParameter("j_username")
        .passwordParameter("j_password").permitAll().and() //

        .logout().logoutUrl("/app/logout").logoutSuccessHandler(ajaxLogoutSuccessHandler).deleteCookies("JSESSIONID")
        .permitAll().and() //

        .csrf().disable() //

        .authorizeRequests() //
        .antMatchers("/app/rest/authenticate").permitAll() //
        .antMatchers("/app/rest/logs/**").hasAuthority(ADMIN) //
        .antMatchers("/app/**").authenticated() //
        .antMatchers("/websocket/tracker").hasAuthority(ADMIN) //
        .antMatchers("/websocket/**").permitAll() //
        .antMatchers("/metrics*").hasAuthority(ADMIN) //
        .antMatchers("/metrics/**").hasAuthority(ADMIN) //
        .antMatchers("/health*").hasAuthority(ADMIN) //
        .antMatchers("/health/**").hasAuthority(ADMIN) //
        .antMatchers("/trace*").hasAuthority(ADMIN) //
        .antMatchers("/trace/**").hasAuthority(ADMIN) //
        .antMatchers("/dump*").hasAuthority(ADMIN) //
        .antMatchers("/dump/**").hasAuthority(ADMIN) //
        .antMatchers("/shutdown*").hasAuthority(ADMIN) //
        .antMatchers("/shutdown/**").hasAuthority(ADMIN) //
        .antMatchers("/beans*").hasAuthority(ADMIN) //
        .antMatchers("/beans/**").hasAuthority(ADMIN) //
        .antMatchers("/info*").hasAuthority(ADMIN) //
        .antMatchers("/info/**").hasAuthority(ADMIN) //
        .antMatchers("/autoconfig*").hasAuthority(ADMIN) //
        .antMatchers("/autoconfig/**").hasAuthority(ADMIN) //
        .antMatchers("/env*").hasAuthority(ADMIN) //
        .antMatchers("/env/**").hasAuthority(ADMIN) //
        .antMatchers("/trace*").hasAuthority(ADMIN) //
        .antMatchers("/trace/**").hasAuthority(ADMIN);
  }

  @EnableGlobalMethodSecurity(prePostEnabled = true, jsr250Enabled = true)
  private static class GlobalSecurityConfiguration extends GlobalMethodSecurityConfiguration {}
}