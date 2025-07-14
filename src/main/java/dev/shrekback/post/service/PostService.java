package dev.shrekback.post.service;

import dev.shrekback.post.dto.NewPostDto;
import dev.shrekback.post.dto.PostDto;
import dev.shrekback.post.dto.QueryDto;
import dev.shrekback.post.dto.OrderrDto;
import dev.shrekback.post.model.Adjustment;
import dev.shrekback.post.model.Orderr;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PostService {

    PostDto addNewPost(String author, NewPostDto newPostDto);


    List<PostDto> getAllPosts();

    List<OrderrDto> getAllReceipts();

    PostDto removePost(String id);

    PostDto updatePost(String id, NewPostDto newPostDto);

    void deleteFileFromS3(String fileName);

    String saveFiles(MultipartFile file);

    String saveOrder(OrderrDto orderr);

    PostDto findPostById(String id);

    List<PostDto> findPostsByIds(String[] ids);

    List<PostDto> findPostsWithCriteriaAndSort(QueryDto query);

    List<PostDto> findPostByCategory(String category);


    List<PostDto> findByCriteriaAndSort(String criteria,String sort,Boolean asc);


}
