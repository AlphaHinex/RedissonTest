package com.example;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.example.entity.User;

@RunWith(Parameterized.class)
public class RedissonOverWriteTests {

    @Service
    public static class CacheableService {
        public static final String CACHE_NAME = "apiSecrets";

        @Cacheable(value = CACHE_NAME)
        public String addCount(String key) {
            return ""+System.currentTimeMillis();
        }
    }
    
    @Service
    @CacheConfig(cacheNames = "users")
    public static class UserService {
        @Cacheable(key = "#p0.id")
        public User save(User user) {
            return user;
        }
    }

    @Configuration
    @ComponentScan
    @EnableCaching
    public static class Application {

        @Bean(destroyMethod="shutdown")
        RedissonClient redisson(@Value("classpath:redisson.yaml") Resource configFile) throws IOException {
            Config config = Config.fromYAML(configFile.getInputStream());
            return Redisson.create(config);
        }

        @Bean
        CacheManager cacheManager(RedissonClient redissonClient) throws IOException {
            return new RedissonSpringCacheManager(redissonClient, "classpath:redisson-config.yaml");
        }

    }

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Iterable<Object[]> data() throws IOException, InterruptedException {
        return Arrays.asList(new Object[][]{
                {new AnnotationConfigApplicationContext(Application.class)}
        });
    }

    @Parameterized.Parameter(0)
    public AnnotationConfigApplicationContext context;

    @Test
    public void testGet() {
        CacheableService service = context.getBean(CacheableService.class);
        service.addCount("key1");

        UserService userService = context.getBean(UserService.class);
        User u = new User();
        u.setId("key1");
        userService.save(u);

        Assert.assertNotEquals(service.addCount("key1"), userService.save(u));
    }
}
