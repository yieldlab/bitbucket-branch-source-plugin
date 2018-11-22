/*
 * The MIT License
 *
 * Copyright (c) 2016-2017, CloudBees, Inc.
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
package com.cloudbees.jenkins.plugins.bitbucket.hooks;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Bitbucket hooks types managed by this plugin.
 */
public enum HookEventType {

    /**
     * See <a href="https://confluence.atlassian.com/bitbucket/event-payloads-740262817.html#EventPayloads-Push">EventPayloads-Push</a>
     */
    PUSH("repo:push", PushHookProcessor.class),

    /**
     * See <a href="https://confluence.atlassian.com/bitbucket/event-payloads-740262817.html#EventPayloads-Created.1">EventPayloads-Created</a>
     */
    PULL_REQUEST_CREATED("pullrequest:created", PullRequestHookProcessor.class),

    /**
     * See <a href="https://confluence.atlassian.com/bitbucket/event-payloads-740262817.html#EventPayloads-Updated.1">EventPayloads-Updated</a>
     */
    PULL_REQUEST_UPDATED("pullrequest:updated", PullRequestHookProcessor.class),

    /**
     * See <a href="https://confluence.atlassian.com/bitbucket/event-payloads-740262817.html#EventPayloads-Merged">EventPayloads-Merged</a>
     */
    PULL_REQUEST_MERGED("pullrequest:fulfilled", PullRequestHookProcessor.class),

    /**
     * See <a href="https://confluence.atlassian.com/bitbucket/event-payloads-740262817.html#EventPayloads-Declined">EventPayloads-Declined</a>
     */
    PULL_REQUEST_DECLINED("pullrequest:rejected", PullRequestHookProcessor.class),

    /**
     * @see <a href="https://confluence.atlassian.com/bitbucketserver054/event-payload-939508609.html#Eventpayload-Push">Eventpayload-Push</a>
     * @since Bitbucket Server 5.4
     */
    SERVER_REFS_CHANGED("repo:refs_changed", NativeServerPushHookProcessor.class),

    /**
     * @see <a href="https://confluence.atlassian.com/bitbucketserver054/event-payload-939508609.html#Eventpayload-Opened">Eventpayload-Opened</a>
     * @since Bitbucket Server 5.4
     */
    SERVER_PULL_REQUEST_OPENED("pr:opened", NativeServerPullRequestHookProcessor.class),

    /**
     * @see <a href="https://confluence.atlassian.com/bitbucketserver054/event-payload-939508609.html#Eventpayload-Merged">Eventpayload-Merged</a>
     * @since Bitbucket Server 5.4
     */
    SERVER_PULL_REQUEST_MERGED("pr:merged", NativeServerPullRequestHookProcessor.class),

    /**
     * See <a href="https://confluence.atlassian.com/bitbucketserver054/event-payload-939508609.html#Eventpayload-Declined">Eventpayload-Declined</a>
     * @since Bitbucket Server 5.4
     */
    SERVER_PULL_REQUEST_DECLINED("pr:declined", NativeServerPullRequestHookProcessor.class),

    /**
     * Sent when hitting the {@literal "Test connection"} button in Bitbucket Server. Apparently undocumented.
     */
    SERVER_PING("diagnostics:ping", PingHookProcessor.class);


    private final String key;
    private final Class<?> clazz;

    <P extends HookProcessor> HookEventType(@NonNull String key, Class<P> clazz) {
        this.key = key;
        this.clazz = clazz;
    }

    @CheckForNull
    public static HookEventType fromString(String key) {
        for (HookEventType value : HookEventType.values()) {
            if (value.getKey().equals(key)) {
                return value;
            }
        }
        return null;
    }

    public HookProcessor getProcessor() {
        try {
            return (HookProcessor) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new AssertionError("Can not instantiate hook payload processor: " + e.getMessage());
        }
    }

    public String getKey() {
        return key;
    }

}
