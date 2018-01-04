package com.cloudbees.jenkins.plugins.bitbucket.api.credentials;

import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketAuthenticator;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import javax.net.ssl.SSLContext;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

public class BitbucketClientCertificateAuthenticator extends BitbucketAuthenticator<StandardCertificateCredentials> {

    public BitbucketClientCertificateAuthenticator(StandardCertificateCredentials credentials) {
        super(credentials);
    }

    @Override
    public void configureBuilder(HttpClientBuilder builder) {
        try {
            builder.setSSLContext(buildSSLContext());
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException ignored) {

        }
    }

    private SSLContext buildSSLContext() throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
        SSLContextBuilder contextBuilder = SSLContexts.custom();
        contextBuilder.loadKeyMaterial(credentials.getKeyStore(), credentials.getPassword().getPlainText().toCharArray());
        return contextBuilder.build();
    }
}
