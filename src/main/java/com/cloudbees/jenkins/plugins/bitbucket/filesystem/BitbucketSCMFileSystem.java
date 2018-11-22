/*
 * The MIT License
 *
 * Copyright (c) 2016 CloudBees, Inc.
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
 *
 */

package com.cloudbees.jenkins.plugins.bitbucket.filesystem;

import com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMSource;
import com.cloudbees.jenkins.plugins.bitbucket.BitbucketTagSCMHead;
import com.cloudbees.jenkins.plugins.bitbucket.BranchSCMHead;
import com.cloudbees.jenkins.plugins.bitbucket.PullRequestSCMHead;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketApi;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketApiFactory;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketAuthenticator;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.scm.SCM;
import hudson.security.ACL;
import java.io.IOException;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;

public class BitbucketSCMFileSystem extends SCMFileSystem {

    private final String ref;
    private final BitbucketApi api;

    protected BitbucketSCMFileSystem(BitbucketApi api, String ref, SCMRevision rev) throws IOException {
        super(rev);
        this.ref = ref;
        this.api = api;
    }

    /**
     * Return timestamp of last commit or of tag if its annotated tag.
     *
     * @return timestamp of last commit or of tag if its annotated tag
     */
    @Override
    public long lastModified() throws IOException {
        // TODO figure out how to implement this
        return 0L;
    }

    @NonNull
    @Override
    public SCMFile getRoot() {
        SCMRevision revision = getRevision();
        return new BitbucketSCMFile(this, api, ref, revision == null ? null : revision.toString());
    }

    @Extension
    public static class BuilderImpl extends SCMFileSystem.Builder {

        @Override
        public boolean supports(SCM source) {
            //TODO: Determine supported by checking if its git/hg bitbucket scm with proper credentials and non wildcard branch
            return false;
        }

        @Override
        public boolean supports(SCMSource source) {
            return source instanceof BitbucketSCMSource;
        }

        @Override
        public SCMFileSystem build(@NonNull Item owner, @NonNull SCM scm, @CheckForNull SCMRevision rev) {
            return null;
        }

        private static StandardCredentials lookupScanCredentials(@CheckForNull Item context,
                @CheckForNull String scanCredentialsId, String serverUrl) {
            if (Util.fixEmpty(scanCredentialsId) == null) {
                return null;
            } else {
                return CredentialsMatchers.firstOrNull(
                        CredentialsProvider.lookupCredentials(
                                StandardCredentials.class,
                                context,
                                context instanceof Queue.Task
                                        ? Tasks.getDefaultAuthenticationOf((Queue.Task) context)
                                        : ACL.SYSTEM
                        ),
                        CredentialsMatchers.allOf(
                                CredentialsMatchers.withId(scanCredentialsId),
                                AuthenticationTokens.matcher(BitbucketAuthenticator.authenticationContext(serverUrl))
                        )
                );
            }
        }

        @Override
        public SCMFileSystem build(@NonNull SCMSource source, @NonNull SCMHead head, @CheckForNull SCMRevision rev)
                throws IOException, InterruptedException {
            BitbucketSCMSource src = (BitbucketSCMSource) source;

            String credentialsId = src.getCredentialsId();
            String owner = src.getRepoOwner();
            String repository = src.getRepository();
            String serverUrl = src.getServerUrl();
            StandardCredentials credentials;
            credentials = lookupScanCredentials(src.getOwner(), credentialsId, serverUrl);

            BitbucketAuthenticator authenticator = AuthenticationTokens.convert(BitbucketAuthenticator.authenticationContext(serverUrl), credentials);

            BitbucketApi apiClient = BitbucketApiFactory.newInstance(serverUrl, authenticator, owner, repository);
            String ref;
            if (head instanceof BranchSCMHead) {
                ref = head.getName();
            } else if (head instanceof PullRequestSCMHead) {
                PullRequestSCMHead pr = (PullRequestSCMHead) head;
                if (!(pr.getCheckoutStrategy() == ChangeRequestCheckoutStrategy.MERGE) && pr.getRepository() != null) {
                    return new BitbucketSCMFileSystem(apiClient, pr.getOriginName(), rev);
                }
                return null; // TODO support merge revisions somehow
            } else if (head instanceof BitbucketTagSCMHead) {
                 ref = "tags/" + head.getName();
            } else {
                return null;
            }

            return new BitbucketSCMFileSystem(apiClient, ref, rev);
        }
    }
}
