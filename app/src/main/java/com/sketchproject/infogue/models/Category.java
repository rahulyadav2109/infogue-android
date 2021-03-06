package com.sketchproject.infogue.models;

/**
 * Category model data.
 * <p>
 * Sketch Project Studio
 * Created by Angga on 24/04/2016 09.16.
 */
public class Category {
    public static final String TABLE = "categories";
    public static final String ID = "id";
    public static final String CATEGORY = "category";

    private int id;
    private String category;

    public Category() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
