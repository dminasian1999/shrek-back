package dev.shrekback.post.dao;

import dev.shrekback.post.model.Post;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.stream.Stream;

public interface PostRepository extends CrudRepository<Post, String> {
	Stream<Post> findByCategory(String category);
	Stream<Post> findByCategoryOrderByNameAsc(String category);
	Stream<Post> findByCategoryOrderByNameDesc(String category);
	Stream<Post> findByCategoryOrderByDateCreatedAsc(String category);
	Stream<Post> findByCategoryOrderByDateCreatedDesc(String category);
	Stream<Post> findByCategoryContainsIgnoreCase(String query);
	Stream<Post> findByCategoryOrderByPriceAsc(String category);
	Stream<Post> findByCategoryOrderByPriceDesc(String category);
	Stream<Post> findByCategoryAndPriceBetween(String type, double price, double price2);
	Stream<Post> findByOrderByDateCreatedDesc();
	Stream<Post>findByNameIgnoreCase(String title);
	Stream<Post> findByIdIn(String [] ids);        // ← “In”, not “Containing”
	Stream<Post> findByColor(String color);
	Stream<Post> findByMaterial(String color);






}
