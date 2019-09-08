package com.pchudzik.blog.example.multicache;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface ItemsCache {
    List<Item> findItems(Collection<UUID> ids);

    void saveItems(Collection<Item> items);
}
