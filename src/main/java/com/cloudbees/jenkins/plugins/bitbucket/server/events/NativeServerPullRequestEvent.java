package com.cloudbees.jenkins.plugins.bitbucket.server.events;

import com.cloudbees.jenkins.plugins.bitbucket.server.client.pullrequest.BitbucketServerPullRequest;

public class NativeServerPullRequestEvent {

    private BitbucketServerPullRequest pullRequest;

    public BitbucketServerPullRequest getPullRequest() {
        return pullRequest;
    }

}
