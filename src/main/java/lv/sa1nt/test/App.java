package lv.sa1nt.test;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import redis.clients.jedis.JedisPoolConfig;
import redis.embedded.RedisServer;

@SpringBootApplication
@Configuration
@EnableResourceServer
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
public class App extends ResourceServerConfigurerAdapter {
    static Logger logger = LoggerFactory.getLogger(App.class);

    @Bean(destroyMethod = "stop")
    public RedisServer redisServer() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();

        logger.info("Embedded Redis server started");

        return redisServer;
    }

    @Bean
    public JedisConnectionFactory connectionFactory() throws IOException {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        JedisConnectionFactory factory = new JedisConnectionFactory(poolConfig);
        return factory;
    }

    @Bean
    public TokenStore tokenStore() throws IOException {
        return new RedisTokenStore(connectionFactory());
    }

    @Bean
    public ResourceServerTokenServices tokenServices() throws IOException {
        DefaultTokenServices services = new DefaultTokenServices();
        services.setTokenStore(tokenStore());
        return services;
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        resources.tokenStore(tokenStore());
        resources.tokenServices(tokenServices());
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
                .antMatcher("/**")
                .authorizeRequests()
                .antMatchers("/help**", "/test**")
                .permitAll()
                .anyRequest()
                .authenticated()
                .and().logout().logoutSuccessUrl("/help").permitAll()
                ;
    }
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}