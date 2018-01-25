package com.cloudbees.jenkins.plugins.bitbucket.api.credentials;

import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketAuthenticator;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.authentication.tokens.api.AuthenticationTokenContext;
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

    @Override
    public boolean isFit(AuthenticationTokenContext ctx) {
        return ctx.mustHave(BitbucketAuthenticator.PROTOCOL_PURPOSE, BitbucketAuthenticator.PROTOCOL_HTTPS)
                && ctx.mustHave(BitbucketAuthenticator.INSTANCE_TYPE_PURPOSE, BitbucketAuthenticator.INSTANCE_TYPE_SERVER);
    }
}
