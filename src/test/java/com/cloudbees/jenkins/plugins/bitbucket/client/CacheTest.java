package com.cloudbees.jenkins.plugins.bitbucket.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class CacheTest {

    @Test
    public void ensure_cache_hit() throws Exception {
        final Cache<String, Long> cache = new Cache<>(5, TimeUnit.HOURS);
        final Callable<Long> callable = mock(Callable.class);
        when(callable.call()).thenReturn(1L);

        assertEquals(Long.valueOf(1L), cache.get("a key", callable));
        assertEquals(Long.valueOf(1L), cache.get("a key", callable));

        verify(callable).call();
        verifyNoMoreInteractions(callable);
    }

    @Test
    public void ensure_expiration_works() throws Exception {
        final Cache<String, Long> cache = new Cache<>(1, TimeUnit.NANOSECONDS);
        final Callable<Long> callable = mock(Callable.class);
        when(callable.call()).thenReturn(1L);

        assertEquals(Long.valueOf(1L), cache.get("a key", callable));
        Thread.sleep(200);
        assertEquals(Long.valueOf(1L), cache.get("a key", callable));

        verify(callable, times(2)).call();
        verifyNoMoreInteractions(callable);
    }
}
