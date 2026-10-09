package com.myfinance.track.category;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> listFor(Long userId) {
        return categories.findVisibleToUser(userId).stream().map(CategoryDto::from).toList();
    }
}