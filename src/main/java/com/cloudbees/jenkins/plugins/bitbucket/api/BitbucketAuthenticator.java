package com.cloudbees.jenkins.plugins.bitbucket.api;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;

public abstract class BitbucketAuthenticator {

    private String id;

    public String getId() {
        return id;
    }

    public BitbucketAuthenticator(StandardCredentials credentials) {
        this.id = credentials.getId();
    }

    public void configureBuilder(HttpClientBuilder builder) { }

    public void configureContext(HttpClientContext context, HttpHost host) { }

    public void configureRequest(HttpRequest request) { }
}
