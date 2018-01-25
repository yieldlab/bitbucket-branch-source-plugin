package com.cloudbees.jenkins.plugins.bitbucket.api;

import com.cloudbees.jenkins.plugins.bitbucket.endpoints.BitbucketCloudEndpoint;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import jenkins.authentication.tokens.api.AuthenticationTokenContext;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;

public abstract class BitbucketAuthenticator {

    private String id;

    public static String PROTOCOL_PURPOSE = "PROTOCOL";

    public static String PROTOCOL_HTTP = "HTTP";

    public static String PROTOCOL_HTTPS = "HTTPS";


    public static String INSTANCE_TYPE_PURPOSE = "INSTANCE_TYPE";

    public static String INSTANCE_TYPE_CLOUD = "CLOUD";

    public static String INSTANCE_TYPE_SERVER = "SERVER";

    public BitbucketAuthenticator(StandardCredentials credentials) {
        id = credentials.getId();
    }

    public String getId() {
        return id;
    }

    public void configureBuilder(HttpClientBuilder builder) { }

    public void configureContext(HttpClientContext context, HttpHost host) { }

    public void configureRequest(HttpRequest request) { }

    public static AuthenticationTokenContext<BitbucketAuthenticator> authenticationContext(String serverUrl) {
        boolean isHttps = serverUrl == null || serverUrl.startsWith("https");
        boolean isCloud = serverUrl == null || serverUrl.equals(BitbucketCloudEndpoint.SERVER_URL);

        return AuthenticationTokenContext.builder(BitbucketAuthenticator.class)
                .with(PROTOCOL_PURPOSE, isHttps ? PROTOCOL_HTTPS : PROTOCOL_HTTP)
                .with(INSTANCE_TYPE_PURPOSE, isCloud ? INSTANCE_TYPE_CLOUD : INSTANCE_TYPE_SERVER)
                .build();
    }
}
