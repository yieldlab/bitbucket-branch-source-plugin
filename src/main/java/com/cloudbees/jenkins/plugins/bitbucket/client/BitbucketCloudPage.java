package com.cloudbees.jenkins.plugins.bitbucket.client;

import com.google.common.collect.ImmutableList;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Vivek Pandey
 */
public class BitbucketCloudPage<T> extends BitbucketPage<T> {
    private final int pageLength;
    private final int page;
    private final int size;
    private final String next;
    private final List<T> values;

    public BitbucketCloudPage(@JsonProperty("pagelen") int pageLength,
                       @JsonProperty("page") int page,
                       @JsonProperty("size") int size,
                       @Nullable @JsonProperty("next") String next,
                       @Nonnull @JsonProperty("values") List<T> values) {
        this.pageLength = pageLength;
        this.page = page;
        this.size = size;
        this.next = next;
        this.values = ImmutableList.copyOf(values);
    }

    public int getSize() {
        return size;
    }

    public List<T> getValues() {
        return values;
    }

    public String getNext(){
        return next;
    }

    public boolean isLastPage() {
        return next == null;
    }

    public int getPageLength() {
        return pageLength;
    }

    public int getPage() {
        return page;
    }
}
