package com.cloudbees.jenkins.plugins.bitbucket.server.client;

import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketApi;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketApiFactory;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketAuthenticator;
import com.cloudbees.jenkins.plugins.bitbucket.endpoints.BitbucketCloudEndpoint;
import com.cloudbees.jenkins.plugins.bitbucket.endpoints.BitbucketServerEndpoint;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import org.apache.commons.lang.StringUtils;

@Extension(ordinal = -1000)
public class BitbucketServerApiFactory extends BitbucketApiFactory {
    @Override
    protected boolean isMatch(@Nullable String serverUrl) {
        return serverUrl != null && !BitbucketCloudEndpoint.SERVER_URL.equals(serverUrl);
    }

    @NonNull
    @Override
    protected BitbucketApi create(@Nullable String serverUrl, @Nullable BitbucketAuthenticator authenticator,
                                  @NonNull String owner, @CheckForNull String repository) {
        if(StringUtils.isBlank(serverUrl)){
            throw new IllegalArgumentException("serverUrl is required");
        }

        return new BitbucketServerAPIClient(serverUrl, owner, repository, authenticator, false,
                BitbucketServerEndpoint.findWebhookImplementation(serverUrl));
    }
}
