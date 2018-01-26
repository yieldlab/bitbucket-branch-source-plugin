package com.cloudbees.jenkins.plugins.bitbucket.api.credentials;

import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketAuthenticator;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.authentication.tokens.api.AuthenticationTokenContext;
import jenkins.authentication.tokens.api.AuthenticationTokenSource;

/**
 * Provider for client-cert authenticators
 */
@Extension
public class BitbucketClientCertificateAuthenticatorSource
        extends AuthenticationTokenSource<BitbucketClientCertificateAuthenticator, StandardCertificateCredentials> {

    /**
     * Constructor.
     */
    public BitbucketClientCertificateAuthenticatorSource() {
        super(BitbucketClientCertificateAuthenticator.class, StandardCertificateCredentials.class);
    }

    /**
     * Convert a {@link StandardCertificateCredentials} into a {@link BitbucketAuthenticator}
     * @param certificateCredentials the cert
     * @return an authenticator that will use the cert
     */
    @NonNull
    @Override
    public BitbucketClientCertificateAuthenticator convert(@NonNull StandardCertificateCredentials certificateCredentials) {
        return new BitbucketClientCertificateAuthenticator(certificateCredentials);
    }

    /**
     * Whether this source works in the given context. For client certs, only HTTPS BitbucketServer instances make sense
     *
     * @param ctx
     * @return whether or not this can authenticate given the context
     */
    @Override
    public boolean isFit(AuthenticationTokenContext ctx) {
        return ctx.mustHave(BitbucketAuthenticator.PROTOCOL_PURPOSE, BitbucketAuthenticator.PROTOCOL_HTTPS)
                && ctx.mustHave(BitbucketAuthenticator.INSTANCE_TYPE_PURPOSE, BitbucketAuthenticator.INSTANCE_TYPE_SERVER);
    }
}
