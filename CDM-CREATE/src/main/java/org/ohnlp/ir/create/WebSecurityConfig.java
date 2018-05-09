package org.ohnlp.ir.create;

import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.InetOrgPerson;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import java.util.Collection;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    private String validGroups = "DL HSR BSI HA 3\n" +
            "DL HSR HAR6";

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/css/**", "/js/**", "/img/**", "/webjars/**")
                .permitAll()
                .anyRequest().fullyAuthenticated()
                .and()
                .formLogin()
                .loginPage("/login")
                .permitAll()
                .defaultSuccessUrl("/?success")
                .failureUrl("/login?failure")
                .and()
                .csrf().disable()
                .logout()
                .permitAll();
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        DefaultSpringSecurityContextSource contextSource =
                new DefaultSpringSecurityContextSource("ldap://mfadldap.mfad.mfroot.org:389");
        try {
            contextSource.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        BindAuthenticator bind = new BindAuthenticator(contextSource);
        bind.setUserDnPatterns(new String[]{
                "CN={0},OU=Users,OU=MCR,DC=mfad,DC=mfroot,DC=org",
                "CN={0},OU=Normal,OU=WorkAccounts,DC=mfad,DC=mfroot,DC=org"
        });
        bind.afterPropertiesSet();
        LdapAuthenticationProvider provider = new LdapAuthenticationProvider(bind);
        provider.setUserDetailsContextMapper(new MayoInternalUserContextMapper());
        auth.authenticationProvider(provider);
    }

    private class MayoInternalUserContextMapper implements UserDetailsContextMapper {
        @Override
        public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) {
            InetOrgPerson.Essence p = new MayoInetOrgPerson.Essence(ctx);

            p.setUsername(username);
            p.setAuthorities(authorities);

            return p.createUserDetails();        }

        @Override
        public void mapUserToContext(UserDetails userDetails, DirContextAdapter dirContextAdapter) {
            throw new UnsupportedOperationException("Mayo LDAP is read-only for our purposes.");
        }
    }

    private static class MayoInetOrgPerson extends InetOrgPerson {

        private String photo;

        public static class Essence extends InetOrgPerson.Essence {
            public Essence() {
            }

            public Essence(DirContextOperations ctx) {
                super(ctx);
                String employeeid = ctx.getStringAttribute("employeeid");

                ((MayoInetOrgPerson)instance).photo = "http://quarterly.mayo.edu/qtphotos/" + employeeid + ".jpg";
            }

            protected MayoInetOrgPerson createTarget() {
                return new MayoInetOrgPerson();
            }
        }

        public String getPhoto() {
            return photo;
        }

    }
}
