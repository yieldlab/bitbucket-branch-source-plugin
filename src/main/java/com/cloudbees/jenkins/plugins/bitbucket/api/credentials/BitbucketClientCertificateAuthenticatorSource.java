package com.cloudbees.jenkins.plugins.bitbucket.api.credentials;

import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.authentication.tokens.api.AuthenticationTokenSource;

@Extension
public class BitbucketClientCertificateAuthenticatorSource
        extends AuthenticationTokenSource<BitbucketClientCertificateAuthenticator, StandardCertificateCredentials> {

    public BitbucketClientCertificateAuthenticatorSource() {
        super(BitbucketClientCertificateAuthenticator.class, StandardCertificateCredentials.class);
    }

    @NonNull
    @Override
    public BitbucketClientCertificateAuthenticator convert(@NonNull StandardCertificateCredentials certificateCredentials) {
        return new BitbucketClientCertificateAuthenticator(certificateCredentials);
    }
}
