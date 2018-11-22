/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
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
package com.cloudbees.jenkins.plugins.bitbucket.server.client;

import static java.util.Objects.requireNonNull;

import com.cloudbees.jenkins.plugins.bitbucket.JsonParser;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketApi;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketAuthenticator;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketBuildStatus;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketCommit;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketPullRequest;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketRepository;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketRepositoryProtocol;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketRepositoryType;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketRequestException;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketTeam;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketWebHook;
import com.cloudbees.jenkins.plugins.bitbucket.api.credentials.BitbucketUsernamePasswordAuthenticator;
import com.cloudbees.jenkins.plugins.bitbucket.client.repository.UserRoleInRepository;
import com.cloudbees.jenkins.plugins.bitbucket.endpoints.AbstractBitbucketEndpoint;
import com.cloudbees.jenkins.plugins.bitbucket.endpoints.BitbucketEndpointConfiguration;
import com.cloudbees.jenkins.plugins.bitbucket.endpoints.BitbucketServerEndpoint;
import com.cloudbees.jenkins.plugins.bitbucket.filesystem.BitbucketSCMFile;
import com.cloudbees.jenkins.plugins.bitbucket.server.BitbucketServerWebhookImplementation;
import com.cloudbees.jenkins.plugins.bitbucket.server.client.branch.BitbucketServerBranch;
import com.cloudbees.jenkins.plugins.bitbucket.server.client.branch.BitbucketServerBranches;
import com.cloudbees.jenkins.plugins.bitbucket.server.client.branch.BitbucketServerCommit;
import com.cloudbees.jenkins.plugins.bitbucket.server.client.pullrequest.BitbucketServerPullRequest;
import com.cloudbees.jenkins.plugins.bitbucket.server.client.pullrequest.BitbucketServerPullRequestCanMerge;
import com.cloudbees.jenkins.plugins.bitbucket.server.client.pullrequest.BitbucketServerPullRequests;
import com.cloudbees.jenkins.plugins.bitbucket.server.client.repository.BitbucketServerProject;
import com.cloudbees.jenkins.plugins.bitbucket.server.client.repository.BitbucketServerRepositories;
import com.cloudbees.jenkins.plugins.bitbucket.server.client.repository.BitbucketServerRepository;
import com.cloudbees.jenkins.plugins.bitbucket.server.client.repository.BitbucketServerWebhooks;
import com.cloudbees.jenkins.plugins.bitbucket.server.client.repository.NativeBitbucketServerWebhooks;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.impl.Operator;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ProxyConfiguration;
import hudson.Util;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMFile;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 * Bitbucket API client.
 * Developed and test with Bitbucket 4.3.2
 */
public class BitbucketServerAPIClient implements BitbucketApi {

    /**
     * Make available commit informations in a lazy way.
     *
     * @author Nikolas Falco
     */
    private class CommitClosure implements Callable<BitbucketCommit> {
        private final String hash;

        public CommitClosure(@NonNull String hash) {
            this.hash = hash;
        }

        @Override
        public BitbucketCommit call() throws Exception {
            return resolveCommit(hash);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(BitbucketServerAPIClient.class.getName());
    private static final String API_BASE_PATH = "/rest/api/1.0";
    private static final String API_REPOSITORIES_PATH = API_BASE_PATH + "/projects/{owner}/repos{?start,limit}";
    private static final String API_REPOSITORY_PATH = API_BASE_PATH + "/projects/{owner}/repos/{repo}";
    private static final String API_DEFAULT_BRANCH_PATH = API_REPOSITORY_PATH + "/branches/default";
    private static final String API_BRANCHES_PATH = API_REPOSITORY_PATH + "/branches{?start,limit}";
    private static final String API_TAGS_PATH = API_REPOSITORY_PATH + "/tags{?start,limit}";
    private static final String API_PULL_REQUESTS_PATH = API_REPOSITORY_PATH + "/pull-requests{?start,limit,at,direction,state}";
    private static final String API_PULL_REQUEST_PATH = API_REPOSITORY_PATH + "/pull-requests/{id}";
    private static final String API_PULL_REQUEST_MERGE_PATH = API_REPOSITORY_PATH + "/pull-requests/{id}/merge";
    private static final String API_BROWSE_PATH = API_REPOSITORY_PATH + "/browse{/path*}{?at}";
    private static final String API_COMMITS_PATH = API_REPOSITORY_PATH + "/commits{/hash}";
    private static final String API_PROJECT_PATH = API_BASE_PATH + "/projects/{owner}";
    private static final String API_COMMIT_COMMENT_PATH = API_REPOSITORY_PATH + "/commits{/hash}/comments";
    private static final String API_WEBHOOKS_PATH = API_BASE_PATH + "/projects/{owner}/repos/{repo}/webhooks{/id}{?start,limit}";

    private static final String WEBHOOK_BASE_PATH = "/rest/webhook/1.0";
    private static final String WEBHOOK_REPOSITORY_PATH = WEBHOOK_BASE_PATH + "/projects/{owner}/repos/{repo}/configurations";
    private static final String WEBHOOK_REPOSITORY_CONFIG_PATH = WEBHOOK_REPOSITORY_PATH + "/{id}";

    private static final String API_COMMIT_STATUS_PATH = "/rest/build-status/1.0/commits{/hash}";
    private static final Integer DEFAULT_PAGE_LIMIT = 200;

    /**
     * Repository owner.
     */
    private final String owner;

    /**
     * The repository that this object is managing.
     */
    private final String repositoryName;

    /**
     * Indicates if the client is using user-centric API endpoints or project API otherwise.
     */
    private final boolean userCentric;

    /**
     * Credentials to access API services.
     * Almost @NonNull (but null is accepted for anonymous access).
     */
    private final BitbucketAuthenticator authenticator;

    private HttpClientContext context;

    private final String baseURL;

    private final BitbucketServerWebhookImplementation webhookImplementation;

    public BitbucketServerAPIClient(@NonNull String baseURL, @NonNull String owner, @CheckForNull String repositoryName,
        @CheckForNull StandardUsernamePasswordCredentials credentials, boolean userCentric) {
        this(baseURL, owner, repositoryName, credentials != null ? new BitbucketUsernamePasswordAuthenticator(credentials) : null, userCentric, BitbucketServerEndpoint.findWebhookImplementation(baseURL));
    }

    public BitbucketServerAPIClient(@NonNull String baseURL, @NonNull String owner, @CheckForNull String repositoryName,
        @CheckForNull BitbucketAuthenticator authenticator, boolean userCentric) {
        this(baseURL, owner, repositoryName, authenticator, userCentric, BitbucketServerEndpoint.findWebhookImplementation(baseURL));
    }

    public BitbucketServerAPIClient(@NonNull String baseURL, @NonNull String owner, @CheckForNull String repositoryName,
            @CheckForNull BitbucketAuthenticator authenticator, boolean userCentric,
            @NonNull BitbucketServerWebhookImplementation webhookImplementation) {
        this.authenticator = authenticator;
        this.userCentric = userCentric;
        this.owner = owner;
        this.repositoryName = repositoryName;
        this.baseURL = Util.removeTrailingSlash(baseURL);
        this.webhookImplementation = requireNonNull(webhookImplementation);
    }

    /**
     * Bitbucket Server manages two top level entities, owner and/or project.
     * Only one of them makes sense for a specific client object.
     */
    @NonNull
    @Override
    public String getOwner() {
        return owner;
    }

    /**
     * In Bitbucket server the top level entity is the Project, but the JSON API accepts users as a replacement
     * of Projects in most of the URLs (it's called user centric API).
     *
     * This method returns the appropriate string to be placed in request URLs taking into account if this client
     * object was created as a user centric instance or not.
     *
     * @return the ~user or project
     */
    public String getUserCentricOwner() {
        return userCentric ? "~" + owner : owner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @CheckForNull
    public String getRepositoryName() {
        return repositoryName;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public String getRepositoryUri(@NonNull BitbucketRepositoryType type,
                                   @NonNull BitbucketRepositoryProtocol protocol,
                                   @CheckForNull String cloneLink,
                                   @NonNull String owner,
                                   @NonNull String repository) {
        switch (type) {
            case GIT:
                URI baseUri;
                try {
                    baseUri = new URI(baseURL);
                } catch (URISyntaxException e) {
                    throw new IllegalStateException("Server URL is not a valid URI", e);
                }

                UriTemplate template = UriTemplate.fromTemplate("{scheme}://{+authority}{+path}{/owner,repository}.git");
                template.set("owner", owner);
                template.set("repository", repository);

                switch (protocol) {
                    case HTTP:
                        template.set("scheme", baseUri.getScheme());
                        template.set("authority", baseUri.getRawAuthority());
                        template.set("path", Objects.toString(baseUri.getRawPath(), "") + "/scm");
                        break;
                    case SSH:
                        template.set("scheme", BitbucketRepositoryProtocol.SSH.getType());
                        template.set("authority", "git@" + baseUri.getHost());
                        if (cloneLink != null) {
                            try {
                                URI cloneLinkUri = new URI(cloneLink);
                                if (cloneLinkUri.getScheme() != null) {
                                    template.set("scheme", cloneLinkUri.getScheme());
                                }
                                if (cloneLinkUri.getRawAuthority() != null) {
                                    template.set("authority", cloneLinkUri.getRawAuthority());
                                }
                            } catch (@SuppressWarnings("unused") URISyntaxException ignored) {
                                // fall through
                            }
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported repository protocol: " + protocol);
                }
                return template.expand();
                default:
                    throw new IllegalArgumentException("Unsupported repository type: " + type);
        }
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public List<BitbucketServerPullRequest> getPullRequests() throws IOException, InterruptedException {
        UriTemplate template = UriTemplate
                .fromTemplate(API_PULL_REQUESTS_PATH)
                .set("owner", getUserCentricOwner())
                .set("repo", repositoryName);
        return getPullRequests(template);
    }

    @NonNull
    public List<BitbucketServerPullRequest> getOutgoingOpenPullRequests(String fromRef) throws IOException, InterruptedException {
        UriTemplate template = UriTemplate
                .fromTemplate(API_PULL_REQUESTS_PATH)
                .set("owner", getUserCentricOwner())
                .set("repo", repositoryName)
                .set("at", fromRef)
                .set("direction", "outgoing")
                .set("state", "OPEN");
        return getPullRequests(template);
    }

    @NonNull
    public List<BitbucketServerPullRequest> getIncomingOpenPullRequests(String toRef) throws IOException, InterruptedException {
        UriTemplate template = UriTemplate
                .fromTemplate(API_PULL_REQUESTS_PATH)
                .set("owner", getUserCentricOwner())
                .set("repo", repositoryName)
                .set("at", toRef)
                .set("direction", "incoming")
                .set("state", "OPEN");
        return getPullRequests(template);
    }

    private List<BitbucketServerPullRequest> getPullRequests(UriTemplate template)
            throws IOException, InterruptedException {
        List<BitbucketServerPullRequest> pullRequests = getResources(template, BitbucketServerPullRequests.class);

        // set commit closure to make commit informations available when need, in a similar way to when request branches
        for (BitbucketServerPullRequest pr : pullRequests) {
            setupClosureForPRBranch(pr);
        }
        AbstractBitbucketEndpoint endpointConfig = BitbucketEndpointConfiguration.get().findEndpoint(baseURL);
        if (endpointConfig instanceof BitbucketServerEndpoint && ((BitbucketServerEndpoint)endpointConfig).isCallCanMerge()) {
            // This is required for Bitbucket Server to update the refs/pull-requests/* references
            // See https://community.atlassian.com/t5/Bitbucket-questions/Change-pull-request-refs-after-Commit-instead-of-after-Approval/qaq-p/194702#M6829
            for (BitbucketServerPullRequest pullRequest : pullRequests) {
                pullRequest.setCanMerge(getPullRequestCanMergeById(Integer.parseInt(pullRequest.getId())));
            }
        }

        return pullRequests;
    }

    private void setupClosureForPRBranch(BitbucketServerPullRequest pr) {
        BitbucketServerBranch branch = (BitbucketServerBranch) pr.getSource().getBranch();
        branch.setCommitClosure(new CommitClosure(branch.getRawNode()));

        branch = (BitbucketServerBranch) pr.getDestination().getBranch();
        branch.setCommitClosure(new CommitClosure(branch.getRawNode()));
    }

    private boolean getPullRequestCanMergeById(@NonNull Integer id) throws IOException {
        String url = UriTemplate
                .fromTemplate(API_PULL_REQUEST_MERGE_PATH)
                .set("owner", getUserCentricOwner())
                .set("repo", repositoryName)
                .set("id", id)
                .expand();
        String response = getRequest(url);
        try {
            return JsonParser.toJava(response, BitbucketServerPullRequestCanMerge.class).isCanMerge();
        } catch (IOException e) {
            throw new IOException("I/O error when accessing URL: " + url, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public BitbucketPullRequest getPullRequestById(@NonNull Integer id) throws IOException {
        String url = UriTemplate
                .fromTemplate(API_PULL_REQUEST_PATH)
                .set("owner", getUserCentricOwner())
                .set("repo", repositoryName)
                .set("id", id)
                .expand();
        String response = getRequest(url);
        try {
            BitbucketServerPullRequest pr = JsonParser.toJava(response, BitbucketServerPullRequest.class);

            setupClosureForPRBranch(pr);

            return pr;
        } catch (IOException e) {
            throw new IOException("I/O error when accessing URL: " + url, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public BitbucketRepository getRepository() throws IOException {
        if (repositoryName == null) {
            throw new UnsupportedOperationException(
                    "Cannot get a repository from an API instance that is not associated with a repository");
        }
        String url = UriTemplate
                .fromTemplate(API_REPOSITORY_PATH)
                .set("owner", getUserCentricOwner())
                .set("repo", repositoryName)
                .expand();
        String response = getRequest(url);
        try {
            return JsonParser.toJava(response, BitbucketServerRepository.class);
        } catch (IOException e) {
            throw new IOException("I/O error when accessing URL: " + url, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postCommitComment(@NonNull String hash, @NonNull String comment) throws IOException {
        postRequest(
            UriTemplate
                .fromTemplate(API_COMMIT_COMMENT_PATH)
                .set("owner", getUserCentricOwner())
                .set("repo", repositoryName)
                .set("hash", hash)
                .expand(),
            Collections.singletonList(
                new BasicNameValuePair("text", comment)
            )
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postBuildStatus(@NonNull BitbucketBuildStatus status) throws IOException {
        postRequest(
            UriTemplate
                .fromTemplate(API_COMMIT_STATUS_PATH)
                .set("hash", status.getHash())
                .expand(),
            JsonParser.toJson(status)
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkPathExists(@NonNull String branchOrHash, @NonNull String path) throws IOException {
        String url = UriTemplate
                .fromTemplate(API_BROWSE_PATH)
                .set("owner", getUserCentricOwner())
                .set("repo", repositoryName)
                .set("path", path.split(Operator.PATH.getSeparator()))
                .set("at", branchOrHash)
                .expand();
        int status = getRequestStatus(url);
        return HttpStatus.SC_OK == status;
    }

    @CheckForNull
    @Override
    public String getDefaultBranch() throws IOException {
        String url = UriTemplate
                .fromTemplate(API_DEFAULT_BRANCH_PATH)
                .set("owner", getUserCentricOwner())
                .set("repo", repositoryName)
                .expand();
        try {
            String response = getRequest(url);
            return JsonParser.toJava(response, BitbucketServerBranch.class).getName();
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.FINE, "Could not find default branch for {0}/{1}",
                    new Object[]{this.owner, this.repositoryName});
            return null;
        } catch (IOException e) {
            throw new IOException("I/O error when accessing URL: " + url, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public List<BitbucketServerBranch> getTags() throws IOException, InterruptedException {
        return getServerBranches(API_TAGS_PATH);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public List<BitbucketServerBranch> getBranches() throws IOException, InterruptedException {
        return getServerBranches(API_BRANCHES_PATH);
    }

    private List<BitbucketServerBranch> getServerBranches(String apiPath) throws IOException, InterruptedException {
        UriTemplate template = UriTemplate
                .fromTemplate(apiPath)
                .set("owner", getUserCentricOwner())
                .set("repo", repositoryName);

        List<BitbucketServerBranch> branches = getResources(template, BitbucketServerBranches.class);
        for (final BitbucketServerBranch branch : branches) {
            branch.setCommitClosure(new CommitClosure(branch.getRawNode()));
        }

        return branches;
    }

    /** {@inheritDoc} */
    @NonNull
    @Override
    public BitbucketCommit resolveCommit(@NonNull String hash) throws IOException {
        String url = UriTemplate
                .fromTemplate(API_COMMITS_PATH)
                .set("owner", getUserCentricOwner())
                .set("repo", repositoryName)
                .set("hash", hash)
                .expand();
        try {
            String response = getRequest(url);
            return JsonParser.toJava(response, BitbucketServerCommit.class);
        } catch (IOException e) {
            throw new IOException("I/O error when accessing URL: " + url, e);
        }
    }

    /** {@inheritDoc} */
    @NonNull
    @Override
    public String resolveSourceFullHash(@NonNull BitbucketPullRequest pull) {
        return pull.getSource().getCommit().getHash();
    }

    @NonNull
    @Override
    public BitbucketCommit resolveCommit(@NonNull BitbucketPullRequest pull) throws IOException, InterruptedException {
        return resolveCommit(resolveSourceFullHash(pull));
    }

    @Override
    public void registerCommitWebHook(BitbucketWebHook hook) throws IOException, InterruptedException {
        switch (webhookImplementation) {
            case PLUGIN:
                putRequest(
                        UriTemplate
                            .fromTemplate(WEBHOOK_REPOSITORY_PATH)
                            .set("owner", getUserCentricOwner())
                            .set("repo", repositoryName)
                            .expand(),
                        JsonParser.toJson(hook)
                    );
                break;

            case NATIVE:
                postRequest(
                        UriTemplate
                            .fromTemplate(API_WEBHOOKS_PATH)
                            .set("owner", getUserCentricOwner())
                            .set("repo", repositoryName)
                            .expand(),
                        JsonParser.toJson(hook)
                    );
                break;

            default:
                LOGGER.log(Level.WARNING, "Cannot register {0} webhook.", webhookImplementation);
                break;
        }
    }

    @Override
    public void updateCommitWebHook(BitbucketWebHook hook) throws IOException, InterruptedException {
        switch (webhookImplementation) {
            case PLUGIN:
                postRequest(
                        UriTemplate
                            .fromTemplate(WEBHOOK_REPOSITORY_CONFIG_PATH)
                            .set("owner", getUserCentricOwner())
                            .set("repo", repositoryName)
                            .set("id", hook.getUuid())
                            .expand(), JsonParser.toJson(hook)
                    );
                break;

            case NATIVE:
                putRequest(
                        UriTemplate
                            .fromTemplate(API_WEBHOOKS_PATH)
                            .set("owner", getUserCentricOwner())
                            .set("repo", repositoryName)
                            .set("id", hook.getUuid())
                            .expand(), JsonParser.toJson(hook)
                    );
                break;

            default:
                LOGGER.log(Level.WARNING, "Cannot update {0} webhook.", webhookImplementation);
                break;
        }
    }

    @Override
    public void removeCommitWebHook(BitbucketWebHook hook) throws IOException, InterruptedException {
        switch (webhookImplementation) {
            case PLUGIN:
                deleteRequest(
                        UriTemplate
                            .fromTemplate(WEBHOOK_REPOSITORY_CONFIG_PATH)
                            .set("owner", getUserCentricOwner())
                            .set("repo", repositoryName)
                            .set("id", hook.getUuid())
                            .expand()
                    );
                break;

            case NATIVE:
                deleteRequest(
                        UriTemplate
                            .fromTemplate(API_WEBHOOKS_PATH)
                            .set("owner", getUserCentricOwner())
                            .set("repo", repositoryName)
                            .set("id", hook.getUuid())
                            .expand()
                    );
                break;

            default:
                LOGGER.log(Level.WARNING, "Cannot remove {0} webhook.", webhookImplementation);
                break;
        }
    }

    @NonNull
    @Override
    public List<? extends BitbucketWebHook> getWebHooks() throws IOException, InterruptedException {
        switch (webhookImplementation) {
            case PLUGIN:
                String url = UriTemplate
                        .fromTemplate(WEBHOOK_REPOSITORY_PATH)
                        .set("owner", getUserCentricOwner())
                        .set("repo", repositoryName)
                        .expand();
                String response = getRequest(url);
                return JsonParser.toJava(response, BitbucketServerWebhooks.class);
            case NATIVE:
                UriTemplate urlTemplate = UriTemplate
                        .fromTemplate(API_WEBHOOKS_PATH)
                        .set("owner", getUserCentricOwner())
                        .set("repo", repositoryName);
                return getResources(urlTemplate, NativeBitbucketServerWebhooks.class);
        }

        return Collections.emptyList();
    }

    /**
     * There is no such Team concept in Bitbucket Server but Project.
     */
    @Override
    public BitbucketTeam getTeam() throws IOException {
        if (userCentric) {
            return null;
        } else {
            String url = UriTemplate.fromTemplate(API_PROJECT_PATH).set("owner", getOwner()).expand();
            try {
                String response = getRequest(url);
                return JsonParser.toJava(response, BitbucketServerProject.class);
            } catch (FileNotFoundException e) {
                return null;
            } catch (IOException e) {
                throw new IOException("I/O error when accessing URL: " + url, e);
            }
        }
    }

    /**
     * The role parameter is ignored for Bitbucket Server.
     */
    @NonNull
    @Override
    public List<BitbucketServerRepository> getRepositories(@CheckForNull UserRoleInRepository role)
            throws IOException, InterruptedException {
        UriTemplate template = UriTemplate
                .fromTemplate(API_REPOSITORIES_PATH)
                .set("owner", getUserCentricOwner());

        List<BitbucketServerRepository> repositories;
        try {
            repositories = getResources(template, BitbucketServerRepositories.class);
        } catch (FileNotFoundException e) {
            return new ArrayList<>();
        }
        Collections.sort(repositories, new Comparator<BitbucketServerRepository>() {
            @Override
            public int compare(BitbucketServerRepository o1, BitbucketServerRepository o2) {
                return o1.getRepositoryName().compareTo(o2.getRepositoryName());
            }
        });

        return repositories;
    }

    /** {@inheritDoc} */
    @NonNull
    @Override
    public List<BitbucketServerRepository> getRepositories() throws IOException, InterruptedException {
        return getRepositories(null);
    }

    @Override
    public boolean isPrivate() throws IOException {
        return getRepository().isPrivate();
    }

    private <V> List<V> getResources(UriTemplate template, Class<? extends PagedApiResponse<V>> clazz) throws IOException, InterruptedException {
        template.set("start", 0).set("limit", DEFAULT_PAGE_LIMIT);
        String url = template.expand();

        try {
            List<V> resources = new ArrayList<>();
            String response = getRequest(url);
            PagedApiResponse<V> page = JsonParser.toJava(response, clazz);
            resources.addAll(page.getValues());
            while (!page.isLastPage()) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                Integer limit = page.getLimit();
                url = template
                        .set("start", page.getNextPageStart())
                        .set("limit", limit == null ? DEFAULT_PAGE_LIMIT : limit)
                        .expand();
                response = getRequest(url);
                page = JsonParser.toJava(response, clazz);
                resources.addAll(page.getValues());
            }
            return resources;
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new IOException("I/O error when accessing URL: " + url, e);
        }
    }

    protected String getRequest(String path) throws IOException {
        HttpGet httpget = new HttpGet(this.baseURL + path);

        if (authenticator != null) {
            authenticator.configureRequest(httpget);
        }

        try(CloseableHttpClient client = getHttpClient(httpget);
                CloseableHttpResponse response = client.execute(httpget, context)) {
            String content;
            long len = response.getEntity().getContentLength();
            if (len == 0) {
                content = "";
            } else {
                ByteArrayOutputStream buf;
                if (len > 0 && len <= Integer.MAX_VALUE / 2) {
                    buf = new ByteArrayOutputStream((int) len);
                } else {
                    buf = new ByteArrayOutputStream();
                }
                try (InputStream is = response.getEntity().getContent()) {
                    IOUtils.copy(is, buf);
                }
                content = new String(buf.toByteArray(), StandardCharsets.UTF_8);
            }
            EntityUtils.consume(response.getEntity());
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                throw new FileNotFoundException("URL: " + path);
            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new BitbucketRequestException(response.getStatusLine().getStatusCode(),
                        "HTTP request error. Status: " + response.getStatusLine().getStatusCode()
                                + ": " + response.getStatusLine().getReasonPhrase() + ".\n" + response);
            }
            return content;
        } catch (BitbucketRequestException | FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new IOException("Communication error for url: " + path, e);
        } finally {
            httpget.releaseConnection();
        }
    }

    /**
     * Create HttpClient from given host/port
     * @param request the {@link HttpRequestBase} for which an HttpClient will be created
     * @return CloseableHttpClient
     */
    private CloseableHttpClient getHttpClient(final HttpRequestBase request) {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

        RequestConfig.Builder requestConfig = RequestConfig.custom();
        requestConfig.setConnectTimeout(10 * 1000);
        requestConfig.setConnectionRequestTimeout(60 * 1000);
        requestConfig.setSocketTimeout(60 * 1000);
        request.setConfig(requestConfig.build());

        final String host = getMethodHost(request);

        if (authenticator != null) {
            authenticator.configureBuilder(httpClientBuilder);

            context = HttpClientContext.create();
            authenticator.configureContext(context, HttpHost.create(host));
        }

        setClientProxyParams(host, httpClientBuilder);

        return httpClientBuilder.build();
    }

    private void setClientProxyParams(String host, HttpClientBuilder builder) {
        Jenkins jenkins = Jenkins.getInstance();
        ProxyConfiguration proxyConfig = null;
        if (jenkins != null) {
            proxyConfig = jenkins.proxy;
        }

        final Proxy proxy;

        if (proxyConfig != null) {
            URI hostURI = URI.create(host);
            proxy = proxyConfig.createProxy(hostURI.getHost());
        } else {
             proxy = Proxy.NO_PROXY;
        }

        if (proxy.type() != Proxy.Type.DIRECT) {
            final InetSocketAddress proxyAddress = (InetSocketAddress)proxy.address();
            LOGGER.log(Level.FINE, "Jenkins proxy: {0}", proxy.address());
            builder.setProxy(new HttpHost(proxyAddress.getHostName(), proxyAddress.getPort()));
            String username = proxyConfig.getUserName();
            String password = proxyConfig.getPassword();
            if (username != null && !"".equals(username.trim())) {
                LOGGER.fine("Using proxy authentication (user=" + username + ")");
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
                AuthCache authCache = new BasicAuthCache();
                authCache.put(HttpHost.create(proxyAddress.getHostName()), new BasicScheme());
                context = HttpClientContext.create();
                context.setCredentialsProvider(credentialsProvider);
                context.setAuthCache(authCache);
            }
        }
    }

    private int getRequestStatus(String path) throws IOException {
        HttpGet httpget = new HttpGet(this.baseURL + path);
        if (authenticator != null) {
            authenticator.configureRequest(httpget);
        }

        try(CloseableHttpClient client = getHttpClient(httpget);
                CloseableHttpResponse response = client.execute(httpget, context)) {
            EntityUtils.consume(response.getEntity());
            return response.getStatusLine().getStatusCode();
        } finally {
            httpget.releaseConnection();
        }
    }

    private static String getMethodHost(HttpRequestBase method) {
        URI uri = method.getURI();
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        return scheme + "://" + uri.getAuthority();
    }

    private String postRequest(String path, List<? extends NameValuePair> params) throws IOException {
        HttpPost request = new HttpPost(this.baseURL + path);
        request.setEntity(new UrlEncodedFormEntity(params));
        return postRequest(request);
    }

    private String postRequest(String path, String content) throws IOException {
        HttpPost request = new HttpPost(this.baseURL + path);
        request.setEntity(new StringEntity(content, ContentType.create("application/json", "UTF-8")));
        return postRequest(request);
    }

    private String nameValueToJson(NameValuePair[] params) {
        JSONObject o = new JSONObject();
        for (NameValuePair pair : params) {
            o.put(pair.getName(), pair.getValue());
        }
        return o.toString();
    }

    private String postRequest(HttpPost httppost) throws IOException {
        return doRequest(httppost);
    }

    private String doRequest(HttpRequestBase request) throws IOException {
        if (authenticator != null) {
            authenticator.configureRequest(request);
        }

        try(CloseableHttpClient client = getHttpClient(request);
                CloseableHttpResponse response = client.execute(request, context)) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                EntityUtils.consume(response.getEntity());
                // 204, no content
                return "";
            }
            String content;
            long len = -1L;
            Header[] headers = request.getHeaders("Content-Length");
            if (headers != null && headers.length > 0) {
                int i = headers.length - 1;
                len = -1L;
                while (i >= 0) {
                    Header header = headers[i];
                    try {
                        len = Long.parseLong(header.getValue());
                        break;
                    } catch (NumberFormatException var5) {
                        --i;
                    }
                }
            }
            if (len == 0) {
                content = "";
            } else {
                ByteArrayOutputStream buf;
                if (len > 0 && len <= Integer.MAX_VALUE / 2) {
                    buf = new ByteArrayOutputStream((int) len);
                } else {
                    buf = new ByteArrayOutputStream();
                }
                try (InputStream is = response.getEntity().getContent()) {
                    IOUtils.copy(is, buf);
                }
                content = new String(buf.toByteArray(), StandardCharsets.UTF_8);
            }
            EntityUtils.consume(response.getEntity());
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK && response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                throw new BitbucketRequestException(response.getStatusLine().getStatusCode(), "HTTP request error. Status: " + response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase() + ".\n" + response);
            }
            return content;
        } finally {
            request.releaseConnection();
        }
    }

    private String putRequest(String path, String content) throws IOException {
        HttpPut request = new HttpPut(this.baseURL + path);
        request.setEntity(new StringEntity(content, ContentType.create("application/json", "UTF-8")));
        return doRequest(request);
    }

    private String deleteRequest(String path) throws IOException {
        HttpDelete request = new HttpDelete(this.baseURL + path);
        return doRequest(request);
    }

    @Override
    public Iterable<SCMFile> getDirectoryContent(BitbucketSCMFile directory) throws IOException, InterruptedException {
        List<SCMFile> files = new ArrayList<>();
        int start=0;
        UriTemplate template = UriTemplate
                .fromTemplate(API_BROWSE_PATH + "{&start,limit}")
                .set("owner", getUserCentricOwner())
                .set("repo", repositoryName)
                .set("path", directory.getPath().split(Operator.PATH.getSeparator()))
                .set("at", directory.getRef())
                .set("start", start)
                .set("limit", 500);
        String url = template.expand();
        String response = getRequest(url);
        Map<String,Object> content = JsonParser.mapper.readValue(response, new TypeReference<Map<String,Object>>(){});
        Map page = (Map) content.get("children");
        List<Map> values = (List<Map>) page.get("values");
        collectFileAndDirectories(directory, values, files);
        while (!(boolean)page.get("isLastPage")){
            start += (int) content.get("size");
            url = template
                    .set("start", start)
                    .expand();
            response = getRequest(url);
            content = JsonParser.mapper.readValue(response, new TypeReference<Map<String,Object>>(){});
            page = (Map) content.get("children");
        }
        return files;
    }

    private void collectFileAndDirectories(BitbucketSCMFile parent, List<Map> values, List<SCMFile> files) {
        for(Map file:values) {
            String type = (String) file.get("type");
            List<String> components = (List<String>) ((Map)file.get("path")).get("components");
            SCMFile.Type fileType = null;
            if(type.equals("FILE")){
                fileType = SCMFile.Type.REGULAR_FILE;
            } else if(type.equals("DIRECTORY")){
                fileType = SCMFile.Type.DIRECTORY;
            }
            if(components.size() > 0 && fileType != null){
                // revision is set to null as fetched values from server API do not give us revision hash
                // Later on hash is not needed anyways when file content is fetched from server API
                files.add(new BitbucketSCMFile(parent, components.get(0), fileType, null));
            }
        }
    }

    @Override
    public InputStream getFileContent(BitbucketSCMFile file) throws IOException, InterruptedException {
        List<String> lines = new ArrayList<>();
        int start=0;
        UriTemplate template = UriTemplate
                .fromTemplate(API_BROWSE_PATH + "{&start,limit}")
                .set("owner", getUserCentricOwner())
                .set("repo", repositoryName)
                .set("path", file.getPath().split(Operator.PATH.getSeparator()))
                .set("at", file.getRef())
                .set("start", start)
                .set("limit", 500);
        String url = template.expand();
        String response = getRequest(url);
        Map<String,Object> content = collectLines(response, lines);

        while(!(boolean)content.get("isLastPage")){
            start += (int) content.get("size");
            url = template
                    .set("start", start)
                    .expand();
            response = getRequest(url);
            content = collectLines(response, lines);
        }
        return IOUtils.toInputStream(StringUtils.join(lines,'\n'), "UTF-8");
    }

    private Map<String,Object> collectLines(String response, final List<String> lines) throws IOException {
        Map<String,Object> content = JsonParser.mapper.readValue(response, new TypeReference<Map<String,Object>>(){});
        List<Map<String, String>> lineMap = (List<Map<String, String>>) content.get("lines");
        for(Map<String,String> line: lineMap){
            String text = line.get("text");
            if(text != null){
                lines.add(text);
            }
        }
        return content;
    }

}
