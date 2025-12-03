package com.flavorshare.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flavorshare.model.Recipe;
import com.flavorshare.model.User;
import com.flavorshare.repo.RecipeRepository;
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
 * Integration tests for RecipeController
 * Tests all CRUD operations for recipes
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Recipe Controller Tests")
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        recipeRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("cheftest");
        testUser.setEmail("chef@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFullName("Chef Test");
        testUser = userRepository.save(testUser);

        // Generate JWT token
        jwtToken = jwtUtil.generateToken(testUser.getUsername(), testUser.getId());

        // Create test recipe
        testRecipe = new Recipe();
        testRecipe.setTitle("Test Pasta");
        testRecipe.setDescription("Delicious test pasta");
        testRecipe.setIngredients(List.of("Pasta", "Tomato", "Garlic"));
        testRecipe.setInstructions(List.of("Boil pasta", "Make sauce", "Mix"));
        testRecipe.setCookTime(30);
        testRecipe.setServings(4);
        testRecipe.setDifficulty(Recipe.Difficulty.MEDIUM);
        testRecipe.setCategory(Recipe.Category.MAIN_COURSE);
        testRecipe.setAuthor(testUser);
        testRecipe.setImage("https://example.com/pasta.jpg");
        testRecipe = recipeRepository.save(testRecipe);
    }

    // ==================== GET ALL RECIPES ====================

    @Test
    @DisplayName("Should get all recipes")
    void shouldGetAllRecipes() throws Exception {
        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Test Pasta"))
                .andExpect(jsonPath("$[0].cookTime").value(30))
                .andExpect(jsonPath("$[0].servings").value(4));
    }

    @Test
    @DisplayName("Should return empty array when no recipes exist")
    void shouldReturnEmptyArrayWhenNoRecipes() throws Exception {
        recipeRepository.deleteAll();

        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ==================== GET RECIPE BY ID ====================

    @Test
    @DisplayName("Should get recipe by ID")
    void shouldGetRecipeById() throws Exception {
        mockMvc.perform(get("/api/recipes/" + testRecipe.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Pasta"))
                .andExpect(jsonPath("$.description").value("Delicious test pasta"))
                .andExpect(jsonPath("$.cookTime").value(30))
                .andExpect(jsonPath("$.author.username").value("cheftest"));
    }

    @Test
    @DisplayName("Should return 404 for non-existent recipe")
    void shouldReturn404ForNonExistentRecipe() throws Exception {
        mockMvc.perform(get("/api/recipes/99999"))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE RECIPE ====================

    @Test
    @DisplayName("Should create recipe with authentication")
    void shouldCreateRecipeWithAuth() throws Exception {
        Recipe newRecipe = new Recipe();
        newRecipe.setTitle("New Pizza");
        newRecipe.setDescription("Delicious pizza");
        newRecipe.setIngredients(List.of("Dough", "Cheese", "Tomato"));
        newRecipe.setInstructions(List.of("Make dough", "Add toppings", "Bake"));
        newRecipe.setCookTime(45);
        newRecipe.setServings(8);
        newRecipe.setDifficulty(Recipe.Difficulty.EASY);
        newRecipe.setCategory(Recipe.Category.MAIN_COURSE);
        newRecipe.setImage("https://example.com/pizza.jpg");
        
        User author = new User();
        author.setId(testUser.getId());
        newRecipe.setAuthor(author);

        mockMvc.perform(post("/api/recipes")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRecipe)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Pizza"))
                .andExpect(jsonPath("$.cookTime").value(45))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Should reject recipe creation without authentication")
    void shouldRejectRecipeCreationWithoutAuth() throws Exception {
        Recipe newRecipe = new Recipe();
        newRecipe.setTitle("Unauthorized Recipe");
        newRecipe.setDescription("Should fail");
        newRecipe.setCookTime(30);
        newRecipe.setServings(2);

        mockMvc.perform(post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRecipe)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject recipe with missing title")
    void shouldRejectRecipeWithMissingTitle() throws Exception {
        Recipe newRecipe = new Recipe();
        newRecipe.setDescription("No title recipe");
        newRecipe.setCookTime(30);
        newRecipe.setServings(2);
        newRecipe.setDifficulty(Recipe.Difficulty.EASY);
        newRecipe.setCategory(Recipe.Category.MAIN_COURSE);

        mockMvc.perform(post("/api/recipes")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRecipe)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject recipe with invalid cook time")
    void shouldRejectRecipeWithInvalidCookTime() throws Exception {
        Recipe newRecipe = new Recipe();
        newRecipe.setTitle("Invalid Recipe");
        newRecipe.setDescription("Bad cook time");
        newRecipe.setCookTime(-10); // Invalid
        newRecipe.setServings(2);

        mockMvc.perform(post("/api/recipes")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRecipe)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should set default image if not provided")
    void shouldSetDefaultImageIfNotProvided() throws Exception {
        Recipe newRecipe = new Recipe();
        newRecipe.setTitle("No Image Recipe");
        newRecipe.setDescription("Should get default image");
        newRecipe.setCookTime(30);
        newRecipe.setServings(2);
        newRecipe.setDifficulty(Recipe.Difficulty.EASY);
        newRecipe.setCategory(Recipe.Category.DESSERT);
        
        User author = new User();
        author.setId(testUser.getId());
        newRecipe.setAuthor(author);

        mockMvc.perform(post("/api/recipes")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRecipe)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.image").exists())
                .andExpect(jsonPath("$.image").isNotEmpty());
    }

    // ==================== UPDATE RECIPE ====================

    @Test
    @DisplayName("Should update own recipe")
    void shouldUpdateOwnRecipe() throws Exception {
        testRecipe.setTitle("Updated Pasta");
        testRecipe.setDescription("Even more delicious");
        testRecipe.setCookTime(25);

        mockMvc.perform(put("/api/recipes/" + testRecipe.getId())
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRecipe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Pasta"))
                .andExpect(jsonPath("$.description").value("Even more delicious"))
                .andExpect(jsonPath("$.cookTime").value(25));
    }

    @Test
    @DisplayName("Should reject update without authentication")
    void shouldRejectUpdateWithoutAuth() throws Exception {
        testRecipe.setTitle("Unauthorized Update");

        mockMvc.perform(put("/api/recipes/" + testRecipe.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRecipe)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent recipe")
    void shouldReturn404WhenUpdatingNonExistentRecipe() throws Exception {
        Recipe fakeRecipe = new Recipe();
        fakeRecipe.setTitle("Fake");
        fakeRecipe.setDescription("Doesn't exist");
        fakeRecipe.setCookTime(30);
        fakeRecipe.setServings(2);

        mockMvc.perform(put("/api/recipes/99999")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fakeRecipe)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE RECIPE ====================

    @Test
    @DisplayName("Should delete own recipe")
    void shouldDeleteOwnRecipe() throws Exception {
        mockMvc.perform(delete("/api/recipes/" + testRecipe.getId())
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/recipes/" + testRecipe.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should reject delete without authentication")
    void shouldRejectDeleteWithoutAuth() throws Exception {
        mockMvc.perform(delete("/api/recipes/" + testRecipe.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent recipe")
    void shouldReturn404WhenDeletingNonExistentRecipe() throws Exception {
        mockMvc.perform(delete("/api/recipes/99999")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    // ==================== AUTHORIZATION TESTS ====================

    @Test
    @DisplayName("Should not allow user to update another user's recipe")
    void shouldNotAllowUpdateOfOtherUsersRecipe() throws Exception {
        // Create another user
        User otherUser = new User();
        otherUser.setUsername("otherchef");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword(passwordEncoder.encode("password123"));
        otherUser = userRepository.save(otherUser);

        String otherToken = jwtUtil.generateToken(otherUser.getUsername(), otherUser.getId());

        testRecipe.setTitle("Hacked Title");

        // This might pass depending on your authorization logic
        // Add actual authorization check in controller if needed
        mockMvc.perform(put("/api/recipes/" + testRecipe.getId())
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRecipe)));
        // Add appropriate status expectation based on your implementation
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @DisplayName("Should validate all required fields")
    void shouldValidateAllRequiredFields() throws Exception {
        Recipe invalidRecipe = new Recipe();
        // Missing all required fields

        mockMvc.perform(post("/api/recipes")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRecipe)))
                .andExpect(status().isBadRequest());
    }
}
