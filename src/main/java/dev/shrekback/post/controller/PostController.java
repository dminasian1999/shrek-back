package dev.shrekback.post.controller;

import dev.shrekback.post.dto.NewPostDto;
import dev.shrekback.post.dto.PostDto;
import dev.shrekback.post.dto.QueryDto;
import dev.shrekback.post.dto.ReceiptDto;
import dev.shrekback.post.model.Adjustment;
import dev.shrekback.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin
public class PostController  {

    final PostService postService;

    @DeleteMapping("/post/file/delete/{file}")
    public void deleteFile(@PathVariable String file) {
        System.out.println("delete");
        postService.deleteFileFromS3(file);
    }

    @PostMapping("/post/file/upload")
    public String uploadFiles(@RequestParam("file") MultipartFile file) {
        return postService.saveFiles(file);
    }


    @PostMapping("/post/{author}")
    public PostDto addNewPost(@PathVariable String author, @RequestBody NewPostDto newPostDto) {

        return postService.addNewPost(author, newPostDto);
    }

    @PostMapping("/posts/wishList")
    public List<PostDto> findPostsByIds(@RequestBody String[] ids) {
        return postService.findPostsByIds(ids);
    }

    @GetMapping("/posts/category/{category}")
    public List<PostDto> findPostByCategory(@PathVariable String category) {
        return postService.findPostByCategory(category);
    }
    @PostMapping("/posts/search")
    public List<PostDto> findPostsWithCriteriaAndSort(@RequestBody QueryDto query) {
        return postService.findPostsWithCriteriaAndSort( query);
    }

    @GetMapping("/posts/criteria/{criteria}/sort/{sort}/asc/{asc}")
    public List<PostDto> findByCriteriaAndSort(@PathVariable String criteria,@PathVariable String sort,@PathVariable Boolean asc) {
        return postService.findByCriteriaAndSort(criteria,sort,asc);
    }


    @GetMapping("/post/{id}")
    public PostDto findPostById(@PathVariable String id) {
        return postService.findPostById(id);
    }




    @GetMapping("/posts/receipts")
    public List<ReceiptDto> getAllReceipts() {
        return postService.getAllReceipts();
    }

    @GetMapping("/posts")
    public List<PostDto> getAllPosts() {
        return postService.getAllPosts();
    }

    @DeleteMapping("/post/{id}")
    public PostDto removePost(@PathVariable String id) {

        return postService.removePost(id);
    }

    @PutMapping("/post/{id}")
    public PostDto updatePost(@PathVariable String id, @RequestBody NewPostDto newPostDto) {
        return postService.updatePost(id, newPostDto);
    }


    @PostMapping("/post/{id}/adjust/{add}/number/{num}/{author}")
    public Adjustment adjust(@PathVariable String id, @PathVariable String author, @PathVariable Boolean add, @PathVariable int num) {
        return postService.adjust(id, author, num, add);

    }


}
