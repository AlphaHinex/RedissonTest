package com.example;

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
import org.springframework.cache.annotation.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;

@RunWith(Parameterized.class)
public class DemoApplicationTests {

    @Service
    @CacheConfig(cacheNames = "test")
    public static class CacheableService {

        private int count = 5;

        @Cacheable(key = "'count'")
        public int addCount() {
            return ++count;
        }

        @CacheEvict(key = "'count'")
        public void clear() { }

        @CachePut(key = "'count'")
        public int newKey() {
            return 10;
        }

        @Cacheable(cacheNames = "apiSecrets")
        public String setKey(String key) {
            return key;
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
        service.clear();
        for (int i = 0; i < 4; i++) {
            Assert.assertEquals(service.addCount(), 6);
        }

        service.clear();
        for (int i = 0; i < 4; i++) {
            Assert.assertEquals(service.addCount(), 7);
        }

        Assert.assertEquals(service.newKey(), 10);
    }

}
