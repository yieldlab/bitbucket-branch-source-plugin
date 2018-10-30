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
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketBranch;
import com.cloudbees.jenkins.plugins.bitbucket.client.BitbucketIntegrationClientFactory;
import com.cloudbees.jenkins.plugins.bitbucket.endpoints.BitbucketCloudEndpoint;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMRevision;
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class BitbucketCommitInformationTest {

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
        private final SCMHeadFilter headFilter;

        public SCMHeadFilterTrait(SCMHeadFilter headFilter) {
            this.headFilter = headFilter;
        }

        @Override
        protected void decorateContext(SCMSourceContext<?, ?> context) {
            context.withFilter(headFilter);
        }

        @Extension
        public static class DescriptorImpl extends SCMSourceTraitDescriptor {
        }
    }

    private static abstract class BranchSCMFilter<R> extends SCMHeadFilter {
        private Set<String> matches = new HashSet<>();

        public abstract void assertBranch(BitbucketBranch branch);

        @Override
        public boolean isExcluded(SCMSourceRequest request, SCMHead head) throws IOException, InterruptedException {
            BitbucketSCMSourceRequest bbRequest = (BitbucketSCMSourceRequest) request;
            for (BitbucketBranch branch : bbRequest.getBranches()) {
                assertBranch(branch);
                // if not failure increments the count of branches that matches
                matches.add(branch.getName());
            }
            return false;
        }

        public int getMatches() {
            return matches.size();
        }
    }

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Before
    public void setup() {
        BitbucketMockApiFactory.clear();
    }

    @Test
    public void verify_that_commit_informations_are_valued_on_cloud() throws Exception {
        BitbucketMockApiFactory.add(BitbucketCloudEndpoint.SERVER_URL, getApiMockClient(BitbucketCloudEndpoint.SERVER_URL));
        BitbucketSCMSource source = new BitbucketSCMSource("amuniz", "test-repos");

        BranchSCMFilter<Integer> commitFilter = spy(new BranchSCMFilter<Integer>() {
            @Override
            public void assertBranch(BitbucketBranch branch) {
                assertThat("commit message is not valued", Util.fixEmptyAndTrim(branch.getMessage()), notNullValue());
                assertThat("commit author is not valued", Util.fixEmptyAndTrim(branch.getAuthor()), notNullValue());
                assertTrue("commit date is not valued", branch.getDateMillis() > 0);
            }
        });

        source.setTraits(Arrays.<SCMSourceTrait> asList( //
                new BranchDiscoveryTrait(true, true), //
                new SCMHeadFilterTrait(commitFilter)));
        SCMHeadObserverImpl observer = new SCMHeadObserverImpl();
        source.fetch(observer, BitbucketClientMockUtils.getTaskListenerMock());

        // verify that the filter was called at least one time
        verify(commitFilter, atLeastOnce()).isExcluded(any(SCMSourceRequest.class), any(SCMHead.class));

        // the head branch should observe only branches which head commit was not filtered out
        assertThat(observer.getRevisions().size(), equalTo(commitFilter.getMatches()));
    }

    @Test
    public void verify_that_commit_informations_are_valued_on_server() throws Exception {
        String serverUrl = "localhost";
        BitbucketMockApiFactory.add(serverUrl, getApiMockClient(serverUrl));

        BitbucketSCMSource source = new BitbucketSCMSource("amuniz", "test-repos");
        source.setServerUrl(serverUrl);

        BranchSCMFilter<Integer> commitFilter = spy(new BranchSCMFilter<Integer>() {
            @Override
            public void assertBranch(BitbucketBranch branch) {
                assertThat("commit message is not valued", Util.fixEmptyAndTrim(branch.getMessage()), notNullValue());
                assertThat("commit author is not valued", Util.fixEmptyAndTrim(branch.getAuthor()), notNullValue());
                assertTrue("commit date is not valued", branch.getDateMillis() > 0);
            }
        });

        source.setTraits(Arrays.<SCMSourceTrait> asList( //
                new BranchDiscoveryTrait(true, true), //
                new SCMHeadFilterTrait(commitFilter)));
        SCMHeadObserverImpl observer = new SCMHeadObserverImpl();
        source.fetch(observer, BitbucketClientMockUtils.getTaskListenerMock());

        // verify that the filter was called at least one time
        verify(commitFilter, atLeastOnce()).isExcluded(any(SCMSourceRequest.class), any(SCMHead.class));

        // the head branch should observe only branches which head commit was not filtered out
        assertThat(observer.getRevisions().size(), equalTo(commitFilter.getMatches()));
    }

    private BitbucketApi getApiMockClient(String serverURL) {
        return BitbucketIntegrationClientFactory.getClient(serverURL, "amuniz", "test-repos");
    }

}
