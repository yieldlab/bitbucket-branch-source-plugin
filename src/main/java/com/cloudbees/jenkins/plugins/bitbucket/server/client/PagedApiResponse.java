package com.cloudbees.jenkins.plugins.bitbucket.server.client;

import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

public class PagedApiResponse<V> {

    private List<V> values;

    private Integer size;
    private Integer limit;
    private Integer start;
    private Integer nextPageStart;
    @JsonProperty("isLastPage")
    private Boolean lastPage;

    public List<V> getValues() {
        return values;
    }

    public void setValues(List<V> values) {
        this.values = values;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getNextPageStart() {
        return nextPageStart;
    }

    public void setNextPageStart(Integer nextPageStart) {
        this.nextPageStart = nextPageStart;
    }

    public boolean isLastPage() {
        return lastPage != null && lastPage;
    }

    public void setLastPage(Boolean lastPage) {
        this.lastPage = lastPage;
    }
}
