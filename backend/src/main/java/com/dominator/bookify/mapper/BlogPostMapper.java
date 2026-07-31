package com.dominator.bookify.mapper;

import com.dominator.bookify.dto.BlogPostDetailDTO;
import com.dominator.bookify.dto.BlogPostSummaryDTO;
import com.dominator.bookify.model.BlogPost;
import org.springframework.stereotype.Component;

/** Maps {@link BlogPost} entities to their list-summary and full-detail DTOs. */
@Component
public class BlogPostMapper {

    public BlogPostSummaryDTO toSummaryDto(BlogPost blogPost) {
        BlogPostSummaryDTO dto = new BlogPostSummaryDTO();
        dto.setId(blogPost.getId());
        dto.setTitle(blogPost.getTitle());
        dto.setAuthor(blogPost.getAuthor());
        dto.setPublishDate(blogPost.getPublishDate());
        dto.setTags(blogPost.getTags());
        return dto;
    }

    public BlogPostDetailDTO toDetailDto(BlogPost blogPost) {
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
