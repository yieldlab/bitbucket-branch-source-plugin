package com.cloudbees.jenkins.plugins.bitbucket.api;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;

public abstract class BitbucketAuthenticator<T extends StandardCredentials> {

    public final T credentials;

    public String getId() {
        return credentials.getId();
    }

    public BitbucketAuthenticator(T credentials) {
        this.credentials = credentials;
    }

    public void configureBuilder(HttpClientBuilder builder) { }

    public void configureContext(HttpClientContext context, HttpHost host) { }

    public void configureRequest(HttpRequest request) { }
}
