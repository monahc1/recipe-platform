package com.flavorshare.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flavorshare.dto.ReviewRequest;
import com.flavorshare.model.Recipe;
import com.flavorshare.model.Review;
import com.flavorshare.model.User;
import com.flavorshare.repo.RecipeRepository;
import com.flavorshare.repo.ReviewRepository;
import com.flavorshare.repo.UserRepository;
import com.flavorshare.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ReviewController
 * Tests review creation, retrieval, and deletion
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Review Controller Tests")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private Recipe testRecipe;
    private Review testReview;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        // Clean database
        reviewRepository.deleteAll();
        recipeRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("reviewer");
        testUser.setEmail("reviewer@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFullName("Test Reviewer");
        testUser = userRepository.save(testUser);

        // Generate JWT token
        jwtToken = jwtUtil.generateToken(testUser.getUsername(), testUser.getId());

        // Create test recipe
        testRecipe = new Recipe();
        testRecipe.setTitle("Reviewable Recipe");
        testRecipe.setDescription("A recipe to review");
        testRecipe.setIngredients(List.of("Ingredient 1", "Ingredient 2"));
        testRecipe.setInstructions(List.of("Step 1", "Step 2"));
        testRecipe.setCookTime(30);
        testRecipe.setServings(4);
        testRecipe.setDifficulty(Recipe.Difficulty.MEDIUM);
        testRecipe.setCategory(Recipe.Category.MAIN_COURSE);
        testRecipe.setAuthor(testUser);
        testRecipe = recipeRepository.save(testRecipe);

        // Create test review
        testReview = new Review();
        testReview.setRating(4);
        testReview.setComment("Great recipe!");
        testReview.setRecipe(testRecipe);
        testReview.setUser(testUser);
        testReview = reviewRepository.save(testReview);
    }

    // ==================== GET REVIEWS TESTS ====================

    @Test
    @DisplayName("Should get all reviews for a recipe")
    void shouldGetAllReviewsForRecipe() throws Exception {
        mockMvc.perform(get("/api/recipes/" + testRecipe.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].rating").value(4))
                .andExpect(jsonPath("$[0].comment").value("Great recipe!"));
    }

    @Test
    @DisplayName("Should return empty array when no reviews exist")
    void shouldReturnEmptyArrayWhenNoReviews() throws Exception {
        reviewRepository.deleteAll();

        mockMvc.perform(get("/api/recipes/" + testRecipe.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should return 404 for non-existent recipe")
    void shouldReturn404ForNonExistentRecipe() throws Exception {
        mockMvc.perform(get("/api/recipes/99999/reviews"))
                .andExpect(status().isNotFound());
    }

    // ==================== ADD REVIEW TESTS ====================

    @Test
    @DisplayName("Should add review with authentication")
    void shouldAddReviewWithAuth() throws Exception {
        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setRating(5);
        reviewRequest.setComment("Absolutely amazing recipe!");

        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/reviews")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Absolutely amazing recipe!"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Should reject review without authentication")
    void shouldRejectReviewWithoutAuth() throws Exception {
        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setRating(5);
        reviewRequest.setComment("Great!");

        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject review with invalid rating (too low)")
    void shouldRejectReviewWithRatingTooLow() throws Exception {
        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setRating(0); // Invalid - must be 1-5
        reviewRequest.setComment("Bad rating");

        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/reviews")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject review with invalid rating (too high)")
    void shouldRejectReviewWithRatingTooHigh() throws Exception {
        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setRating(6); // Invalid - must be 1-5
        reviewRequest.setComment("Bad rating");

        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/reviews")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject review with blank comment")
    void shouldRejectReviewWithBlankComment() throws Exception {
        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setRating(5);
        reviewRequest.setComment(""); // Invalid - cannot be blank

        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/reviews")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should accept all valid rating values (1-5)")
    void shouldAcceptAllValidRatings() throws Exception {
        for (int rating = 1; rating <= 5; rating++) {
            ReviewRequest reviewRequest = new ReviewRequest();
            reviewRequest.setRating(rating);
            reviewRequest.setComment("Rating " + rating + " stars");

            mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/reviews")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reviewRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.rating").value(rating));
        }
    }

    // ==================== DELETE REVIEW TESTS ====================

    @Test
    @DisplayName("Should delete own review")
    void shouldDeleteOwnReview() throws Exception {
        mockMvc.perform(delete("/api/recipes/" + testRecipe.getId() + 
                "/reviews/" + testReview.getId())
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/recipes/" + testRecipe.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should reject delete without authentication")
    void shouldRejectDeleteWithoutAuth() throws Exception {
        mockMvc.perform(delete("/api/recipes/" + testRecipe.getId() + 
                "/reviews/" + testReview.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent review")
    void shouldReturn404WhenDeletingNonExistentReview() throws Exception {
        mockMvc.perform(delete("/api/recipes/" + testRecipe.getId() + "/reviews/99999")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should not allow deleting another user's review")
    void shouldNotAllowDeletingOtherUsersReview() throws Exception {
        // Create another user
        User otherUser = new User();
        otherUser.setUsername("otherreviewer");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword(passwordEncoder.encode("password123"));
        otherUser = userRepository.save(otherUser);

        String otherToken = jwtUtil.generateToken(otherUser.getUsername(), otherUser.getId());

        // Try to delete testUser's review with otherUser's token
        mockMvc.perform(delete("/api/recipes/" + testRecipe.getId() + 
                "/reviews/" + testReview.getId())
                .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    // ==================== AVERAGE RATING TESTS ====================

    @Test
    @DisplayName("Should calculate average rating correctly")
    void shouldCalculateAverageRatingCorrectly() throws Exception {
        // Add multiple reviews
        Review review2 = new Review();
        review2.setRating(5);
        review2.setComment("Excellent!");
        review2.setRecipe(testRecipe);
        review2.setUser(testUser);
        reviewRepository.save(review2);

        Review review3 = new Review();
        review3.setRating(3);
        review3.setComment("Good");
        review3.setRecipe(testRecipe);
        review3.setUser(testUser);
        reviewRepository.save(review3);

        // Average should be (4 + 5 + 3) / 3 = 4.0
        mockMvc.perform(get("/api/recipes/" + testRecipe.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating", closeTo(4.0, 0.1)));
    }

    // ==================== MULTIPLE REVIEWS TESTS ====================

    @Test
    @DisplayName("Should allow user to add multiple reviews to different recipes")
    void shouldAllowMultipleReviewsToDifferentRecipes() throws Exception {
        // Create second recipe
        Recipe recipe2 = new Recipe();
        recipe2.setTitle("Second Recipe");
        recipe2.setDescription("Another recipe");
        recipe2.setIngredients(List.of("Ingredient"));
        recipe2.setInstructions(List.of("Step"));
        recipe2.setCookTime(20);
        recipe2.setServings(2);
        recipe2.setDifficulty(Recipe.Difficulty.EASY);
        recipe2.setCategory(Recipe.Category.DESSERT);
        recipe2.setAuthor(testUser);
        recipe2 = recipeRepository.save(recipe2);

        // Review first recipe
        ReviewRequest review1 = new ReviewRequest();
        review1.setRating(4);
        review1.setComment("Good recipe");

        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/reviews")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(review1)))
                .andExpect(status().isCreated());

        // Review second recipe
        ReviewRequest review2 = new ReviewRequest();
        review2.setRating(5);
        review2.setComment("Great recipe");

        mockMvc.perform(post("/api/recipes/" + recipe2.getId() + "/reviews")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(review2)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should show reviews from multiple users")
    void shouldShowReviewsFromMultipleUsers() throws Exception {
        // Create second user
        User user2 = new User();
        user2.setUsername("reviewer2");
        user2.setEmail("reviewer2@example.com");
        user2.setPassword(passwordEncoder.encode("password123"));
        user2 = userRepository.save(user2);

        String token2 = jwtUtil.generateToken(user2.getUsername(), user2.getId());

        // User 2 adds review
        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setRating(5);
        reviewRequest.setComment("Love this recipe!");

        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/reviews")
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isCreated());

        // Get all reviews - should show 2 (original + new)
        mockMvc.perform(get("/api/recipes/" + testRecipe.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle long comments")
    void shouldHandleLongComments() throws Exception {
        String longComment = "A".repeat(1000); // 1000 characters

        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setRating(5);
        reviewRequest.setComment(longComment);

        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/reviews")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comment").value(longComment));
    }

    @Test
    @DisplayName("Should handle special characters in comments")
    void shouldHandleSpecialCharactersInComments() throws Exception {
        String specialComment = "Great recipe! 😋 👨‍🍳 ★★★★★";

        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setRating(5);
        reviewRequest.setComment(specialComment);

        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/reviews")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comment").value(specialComment));
    }
}
