package com.example.entity;

import java.io.Serializable;

public class User implements Serializable{
    private static final long serialVersionUID = 7487052922529965977L;
    
    public String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return super.toString() + " [User: " + id + "]";
    }
}
