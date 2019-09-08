package com.pchudzik.blog.example.multicache;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;

import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.times;

public class ItemsServiceTest {
    @Rule
    public GenericContainer redis = new GenericContainer<>("redis:5.0.3-alpine").withExposedPorts(6379);

    private ItemsService itemsService;

    private JedisConnectionFactory jedisConnectionFactory;

    private SlowItemRepository slowItemRepository;

    @Before
    public void setup() {
        final CacheConfiguration config = new CacheConfiguration(redis.getContainerIpAddress(), redis.getFirstMappedPort());

        final RedisTemplate<String, Item> redisTemplate = config.redisTemplate();
        redisTemplate.afterPropertiesSet();

        jedisConnectionFactory = config.jedisConnectionFactory();
        slowItemRepository = Mockito.mock(SlowItemRepository.class);
        itemsService = new ItemsService(
                slowItemRepository,
                new ItemsCacheImpl(redisTemplate));
    }

    @Test
    public void items_are_loaded_from_cache() {
        //given
        final Item item1 = anyItem();
        final Item item2 = anyItem();
        Mockito
                .when(slowItemRepository.get(item1.getId()))
                .thenReturn(item1);
        Mockito
                .when(slowItemRepository.get(item2.getId()))
                .thenReturn(item2);

        //when
        itemsService.findItems(singleton(item1.getId()));
        itemsService.findItems(singleton(item2.getId()));
        itemsService.findItems(asList(item1.getId(), item2.getId()));

        //then
        Mockito.verify(slowItemRepository, times(1)).get(item1.getId());
        Mockito.verify(slowItemRepository, times(1)).get(item2.getId());
    }

    @Test
    public void ttl_is_set_on_items() {
        //given
        final Item item = anyItem();
        Mockito
                .when(slowItemRepository.get(item.getId()))
                .thenReturn(item);

        //when
        itemsService.findItems(singleton(item.getId()));

        //then
        long ttl = jedisConnectionFactory.getConnection().pTtl(("Item:" + item.getId()).getBytes());
        Assert.assertTrue(ttl > 0);
    }

    private Item anyItem() {
        return new Item(UUID.randomUUID(), "item", "item", singletonList(new Category(UUID.randomUUID(), "category")));
    }
}