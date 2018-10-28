/*
 * The MIT License
 *
 * Copyright (c) 2018, Nikolas Falco
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.jenkins.plugins.bitbucket;

import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketApi;
import com.cloudbees.jenkins.plugins.bitbucket.client.BitbucketIntegrationClientFactory;
import com.cloudbees.jenkins.plugins.bitbucket.endpoints.BitbucketCloudEndpoint;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.trait.SCMHeadFilter;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class BitbucketGitSCMRevisionTest {

    private final class SCMHeadObserverImpl extends SCMHeadObserver {

        public List<SCMRevision> revisions = new ArrayList<>();

        @Override
        public void observe(@NonNull SCMHead head, @NonNull SCMRevision revision) {
            revisions.add(revision);
        }

        public List<SCMRevision> getRevisions() {
            return revisions;
        }
    }

    private static class SCMHeadFilterTrait extends SCMSourceTrait {
        private String branchName;

        public SCMHeadFilterTrait(String branchName) {
            this.branchName = branchName;
        }

        @Override
        protected void decorateContext(SCMSourceContext<?, ?> context) {
            context.withFilter(new SCMHeadFilter() {
                
                @Override
                public boolean isExcluded(SCMSourceRequest request, SCMHead head) throws IOException, InterruptedException {
                    return !(branchName.equals(head.getName()));
                }
            });
        }

        @Extension
        public static class DescriptorImpl extends SCMSourceTraitDescriptor {
        }
    }

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Before
    public void setup() {
        BitbucketMockApiFactory.clear();
    }

    @Test
    public void verify_that_revision_is_valued_for_PRs_on_cloud() throws Exception {
        BitbucketMockApiFactory.add(BitbucketCloudEndpoint.SERVER_URL, getApiMockClient(BitbucketCloudEndpoint.SERVER_URL));
        BitbucketSCMSource source = new BitbucketSCMSource("amuniz", "test-repos");

        source.setTraits(Arrays.<SCMSourceTrait> asList( //
                new OriginPullRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.HEAD)), //
                new SCMHeadFilterTrait("PR-1")));
        SCMHeadObserverImpl observer = new SCMHeadObserverImpl();
        source.fetch(observer, BitbucketClientMockUtils.getTaskListenerMock());

        // the PR observed should be only branches that matches PR-1
        assertThat(observer.getRevisions().size(), equalTo(1));

        PullRequestSCMRevision<?> pullRev = (PullRequestSCMRevision<?>) observer.getRevisions().get(0);

        // check that PR has correct revision with full info
        BitbucketGitSCMRevision sourceRevision = (BitbucketGitSCMRevision) pullRev.getPull();
        assertThat(sourceRevision.getHash(), equalTo("bf0e8b7962c024026ad01ae09d3a11732e26c0d4"));
        assertThat(sourceRevision.getMessage(), equalTo("[CI] Release version 1.0.0"));
        assertThat(sourceRevision.getAuthor(), equalTo("Builder <no-reply@acme.com>"));

        BitbucketGitSCMRevision targetRevision = (BitbucketGitSCMRevision) pullRev.getTarget();
        assertThat(targetRevision.getHash(), equalTo("bf4f4ce8a3a8d5c7dbfe7d609973a81a6c6664cf"));
        assertThat(targetRevision.getMessage(), equalTo("Add sample script hello world"));
        assertThat(targetRevision.getAuthor(), equalTo("Antonio Muniz <amuniz@example.com>"));
    }

    @Test
    public void verify_that_revision_is_valued_for_PRs_on_server() throws Exception {
        String serverUrl = "localhost";
        BitbucketMockApiFactory.add(serverUrl, getApiMockClient(serverUrl));

        BitbucketSCMSource source = new BitbucketSCMSource("amuniz", "test-repos");
        source.setServerUrl(serverUrl);

        source.setTraits(Arrays.<SCMSourceTrait> asList( //
                new OriginPullRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.HEAD)), //
                new SCMHeadFilterTrait("PR-1")));
        SCMHeadObserverImpl observer = new SCMHeadObserverImpl();
        source.fetch(observer, BitbucketClientMockUtils.getTaskListenerMock());

        // the PR observed should be only branches that matches PR-1
        assertThat(observer.getRevisions().size(), equalTo(1));

        PullRequestSCMRevision<?> pullRev = (PullRequestSCMRevision<?>) observer.getRevisions().get(0);

        // check that PR has correct revision with full info
        BitbucketGitSCMRevision sourceRevision = (BitbucketGitSCMRevision) pullRev.getPull();
        assertThat(sourceRevision.getHash(), equalTo("bf0e8b7962c024026ad01ae09d3a11732e26c0d4"));
        assertThat(sourceRevision.getMessage(), equalTo("[CI] Release version 1.0.0"));
        assertThat(sourceRevision.getAuthor(), equalTo("Builder <no-reply@acme.com>"));

        BitbucketGitSCMRevision targetRevision = (BitbucketGitSCMRevision) pullRev.getTarget();
        assertThat(targetRevision.getHash(), equalTo("bf4f4ce8a3a8d5c7dbfe7d609973a81a6c6664cf"));
        assertThat(targetRevision.getMessage(), equalTo("Add sample script hello world"));
        assertThat(targetRevision.getAuthor(), equalTo("Antonio Muniz <amuniz@example.com>"));
    }

    private BitbucketApi getApiMockClient(String serverURL) {
        return BitbucketIntegrationClientFactory.getClient(serverURL, "amuniz", "test-repos");
    }

}
