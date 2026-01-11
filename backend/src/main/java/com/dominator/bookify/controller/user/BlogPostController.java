package com.dominator.bookify.controller.user;

import com.dominator.bookify.dto.BlogPostDetailDTO;
import com.dominator.bookify.dto.BlogPostSummaryDTO;
import com.dominator.bookify.service.user.BlogPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/blogposts")
public class BlogPostController {
    private final BlogPostService blogPostService;
    @GetMapping
    public ResponseEntity<List<BlogPostSummaryDTO>> getAllBlogSummaries() {
        List<BlogPostSummaryDTO> summaries = blogPostService.getAllBlogSummaries();
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogPostDetailDTO> getBlogPostDetails(@PathVariable String id) {
        BlogPostDetailDTO details = blogPostService.getBlogPostDetails(id);
        return ResponseEntity.ok(details);
    }
}