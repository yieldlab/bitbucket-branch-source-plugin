package com.cloudbees.jenkins.plugins.bitbucket.client;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Cache<K, V> extends LinkedHashMap<K, V> {

    private static final int MAX_ENTRIES = 100;

    private final Map<K, Long> entryDates = new HashMap<>();

    private final long expireAfterNanos;

    public Cache(final int duration, final TimeUnit unit) {
        super(16, 0.75F, true);
        this.expireAfterNanos = unit.toNanos(duration);
    }

    @Override
    protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
        return size() > MAX_ENTRIES;
    }

    public synchronized V get(final K key, final Callable<V> callable) throws ExecutionException {
        if (isExpired(key)) {
            doRemove(key);
        }

        if (containsKey(key)) {
            return get(key);
        }

        V result;
        try {
            result = callable.call();
        } catch (final Exception e) {
            throw new ExecutionException("Cannot load value for key: " + key, e);
        }

        return doPut(key, result);
    }

    private boolean isExpired(final K key) {
        return entryDates.get(key) != null && System.nanoTime() - entryDates.get(key) > expireAfterNanos;
    }

    private void doRemove(final K key) {
        entryDates.remove(key);
        remove(key);
    }

    private V doPut(final K key, final V value) {
        entryDates.put(key, System.nanoTime());
        put(key, value);
        return value;
    }

}
