package com.cloudbees.jenkins.plugins.bitbucket.api.credentials;

import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketAuthenticator;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import hudson.util.Secret;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

/**
 * Authenticates against Bitbucket using a TLS client certificate
 */
public class BitbucketClientCertificateAuthenticator extends BitbucketAuthenticator {

    private KeyStore keyStore;
    private Secret password;

    private static final Logger LOGGER = Logger.getLogger(BitbucketClientCertificateAuthenticator.class.getName());

    /**
     * {@inheritDoc}
     */
    public BitbucketClientCertificateAuthenticator(StandardCertificateCredentials credentials) {
        super(credentials);
        keyStore = credentials.getKeyStore();
        password = credentials.getPassword();
    }

    /**
     * Sets the SSLContext for the builder to one that will connect with the selected certificate.
     * @param builder The client builder.
     */
    @Override
    public void configureBuilder(HttpClientBuilder builder) {
        try {
            builder.setSSLContext(buildSSLContext());
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException e) {
            LOGGER.log(Level.WARNING, "Failed to set up SSL context from provided client certificate: " + e.getMessage());
            // TODO: handle this error in a way that provides feedback to the user
        }
    }

    private SSLContext buildSSLContext() throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
        SSLContextBuilder contextBuilder = SSLContexts.custom();
        contextBuilder.loadKeyMaterial(keyStore, password.getPlainText().toCharArray());
        return contextBuilder.build();
    }
}
