package ru.practicum.explorewithme.category.service;


import ru.practicum.explorewithme.category.dto.NewCategoryRequest;
import ru.practicum.explorewithme.category.dto.UpdateCategoryRequest;
import ru.practicum.explorewithme.interaction.dto.CategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto createCategory(NewCategoryRequest request);

    CategoryDto changeCategory(Long catId, UpdateCategoryRequest request);

    void removeCategory(Long catId);

    List<CategoryDto> getAllCategories(Integer from, Integer size);

    CategoryDto getCategoryById(Long catId);
}
