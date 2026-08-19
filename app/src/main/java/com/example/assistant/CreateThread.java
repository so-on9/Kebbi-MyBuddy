package com.example.assistant;



public class CreateThread {
    public String id;
    public String object;
    public long created_at;
    public Metadata metadata;
    public void setId(String id) {
        this.id = id;
    }
    public String getId() {
        return id;
    }
    public void setObject(String object) {
        this.object = object;
    }
    public String getObject() {
        return object;
    }
    public void setCreated_at(long created_at) {
        this.created_at = created_at;
    }
    public long getCreated_at() {
        return created_at;
    }
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }
    public Metadata getMetadata() {
        return metadata;
    }

    public class Metadata {

    }
}
