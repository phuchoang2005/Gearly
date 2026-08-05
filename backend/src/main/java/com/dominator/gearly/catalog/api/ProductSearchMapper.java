package com.dominator.gearly.catalog.api;

import com.dominator.gearly.catalog.domain.ProductQuery;
import com.dominator.gearly.catalog.domain.ProductSort;
import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.shared.domain.CategoryId;
import com.dominator.gearly.shared.domain.ProductCondition;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Turns the storefront's query string into a {@link ProductQuery}.
 *
 * <p>This is where the wire vocabulary stops. The DTO's {@code condition} is a display token
 * (including one with a space in it), its {@code sortBy} is a hyphenated slug, and its
 * {@code genres} are hex strings; none of those words appear past this class.
 */
@Component
public class ProductSearchMapper {

    public ProductQuery toQuery(ProductSearchDTO dto) {
        return new ProductQuery(
                parseCondition(dto.getCondition()),
                dto.getMinPrice(),
                dto.getMaxPrice(),
                parseGenres(dto.getGenres()),
                dto.getSearch(),
                dto.getMinRating(),
                ProductSort.fromWireValue(dto.getSortBy()),
                dto.getPage(),
                dto.getSize());
    }

    /**
     * Blank means "no filter", as it always has. An unrecognized value is a 400 rather than a
     * string-equality match that could only ever return nothing — the difference between
     * telling the caller their filter is wrong and showing them an empty shop. S9 made that
     * change and verified the storefront only ever sends values from its own fixed list.
     */
    private ProductCondition parseCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            return null;
        }
        try {
            return ProductCondition.fromWireValue(condition);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * The {@code genres} parameter carries category ids as hex strings. A malformed one is a
     * 400; it used to reach {@code new ObjectId(...)} inside the repository and surface as a
     * 500.
     */
    private List<CategoryId> parseGenres(List<String> genres) {
        if (genres == null || genres.isEmpty()) {
            return List.of();
        }
        try {
            return genres.stream().map(CategoryId::of).toList();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid category id in 'genres': " + e.getMessage());
        }
    }
}
