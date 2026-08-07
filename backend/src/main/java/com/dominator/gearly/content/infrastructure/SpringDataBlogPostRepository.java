package com.dominator.gearly.content.infrastructure;

import com.dominator.gearly.content.domain.BlogPost;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Spring Data's view of the articles collection. Reached only through {@link MongoBlogPostRepository}. */
interface SpringDataBlogPostRepository extends MongoRepository<BlogPost, String> {
}
