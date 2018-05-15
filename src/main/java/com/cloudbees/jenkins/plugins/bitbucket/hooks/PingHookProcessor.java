package com.cloudbees.jenkins.plugins.bitbucket.hooks;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PingHookProcessor extends HookProcessor {

    private static final Logger LOGGER = Logger.getLogger(PingHookProcessor.class.getName());

    @Override
    public void process(HookEventType hookEvent, String payload, BitbucketType instanceType, String origin) {
        LOGGER.log(Level.INFO, "Received webhook ping event from {0}", origin);
    }

}
