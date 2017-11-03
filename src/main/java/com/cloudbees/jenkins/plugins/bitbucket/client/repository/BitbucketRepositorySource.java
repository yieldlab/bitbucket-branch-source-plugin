package com.cloudbees.jenkins.plugins.bitbucket.client.repository;

import com.cloudbees.jenkins.plugins.bitbucket.filesystem.BitbucketSCMFile;
import java.util.List;
import jenkins.scm.api.SCMFile;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Vivek Pandey
 */
public class BitbucketRepositorySource {
    private final String path;
    private final String type;
    private final List<String> attributes;

    @JsonCreator
    public BitbucketRepositorySource(@JsonProperty("path") String path, @JsonProperty("type") String type, @JsonProperty("attributes") List<String> attributes) {
        this.path = path;
        this.type = type;
        this.attributes = attributes;
    }

    @JsonProperty("path")
    public String getPath() {
        return path;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("attributes")
    public List<String> getAttributes() {
        return attributes;
    }

    @JsonIgnore
    public boolean isDirectory() {
        return type.equals("commit_directory");
    }

    public BitbucketSCMFile toBitbucketScmFile(BitbucketSCMFile parent){
        SCMFile.Type fileType;
        if(isDirectory()){
            fileType = SCMFile.Type.DIRECTORY;
        } else {
            fileType = SCMFile.Type.REGULAR_FILE;
            for(String attribute: getAttributes()){
                if(attribute.equals("link")){
                    fileType = SCMFile.Type.LINK;
                } else if(attribute.equals("subrepository")){
                    fileType = SCMFile.Type.OTHER; // sub-module or sub-repo
                }
            }
        }
        return new BitbucketSCMFile(parent, path, fileType);
    }
}
