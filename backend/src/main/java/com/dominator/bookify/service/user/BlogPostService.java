package com.dominator.bookify.service.user;

import com.dominator.bookify.dto.BlogPostDetailDTO;
import com.dominator.bookify.dto.BlogPostSummaryDTO;
import com.dominator.bookify.mapper.BlogPostMapper;
import com.dominator.bookify.model.BlogPost;
import com.dominator.bookify.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.dominator.bookify.exception.ResourceNotFoundException;

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
