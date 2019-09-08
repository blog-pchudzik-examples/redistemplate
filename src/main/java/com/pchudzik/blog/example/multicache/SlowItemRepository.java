package com.pchudzik.blog.example.multicache;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class SlowItemRepository {
    private final Map<UUID, Item> repository = new HashMap<>();

    public void put(UUID id, Item item) {
        repository.put(id, item);
    }

    @SneakyThrows
    public Item get(UUID id) {
        Thread.sleep(1000L);
        return repository.get(id);
    }
}
