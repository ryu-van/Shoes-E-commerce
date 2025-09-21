package com.example.shoozy_shop.service;

import com.example.shoozy_shop.exception.ResourceNotFoundException;
import com.example.shoozy_shop.model.Category;
import com.example.shoozy_shop.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService implements ICategoryService {

    private final CategoryRepository categoryRepository;

//    private final ModelMapper modelMapper;


    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public List<Category> getActiveCategories() {
        return categoryRepository.findAllByStatusTrue();
    }

    @Override
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("category", id));
    }

    @Override
    @Transactional
    public Category addCategory(Category category) {
        if(categoryRepository.existsByName(category.getName())) {
            throw new IllegalArgumentException("Category " + category.getName() +" already exists!");
        }
        category.setStatus(true);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category updateCategory(Long id, Category category) {
        Category existingCategory = this.getCategoryById(id);
        existingCategory.setName(category.getName());
        existingCategory.setDescription(category.getDescription());
        existingCategory.setStatus(category.getStatus());
        return categoryRepository.save(existingCategory);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = this.getCategoryById(id);
        category.setStatus(false);
        categoryRepository.save(category);
    }

    @Override
    public Boolean restoreCategory(Long id) {
        Optional<Category> optional = categoryRepository.findById(id);
        if (optional.isPresent()) {
            Category category = optional.get();
            category.setStatus(true); // khôi phục
            categoryRepository.save(category);
            return true;
        }
        return false;
    }
}
