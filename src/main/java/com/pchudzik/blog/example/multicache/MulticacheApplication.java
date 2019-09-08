package com.pchudzik.blog.example.multicache;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@Slf4j
@SpringBootApplication
public class MulticacheApplication {

    public static void main(String[] args) {
        final ConfigurableApplicationContext appCtx = SpringApplication.run(MulticacheApplication.class, args);
        final SlowItemRepository itemRepository = appCtx.getBean(SlowItemRepository.class);
        final ItemsService itemsService = appCtx.getBean(ItemsService.class);

        final Item item1 = createItem();
        final Item item2 = createItem();
        final Item item3 = createItem();
        final Item item4 = createItem();

        Stream.of(item1, item2, item3, item4).forEach(item -> itemRepository.put(item.getId(), item));

        meassure(
                () -> itemsService.findItems(asList(item1.getId(), item2.getId())),
                (watch, result) -> log.info("Loaded {} items in {}", result.size(), watch));

        meassure(
                () -> itemsService.findItems(singletonList(item1.getId())),
                (watch, result) -> log.info("Loaded {} item in {}", result.size(), watch));

        meassure(
                () -> itemsService.findItems(Stream
                        .of(item1, item2, item3, item4)
                        .map(Item::getId)
                        .collect(Collectors.toList())),
                (watch, result) -> log.info("Loaded {} item in {}", result.size(), watch));
    }

    @SneakyThrows
    private static <V> void meassure(Callable<V> action, BiConsumer<StopWatch, V> logger) {
        StopWatch watch = new StopWatch();
        watch.start();

        final V result = action.call();

        watch.stop();

        logger.accept(watch, result);
    }

    public static Item createItem() {
        return new Item(
                UUID.randomUUID(),
                RandomStringUtils.random(10, true, false),
                RandomStringUtils.random(20, false, true),
                asList(
                        new Category(UUID.randomUUID(), "category1"),
                        new Category(UUID.randomUUID(), "category2")));
    }
}
