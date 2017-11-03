package com.cloudbees.jenkins.plugins.bitbucket.hooks;

import com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMSourceContext;
import jenkins.scm.api.SCMHeadObserver;
import org.junit.Test;

import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertThat;

public class WebhookConfigurationTraitTest {

    @Test
    public void given__webhookConfiguration__when__appliedToContext__then__sameWebhookConfiguration()
            throws Exception {
        String committersToIgnore = "someUserNameToIgnoreOnCommit";
        BitbucketSCMSourceContext ctx = new BitbucketSCMSourceContext(null, SCMHeadObserver.none());
        WebhookConfigurationTrait instance = new WebhookConfigurationTrait(committersToIgnore);
        instance.decorateContext(ctx);
        assertThat(ctx.webhookConfiguration(), samePropertyValuesAs(new WebhookConfiguration(committersToIgnore)));
    }

}
