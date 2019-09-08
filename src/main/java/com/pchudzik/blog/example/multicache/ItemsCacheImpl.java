package com.pchudzik.blog.example.multicache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class ItemsCacheImpl implements ItemsCache {
    private final RedisTemplate<String, Item> redisTemplate;

    @Override
    public List<Item> findItems(Collection<UUID> ids) {
        final List<Item> result = redisTemplate.opsForValue()
                .multiGet(ids.stream().map(this::generateCacheKey).collect(toList()))
                .stream()
                .filter(Objects::nonNull)
                .collect(toList());

        refreshTTL(result);

        return result;
    }

    @Override
    public void saveItems(Collection<Item> items) {
        redisTemplate.opsForValue()
                .multiSet(items.stream()
                        .collect(Collectors.toMap(
                                item -> generateCacheKey(item.getId()),
                                Function.identity())));
        refreshTTL(items);
    }

    private void refreshTTL(Collection<Item> result) {
        redisTemplate.executePipelined((SessionCallback) callback -> {
            result.stream()
                    .map(item -> generateCacheKey(item.getId()))
                    .forEach(key -> callback.expire(key, 10, TimeUnit.MINUTES));
            return null;
        });
    }

    private String generateCacheKey(UUID id) {
        return "Item:" + id;
    }
}
