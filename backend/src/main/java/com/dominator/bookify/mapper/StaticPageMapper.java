package com.dominator.bookify.mapper;

import com.dominator.bookify.dto.StaticPageDTO;
import com.dominator.bookify.model.StaticPage;
import org.springframework.stereotype.Component;

/** Maps {@link StaticPage} entities to their response DTO. */
@Component
public class StaticPageMapper {

    public StaticPageDTO toDto(StaticPage staticPage) {
        StaticPageDTO dto = new StaticPageDTO();
        dto.setId(staticPage.getId());
        dto.setTitle(staticPage.getTitle());
        dto.setSlug(staticPage.getSlug());
        dto.setContent(staticPage.getContent());
        dto.setLastModified(staticPage.getLastModified());
        return dto;
    }
}
