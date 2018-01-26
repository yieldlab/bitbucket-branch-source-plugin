package com.cloudbees.jenkins.plugins.bitbucket.api;

import com.cloudbees.jenkins.plugins.bitbucket.endpoints.BitbucketCloudEndpoint;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import jenkins.authentication.tokens.api.AuthenticationTokenContext;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Support for various different methods of authenticating with Bitbucket
 */
public abstract class BitbucketAuthenticator {

    private String id;

    /**
     * The URL protocol as reported in an {@link AuthenticationTokenContext}
     */
    public static final String PROTOCOL_PURPOSE = "PROTOCOL";

    /**
     * The purpose value for HTTP instances
     */
    public static final String PROTOCOL_HTTP = "HTTP";

    /**
     * The purpose value for HTTPS instances
     */
    public static final String PROTOCOL_HTTPS = "HTTPS";

    /**
     * The Bitbucket instance type as reported in an {@link AuthenticationTokenContext}
     */
    public static final String INSTANCE_TYPE_PURPOSE = "INSTANCE_TYPE";

    /**
     * Purpose value for bitbucket cloud (i.e. bitbucket.org)
     */
    public static final String INSTANCE_TYPE_CLOUD = "CLOUD";

    /**
     * Purpose value for bitbucket server
     */
    public static final String INSTANCE_TYPE_SERVER = "SERVER";

    /**
     * Constructor
     *
     * @param credentials credentials instance this authenticator will use
     */
    public BitbucketAuthenticator(StandardCredentials credentials) {
        id = credentials.getId();
    }

    /**
     * @return id of the credentials used.
     */
    public String getId() {
        return id;
    }

    /**
     * Configures an {@link HttpClientBuilder}. Override if you need to adjust connection setup.
     * @param builder The client builder.
     */
    public void configureBuilder(HttpClientBuilder builder) { }

    /**
     * Configures an {@link HttpClientContext}. Override
     * @param context The connection context
     * @param host host being connected to
     */
    public void configureContext(HttpClientContext context, HttpHost host) { }

    /**
     * Configures an {@link HttpRequest}. Override this if your authentication method needs to set headers on a
     * per-request basis.
     *
     * @param request the request.
     */
    public void configureRequest(HttpRequest request) { }

    /**
     * Generates context that sub-classes can use to determine if they would be able to authenticate against the
     * provided server.
     *
     * @param serverUrl The URL being authenticated against
     * @return an {@link AuthenticationTokenContext} for use with the AuthenticationTokens APIs
     */
    public static AuthenticationTokenContext<BitbucketAuthenticator> authenticationContext(String serverUrl) {
        boolean isHttps = serverUrl == null || serverUrl.startsWith("https");
        boolean isCloud = serverUrl == null || serverUrl.equals(BitbucketCloudEndpoint.SERVER_URL);

        return AuthenticationTokenContext.builder(BitbucketAuthenticator.class)
                .with(PROTOCOL_PURPOSE, isHttps ? PROTOCOL_HTTPS : PROTOCOL_HTTP)
                .with(INSTANCE_TYPE_PURPOSE, isCloud ? INSTANCE_TYPE_CLOUD : INSTANCE_TYPE_SERVER)
                .build();
    }
}
