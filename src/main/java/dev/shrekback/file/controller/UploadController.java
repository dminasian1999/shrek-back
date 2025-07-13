package dev.shrekback.file.controller;//package dev.productsback.file.controller;
//
//import dev.productsback.file.service.S3Service;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//@RequestMapping("/museum")
//@RestController
//@RequiredArgsConstructor
//@CrossOrigin
//public class UploadController {
//
//    final S3Service s3Service;
//
//
//    @PostMapping("/blog/post/file/upload")
//    public String uploadImage(@RequestParam("file") MultipartFile file) {
//        return s3Service.uploadFile(file);
//    }
//}
