package ru.practicum.explorewithme.category.mapper;


import ru.practicum.explorewithme.category.dto.NewCategoryRequest;
import ru.practicum.explorewithme.category.dto.UpdateCategoryRequest;
import ru.practicum.explorewithme.category.model.Category;
import ru.practicum.explorewithme.interaction.dto.CategoryDto;

public class CategoryMapper {

    public static Category mapToCategory(NewCategoryRequest request) {
        return new Category(
                null,
                request.getName()
        );
    }

    public static Category mapToUpdateCategory(UpdateCategoryRequest request, Category category) {
        category.setName(request.getName());
        return category;
    }

    public static CategoryDto mapToCategoryDto(Category category) {
        return new CategoryDto(category.getId(), category.getName());
    }
}
