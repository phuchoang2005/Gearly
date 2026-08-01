package com.dominator.gearly.service.user;

import com.dominator.gearly.dto.StaticPageDTO;
import com.dominator.gearly.mapper.StaticPageMapper;
import com.dominator.gearly.model.StaticPage;
import com.dominator.gearly.repository.StaticPageRepository;
import com.dominator.gearly.exception.ResourceNotFoundException;
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
