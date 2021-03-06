/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.echoes.labs.ksf.users.security.auth;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import fr.echoes.labs.ksf.users.security.config.LdapSecurityConfiguration;
import fr.echoes.labs.ksf.users.security.config.SecurityConfiguration;
import fr.echoes.labs.ksf.users.security.utils.SecurityLoggers;

/**
 * This class default initializes the KSF Security Authentication mechanism
 * (ldap, embedded users)...
 *
 * @author sleroy
 */
@Profile("internalAuth")
@Service
public class InternalUserAuthenticationManager implements IUserAuthenticationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalUserAuthenticationManager.class);

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private LdapSecurityConfiguration ldapSecurity;

    @Autowired
    private SecurityConfiguration security;

    @Autowired()
    private ContextSource ldapContextSource;

    @Override
    public void initializeKsfAuth(final AuthenticationManagerBuilder auth) throws Exception {
        // also see LdapAuthenticationProviderConfigurer and @code ContextSourceBuilder

        //		auth.inMemoryAuthentication().withUser("user").password("password1")
        //				.roles("GUI")
        if (this.ldapSecurity.hasConfigurationProvided()) {
            LOGGER.info("Initialization of LDAP Authentication provider");
            initializeLdapAuthenticationManager(auth);
        }
        LOGGER.info("Initialization of DAO Authentication provider");
        final DaoAuthenticationProvider userEmbeddedAuthenticationProvider = new DaoAuthenticationProvider();
        userEmbeddedAuthenticationProvider.setPasswordEncoder(new BCryptPasswordEncoder(this.security.getPasswordStrength()));
        userEmbeddedAuthenticationProvider.setUserDetailsService(this.userDetailsService);
        auth.authenticationProvider(userEmbeddedAuthenticationProvider);

        // keep the credentials in the session for using them against a REST API
        auth.eraseCredentials(false);
    }

    private void initializeLdapAuthenticationManager(final AuthenticationManagerBuilder auth) throws Exception {
        LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder> ldapAuth = auth.ldapAuthentication();

        SecurityLoggers.LDAP_LOGGER.info("Enabling LDAP Authentication on server {}", this.ldapSecurity.getUrl());
        this.ldapSecurity.clearManagerDN();
        Validate.notNull(this.ldapContextSource);
        ldapAuth = ldapAuth.contextSource((BaseLdapPathContextSource) this.ldapContextSource);

        if (this.ldapSecurity.hasUserDN()) {
            SecurityLoggers.LDAP_LOGGER.info("Ldap Security base on UserDN pattern");
            ldapAuth = ldapAuth
                    .userDnPatterns(this.ldapSecurity.getUserDnPattern());

        } else if (this.ldapSecurity.hasUserSearch()) {
            SecurityLoggers.LDAP_LOGGER.info("Ldap Security base on UserDN pattern");
            ldapAuth = ldapAuth
                    .userSearchBase(this.ldapSecurity.getUserSearchBase())
                    .userSearchFilter(this.ldapSecurity.getUserSearchFilter());
        }
        if (!this.ldapSecurity.getGroupSearchBase().isEmpty()) {
            SecurityLoggers.LDAP_LOGGER.info("Setting groupSearchBase={}", this.ldapSecurity.getGroupSearchBase());
            ldapAuth = ldapAuth.groupSearchBase(this.ldapSecurity.getGroupSearchBase());
        }
        if (!this.ldapSecurity.getGroupRoleAttribute().isEmpty()) {
            SecurityLoggers.LDAP_LOGGER.info("Setting groupRoleAttribute={}", this.ldapSecurity.getGroupRoleAttribute());
            ldapAuth = ldapAuth.groupRoleAttribute(this.ldapSecurity.getGroupRoleAttribute());
        }
        if (!this.ldapSecurity.getGroupSearchFilter().isEmpty()) {
            SecurityLoggers.LDAP_LOGGER.info("Setting groupSearchFilter={}", this.ldapSecurity.getGroupSearchFilter());
            ldapAuth = ldapAuth.groupSearchFilter(this.ldapSecurity.getGroupSearchFilter());

        }

    }
}
