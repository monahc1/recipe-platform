package com.flavorshare.web;

import com.flavorshare.model.Like;
import com.flavorshare.model.Recipe;
import com.flavorshare.model.User;
import com.flavorshare.repo.LikeRepository;
import com.flavorshare.repo.RecipeRepository;
import com.flavorshare.repo.UserRepository;
import com.flavorshare.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for LikeController
 * Tests like/unlike functionality
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Like Controller Tests")
class LikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LikeRepository likeRepository;

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
    private String jwtToken;

    @BeforeEach
    void setUp() {
        // Clean database
        likeRepository.deleteAll();
        recipeRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("liker");
        testUser.setEmail("liker@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFullName("Test Liker");
        testUser = userRepository.save(testUser);

        // Generate JWT token
        jwtToken = jwtUtil.generateToken(testUser.getUsername(), testUser.getId());

        // Create test recipe
        testRecipe = new Recipe();
        testRecipe.setTitle("Likeable Recipe");
        testRecipe.setDescription("A recipe to like");
        testRecipe.setIngredients(List.of("Ingredient 1", "Ingredient 2"));
        testRecipe.setInstructions(List.of("Step 1", "Step 2"));
        testRecipe.setCookTime(30);
        testRecipe.setServings(4);
        testRecipe.setDifficulty(Recipe.Difficulty.EASY);
        testRecipe.setCategory(Recipe.Category.DESSERT);
        testRecipe.setAuthor(testUser);
        testRecipe = recipeRepository.save(testRecipe);
    }

    // ==================== LIKE RECIPE TESTS ====================

    @Test
    @DisplayName("Should like recipe with authentication")
    void shouldLikeRecipeWithAuth() throws Exception {
        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/like")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Recipe liked"));

        // Verify like exists in database
        boolean exists = likeRepository.existsByUserIdAndRecipeId(
                testUser.getId(), testRecipe.getId());
        assert exists;
    }

    @Test
    @DisplayName("Should reject like without authentication")
    void shouldRejectLikeWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/like"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 when liking non-existent recipe")
    void shouldReturn404WhenLikingNonExistentRecipe() throws Exception {
        mockMvc.perform(post("/api/recipes/99999/like")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should prevent duplicate likes")
    void shouldPreventDuplicateLikes() throws Exception {
        // First like - should succeed
        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/like")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        // Second like - should fail
        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/like")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", 
                    containsStringIgnoringCase("already liked")));
    }

    // ==================== UNLIKE RECIPE TESTS ====================

    @Test
    @DisplayName("Should unlike recipe")
    void shouldUnlikeRecipe() throws Exception {
        // First, like the recipe
        Like like = new Like();
        like.setUser(testUser);
        like.setRecipe(testRecipe);
        likeRepository.save(like);

        // Then unlike
        mockMvc.perform(delete("/api/recipes/" + testRecipe.getId() + "/like")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Recipe unliked"));

        // Verify like is removed
        boolean exists = likeRepository.existsByUserIdAndRecipeId(
                testUser.getId(), testRecipe.getId());
        assert !exists;
    }

    @Test
    @DisplayName("Should reject unlike without authentication")
    void shouldRejectUnlikeWithoutAuth() throws Exception {
        mockMvc.perform(delete("/api/recipes/" + testRecipe.getId() + "/like"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 when unliking non-existent recipe")
    void shouldReturn404WhenUnlikingNonExistentRecipe() throws Exception {
        mockMvc.perform(delete("/api/recipes/99999/like")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return error when unliking recipe that was not liked")
    void shouldReturnErrorWhenUnlikingNotLikedRecipe() throws Exception {
        mockMvc.perform(delete("/api/recipes/" + testRecipe.getId() + "/like")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", 
                    containsStringIgnoringCase("not liked")));
    }

    // ==================== CHECK LIKE STATUS TESTS ====================

    @Test
    @DisplayName("Should check if recipe is liked")
    void shouldCheckIfRecipeIsLiked() throws Exception {
        // First like the recipe
        Like like = new Like();
        like.setUser(testUser);
        like.setRecipe(testRecipe);
        likeRepository.save(like);

        mockMvc.perform(get("/api/recipes/" + testRecipe.getId() + "/like")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true));
    }

    @Test
    @DisplayName("Should return false when recipe is not liked")
    void shouldReturnFalseWhenRecipeNotLiked() throws Exception {
        mockMvc.perform(get("/api/recipes/" + testRecipe.getId() + "/like")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false));
    }

    @Test
    @DisplayName("Should reject check like status without authentication")
    void shouldRejectCheckLikeStatusWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/recipes/" + testRecipe.getId() + "/like"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== MULTIPLE USERS TESTS ====================

    @Test
    @DisplayName("Should allow multiple users to like same recipe")
    void shouldAllowMultipleUsersToLikeSameRecipe() throws Exception {
        // User 1 likes
        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/like")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        // Create User 2
        User user2 = new User();
        user2.setUsername("liker2");
        user2.setEmail("liker2@example.com");
        user2.setPassword(passwordEncoder.encode("password123"));
        user2 = userRepository.save(user2);

        String token2 = jwtUtil.generateToken(user2.getUsername(), user2.getId());

        // User 2 likes same recipe - should succeed
        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/like")
                .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());

        // Verify both likes exist
        assert likeRepository.existsByUserIdAndRecipeId(testUser.getId(), testRecipe.getId());
        assert likeRepository.existsByUserIdAndRecipeId(user2.getId(), testRecipe.getId());
    }

    @Test
    @DisplayName("Should allow user to like multiple recipes")
    void shouldAllowUserToLikeMultipleRecipes() throws Exception {
        // Create second recipe
        Recipe recipe2 = new Recipe();
        recipe2.setTitle("Second Recipe");
        recipe2.setDescription("Another likeable recipe");
        recipe2.setIngredients(List.of("Ingredient"));
        recipe2.setInstructions(List.of("Step"));
        recipe2.setCookTime(20);
        recipe2.setServings(2);
        recipe2.setDifficulty(Recipe.Difficulty.EASY);
        recipe2.setCategory(Recipe.Category.SNACK);
        recipe2.setAuthor(testUser);
        recipe2 = recipeRepository.save(recipe2);

        // Like first recipe
        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/like")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        // Like second recipe
        mockMvc.perform(post("/api/recipes/" + recipe2.getId() + "/like")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        // Verify both likes exist
        assert likeRepository.existsByUserIdAndRecipeId(testUser.getId(), testRecipe.getId());
        assert likeRepository.existsByUserIdAndRecipeId(testUser.getId(), recipe2.getId());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle like/unlike/like sequence")
    void shouldHandleLikeUnlikeLikeSequence() throws Exception {
        // Like
        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/like")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        // Unlike
        mockMvc.perform(delete("/api/recipes/" + testRecipe.getId() + "/like")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        // Like again - should succeed
        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/like")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        // Verify like exists
        boolean exists = likeRepository.existsByUserIdAndRecipeId(
                testUser.getId(), testRecipe.getId());
        assert exists;
    }

    @Test
    @DisplayName("Should handle concurrent likes gracefully")
    void shouldHandleConcurrentLikesGracefully() throws Exception {
        // This test checks if the unique constraint works
        // First like succeeds
        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/like")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        // Second attempt fails due to unique constraint
        mockMvc.perform(post("/api/recipes/" + testRecipe.getId() + "/like")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest());
    }
}
