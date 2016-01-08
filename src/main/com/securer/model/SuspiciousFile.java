package com.securer.model;

import com.box.sdk.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created by faraz on 12/28/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SuspiciousFile {
    String fileName;
    User Owner;
    String createdAt;
    List<String> suspiciousData;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public User getOwner() {
        return Owner;
    }

    public void setOwner(User owner) {
        Owner = owner;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getSuspiciousData() {
        return suspiciousData;
    }

    public void setSuspiciousData(List<String> suspiciousData) {
        this.suspiciousData = suspiciousData;
    }
}


