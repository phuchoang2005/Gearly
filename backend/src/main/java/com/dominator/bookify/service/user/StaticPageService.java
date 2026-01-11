package com.dominator.bookify.service.user;

import com.dominator.bookify.dto.StaticPageDTO;
import com.dominator.bookify.model.StaticPage;
import com.dominator.bookify.repository.StaticPageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StaticPageService {

    private final StaticPageRepository staticPageRepository;

    public StaticPageService(StaticPageRepository staticPageRepository) {
        this.staticPageRepository = staticPageRepository;
    }

    public StaticPageDTO getStaticPageBySlug(String slug) {
        StaticPage staticPage = staticPageRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Static page not found with slug: " + slug));
        return convertToDto(staticPage);
    }

    private StaticPageDTO convertToDto(StaticPage staticPage) {
        StaticPageDTO dto = new StaticPageDTO();
        dto.setId(staticPage.getId());
        dto.setTitle(staticPage.getTitle());
        dto.setSlug(staticPage.getSlug());
        dto.setContent(staticPage.getContent());
        dto.setLastModified(staticPage.getLastModified());
        return dto;
    }
}