package com.dominator.bookify.service.user;

import com.dominator.bookify.dto.StaticPageDTO;
import com.dominator.bookify.mapper.StaticPageMapper;
import com.dominator.bookify.model.StaticPage;
import com.dominator.bookify.repository.StaticPageRepository;
import com.dominator.bookify.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaticPageService {

    private final StaticPageRepository staticPageRepository;
    private final StaticPageMapper staticPageMapper;

    public StaticPageDTO getStaticPageBySlug(String slug) {
        StaticPage staticPage = staticPageRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Static page not found with slug: " + slug));
        return staticPageMapper.toDto(staticPage);
    }
}
