package com.cloudbees.jenkins.plugins.bitbucket.hooks;

import java.util.logging.Level;
import java.util.logging.Logger;

public class NativeServerPullRequestHookProcessor extends HookProcessor {

    private static final Logger LOGGER = Logger.getLogger(NativeServerPullRequestHookProcessor.class.getName());

    @Override
    public void process(HookEventType hookEvent, String payload, BitbucketType instanceType, String origin) {
        LOGGER.log(Level.INFO, "Received {0} hook from Bitbucket, which is not implemented. ", hookEvent);
    }
}
