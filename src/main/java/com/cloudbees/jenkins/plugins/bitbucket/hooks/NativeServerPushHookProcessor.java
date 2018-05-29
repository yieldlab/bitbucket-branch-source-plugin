package com.cloudbees.jenkins.plugins.bitbucket.hooks;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMNavigator;
import com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMSource;
import com.cloudbees.jenkins.plugins.bitbucket.BranchSCMHead;
import com.cloudbees.jenkins.plugins.bitbucket.JsonParser;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketRepositoryType;
import com.cloudbees.jenkins.plugins.bitbucket.server.events.NativeServerRefsChangedEvent;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.scm.SCM;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMEvent;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;

public class NativeServerPushHookProcessor extends HookProcessor {

    private static final Logger LOGGER = Logger.getLogger(NativeServerPushHookProcessor.class.getName());

    @Override
    public void process(HookEventType hookEvent, String payload, BitbucketType instanceType, String origin) {
        if (payload == null) {
            return;
        }

        final NativeServerRefsChangedEvent refsChangedEvent;
        try {
            refsChangedEvent = JsonParser.toJava(payload, NativeServerRefsChangedEvent.class);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Can not read hook payload", e);
            return;
        }

        String owner = refsChangedEvent.getRepository().getOwnerName();
        final String repository = refsChangedEvent.getRepository().getRepositoryName();
        if (refsChangedEvent.getChanges().isEmpty()) {
            LOGGER.log(Level.INFO, "Received hook from Bitbucket. Processing push event on {0}/{1}",
                    new Object[] { owner, repository });
            scmSourceReIndex(owner, repository);
            return;
        }

        Multimap<SCMEvent.Type, NativeServerRefsChangedEvent.Change> events = HashMultimap.create();
        for (NativeServerRefsChangedEvent.Change change : refsChangedEvent.getChanges()) {
            final String type = change.getType();
            if ("UPDATE".equals(type)) {
                events.put(SCMEvent.Type.UPDATED, change);
            } else if ("DELETE".equals(type)) {
                events.put(SCMEvent.Type.REMOVED, change);
            } else if ("ADD".equals(type)) {
                events.put(SCMEvent.Type.CREATED, change);
            } else {
                LOGGER.log(Level.INFO, "Unknown change event type of {} received from Bitbucket Server", type);
            }
        }

        for (SCMEvent.Type type : events.keySet()) {
            SCMHeadEvent.fireNow(new HeadEvent(type, events.get(type), origin, refsChangedEvent));
        }
    }

    private static final class HeadEvent extends SCMHeadEvent<Collection<NativeServerRefsChangedEvent.Change>> {
        private final NativeServerRefsChangedEvent refsChangedEvent;

        HeadEvent(Type type, Collection<NativeServerRefsChangedEvent.Change> payload, String origin,
                NativeServerRefsChangedEvent refsChangedEvent) {
            super(type, payload, origin);
            this.refsChangedEvent = refsChangedEvent;
        }

        @Override
        public boolean isMatch(@NonNull SCMNavigator navigator) {
            if (!(navigator instanceof BitbucketSCMNavigator)) {
                return false;
            }
            final BitbucketSCMNavigator bbNav = (BitbucketSCMNavigator) navigator;
            return isServerUrlMatch(bbNav.getServerUrl())
                    && bbNav.getRepoOwner().equalsIgnoreCase(refsChangedEvent.getRepository().getOwnerName());
        }

        private boolean isServerUrlMatch(String serverUrl) {
            // FIXME verify that we have the right instance
            return true;
        }

        @NonNull
        @Override
        public String getSourceName() {
            return refsChangedEvent.getRepository().getRepositoryName();
        }

        @NonNull
        @Override
        public Map<SCMHead, SCMRevision> heads(@NonNull SCMSource source) {
            if (!(source instanceof BitbucketSCMSource)) {
                return Collections.emptyMap();
            }
            BitbucketSCMSource src = (BitbucketSCMSource) source;
            if (!isServerUrlMatch(src.getServerUrl())) {
                return Collections.emptyMap();
            }
            if (!src.getRepoOwner().equalsIgnoreCase(refsChangedEvent.getRepository().getOwnerName())) {
                return Collections.emptyMap();
            }
            if (!src.getRepository().equalsIgnoreCase(refsChangedEvent.getRepository().getRepositoryName())) {
                return Collections.emptyMap();
            }
            BitbucketRepositoryType type = BitbucketRepositoryType
                .fromString(refsChangedEvent.getRepository().getScm());
            if (type == null) {
                LOGGER.log(Level.INFO, "Received event for unknown repository type: {0}",
                        refsChangedEvent.getRepository().getScm());
                return Collections.emptyMap();
            }
            Map<SCMHead, SCMRevision> result = new HashMap<>();
            for (NativeServerRefsChangedEvent.Change change : getPayload()) {
                if ("BRANCH".equals(change.getRef().getType())) {
                    BranchSCMHead head = new BranchSCMHead(change.getRef().getDisplayId(), type);
                    final SCMRevision revision;
                    if (getType() == SCMEvent.Type.REMOVED) {
                        revision = null;
                    } else if (type == BitbucketRepositoryType.GIT) {
                        revision = new AbstractGitSCMSource.SCMRevisionImpl(head, change.getToHash());
                    } else {
                        LOGGER.log(Level.INFO, "Received event for unsupported repository type: {0}", type);
                        continue;
                    }

                    result.put(head, revision);
                } else {
                    LOGGER.log(Level.INFO, "Received event for unknown ref type: {0}", change.getRef().getType());
                }
            }
            return result;
        }

        @Override
        public boolean isMatch(@NonNull SCM scm) {
            // TODO
            return false;
        }
    }
}
