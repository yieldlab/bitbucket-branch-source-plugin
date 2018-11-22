/*
 * The MIT License
 *
 * Copyright (c) 2017, CloudBees, Inc.
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

import com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMSource;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketWebHook;
import com.cloudbees.jenkins.plugins.bitbucket.client.repository.BitbucketRepositoryHook;
import com.cloudbees.jenkins.plugins.bitbucket.endpoints.BitbucketCloudEndpoint;
import com.cloudbees.jenkins.plugins.bitbucket.endpoints.BitbucketServerEndpoint;
import com.cloudbees.jenkins.plugins.bitbucket.server.client.repository.BitbucketServerWebhook;
import com.cloudbees.jenkins.plugins.bitbucket.server.client.repository.NativeBitbucketServerWebhook;
import com.damnhandy.uri.template.UriTemplate;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import jenkins.model.Jenkins;

/**
 * Contains the webhook configuration
 */
public class WebhookConfiguration {

    /**
     * The list of events available in Bitbucket Cloud.
     */
    private static final List<String> CLOUD_EVENTS = Collections.unmodifiableList(Arrays.asList(
            HookEventType.PUSH.getKey(),
            HookEventType.PULL_REQUEST_CREATED.getKey(),
            HookEventType.PULL_REQUEST_UPDATED.getKey(),
            HookEventType.PULL_REQUEST_MERGED.getKey(),
            HookEventType.PULL_REQUEST_DECLINED.getKey()
    ));

    /**
     * The list of events available in Bitbucket Server.
     */
    private static final List<String> NATIVE_SERVER_EVENTS = Collections.unmodifiableList(Arrays.asList(
            HookEventType.SERVER_REFS_CHANGED.getKey(),
            HookEventType.SERVER_PULL_REQUEST_OPENED.getKey(),
            HookEventType.SERVER_PULL_REQUEST_MERGED.getKey(),
            HookEventType.SERVER_PULL_REQUEST_DECLINED.getKey()
    ));

    /**
     * The title of the webhook.
     */
    private static final String description = "Jenkins hook";

    /**
     * The comma separated list of committers to ignore.
     */
    private final String committersToIgnore;

    public WebhookConfiguration() {
        this.committersToIgnore = null;
    }

    public WebhookConfiguration(@CheckForNull final String committersToIgnore) {
        this.committersToIgnore = committersToIgnore;
    }

    boolean updateHook(BitbucketWebHook hook, BitbucketSCMSource owner) {
        if (hook instanceof BitbucketRepositoryHook) {
            if (!hook.getEvents().containsAll(CLOUD_EVENTS)) {
                Set<String> events = new TreeSet<>(hook.getEvents());
                events.addAll(CLOUD_EVENTS);
                BitbucketRepositoryHook repoHook = (BitbucketRepositoryHook) hook;
                repoHook.setEvents(new ArrayList<>(events));
                return true;
            }

            return false;
        }

        if (hook instanceof BitbucketServerWebhook) {
            BitbucketServerWebhook serverHook = (BitbucketServerWebhook) hook;

            // Handle null case
            String hookCommittersToIgnore = ((BitbucketServerWebhook) hook).getCommittersToIgnore();
            if (hookCommittersToIgnore == null) {
                hookCommittersToIgnore = "";
            }

            // Handle null case
            String thisCommittersToIgnore = committersToIgnore;
            if (thisCommittersToIgnore == null) {
                thisCommittersToIgnore = "";
            }

            if (!hookCommittersToIgnore.trim().equals(thisCommittersToIgnore.trim())) {
                serverHook.setCommittersToIgnore(committersToIgnore);
                return true;
            }

            return false;
        }

        if (hook instanceof NativeBitbucketServerWebhook) {
            boolean updated = false;

            NativeBitbucketServerWebhook serverHook = (NativeBitbucketServerWebhook) hook;
            String url = getNativeServerWebhookUrl(owner.getServerUrl());

            if (!url.equals(serverHook.getUrl())) {
                serverHook.setUrl(url);
                updated = true;
            }

            List<String> events = serverHook.getEvents();
            if (events == null) {
                serverHook.setEvents(NATIVE_SERVER_EVENTS);
                updated = true;
            } else if (!events.containsAll(NATIVE_SERVER_EVENTS)) {
                Set<String> newEvents = new TreeSet<>(events);
                newEvents.addAll(NATIVE_SERVER_EVENTS);
                serverHook.setEvents(new ArrayList<>(newEvents));
                updated = true;
            }

            return updated;
        }

        return false;
    }

    BitbucketWebHook getHook(BitbucketSCMSource owner) {
        final String serverUrl = owner.getServerUrl();
        if (BitbucketCloudEndpoint.SERVER_URL.equals(serverUrl)) {
            BitbucketRepositoryHook hook = new BitbucketRepositoryHook();
            hook.setEvents(CLOUD_EVENTS);
            hook.setActive(true);
            hook.setDescription(description);
            hook.setUrl(Jenkins.getActiveInstance().getRootUrl() + BitbucketSCMSourcePushHookReceiver.FULL_PATH);
            return hook;
        }

        switch (BitbucketServerEndpoint.findWebhookImplementation(serverUrl)) {
            case NATIVE: {
                NativeBitbucketServerWebhook hook = new NativeBitbucketServerWebhook();
                hook.setActive(true);
                hook.setEvents(NATIVE_SERVER_EVENTS);
                hook.setDescription(description);
                hook.setUrl(getNativeServerWebhookUrl(serverUrl));
                return hook;
            }

            case PLUGIN:
            default: {
                BitbucketServerWebhook hook = new BitbucketServerWebhook();
                hook.setActive(true);
                hook.setDescription(description);
                hook.setUrl(Jenkins.getActiveInstance().getRootUrl() + BitbucketSCMSourcePushHookReceiver.FULL_PATH);
                hook.setCommittersToIgnore(committersToIgnore);
                return hook;
            }
        }
    }

    private static String getNativeServerWebhookUrl(String serverUrl) {
        return UriTemplate.buildFromTemplate(Jenkins.getActiveInstance().getRootUrl())
            .template(BitbucketSCMSourcePushHookReceiver.FULL_PATH)
            .query("server_url")
            .build()
            .set("server_url", serverUrl)
            .expand();
    }
}
