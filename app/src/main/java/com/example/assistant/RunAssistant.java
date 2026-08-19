package com.example.assistant;

import java.util.ArrayList;

public class RunAssistant {
    public String id;
    public String object;
    public long created_at;
    public String assistant_id;
    public String thread_id;
    public String status;
    public String started_at;
    public long expires_at;
    public String cancelled_at;
    public String failed_at;
    public String completed_at;
    public String required_action;
    public String last_error;
    public String model;
    public String instructions;
    public ArrayList<Tools> tools;
    public ArrayList<File_ids> file_ids;
    public Metadata metadata;
    public String usage;
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

    public void setAssistant_id(String assistant_id) {
        this.assistant_id = assistant_id;
    }
    public String getAssistant_id() {
        return assistant_id;
    }

    public void setThread_id(String thread_id) {
        this.thread_id = thread_id;
    }
    public String getThread_id() {
        return thread_id;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public String getStatus() {
        return status;
    }

    public void setStarted_at(String started_at) {
        this.started_at = started_at;
    }
    public String getStarted_at() {
        return started_at;
    }

    public void setExpires_at(long expires_at) {
        this.expires_at = expires_at;
    }
    public long getExpires_at() {
        return expires_at;
    }

    public void setCancelled_at(String cancelled_at) {
        this.cancelled_at = cancelled_at;
    }
    public String getCancelled_at() {
        return cancelled_at;
    }

    public void setFailed_at(String failed_at) {
        this.failed_at = failed_at;
    }
    public String getFailed_at() {
        return failed_at;
    }

    public void setCompleted_at(String completed_at) {
        this.completed_at = completed_at;
    }
    public String getCompleted_at() {
        return completed_at;
    }

    public void setRequired_action(String required_action) {
        this.required_action = required_action;
    }
    public String getRequired_action() {
        return required_action;
    }

    public void setLast_error(String last_error) {
        this.last_error = last_error;
    }
    public String getLast_error() {
        return last_error;
    }

    public void setModel(String model) {
        this.model = model;
    }
    public String getModel() {
        return model;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
    public String getInstructions() {
        return instructions;
    }

    public void setTools(ArrayList<Tools> tools) {
        this.tools = tools;
    }
    public ArrayList<Tools> getTools() {
        return tools;
    }

    public void setFile_ids(ArrayList<File_ids> file_ids) {
        this.file_ids = file_ids;
    }
    public ArrayList<File_ids> getFile_ids() {
        return file_ids;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }
    public Metadata getMetadata() {
        return metadata;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }
    public String getUsage() {
        return usage;
    }

    public class Tools {
        public String type;
        public void setType(String type) {
            this.type = type;
        }
        public String getType() {
            return type;
        }
    }

    public class File_ids {

    }

    public class Metadata {

    }
}
