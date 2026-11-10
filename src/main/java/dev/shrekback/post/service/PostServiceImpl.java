package dev.shrekback.post.service;

import dev.shrekback.post.dao.PostRepository;
import dev.shrekback.file.service.S3Service;
import dev.shrekback.post.dto.NewPostDto;
import dev.shrekback.accounting.dto.OrderDto;
import dev.shrekback.post.dto.PostDto;
import dev.shrekback.post.dto.QueryDto;
import dev.shrekback.post.dto.exceptions.PostNotFoundException;
import dev.shrekback.post.model.Post;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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

    private final S3Service s3Service;

    @Override
    public PostDto addNewPost(String author, NewPostDto newPostDto) {
        validateProductInput(newPostDto, false);
        Post post = modelMapper.map(newPostDto, Post.class);


        post = postRepository.save(post);
        return modelMapper.map(post, PostDto.class);
    }

    @Override
    public void deleteFileFromS3(String fileName) {
        s3Service.deleteFile(fileName);
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
        validateSearch(queryDto);
        Query mongoQuery = new Query();

        if (queryDto.getId() != null && !queryDto.getId().isBlank()) {
            mongoQuery.addCriteria(Criteria.where("id").regex(Pattern.compile(Pattern.quote(queryDto.getId()), Pattern.CASE_INSENSITIVE)));
        }
        if (queryDto.getName() != null && !queryDto.getName().isBlank()) {
            mongoQuery.addCriteria(Criteria.where("name").regex(Pattern.compile(Pattern.quote(queryDto.getName()), Pattern.CASE_INSENSITIVE)));
        }
        if (queryDto.getCategory() != null && !queryDto.getCategory().isBlank()) {
            mongoQuery.addCriteria(Criteria.where("category").regex(Pattern.compile(Pattern.quote(queryDto.getCategory()), Pattern.CASE_INSENSITIVE)));
        }
        if (queryDto.getSubCategory() != null && !queryDto.getSubCategory().isBlank()) {
            mongoQuery.addCriteria(Criteria.where("subCategory").regex(Pattern.compile(Pattern.quote(queryDto.getSubCategory()), Pattern.CASE_INSENSITIVE)));
        }
        if (queryDto.getColor() != null && !queryDto.getColor().isBlank()) {
            mongoQuery.addCriteria(Criteria.where("color").regex(Pattern.compile(Pattern.quote(queryDto.getColor()), Pattern.CASE_INSENSITIVE)));
        }
        if (queryDto.getDesc() != null && !queryDto.getDesc().isBlank()) {
            mongoQuery.addCriteria(Criteria.where("desc").regex(Pattern.compile(Pattern.quote(queryDto.getDesc()), Pattern.CASE_INSENSITIVE)));
        }

        if (queryDto.getDateFrom() != null || queryDto.getDateTo() != null) {
            Criteria dates = Criteria.where("dateCreated");
            if (queryDto.getDateFrom() != null) dates.gte(queryDto.getDateFrom().atStartOfDay());
            if (queryDto.getDateTo() != null) dates.lt(queryDto.getDateTo().plusDays(1).atStartOfDay());
            mongoQuery.addCriteria(dates);
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
            mongoQuery.addCriteria(Criteria.where("material").regex(Pattern.compile(Pattern.quote(queryDto.getMaterial()), Pattern.CASE_INSENSITIVE)));
        }

        // ✅ Sorting
        Sort.Direction direction = Boolean.TRUE.equals(queryDto.getAsc()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        String sortField = (queryDto.getSortField() != null && !queryDto.getSortField().trim().isEmpty())
                ? queryDto.getSortField()
                : "dateCreated";

        mongoQuery.with(Sort.by(direction, sortField)).limit(500);

        List<Post> posts = mongoTemplate.find(mongoQuery, Post.class);
        return posts.stream()
                .map(post -> modelMapper.map(post, PostDto.class))
                .collect(Collectors.toList());
    }


    private static void validateSearch(QueryDto query) {
        if (query == null) throw invalid("Search details are required.");
        String[] text = {query.getId(), query.getName(), query.getCategory(), query.getSubCategory(),
                query.getColor(), query.getDesc(), query.getMaterial()};
        for (String value : text) if (value != null && value.length() > 200) throw invalid("Search text is too long.");
        if (query.getSortField() != null && !query.getSortField().isBlank()
                && !Set.of("name", "price", "dateCreated", "category").contains(query.getSortField())) {
            throw invalid("Unsupported sort field.");
        }
        if ((query.getMinPrice() != null && (!Double.isFinite(query.getMinPrice()) || query.getMinPrice() < 0))
                || (query.getMaxPrice() != null && (!Double.isFinite(query.getMaxPrice()) || query.getMaxPrice() < 0))
                || (query.getMinPrice() != null && query.getMaxPrice() != null && query.getMinPrice() > query.getMaxPrice())) {
            throw invalid("Enter a valid price range.");
        }
        if (query.getDateFrom() != null && query.getDateTo() != null && query.getDateFrom().isAfter(query.getDateTo())) {
            throw invalid("Enter a valid date range.");
        }
    }

    private static void validateProductInput(NewPostDto dto, boolean partial) {
        if (dto == null) throw invalid("Product details are required.");
        if ((!partial || dto.getName() != null) && (dto.getName() == null || dto.getName().isBlank() || dto.getName().length() > 200)) {
            throw invalid("Enter a product name of up to 200 characters.");
        }
        if ((!partial || dto.getPrice() != null) && (dto.getPrice() == null || !Double.isFinite(dto.getPrice()) || dto.getPrice() <= 0)) {
            throw invalid("Product price must be a positive number.");
        }
        if ((!partial || dto.getWeight() != null) && (dto.getWeight() == null || !Double.isFinite(dto.getWeight()) || dto.getWeight() <= 0 || dto.getWeight() > 30000)) {
            throw invalid("Product weight must be between 0 and 30000 grams.");
        }
        if (dto.getSizeQuantities() != null) {
            Set<String> sizes = new HashSet<>();
            for (var stock : dto.getSizeQuantities()) {
                if (stock == null || stock.getSize() == null || stock.getSize().isBlank() || stock.getSize().length() > 50
                        || stock.getQuantity() == null || stock.getQuantity() < 0 || !sizes.add(stock.getSize())) {
                    throw invalid("Each size must be unique and have a nonnegative stock quantity.");
                }
            }
        }
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
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
        validateProductInput(newPostDto, true);
        // Fetch the existing post
        Post post = postRepository.findById(id)
                .orElseThrow(PostNotFoundException::new);

        // Manually update fields you want to allow updating
        if (newPostDto.getName() != null) post.setName(newPostDto.getName());
        if (newPostDto.getImageUrls() != null) post.setImageUrls(newPostDto.getImageUrls());
        if (newPostDto.getPrice() != null) post.setPrice(newPostDto.getPrice());
        if (newPostDto.getCategory() != null) post.setCategory(newPostDto.getCategory());
        if (newPostDto.getSubCategory() != null) post.setSubCategory(newPostDto.getSubCategory());
        if (newPostDto.getWeight() != null) post.setWeight(newPostDto.getWeight());
        if (newPostDto.getDesc() != null) post.setDesc(newPostDto.getDesc());
        if (newPostDto.getColor() != null) post.setColor(newPostDto.getColor());
        if (newPostDto.getMaterial() != null) post.setMaterial(newPostDto.getMaterial());
        if (newPostDto.getSizeQuantities() != null) post.setSizeQuantities(newPostDto.getSizeQuantities());

        // Save updated post
        postRepository.save(post);

        // Map to PostDto for response
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
        if (ids == null || ids.length > 100 || java.util.Arrays.stream(ids).anyMatch(id -> id == null || id.isBlank() || id.length() > 100)) {
            throw invalid("Provide up to 100 valid product IDs.");
        }
        return postRepository.findByIdIn(ids)
                .map(p -> modelMapper.map(p, PostDto.class))
                .collect(Collectors.toList());
    }


    @Override
    public String saveFiles(MultipartFile file) {
        return s3Service.uploadFile(file);
    }

    @Override
    public String saveOrder(OrderDto orderr) {
        return "";
    }


}
