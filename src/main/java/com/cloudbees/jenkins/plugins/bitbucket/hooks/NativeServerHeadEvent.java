package com.cloudbees.jenkins.plugins.bitbucket.hooks;

import com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMNavigator;
import com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMSource;
import com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMSourceContext;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketRepository;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketRepositoryType;
import com.cloudbees.jenkins.plugins.bitbucket.endpoints.BitbucketCloudEndpoint;
import com.cloudbees.jenkins.plugins.bitbucket.server.client.repository.BitbucketServerRepository;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.scm.SCM;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;

abstract class NativeServerHeadEvent<P> extends SCMHeadEvent<P> {
    private static final Logger LOGGER = Logger.getLogger(NativeServerHeadEvent.class.getName());

    @NonNull
    private final String serverUrl;

    NativeServerHeadEvent(String serverUrl, Type type, P payload, String origin) {
        super(type, payload, origin);
        this.serverUrl = serverUrl;
    }

    @NonNull
    @Override
    public String getSourceName() {
        return getRepository().getRepositoryName();
    }

    @Override
    public boolean isMatch(@NonNull SCMNavigator navigator) {
        if (!(navigator instanceof BitbucketSCMNavigator)) {
            return false;
        }

        final BitbucketSCMNavigator bbNav = (BitbucketSCMNavigator) navigator;

        return isServerUrlMatch(bbNav.getServerUrl()) && bbNav.getRepoOwner().equalsIgnoreCase(getRepository().getOwnerName());
    }

    @Override
    public boolean isMatch(@NonNull SCM scm) {
        // TODO
        return false;
    }

    @NonNull
    @Override
    public Map<SCMHead, SCMRevision> heads(@NonNull SCMSource source) {
        final BitbucketSCMSource src = getMatchingBitbucketSource(source);
        return src == null ? Collections.<SCMHead, SCMRevision> emptyMap() : heads(src);
    }

    protected abstract BitbucketServerRepository getRepository();

    @NonNull
    protected abstract Map<SCMHead, SCMRevision> heads(@NonNull BitbucketSCMSource source);

    protected boolean isServerUrlMatch(String serverUrl) {
        if (serverUrl == null || BitbucketCloudEndpoint.SERVER_URL.equals(serverUrl)) {
            return false; // this is Bitbucket Cloud, which is not handled by this processor
        }

        return serverUrl.equals(this.serverUrl);
    }

    protected boolean eventMatchesRepo(BitbucketSCMSource source) {
        final BitbucketRepository repo = getRepository();
        return repo.getOwnerName().equalsIgnoreCase(source.getRepoOwner())
            && repo.getRepositoryName().equalsIgnoreCase(source.getRepository());
    }

    protected BitbucketSCMSourceContext contextOf(BitbucketSCMSource source) {
        return new BitbucketSCMSourceContext(null, SCMHeadObserver.none()).withTraits(source.getTraits());
    }

    private BitbucketSCMSource getMatchingBitbucketSource(SCMSource source) {
        if (!(source instanceof BitbucketSCMSource)) {
            return null;
        }

        final BitbucketSCMSource src = (BitbucketSCMSource) source;
        if (!isServerUrlMatch(src.getServerUrl())) {
            return null;
        }

        final BitbucketRepositoryType type = BitbucketRepositoryType.fromString(getRepository().getScm());
        if (type != BitbucketRepositoryType.GIT) {
            LOGGER.log(Level.INFO, "Received event for unknown repository type: {0}", getRepository().getScm());
            return null;
        }

        return src;
    }
}
