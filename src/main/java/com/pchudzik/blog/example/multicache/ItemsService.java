package com.pchudzik.blog.example.multicache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class ItemsService {
    private final SlowItemRepository repository;
    private final ItemsCache itemsCache;

    public List<Item> findItems(Collection<UUID> ids) {
        final List<Item> foundInCache = itemsCache.findItems(ids);
        final List<Item> notCached = loadMissingItems(ids, foundInCache);

        final List<Item> result = Stream
                .concat(foundInCache.stream(), notCached.stream())
                .collect(toList());

        itemsCache.saveItems(notCached);

        return result;
    }

    private List<Item> loadMissingItems(Collection<UUID> ids, List<Item> foundInCache) {
        final Set<UUID> cachedIds = foundInCache.stream()
                .map(Item::getId)
                .collect(Collectors.toSet());
        return ids.stream()
                .filter(id -> !cachedIds.contains(id))
                .map(this::lookupInRepository)
                .collect(toList());
    }

    private Item lookupInRepository(UUID id) {
        return repository.get(id);
    }
}
