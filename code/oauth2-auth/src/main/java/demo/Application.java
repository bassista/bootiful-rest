package demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;

/**
 * Easy to retreive an access token using
 *  <code>
 *   curl -X POST -vu acme:acmesecret http://localhost:8002/auth/oauth/token -H Accept: application/json -d password=password&username=jlong&grant_type=password&scope=read&client_secret=acmesecret&client_id=acme
 *  </code>
 *
 * Then, send the access token to an OAuth2 secured REST resource using:
 *
 * <CODE>
 *  curl http://localhost:8080/api -H "Authorization: Bearer _INSERT TOKEN_
 * </CODE>
 *
 *
 * @author  Josh Long
 *
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    UserDetailsService userDetailsService(JdbcTemplate jdbcTemplate) {
        RowMapper<User> userRowMapper = (resultSet, i) -> {
            boolean enabled = resultSet.getBoolean("ENABLED");
            return new User(
                    resultSet.getString("ACCOUNT_NAME"),
                    resultSet.getString("PASSWORD"),
                    enabled,
                    enabled,
                    enabled,
                    enabled,
                    AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN")
            );
        };
        return username -> jdbcTemplate.queryForObject("select * from ACCOUNT where ACCOUNT_NAME = ?", userRowMapper, username);
    }

    @Bean
    AuthenticationManager authenticationManager(
            ObjectPostProcessor<Object> objectPostProcessor,
            UserDetailsService userDetailsService) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = new AuthenticationManagerBuilder(objectPostProcessor);
        authenticationManagerBuilder.userDetailsService(userDetailsService);
        return authenticationManagerBuilder.getOrBuild();
    }

    @Configuration
    @EnableAuthorizationServer
    static class OAuth2AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {

        @Autowired
        private AuthenticationManager authenticationManager;

        @Override
        public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
            clients.inMemory()
                    .withClient("acme")
                    .secret("acmesecret")
                    .authorizedGrantTypes("authorization_code", "refresh_token", "password")
                    .scopes("read");
        }

        @Override
        public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
            endpoints.authenticationManager(authenticationManager);
        }

        @Override
        public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
            oauthServer.checkTokenAccess("permitAll()");
        }

    }
}
