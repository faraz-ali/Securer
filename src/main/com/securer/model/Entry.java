package com.securer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by faraz on 12/27/15.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class Entry {
    String type;
    String id;
    String name;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

