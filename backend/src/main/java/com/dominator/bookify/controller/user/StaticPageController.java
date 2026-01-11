package com.dominator.bookify.controller.user;

import com.dominator.bookify.dto.StaticPageDTO;
import com.dominator.bookify.service.user.StaticPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/pages")
public class StaticPageController {

    private final StaticPageService staticPageService;

    @GetMapping("/{slug}")
    public ResponseEntity<StaticPageDTO> getStaticPageBySlug(@PathVariable String slug) {
        StaticPageDTO staticPage = staticPageService.getStaticPageBySlug(slug);
        return ResponseEntity.ok(staticPage);
    }
}