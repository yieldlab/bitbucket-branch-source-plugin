package com.cloudbees.jenkins.plugins.bitbucket.server;

import hudson.model.ModelObject;

/** The different webhook implementations available for Bitbucket Server. */
public enum BitbucketServerWebhookImplementation implements ModelObject {
    /** Plugin-based webhooks. */
    PLUGIN {
        @Override
        public String getDisplayName() {
            return "Plugin";
        }
    },

    /** Native webhooks, available since Bitbucket Server 5.4. */
    NATIVE {
        @Override
        public String getDisplayName() {
            return "Native";
        }
    }
}

