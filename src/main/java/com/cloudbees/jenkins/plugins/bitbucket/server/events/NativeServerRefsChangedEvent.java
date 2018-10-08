package com.cloudbees.jenkins.plugins.bitbucket.server.events;

import com.cloudbees.jenkins.plugins.bitbucket.server.client.repository.BitbucketServerRepository;
import java.util.Collections;
import java.util.List;

public class NativeServerRefsChangedEvent {

    private BitbucketServerRepository repository;
    private List<Change> changes;

    public BitbucketServerRepository getRepository() {
        return repository;
    }

    public List<Change> getChanges() {
        return changes == null ? Collections.<Change> emptyList() : Collections.unmodifiableList(changes);
    }

    public static class Change {
        private Ref ref;
        private String refId, fromHash, toHash, type;

        public Ref getRef() {
            return ref;
        }

        public void setRef(Ref ref) {
            this.ref = ref;
        }

        public String getRefId() {
            return refId;
        }

        public void setRefId(String refId) {
            this.refId = refId;
        }

        public String getFromHash() {
            return fromHash;
        }

        public void setFromHash(String fromHash) {
            this.fromHash = fromHash;
        }

        public String getToHash() {
            return toHash;
        }

        public void setToHash(String toHash) {
            this.toHash = toHash;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class Ref {
        private String id, displayId, type;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDisplayId() {
            return displayId;
        }

        public void setDisplayId(String displayId) {
            this.displayId = displayId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
