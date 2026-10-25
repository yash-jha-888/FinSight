package com.finsight.model;

import java.util.Objects;

/**
 * A label used to group transactions, such as "Groceries" or "Salary".
 *
 * <p><b>OOP: encapsulation.</b> Fields are private and validated in setters,
 * so a category can never have a blank name or a malformed colour.
 */
public class Category implements Identifiable {

    private long id;
    private String name;
    private TransactionType type;
    private String colorHex;

    public Category(long id, String name, TransactionType type, String colorHex) {
        this.id = id;
        setName(name);
        setType(type);
        setColorHex(colorHex);
    }

    public Category(String name, TransactionType type, String colorHex) {
        this(0, name, type, colorHex);
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be blank");
        }
        this.name = name.strip();
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        if (colorHex == null || !colorHex.matches("#[0-9A-Fa-f]{6}")) {
            throw new IllegalArgumentException("Colour must look like #RRGGBB: " + colorHex);
        }
        this.colorHex = colorHex.toUpperCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category other)) return false;
        return id != 0 ? id == other.id : name.equalsIgnoreCase(other.name) && type == other.type;
    }

    @Override
    public int hashCode() {
        return id != 0 ? Long.hashCode(id) : Objects.hash(name.toLowerCase(), type);
    }

    @Override
    public String toString() {
        return name;
    }
}
