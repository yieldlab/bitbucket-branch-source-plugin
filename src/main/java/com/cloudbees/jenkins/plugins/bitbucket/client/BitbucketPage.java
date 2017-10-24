package com.cloudbees.jenkins.plugins.bitbucket.client;

import java.util.List;

/**
 * Bitbucket API pagination
 *
 * @author Vivek Pandey
 */
public abstract class BitbucketPage <T> {
    /**
     * Page size
     *
     * @return page size
     */
    public abstract int getSize();

    /**
     * List of values in this page
     *
     * @return list of values
     */
    public abstract List<T> getValues();

    /**
     * URL to the next page
     *
     * @return next page url
     */
    public abstract String getNext();

    /**
     *  Boolean to tell if this is last page
     * @return If true then last page, otherwise
     */
    public abstract boolean isLastPage();

    /**
     * Number of pages
     *
     * @return number of pages
     */
    public abstract int getPageLength();

    /**
     * Page number
     *
     * @return page number
     */
    public abstract int getPage();
}
