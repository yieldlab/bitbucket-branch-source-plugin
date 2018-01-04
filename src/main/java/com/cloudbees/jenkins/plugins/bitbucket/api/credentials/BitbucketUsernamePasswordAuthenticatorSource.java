package com.cloudbees.jenkins.plugins.bitbucket.api.credentials;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.authentication.tokens.api.AuthenticationTokenSource;

@Extension
public class BitbucketUsernamePasswordAuthenticatorSource extends AuthenticationTokenSource<BitbucketUsernamePasswordAuthenticator, StandardUsernamePasswordCredentials> {

    public BitbucketUsernamePasswordAuthenticatorSource() {
        super(BitbucketUsernamePasswordAuthenticator.class, StandardUsernamePasswordCredentials.class);
    }

    @NonNull
    @Override
    public BitbucketUsernamePasswordAuthenticator convert(@NonNull StandardUsernamePasswordCredentials standardUsernamePasswordCredentials) {
        return new BitbucketUsernamePasswordAuthenticator(standardUsernamePasswordCredentials);
    }
}
