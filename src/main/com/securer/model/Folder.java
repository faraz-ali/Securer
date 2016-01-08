package com.securer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sun.tools.javac.util.List;

import javax.persistence.Entity;

/**
 * Created by faraz on 12/27/15.
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class Folder {

    String type;
    String id;
    String name;
    Item_collection item_collection;

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

    public Item_collection getItem_collection() {
        return item_collection;
    }

    public void setItem_collection(Item_collection item_collection) {
        this.item_collection = item_collection;
    }
}
