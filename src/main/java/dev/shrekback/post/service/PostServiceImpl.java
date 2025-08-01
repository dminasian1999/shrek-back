package dev.shrekback.post.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import dev.shrekback.post.dao.PostRepository;
import dev.shrekback.post.dto.NewPostDto;
import dev.shrekback.accounting.dto.OrderDto;
import dev.shrekback.post.dto.PostDto;
import dev.shrekback.post.dto.QueryDto;
import dev.shrekback.post.dto.exceptions.PostNotFoundException;
import dev.shrekback.post.model.Post;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final ModelMapper modelMapper;
    private final MongoTemplate mongoTemplate;
    private final PostRepository postRepository;

    @Value("${aws.s3.access.key}")
    private String accessKey;

    @Value("${aws.s3.secret.key}")
    private String secretKey;

    @Override
    public PostDto addNewPost(String author, NewPostDto newPostDto) {
        Post post = modelMapper.map(newPostDto, Post.class);


        post = postRepository.save(post);
        return modelMapper.map(post, PostDto.class);
    }

    private String saveFilePicToS3(MultipartFile file) {
        try {
            String s3FileName = file.getOriginalFilename();
            BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
            AmazonS3 amazonS3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                    .withRegion(Regions.US_EAST_1)
                    .build();

            InputStream inputStream = file.getInputStream();
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(file.getContentType());
            String bucketName = "file-upload-dav";
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, s3FileName, inputStream, objectMetadata);
            amazonS3Client.putObject(putObjectRequest);
            return String.format("https://%s.s3.amazonaws.com/%s", bucketName, s3FileName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    @Override
    public void deleteFileFromS3(String fileName) {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        AmazonS3 amazonS3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(Regions.US_EAST_1)
                .build();
        String bucketName = "file-upload-dav";
        amazonS3Client.deleteObject(bucketName, fileName);
    }

    @Override
    public PostDto findPostById(String id) {
        Post post = postRepository.findById(id).orElseThrow(PostNotFoundException::new);
        return modelMapper.map(post, PostDto.class);
    }

    @Override
    public List<PostDto> findPostByCategory(String category) {
        return postRepository.findByCategory(category)
                .map(post -> modelMapper.map(post, PostDto.class))
                .toList();
    }



    @Override
    public List<PostDto> findByCriteriaAndSort(String criteria, String sort, Boolean asc) {
        boolean isAsc = Boolean.TRUE.equals(asc);

        Stream<Post> resultStream = switch (sort) {
            case "price" -> isAsc
                    ? postRepository.findByCategoryOrderByPriceAsc(criteria)
                    : postRepository.findByCategoryOrderByPriceDesc(criteria);
            case "name" -> isAsc
                    ? postRepository.findByCategoryOrderByNameAsc(criteria)
                    : postRepository.findByCategoryOrderByNameDesc(criteria);
            case "dateCreated" -> isAsc
                    ? postRepository.findByCategoryOrderByDateCreatedAsc(criteria)
                    : postRepository.findByCategoryOrderByDateCreatedDesc(criteria);
            default -> postRepository.findByCategoryContainsIgnoreCase(criteria); // fallback search
        };

        return resultStream
                .map(post -> modelMapper.map(post, PostDto.class))
                .toList();
    }
    @Override
    public List<PostDto> findPostsWithCriteriaAndSort(QueryDto queryDto) {
        Query mongoQuery = new Query();

        if (queryDto.getId() != null && !queryDto.getId().isBlank()) {
            mongoQuery.addCriteria(Criteria.where("id").regex(Pattern.compile(queryDto.getId(), Pattern.CASE_INSENSITIVE)));
        }
        if (queryDto.getName() != null && !queryDto.getName().isBlank()) {
            mongoQuery.addCriteria(Criteria.where("name").regex(Pattern.compile(queryDto.getName(), Pattern.CASE_INSENSITIVE)));
        }
        if (queryDto.getCategory() != null && !queryDto.getCategory().isBlank()) {
            mongoQuery.addCriteria(Criteria.where("category").regex(Pattern.compile(queryDto.getCategory(), Pattern.CASE_INSENSITIVE)));
        }
        if (queryDto.getColor() != null && !queryDto.getColor().isBlank()) {
            mongoQuery.addCriteria(Criteria.where("color").regex(Pattern.compile(queryDto.getColor(), Pattern.CASE_INSENSITIVE)));
        }
        if (queryDto.getDesc() != null && !queryDto.getDesc().isBlank()) {
            mongoQuery.addCriteria(Criteria.where("desc").regex(Pattern.compile(queryDto.getDesc(), Pattern.CASE_INSENSITIVE)));
        }

        if (queryDto.getDateFrom() != null) {
            mongoQuery.addCriteria(Criteria.where("dateCreated").gte(queryDto.getDateFrom().atStartOfDay()));
        }

        if (queryDto.getDateTo() != null) {
            mongoQuery.addCriteria(Criteria.where("dateCreated").lte(queryDto.getDateTo().atTime(23, 59, 59)));
        }

        if (queryDto.getMinPrice() != null || queryDto.getMaxPrice() != null) {
            Criteria priceCriteria = Criteria.where("price");

            if (queryDto.getMinPrice() != null) {
                priceCriteria = priceCriteria.gte(queryDto.getMinPrice());
            }
            if (queryDto.getMaxPrice() != null) {
                priceCriteria = priceCriteria.lte(queryDto.getMaxPrice()); // or .lt() if exclusive
            }

            mongoQuery.addCriteria(priceCriteria);
        }


        // ✅ Handle List<String> materials
        if (queryDto.getMaterial() != null && !queryDto.getMaterial().isEmpty()) {
            mongoQuery.addCriteria(Criteria.where("material").regex(queryDto.getMaterial()));
        }

        // ✅ Sorting
        Sort.Direction direction = Boolean.TRUE.equals(queryDto.getAsc()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        String sortField = (queryDto.getSortField() != null && !queryDto.getSortField().trim().isEmpty())
                ? queryDto.getSortField()
                : "dateCreated";

        mongoQuery.with(Sort.by(direction, sortField));

        List<Post> posts = mongoTemplate.find(mongoQuery, Post.class);
        return posts.stream()
                .map(post -> modelMapper.map(post, PostDto.class))
                .collect(Collectors.toList());
    }


    @Override
    public List<PostDto> getAllPosts() {
        return StreamSupport.stream(postRepository.findAll().spliterator(), false)
                .map(p -> modelMapper.map(p, PostDto.class))
                .collect(Collectors.toList());
    }


    @Override
    public PostDto removePost(String id) {
        Post post = postRepository.findById(id).orElseThrow(PostNotFoundException::new);
        postRepository.delete(post);
        return modelMapper.map(post, PostDto.class);
    }

    @Override
    public PostDto updatePost(String id, NewPostDto newPostDto) {
        Post post = postRepository.findById(id).orElseThrow(PostNotFoundException::new);
        modelMapper.map(newPostDto, post);


        postRepository.save(post);
        return modelMapper.map(post, PostDto.class);
    }

//    @Override
//    public Adjustment saveOrder(String id, String author, int num, boolean add) {
//        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException());
//        Adjustment adjustment = new Adjustment(num, add, author);
//        post.adjust(adjustment);
//        postRepository.save(post);
////        double totalSell = post.getSell() * num;
////        double totalbuy = post.getBuy() * num;
////        double income = post.getSell() - post.getBuy();
//        double totalSell = post.getPrice() * num;
//        double totalbuy = post.getPrice() * num;
//        double income = post.getPrice() - post.getPrice();
////        Receipt receipt = new Receipt(post.getName(), post.getImageUrl(), num, totalSell, totalbuy, totalSell - totalbuy, author, post.getCategory());
//
//        Orderr orderr = new Orderr(post.getName(), post.getImageUrls().get(0), num, totalSell, totalbuy, totalSell - totalbuy, author, post.getCategory());
//        orderrRepository.save(orderr);
//        return adjustment;
//    }

    @Override
    public List<PostDto> findPostsByIds(String[] ids) {
        return postRepository.findByIdIn(ids)
                .map(p -> modelMapper.map(p, PostDto.class))
                .collect(Collectors.toList());
    }


    @Override
//    public String saveFiles(MultipartFile file) {
    public String saveFiles(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null || file.isEmpty()) {
                throw new IllegalArgumentException("Invalid file");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("Only image files are allowed.");
            }

            BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
            AmazonS3 amazonS3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                    .withRegion(Regions.US_EAST_1)
                    .build();

            InputStream inputStream = file.getInputStream();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(file.getSize());

            String bucketName = "file-upload-dav";
            String key = "ecom-sevan/" + fileName;

            amazonS3Client.putObject(new PutObjectRequest(bucketName, key, inputStream, metadata));

            return String.format("https://%s.s3.amazonaws.com/%s", bucketName, key);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("S3 upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String saveOrder(OrderDto orderr) {
        return "";
    }


}
