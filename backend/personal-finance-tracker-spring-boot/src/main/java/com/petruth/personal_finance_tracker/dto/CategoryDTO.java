package com.petruth.personal_finance_tracker.dto;

public class CategoryDTO {
    private Long id;
    private String name;
    private Long userId;

    public CategoryDTO(){

    }

    public CategoryDTO(Long id, String name, Long userId) {
        this.id = id;
        this.name = name;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getUserId() { return userId; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setUserId(Long userId) { this.userId = userId; }
}

