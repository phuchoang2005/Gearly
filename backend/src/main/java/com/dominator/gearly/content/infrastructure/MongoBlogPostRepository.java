package com.dominator.gearly.content.infrastructure;

import com.dominator.gearly.content.domain.BlogPost;
import com.dominator.gearly.content.domain.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * The {@link BlogPostRepository} adapter.
 *
 * <p>Thin to the point of looking pointless, and it is not: the Spring Data interface is
 * package-private, so {@code content.application} cannot reach it even by accident. That is what
 * {@code spring_data_repositories_live_only_in_infrastructure} asks for, and the same shape the
 * other contexts' adapters take.
 */
@Repository
@RequiredArgsConstructor
public class MongoBlogPostRepository implements BlogPostRepository {

    private final SpringDataBlogPostRepository posts;

    @Override
    public List<BlogPost> findAll() {
        return posts.findAll();
    }

    @Override
    public Optional<BlogPost> findById(String id) {
        return posts.findById(id);
    }
}
