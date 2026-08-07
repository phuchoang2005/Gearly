package com.dominator.gearly.content.application;

import com.dominator.gearly.content.api.BlogPostDetailDTO;
import com.dominator.gearly.content.api.BlogPostSummaryDTO;
import com.dominator.gearly.content.api.BlogPostMapper;
import com.dominator.gearly.content.domain.BlogPost;
import com.dominator.gearly.content.domain.BlogPostRepository;
import com.dominator.gearly.content.domain.ContentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogPostService {

    private final BlogPostRepository blogPostRepository;
    private final BlogPostMapper blogPostMapper;

    public List<BlogPostSummaryDTO> getAllBlogSummaries() {
        return blogPostRepository.findAll().stream()
                .map(blogPostMapper::toSummaryDto)
                .collect(Collectors.toList());
    }

    public BlogPostDetailDTO getBlogPostDetails(String id) {
        BlogPost blogPost = blogPostRepository.findById(id)
                .orElseThrow(() -> ContentNotFoundException.blogPost(id));
        return blogPostMapper.toDetailDto(blogPost);
    }
}
