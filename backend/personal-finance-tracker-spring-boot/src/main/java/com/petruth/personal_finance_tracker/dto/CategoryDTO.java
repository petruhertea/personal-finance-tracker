package com.petruth.personal_finance_tracker.dto;


public class CategoryDTO {
    private Long id;
    private String name;
    private Long userId;
    private String icon;
    private String color;

    public CategoryDTO() {

    }

    public CategoryDTO(Long id, String name, Long userId, String icon, String color) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.icon = icon;
        this.color = color;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getUserId() {
        return userId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}

