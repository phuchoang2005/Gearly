package com.dominator.gearly.content.application;

import com.dominator.gearly.content.api.StaticPageDTO;
import com.dominator.gearly.content.api.StaticPageMapper;
import com.dominator.gearly.content.domain.StaticPage;
import com.dominator.gearly.content.domain.ContentNotFoundException;
import com.dominator.gearly.content.domain.StaticPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaticPageService {

    private final StaticPageRepository staticPageRepository;
    private final StaticPageMapper staticPageMapper;

    public StaticPageDTO getStaticPageBySlug(String slug) {
        StaticPage staticPage = staticPageRepository.findBySlug(slug)
                .orElseThrow(() -> ContentNotFoundException.page(slug));
        return staticPageMapper.toDto(staticPage);
    }
}
