package com.cloudbees.jenkins.plugins.bitbucket.server;
/** The different webhook implementations available for Bitbucket Server. */
public enum BitbucketServerWebhookImplementation {
    /** Plugin-based webhooks. */
    PLUGIN {
        @Override
        public String toString() {
            return "Plugin";
        }
    },

    /** Native webhooks, available since Bitbucket Server 5.4. */
    NATIVE {
        @Override
        public String toString() {
            return "Native";
        }
    }
}

