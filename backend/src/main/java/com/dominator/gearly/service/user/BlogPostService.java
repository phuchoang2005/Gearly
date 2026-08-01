package com.dominator.gearly.service.user;

import com.dominator.gearly.dto.BlogPostDetailDTO;
import com.dominator.gearly.dto.BlogPostSummaryDTO;
import com.dominator.gearly.mapper.BlogPostMapper;
import com.dominator.gearly.model.BlogPost;
import com.dominator.gearly.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.dominator.gearly.exception.ResourceNotFoundException;

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
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with ID: " + id));
        return blogPostMapper.toDetailDto(blogPost);
    }
}
