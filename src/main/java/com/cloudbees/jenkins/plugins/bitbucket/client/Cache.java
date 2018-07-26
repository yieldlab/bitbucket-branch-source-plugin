/*
 * The MIT License
 *
 * Copyright (c) 2017-2018, bguerin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.jenkins.plugins.bitbucket.client;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Cache<K, V> {

    private static final int MAX_ENTRIES_DEFAULT = 100;

    private final Map<K, Entry<V>> entries;

    private long expireAfterNanos;

    public Cache(final int duration, final TimeUnit unit) {
        this(duration, unit, MAX_ENTRIES_DEFAULT);
    }

    public Cache(final int duration, final TimeUnit unit, final int maxEntries) {
        this.expireAfterNanos = unit.toNanos(duration);
        this.entries = new LimitedMap<>(maxEntries);
    }

    public synchronized V get(final K key, final Callable<V> callable) throws ExecutionException {
        if (isExpired(key)) {
            doRemove(key);
        }

        if (entries.containsKey(key)) {
            return entries.get(key).value;
        }

        V result;
        try {
            result = callable.call();
        } catch (final Exception e) {
            throw new ExecutionException("Cannot load value for key: " + key, e);
        }

        return doPut(key, result);
    }

    public void evictAll() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }

    public void setExpireDuration(final int duration, final TimeUnit unit) {
        this.expireAfterNanos = unit.toNanos(duration);
    }

    private boolean isExpired(final K key) {
        final Entry<V> entry = entries.get(key);
        return entry != null && System.nanoTime() - entry.nanos > expireAfterNanos;
    }

    private void doRemove(final K key) {
        entries.remove(key);
    }

    private V doPut(final K key, final V value) {
        entries.put(key, new Entry<V>(value));
        return value;
    }

    private static class LimitedMap<K, V> extends LinkedHashMap<K, V> {
        private final int maxEntries;

        public LimitedMap(final int maxEntries) {
            this.maxEntries = maxEntries;
        }

        @Override
        protected boolean removeEldestEntry(final java.util.Map.Entry<K, V> eldest) {
            return size() > maxEntries;
        }
    }

    private static class Entry<V> {
        private final V value;

        private final long nanos;

        public Entry(final V value) {
            this.value = value;
            nanos = System.nanoTime();
        }
    }

}
