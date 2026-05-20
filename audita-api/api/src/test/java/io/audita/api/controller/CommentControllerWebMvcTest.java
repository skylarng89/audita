package io.audita.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.audita.api.security.UserPrincipal;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.CommentEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CommentControllerWebMvcTest {

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @Mock
    CommentService commentService;

    @BeforeEach
    void setUp() {
        CommentController controller = new CommentController(commentService);
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void list_returns_comment_payload() throws Exception {
        UUID changeRequestId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();

        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        ReflectionTestUtils.setField(changeRequest, "id", changeRequestId);
        UserEntity author = new UserEntity("author@example.com", "Author One");
        ReflectionTestUtils.setField(author, "id", authorId);
        CommentEntity comment = new CommentEntity(changeRequest, author, "hello world");
        ReflectionTestUtils.setField(comment, "id", commentId);
        ReflectionTestUtils.setField(comment, "createdAt", OffsetDateTime.now());
        ReflectionTestUtils.setField(comment, "updatedAt", OffsetDateTime.now());

        when(commentService.list(changeRequestId)).thenReturn(List.of(comment));

        mockMvc.perform(get("/api/v1/change-requests/{id}/comments", changeRequestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(commentId.toString()))
                .andExpect(jsonPath("$[0].body").value("hello world"))
                .andExpect(jsonPath("$[0].author.email").value("author@example.com"));
    }

    @Test
    void create_returns_created_comment() throws Exception {
        UUID changeRequestId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();

        UserPrincipal principal = UserPrincipal.ofTenantUser(
                authorId,
                "author@example.com",
                "REQUESTER",
                List.of("REQUESTER"),
                List.of(),
                "tenant-acme");

        ChangeRequestEntity changeRequest = new ChangeRequestEntity();
        ReflectionTestUtils.setField(changeRequest, "id", changeRequestId);
        UserEntity author = new UserEntity("author@example.com", "Author One");
        ReflectionTestUtils.setField(author, "id", authorId);
        CommentEntity created = new CommentEntity(changeRequest, author, "new comment");
        ReflectionTestUtils.setField(created, "id", commentId);
        ReflectionTestUtils.setField(created, "createdAt", OffsetDateTime.now());
        ReflectionTestUtils.setField(created, "updatedAt", OffsetDateTime.now());

        when(commentService.create(eq(changeRequestId), eq(authorId), eq("new comment"))).thenReturn(created);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        try {
            mockMvc.perform(post("/api/v1/change-requests/{id}/comments", changeRequestId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("body", "new comment"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(commentId.toString()))
                    .andExpect(jsonPath("$.body").value("new comment"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(commentService).create(changeRequestId, authorId, "new comment");
    }
}
