package dev.shrekback.file.service;//package dev.productsback.file.service;
//
//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.model.ObjectMetadata;
//import com.amazonaws.services.s3.model.PutObjectRequest;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.io.InputStream;
//
//@Service
//@RequiredArgsConstructor
//
//public class S3ServiceImpl implements S3Service {
//
//    final AmazonS3 s3;
//    String bucketName = "file-upload-dav";
//
//    @Override
//
//    public String uploadFile(MultipartFile file)  {
//        try (InputStream in = file.getInputStream()) {
//
//            String s3FileName = file.getOriginalFilename();
//            ObjectMetadata objectMetadata = new ObjectMetadata();
//            objectMetadata.setContentType("image/jpeg");
//            PutObjectRequest req = new PutObjectRequest(bucketName, s3FileName, in, objectMetadata);
//            s3.putObject(req);
//            return "https://" + bucketName + ".s3.amazonaws.com/" + s3FileName;
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException(e.getMessage());
//        }
//    }
//}
