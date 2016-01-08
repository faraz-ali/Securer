package com.securer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sun.tools.javac.util.List;

import javax.persistence.Entity;

/**
 * Created by faraz on 12/27/15.
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item_collection {

    String total_count;
    Entry[] entries;

    public String getTotal_count() {
        return total_count;
    }

    public void setTotal_count(String total_count) {
        this.total_count = total_count;
    }

    public Entry[] getEntries() {
        return entries;
    }

    public void setEntries(Entry[] entries) {
        this.entries = entries;
    }
}
