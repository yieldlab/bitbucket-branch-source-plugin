package com.cloudbees.jenkins.plugins.bitbucket.server.client.repository;


import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketWebHook;

public class BitbucketServerWebhook implements BitbucketWebHook {
    @JsonProperty("id")
    private Integer uid;
    @JsonProperty("name")
    private String description;
    @JsonProperty("events")
    private List<String> events;
    @JsonProperty("url")
    private String url;
    @JsonProperty("active")
    private boolean active;
    
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)     // If null, don't marshal to allow for backwards compatibility
    private String committersToIgnore; // Since Bitbucket Webhooks version 1.5.0 

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCommittersToIgnore() {
        return committersToIgnore;
    }

    public void setCommittersToIgnore(String committersToIgnore) {
        this.committersToIgnore = committersToIgnore;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public List<String> getEvents() {
        return events;
    }

    @Override
    @JsonIgnore
    public String getUuid() {
        if (uid != null) {
            return String.valueOf(uid);
        }
        return null;
    }

    public void setEvents(List<String> events) {
        this.events = events;
    }
}
