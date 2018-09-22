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
package com.cloudbees.jenkins.plugins.bitbucket.client.branch;

import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketBranch;
import com.cloudbees.jenkins.plugins.bitbucket.client.repository.BitbucketCloudRepository;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BitbucketCloudBranch implements BitbucketBranch {
    private final String name;
    private final boolean isActive;
    private long dateInMillis;
    private String hash;

    @JsonCreator
    public BitbucketCloudBranch(@Nonnull @JsonProperty("name") String name,
                                @Nullable @JsonProperty("target") BitbucketCloudBranch.Target target,
                                @Nullable @JsonProperty("heads") List<Head> heads) {
        this.name = name;
        if(target != null) {
            this.dateInMillis = target.repo.getUpdatedOn() != null ? target.repo.getUpdatedOn().getTime() : 0;
            this.hash = target.hash;
        }

        // For Hg repositories, Bitbucket returns all branches, including the closed/inactive ones.
        // To determine if a branch has been closed, we look at the heads property:
        // - Branches with non-empty heads are active.
        // - Branches with empty heads are inactive.
        // - On branches from git repositories heads is null. They all are active.
        this.isActive = heads == null || !heads.isEmpty();
    }

    public BitbucketCloudBranch(@Nonnull String name, String hash, long dateInMillis) {
        this.name = name;
        this.dateInMillis = dateInMillis;
        this.hash = hash;
        this.isActive = true;
    }

    public String getRawNode() {
        return hash;
    }

    public void setDateMillis(long dateInMillis) {
        this.dateInMillis = dateInMillis;
    }

    public void setRawNode(String hash) {
        this.hash = hash;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getDateMillis() {
        return dateInMillis;
    }

    public boolean isActive() {
        return isActive;
    }

    public static class Target {
        private final String hash;
        private final BitbucketCloudRepository repo;

        @JsonCreator
        public Target(@Nonnull @JsonProperty("hash") String hash, @Nonnull @JsonProperty("repository") BitbucketCloudRepository repo) {
            this.hash = hash;
            this.repo = repo;
        }
    }

    public static class Head {
        private final String hash;

        @JsonCreator
        public Head(@Nonnull @JsonProperty("hash") String hash) {
            this.hash = hash;
        }
    }
}
