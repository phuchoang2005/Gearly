package com.dominator.bookify.service.user;

import com.dominator.bookify.dto.BlogPostDetailDTO;
import com.dominator.bookify.dto.BlogPostSummaryDTO;
import com.dominator.bookify.model.BlogPost;
import com.dominator.bookify.repository.BlogPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BlogPostService {

    private final BlogPostRepository blogPostRepository;

    public BlogPostService(BlogPostRepository blogPostRepository) {
        this.blogPostRepository = blogPostRepository;
    }

    public List<BlogPostSummaryDTO> getAllBlogSummaries() {
        return blogPostRepository.findAll().stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
    }

    public BlogPostDetailDTO getBlogPostDetails(String id) {
        BlogPost blogPost = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog post not found with ID: " + id));
        return convertToDetailDto(blogPost);
    }

    private BlogPostSummaryDTO convertToSummaryDto(BlogPost blogPost) {
        BlogPostSummaryDTO dto = new BlogPostSummaryDTO();
        dto.setId(blogPost.getId());
        dto.setTitle(blogPost.getTitle());
        dto.setAuthor(blogPost.getAuthor());
        dto.setPublishDate(blogPost.getPublishDate());
        dto.setTags(blogPost.getTags());
        return dto;
    }

    private BlogPostDetailDTO convertToDetailDto(BlogPost blogPost) {
        BlogPostDetailDTO dto = new BlogPostDetailDTO();
        dto.setId(blogPost.getId());
        dto.setTitle(blogPost.getTitle());
        dto.setAuthor(blogPost.getAuthor());
        dto.setPublishDate(blogPost.getPublishDate());
        dto.setBookId(blogPost.getBookId());
        dto.setTags(blogPost.getTags());
        dto.setContent(blogPost.getContent());
        return dto;
    }
}