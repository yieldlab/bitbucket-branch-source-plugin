package com.cloudbees.jenkins.plugins.bitbucket.api.credentials;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.authentication.tokens.api.AuthenticationTokenSource;

/**
 * Source for username/password authenticators.
 */
@Extension
public class BitbucketUsernamePasswordAuthenticatorSource extends AuthenticationTokenSource<BitbucketUsernamePasswordAuthenticator, StandardUsernamePasswordCredentials> {

    /**
     * Constructor.
     */
    public BitbucketUsernamePasswordAuthenticatorSource() {
        super(BitbucketUsernamePasswordAuthenticator.class, StandardUsernamePasswordCredentials.class);
    }

    /**
     * Converts username/password credentials to an authenticator.
     * @param standardUsernamePasswordCredentials the username/password combo
     * @return an authenticator that will use them.
     */
    @NonNull
    @Override
    public BitbucketUsernamePasswordAuthenticator convert(@NonNull StandardUsernamePasswordCredentials standardUsernamePasswordCredentials) {
        return new BitbucketUsernamePasswordAuthenticator(standardUsernamePasswordCredentials);
    }
}
