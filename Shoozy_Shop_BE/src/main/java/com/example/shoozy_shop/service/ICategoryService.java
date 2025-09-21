package com.example.shoozy_shop.service;

import com.example.shoozy_shop.model.Category;

import java.util.List;

public interface ICategoryService {
    List<Category> getAllCategories();
    List<Category> getActiveCategories();
    Category getCategoryById(Long id);
    Category addCategory(Category category);
    Category updateCategory(Long id, Category category);
    void deleteCategory(Long id);
    Boolean restoreCategory(Long id);
}
