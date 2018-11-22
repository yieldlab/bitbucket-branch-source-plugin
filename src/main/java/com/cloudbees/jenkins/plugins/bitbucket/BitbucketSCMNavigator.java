/*
 * The MIT License
 *
 * Copyright (c) 2016-2017, CloudBees, Inc.
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
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketApiFactory;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketAuthenticator;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketHref;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketRepository;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketTeam;
import com.cloudbees.jenkins.plugins.bitbucket.client.repository.UserRoleInRepository;
import com.cloudbees.jenkins.plugins.bitbucket.endpoints.AbstractBitbucketEndpoint;
import com.cloudbees.jenkins.plugins.bitbucket.endpoints.BitbucketCloudEndpoint;
import com.cloudbees.jenkins.plugins.bitbucket.endpoints.BitbucketEndpointConfiguration;
import com.cloudbees.jenkins.plugins.bitbucket.server.client.repository.BitbucketServerProject;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.damnhandy.uri.template.UriTemplate;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.RestrictedSince;
import hudson.Util;
import hudson.console.HyperlinkNote;
import hudson.model.Action;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.model.queue.Tasks;
import hudson.plugins.git.GitSCM;
import hudson.plugins.mercurial.MercurialSCM;
import hudson.plugins.mercurial.traits.MercurialBrowserSCMSourceTrait;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import jenkins.model.Jenkins;
import jenkins.plugins.git.traits.GitBrowserSCMSourceTrait;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMNavigatorDescriptor;
import jenkins.scm.api.SCMNavigatorEvent;
import jenkins.scm.api.SCMNavigatorOwner;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceCategory;
import jenkins.scm.api.SCMSourceObserver;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.metadata.ObjectMetadataAction;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.trait.SCMNavigatorRequest;
import jenkins.scm.api.trait.SCMNavigatorTrait;
import jenkins.scm.api.trait.SCMNavigatorTraitDescriptor;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMTrait;
import jenkins.scm.api.trait.SCMTraitDescriptor;
import jenkins.scm.impl.UncategorizedSCMSourceCategory;
import jenkins.scm.impl.form.NamedArrayList;
import jenkins.scm.impl.trait.Discovery;
import jenkins.scm.impl.trait.RegexSCMSourceFilterTrait;
import jenkins.scm.impl.trait.Selection;
import jenkins.scm.impl.trait.WildcardSCMHeadFilterTrait;
import org.apache.commons.lang.StringUtils;
import org.jenkins.ui.icon.Icon;
import org.jenkins.ui.icon.IconSet;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class BitbucketSCMNavigator extends SCMNavigator {

    private static final Logger LOGGER = Logger.getLogger(BitbucketSCMSource.class.getName());

    @NonNull
    private String serverUrl;
    @CheckForNull
    private String credentialsId;
    @NonNull
    private final String repoOwner;
    @NonNull
    private List<SCMTrait<? extends SCMTrait<?>>> traits;
    @Deprecated
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.2.0")
    private transient String checkoutCredentialsId;
    @Deprecated
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.2.0")
    private transient String pattern;
    @Deprecated
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.2.0")
    private transient boolean autoRegisterHooks;
    @Deprecated
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.2.0")
    private transient String includes;
    @Deprecated
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.2.0")
    private transient String excludes;
    @Deprecated
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.2.0")
    private transient String bitbucketServerUrl;


    @DataBoundConstructor
    public BitbucketSCMNavigator(String repoOwner) {
        this.serverUrl = BitbucketCloudEndpoint.SERVER_URL;
        this.repoOwner = repoOwner;
        this.traits = new ArrayList<>();
        this.credentialsId = null; // highlighting the default is anonymous unless you configure explicitly
    }

    @Deprecated // retained for binary compatibility
    public BitbucketSCMNavigator(String repoOwner, String credentialsId, String checkoutCredentialsId) {
        this.serverUrl = BitbucketCloudEndpoint.SERVER_URL;
        this.repoOwner = repoOwner;
        this.traits = new ArrayList<>();
        this.credentialsId = Util.fixEmpty(credentialsId);
        // code invoking legacy constructor will want the legacy discovery model
        this.traits.add(new BranchDiscoveryTrait(true, true));
        this.traits.add(new OriginPullRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.HEAD)));
        this.traits.add(new ForkPullRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.HEAD),
                new ForkPullRequestDiscoveryTrait.TrustEveryone()));
        this.traits.add(new PublicRepoPullRequestFilterTrait());
        if (checkoutCredentialsId != null
                && !BitbucketSCMSource.DescriptorImpl.SAME.equals(checkoutCredentialsId)) {
            this.traits.add(new SSHCheckoutTrait(checkoutCredentialsId));
        }
    }

    @SuppressWarnings({"ConstantConditions", "deprecation"})
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
                        justification = "Only non-null after we set them here!")
    private Object readResolve() throws ObjectStreamException {
        if (serverUrl == null) {
            serverUrl = BitbucketEndpointConfiguration.get().readResolveServerUrl(bitbucketServerUrl);
        }
        if (traits == null) {
            // legacy instance, reconstruct traits to reflect legacy behaviour
            traits = new ArrayList<>();
            this.traits.add(new BranchDiscoveryTrait(true, true));
            this.traits.add(new OriginPullRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.HEAD)));
            this.traits.add(new ForkPullRequestDiscoveryTrait(
                    EnumSet.of(ChangeRequestCheckoutStrategy.HEAD),
                    new ForkPullRequestDiscoveryTrait.TrustEveryone())
            );
            this.traits.add(new PublicRepoPullRequestFilterTrait());
            if ((includes != null && !"*".equals(includes)) || (excludes != null && !"".equals(excludes))) {
                traits.add(new WildcardSCMHeadFilterTrait(
                        StringUtils.defaultIfBlank(includes, "*"),
                        StringUtils.defaultIfBlank(excludes, "")));
            }
            if (checkoutCredentialsId != null
                    && !BitbucketSCMSource.DescriptorImpl.SAME.equals(checkoutCredentialsId)
                    && !checkoutCredentialsId.equals(credentialsId)) {
                traits.add(new SSHCheckoutTrait(checkoutCredentialsId));
            }
            traits.add(new WebhookRegistrationTrait(
                    autoRegisterHooks ? WebhookRegistration.ITEM : WebhookRegistration.DISABLE)
            );
            if (pattern != null && !".*".equals(pattern)) {
                traits.add(new RegexSCMSourceFilterTrait(pattern));
            }
        }
        return this;
    }

    @CheckForNull
    public String getCredentialsId() {
        return credentialsId;
    }

    public String getRepoOwner() {
        return repoOwner;
    }

    @NonNull
    public List<SCMTrait<?>> getTraits() {
        return Collections.<SCMTrait<?>>unmodifiableList(traits);
    }

    @DataBoundSetter
    public void setCredentialsId(@CheckForNull String credentialsId) {
        this.credentialsId = Util.fixEmpty(credentialsId);
    }

    @SuppressWarnings("unchecked")
    @DataBoundSetter
    public void setTraits(@NonNull List<SCMTrait> traits) {
        // the reduced generics in the method signature are a workaround for JENKINS-26535
        this.traits = new ArrayList<>((List)/*defensive*/Util.fixNull(traits));
    }

    public String getServerUrl() {
        return serverUrl;
    }

    @DataBoundSetter
    public void setServerUrl(String serverUrl) {
        serverUrl = BitbucketEndpointConfiguration.normalizeServerUrl(serverUrl);
        if (!StringUtils.equals(this.serverUrl, serverUrl)) {
            this.serverUrl = serverUrl;
            resetId();
        }
    }

    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    @DataBoundSetter
    public void setPattern(String pattern) {
        for (int i = 0; i < traits.size(); i++) {
            SCMTrait<?> trait = traits.get(i);
            if (trait instanceof RegexSCMSourceFilterTrait) {
                if (".*".equals(pattern)) {
                    traits.remove(i);
                } else {
                    traits.set(i, new RegexSCMSourceFilterTrait(pattern));
                }
                return;
            }
        }
        if (!".*".equals(pattern)) {
            traits.add(new RegexSCMSourceFilterTrait(pattern));
        }
    }

    @Deprecated
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.2.0")
    @DataBoundSetter
    public void setAutoRegisterHooks(boolean autoRegisterHook) {
        for (Iterator<SCMTrait<? extends SCMTrait<?>>> iterator = traits.iterator(); iterator.hasNext(); ) {
            if (iterator.next() instanceof WebhookRegistrationTrait) {
                iterator.remove();
            }
        }
        traits.add(new WebhookRegistrationTrait(
                autoRegisterHook ? WebhookRegistration.ITEM : WebhookRegistration.DISABLE
        ));
    }

    @Deprecated
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.2.0")
    public boolean isAutoRegisterHooks() {
        for (SCMTrait<? extends SCMTrait<?>> t : traits) {
            if (t instanceof WebhookRegistrationTrait) {
                return ((WebhookRegistrationTrait) t).getMode() != WebhookRegistration.DISABLE;
            }
        }
        return true;
    }


    @Deprecated
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.2.0")
    @NonNull
    public String getCheckoutCredentialsId() {
        for (SCMTrait<?> t : traits) {
            if (t instanceof SSHCheckoutTrait) {
                return StringUtils.defaultString(((SSHCheckoutTrait) t).getCredentialsId(), BitbucketSCMSource
                        .DescriptorImpl.ANONYMOUS);
            }
        }
        return BitbucketSCMSource.DescriptorImpl.SAME;
    }

    @Deprecated
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.2.0")
    @DataBoundSetter
    public void setCheckoutCredentialsId(String checkoutCredentialsId) {
        for (Iterator<SCMTrait<? extends SCMTrait<?>>> iterator = traits.iterator(); iterator.hasNext(); ) {
            if (iterator.next() instanceof SSHCheckoutTrait) {
                iterator.remove();
            }
        }
        if (checkoutCredentialsId != null && !BitbucketSCMSource.DescriptorImpl.SAME.equals(checkoutCredentialsId)) {
            traits.add(new SSHCheckoutTrait(checkoutCredentialsId));
        }
    }

    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    public String getPattern() {
        for (SCMTrait<?> trait : traits) {
            if (trait instanceof RegexSCMSourceFilterTrait) {
                return ((RegexSCMSourceFilterTrait) trait).getRegex();
            }
        }
        return ".*";
    }

    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    @DataBoundSetter
    public void setBitbucketServerUrl(String url) {
        url = BitbucketEndpointConfiguration.normalizeServerUrl(url);
        AbstractBitbucketEndpoint endpoint = BitbucketEndpointConfiguration.get().findEndpoint(url);
        if (endpoint != null) {
            // we have a match
            setServerUrl(url);
            return;
        }
        LOGGER.log(Level.WARNING, "Call to legacy setBitbucketServerUrl({0}) method is configuring an url missing "
                + "from the global configuration.", url);
        setServerUrl(url);
    }

    @Deprecated
    @Restricted(DoNotUse.class)
    @RestrictedSince("2.2.0")
    @CheckForNull
    public String getBitbucketServerUrl() {
        if (BitbucketEndpointConfiguration.get().findEndpoint(serverUrl) instanceof BitbucketCloudEndpoint) {
            return null;
        }
        return serverUrl;
    }

    @Deprecated
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.2.0")
    @NonNull
    public String getIncludes() {
        for (SCMTrait<?> trait : traits) {
            if (trait instanceof WildcardSCMHeadFilterTrait) {
                return ((WildcardSCMHeadFilterTrait) trait).getIncludes();
            }
        }
        return "*";
    }

    @Deprecated
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.2.0")
    @DataBoundSetter
    public void setIncludes(@NonNull String includes) {
        for (int i = 0; i < traits.size(); i++) {
            SCMTrait<?> trait = traits.get(i);
            if (trait instanceof WildcardSCMHeadFilterTrait) {
                WildcardSCMHeadFilterTrait existing = (WildcardSCMHeadFilterTrait) trait;
                if ("*".equals(includes) && "".equals(existing.getExcludes())) {
                    traits.remove(i);
                } else {
                    traits.set(i, new WildcardSCMHeadFilterTrait(includes, existing.getExcludes()));
                }
                return;
            }
        }
        if (!"*".equals(includes)) {
            traits.add(new WildcardSCMHeadFilterTrait(includes, ""));
        }
    }

    @Deprecated
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.2.0")
    @NonNull
    public String getExcludes() {
        for (SCMTrait<?> trait : traits) {
            if (trait instanceof WildcardSCMHeadFilterTrait) {
                return ((WildcardSCMHeadFilterTrait) trait).getExcludes();
            }
        }
        return "";
    }

    @Deprecated
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.2.0")
    @DataBoundSetter
    public void setExcludes(@NonNull String excludes) {
        for (int i = 0; i < traits.size(); i++) {
            SCMTrait<?> trait = traits.get(i);
            if (trait instanceof WildcardSCMHeadFilterTrait) {
                WildcardSCMHeadFilterTrait existing = (WildcardSCMHeadFilterTrait) trait;
                if ("*".equals(existing.getIncludes()) && "".equals(excludes)) {
                    traits.remove(i);
                } else {
                    traits.set(i, new WildcardSCMHeadFilterTrait(existing.getIncludes(), excludes));
                }
                return;
            }
        }
        if (!"".equals(excludes)) {
            traits.add(new WildcardSCMHeadFilterTrait("*", excludes));
        }
    }


    @NonNull
    @Override
    protected String id() {
        return serverUrl + "::" + repoOwner;
    }

    @Override
    public void visitSources(SCMSourceObserver observer) throws IOException, InterruptedException {
        TaskListener listener = observer.getListener();

        if (StringUtils.isBlank(repoOwner)) {
            listener.getLogger().format("Must specify a repository owner%n");
            return;
        }
        StandardCredentials credentials = BitbucketCredentials.lookupCredentials(
                serverUrl,
                observer.getContext(),
                credentialsId,
                StandardCredentials.class
        );

        if (credentials == null) {
            listener.getLogger().format("Connecting to %s with no credentials, anonymous access%n", serverUrl);
        } else {
            listener.getLogger()
                    .format("Connecting to %s using %s%n", serverUrl, CredentialsNameProvider.name(credentials));
        }
        try (final BitbucketSCMNavigatorRequest request = new BitbucketSCMNavigatorContext().withTraits(traits)
                .newRequest(this, observer)) {
            SourceFactory sourceFactory = new SourceFactory(request);
            WitnessImpl witness = new WitnessImpl(request, listener);

            BitbucketAuthenticator authenticator = AuthenticationTokens.convert(BitbucketAuthenticator.authenticationContext(serverUrl), credentials);

            BitbucketApi bitbucket = BitbucketApiFactory.newInstance(serverUrl, authenticator, repoOwner, null);
            BitbucketTeam team = bitbucket.getTeam();
            if (team != null) {
                // Navigate repositories of the team
                listener.getLogger().format("Looking up repositories of team %s%n", repoOwner);
                request.withRepositories(bitbucket.getRepositories());
            } else {
                // Navigate the repositories of the repoOwner as a user
                listener.getLogger().format("Looking up repositories of user %s%n", repoOwner);
                request.withRepositories(bitbucket.getRepositories(UserRoleInRepository.OWNER));
            }
            for (BitbucketRepository repo : request.repositories()) {
                if (request.process(repo.getRepositoryName(), sourceFactory, null, witness)) {
                    listener.getLogger().format(
                            "%d repositories were processed (query completed)%n", witness.getCount()
                    );
                }
            }
            listener.getLogger().format("%d repositories were processed%n", witness.getCount());
        }
    }

    @NonNull
    @Override
    public List<Action> retrieveActions(@NonNull SCMNavigatorOwner owner,
                                        @CheckForNull SCMNavigatorEvent event,
                                        @NonNull TaskListener listener)
            throws IOException, InterruptedException {
        // TODO when we have support for trusted events, use the details from event if event was from trusted source
        listener.getLogger().printf("Looking up team details of %s...%n", getRepoOwner());
        List<Action> result = new ArrayList<>();
        StandardCredentials credentials = BitbucketCredentials.lookupCredentials(
                serverUrl,
                owner,
                credentialsId,
                StandardCredentials.class
        );

        if (credentials == null) {
            listener.getLogger().format("Connecting to %s with no credentials, anonymous access%n",
                    serverUrl);
        } else {
            listener.getLogger().format("Connecting to %s using %s%n",
                    serverUrl,
                    CredentialsNameProvider.name(credentials));
        }

        BitbucketAuthenticator authenticator = AuthenticationTokens.convert(BitbucketAuthenticator.authenticationContext(serverUrl), credentials);

        BitbucketApi bitbucket = BitbucketApiFactory.newInstance(serverUrl, authenticator, repoOwner, null);
        BitbucketTeam team = bitbucket.getTeam();
        if (team != null) {
            String defaultTeamUrl;
            if (team instanceof BitbucketServerProject) {
                defaultTeamUrl = serverUrl + "/projects/" + team.getName();
            } else {
                defaultTeamUrl = serverUrl + "/" + team.getName();
            }
            String teamUrl = StringUtils.defaultIfBlank(getLink(team.getLinks(), "html"), defaultTeamUrl);
            String teamDisplayName = StringUtils.defaultIfBlank(team.getDisplayName(), team.getName());
            result.add(new ObjectMetadataAction(
                    teamDisplayName,
                    null,
                    teamUrl
            ));
            String avatarUrl;
            if (team instanceof BitbucketServerProject) {
                avatarUrl = UriTemplate
                        .fromTemplate(serverUrl + "/rest/api/1.0/projects/{repo}/avatar.png")
                        .set("repo", repoOwner)
                        .expand();
            }else {
                avatarUrl = getLink(team.getLinks(), "avatar");
            }
            result.add(new BitbucketTeamMetadataAction(avatarUrl));
            result.add(new BitbucketLink("icon-bitbucket-logo", teamUrl));
            listener.getLogger().printf("Team: %s%n", HyperlinkNote.encodeTo(teamUrl, teamDisplayName));
        } else {
            String teamUrl = serverUrl + "/" + repoOwner;
            result.add(new ObjectMetadataAction(
                    repoOwner,
                    null,
                    teamUrl
            ));
            result.add(new BitbucketTeamMetadataAction(null));
            result.add(new BitbucketLink("icon-bitbucket-logo", teamUrl));
            listener.getLogger().println("Could not resolve team details");
        }
        return result;
    }

    private static String getLink(Map<String, List<BitbucketHref>> links, String name) {
        if (links == null) {
            return null;
        }
        List<BitbucketHref> hrefs = links.get(name);
        if (hrefs == null || hrefs.isEmpty()) {
            return null;
        }
        BitbucketHref href = hrefs.get(0);
        return href == null ? null : href.getHref();
    }

    @Symbol("bitbucket")
    @Extension
    public static class DescriptorImpl extends SCMNavigatorDescriptor {

        public static final String ANONYMOUS = BitbucketSCMSource.DescriptorImpl.ANONYMOUS;
        public static final String SAME = BitbucketSCMSource.DescriptorImpl.SAME;

        @Override
        public String getDisplayName() {
            return Messages.BitbucketSCMNavigator_DisplayName();
        }

        @Override
        public String getDescription() {
            return Messages.BitbucketSCMNavigator_Description();
        }

        @Override
        public String getIconFilePathPattern() {
            return "plugin/cloudbees-bitbucket-branch-source/images/:size/bitbucket-scmnavigator.png";
        }

        @Override
        public String getIconClassName() {
            return "icon-bitbucket-scmnavigator";
        }

        @SuppressWarnings("unchecked")
        @Override
        public SCMNavigator newInstance(String name) {
            BitbucketSCMNavigator instance = new BitbucketSCMNavigator(StringUtils.defaultString(name));
            instance.setTraits((List) getTraitsDefaults());
            return instance;
        }

        public boolean isServerUrlSelectable() {
            return BitbucketEndpointConfiguration.get().isEndpointSelectable();
        }

        public ListBoxModel doFillServerUrlItems() {
            return BitbucketEndpointConfiguration.get().getEndpointItems();
        }

        public FormValidation doCheckCredentialsId(@AncestorInPath SCMSourceOwner context, @QueryParameter String serverUrl, @QueryParameter String value) {
            if (!value.isEmpty()) {
                if (CredentialsMatchers.firstOrNull(
                        CredentialsProvider.lookupCredentials(
                                StandardCertificateCredentials.class,
                                context,
                                context instanceof Queue.Task ? Tasks.getDefaultAuthenticationOf((Queue.Task) context) : ACL.SYSTEM,
                                URIRequirementBuilder.fromUri(serverUrl).build()),
                        CredentialsMatchers.allOf(
                                CredentialsMatchers.withId(value),
                                AuthenticationTokens.matcher(BitbucketAuthenticator.authenticationContext(serverUrl))
                        )
                ) != null) {
                       return FormValidation.warning("A certificate was selected. You will likely need to configure Checkout over SSH.");
                }
                return FormValidation.ok();
            } else {
                return FormValidation.warning("Credentials are required for build notifications");
            }
        }

        @Restricted(DoNotUse.class)
        @Deprecated
        public FormValidation doCheckBitbucketServerUrl(@QueryParameter String bitbucketServerUrl) {
            return BitbucketSCMSource.DescriptorImpl.doCheckBitbucketServerUrl(bitbucketServerUrl);
        }

        public static FormValidation doCheckServerUrl(@QueryParameter String value) {
            if (BitbucketEndpointConfiguration.get().findEndpoint(value) == null) {
                return FormValidation.error("Unregistered Server: " + value);
            }
            return FormValidation.ok();
        }


        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath SCMSourceOwner context,
                                                     @QueryParameter String serverUrl) {
            StandardListBoxModel result = new StandardListBoxModel();
            result.withEmptySelection();
            result.includeMatchingAs(
                    context instanceof Queue.Task
                            ? Tasks.getDefaultAuthenticationOf((Queue.Task) context)
                            : ACL.SYSTEM,
                    context,
                    StandardCredentials.class,
                    URIRequirementBuilder.fromUri(serverUrl).build(),
                    AuthenticationTokens.matcher(BitbucketAuthenticator.authenticationContext(serverUrl))
            );
            return result;
        }

        public List<NamedArrayList<? extends SCMTraitDescriptor<?>>> getTraitsDescriptorLists() {
            BitbucketSCMSource.DescriptorImpl sourceDescriptor =
                    Jenkins.getActiveInstance().getDescriptorByType(BitbucketSCMSource.DescriptorImpl.class);
            List<SCMTraitDescriptor<?>> all = new ArrayList<>();
            all.addAll(
                    SCMNavigatorTrait._for(this, BitbucketSCMNavigatorContext.class, BitbucketSCMSourceBuilder.class));
            all.addAll(SCMSourceTrait._for(sourceDescriptor, BitbucketSCMSourceContext.class, null));
            all.addAll(SCMSourceTrait._for(sourceDescriptor, null, BitbucketGitSCMBuilder.class));
            all.addAll(SCMSourceTrait._for(sourceDescriptor, null, BitbucketHgSCMBuilder.class));
            Set<SCMTraitDescriptor<?>> dedup = new HashSet<>();
            for (Iterator<SCMTraitDescriptor<?>> iterator = all.iterator(); iterator.hasNext(); ) {
                SCMTraitDescriptor<?> d = iterator.next();
                if (dedup.contains(d)
                        || d instanceof MercurialBrowserSCMSourceTrait.DescriptorImpl
                        || d instanceof GitBrowserSCMSourceTrait.DescriptorImpl) {
                    // remove any we have seen already and ban the browser configuration as it will always be bitbucket
                    iterator.remove();
                } else {
                    dedup.add(d);
                }
            }
            List<NamedArrayList<? extends SCMTraitDescriptor<?>>> result = new ArrayList<>();
            NamedArrayList.select(all, "Repositories", new NamedArrayList.Predicate<SCMTraitDescriptor<?>>() {
                        @Override
                        public boolean test(SCMTraitDescriptor<?> scmTraitDescriptor) {
                            return scmTraitDescriptor instanceof SCMNavigatorTraitDescriptor;
                        }
                    },
                    true, result);
            NamedArrayList.select(all, "Within repository", NamedArrayList
                            .anyOf(NamedArrayList.withAnnotation(Discovery.class),
                                    NamedArrayList.withAnnotation(Selection.class)),
                    true, result);
            int insertionPoint = result.size();
            NamedArrayList.select(all, "Git", new NamedArrayList.Predicate<SCMTraitDescriptor<?>>() {
                @Override
                public boolean test(SCMTraitDescriptor<?> d) {
                    return GitSCM.class.isAssignableFrom(d.getScmClass());
                }
            }, true, result);
            NamedArrayList.select(all, "Mercurial", new NamedArrayList.Predicate<SCMTraitDescriptor<?>>() {
                @Override
                public boolean test(SCMTraitDescriptor<?> d) {
                    return MercurialSCM.class.isAssignableFrom(d.getScmClass());
                }
            }, true, result);
            NamedArrayList.select(all, "Additional", null, true, result, insertionPoint);
            return result;
        }

        public List<SCMTrait<?>> getTraitsDefaults() {
            return Arrays.<SCMTrait<?>>asList(
                    new BranchDiscoveryTrait(true, false),
                    new OriginPullRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.MERGE)),
                    new ForkPullRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.MERGE),
                            new ForkPullRequestDiscoveryTrait.TrustTeamForks())
            );
        }

        @Restricted(DoNotUse.class)
        @Deprecated
        public ListBoxModel doFillCheckoutCredentialsIdItems(@AncestorInPath SCMSourceOwner context,
                                                             @QueryParameter String bitbucketServerUrl) {
            StandardListBoxModel result = new StandardListBoxModel();
            result.add("- same as scan credentials -", BitbucketSCMSource.DescriptorImpl.SAME);
            result.add("- anonymous -", BitbucketSCMSource.DescriptorImpl.ANONYMOUS);
            result.includeMatchingAs(
                    context instanceof Queue.Task
                            ? Tasks.getDefaultAuthenticationOf((Queue.Task) context)
                            : ACL.SYSTEM,
                    context,
                    StandardCredentials.class,
                    URIRequirementBuilder.fromUri(bitbucketServerUrl).build(),
                    AuthenticationTokens.matcher(BitbucketAuthenticator.authenticationContext(bitbucketServerUrl))
            );
            return result;
        }


        @NonNull
        @Override
        protected SCMSourceCategory[] createCategories() {
            return new SCMSourceCategory[]{
                    new UncategorizedSCMSourceCategory(
                            Messages._BitbucketSCMNavigator_UncategorizedSCMSourceCategory_DisplayName())
            };
        }

        static {
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-scm-navigator icon-sm",
                            "plugin/cloudbees-bitbucket-branch-source/images/16x16/bitbucket-scmnavigator.png",
                            Icon.ICON_SMALL_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-scm-navigator icon-md",
                            "plugin/cloudbees-bitbucket-branch-source/images/24x24/bitbucket-scmnavigator.png",
                            Icon.ICON_MEDIUM_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-scm-navigator icon-lg",
                            "plugin/cloudbees-bitbucket-branch-source/images/32x32/bitbucket-scmnavigator.png",
                            Icon.ICON_LARGE_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-scm-navigator icon-xlg",
                            "plugin/cloudbees-bitbucket-branch-source/images/48x48/bitbucket-scmnavigator.png",
                            Icon.ICON_XLARGE_STYLE));

            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-logo icon-sm",
                            "plugin/cloudbees-bitbucket-branch-source/images/16x16/bitbucket-logo.png",
                            Icon.ICON_SMALL_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-logo icon-md",
                            "plugin/cloudbees-bitbucket-branch-source/images/24x24/bitbucket-logo.png",
                            Icon.ICON_MEDIUM_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-logo icon-lg",
                            "plugin/cloudbees-bitbucket-branch-source/images/32x32/bitbucket-logo.png",
                            Icon.ICON_LARGE_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-logo icon-xlg",
                            "plugin/cloudbees-bitbucket-branch-source/images/48x48/bitbucket-logo.png",
                            Icon.ICON_XLARGE_STYLE));

            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-repo icon-sm",
                            "plugin/cloudbees-bitbucket-branch-source/images/16x16/bitbucket-repository.png",
                            Icon.ICON_SMALL_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-repo icon-md",
                            "plugin/cloudbees-bitbucket-branch-source/images/24x24/bitbucket-repository.png",
                            Icon.ICON_MEDIUM_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-repo icon-lg",
                            "plugin/cloudbees-bitbucket-branch-source/images/32x32/bitbucket-repository.png",
                            Icon.ICON_LARGE_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-repo icon-xlg",
                            "plugin/cloudbees-bitbucket-branch-source/images/48x48/bitbucket-repository.png",
                            Icon.ICON_XLARGE_STYLE));

            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-repo-git icon-sm",
                            "plugin/cloudbees-bitbucket-branch-source/images/16x16/bitbucket-repository-git.png",
                            Icon.ICON_SMALL_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-repo-git icon-md",
                            "plugin/cloudbees-bitbucket-branch-source/images/24x24/bitbucket-repository-git.png",
                            Icon.ICON_MEDIUM_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-repo-git icon-lg",
                            "plugin/cloudbees-bitbucket-branch-source/images/32x32/bitbucket-repository-git.png",
                            Icon.ICON_LARGE_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-repo-git icon-xlg",
                            "plugin/cloudbees-bitbucket-branch-source/images/48x48/bitbucket-repository-git.png",
                            Icon.ICON_XLARGE_STYLE));

            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-repo-hg icon-sm",
                            "plugin/cloudbees-bitbucket-branch-source/images/16x16/bitbucket-repository-hg.png",
                            Icon.ICON_SMALL_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-repo-hg icon-md",
                            "plugin/cloudbees-bitbucket-branch-source/images/24x24/bitbucket-repository-hg.png",
                            Icon.ICON_MEDIUM_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-repo-hg icon-lg",
                            "plugin/cloudbees-bitbucket-branch-source/images/32x32/bitbucket-repository-hg.png",
                            Icon.ICON_LARGE_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-repo-hg icon-xlg",
                            "plugin/cloudbees-bitbucket-branch-source/images/48x48/bitbucket-repository-hg.png",
                            Icon.ICON_XLARGE_STYLE));

            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-branch icon-sm",
                            "plugin/cloudbees-bitbucket-branch-source/images/16x16/bitbucket-branch.png",
                            Icon.ICON_SMALL_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-branch icon-md",
                            "plugin/cloudbees-bitbucket-branch-source/images/24x24/bitbucket-branch.png",
                            Icon.ICON_MEDIUM_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-branch icon-lg",
                            "plugin/cloudbees-bitbucket-branch-source/images/32x32/bitbucket-branch.png",
                            Icon.ICON_LARGE_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-bitbucket-branch icon-xlg",
                            "plugin/cloudbees-bitbucket-branch-source/images/48x48/bitbucket-branch.png",
                            Icon.ICON_XLARGE_STYLE));
        }
    }

    private static class WitnessImpl implements SCMNavigatorRequest.Witness {
        private int count;

        private final BitbucketSCMNavigatorRequest request;
        private final TaskListener listener;

        public WitnessImpl(BitbucketSCMNavigatorRequest request, TaskListener listener) {
            this.request = request;
            this.listener = listener;
        }

        @Override
        public void record(@NonNull String name, boolean isMatch) {
            BitbucketRepository repository = this.request.getBitbucketRepository(name);

            if (isMatch) {
                listener.getLogger().format("Proposing %s%n", repository.getFullName());
                count++;
            } else {
                listener.getLogger().format("Ignoring %s%n", repository.getFullName());
            }
        }

        public int getCount() {
            return count;
        }
    }

    private class SourceFactory implements SCMNavigatorRequest.SourceLambda {
        private final BitbucketSCMNavigatorRequest request;

        public SourceFactory(BitbucketSCMNavigatorRequest request) {
            this.request = request;
        }

        @NonNull
        @Override
        public SCMSource create(@NonNull String projectName) throws IOException, InterruptedException {
            return new BitbucketSCMSourceBuilder(
                    getId() + "::" + projectName,
                    serverUrl,
                    credentialsId,
                    repoOwner,
                    projectName)
                    .withRequest(request)
                    .build();
        }
    }
}
