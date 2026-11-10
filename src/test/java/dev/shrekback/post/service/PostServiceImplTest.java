package dev.shrekback.post.service;

import dev.shrekback.file.service.S3Service;
import dev.shrekback.post.dao.PostRepository;
import dev.shrekback.post.dto.NewPostDto;
import dev.shrekback.post.dto.QueryDto;
import dev.shrekback.post.model.Post;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PostServiceImplTest {
    MongoTemplate mongo = mock(MongoTemplate.class);
    PostRepository products = mock(PostRepository.class);
    PostServiceImpl service = new PostServiceImpl(new ModelMapper(), mongo, products, mock(S3Service.class));
    ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void combinesDateBoundsAndTreatsSearchAsLiteralText() throws Exception {
        when(mongo.find(any(Query.class), eq(Post.class))).thenReturn(List.of());
        QueryDto criteria = json.readValue("{\"name\":\"[gift+\",\"dateFrom\":\"2026-01-01\",\"dateTo\":\"2026-01-31\"}", QueryDto.class);
        assertDoesNotThrow(() -> service.findPostsWithCriteriaAndSort(criteria));
        var query = org.mockito.ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(Post.class));
        var filter = query.getValue().getQueryObject();
        assertEquals(Pattern.quote("[gift+"), ((Pattern) filter.get("name")).pattern());
        var dates = (org.bson.Document) filter.get("dateCreated");
        assertTrue(dates.containsKey("$gte")); assertTrue(dates.containsKey("$lt"));
    }

    @Test
    void rejectsUnsafeSortAndInvalidRangesBeforeDatabaseCall() throws Exception {
        for (String body : List.of("{\"sortField\":\"$where\"}", "{\"minPrice\":100,\"maxPrice\":10}",
                "{\"dateFrom\":\"2026-02-01\",\"dateTo\":\"2026-01-01\"}")) {
            QueryDto query = json.readValue(body, QueryDto.class);
            assertThrows(ResponseStatusException.class, () -> service.findPostsWithCriteriaAndSort(query));
        }
        verifyNoInteractions(mongo);
    }

    @Test
    void rejectsNegativeProductPricesBeforeSave() {
        NewPostDto input = new NewPostDto("Gift", List.of(), -1.0, "gifts", "", 100.0, "", "", "", List.of());
        assertThrows(ResponseStatusException.class, () -> service.addNewPost("admin", input));
        verifyNoInteractions(products);
    }
}
