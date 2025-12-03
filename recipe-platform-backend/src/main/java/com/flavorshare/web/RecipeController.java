package com.flavorshare.web;

import com.flavorshare.model.Recipe;
import com.flavorshare.model.User;
import com.flavorshare.repo.RecipeRepository;
import com.flavorshare.repo.UserRepository;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/recipes")
@CrossOrigin(origins = "http://localhost:5173")
public class RecipeController {
  private final RecipeRepository recipeRepo;
  private final UserRepository userRepo;
  
  public RecipeController(RecipeRepository recipeRepo, UserRepository userRepo) { 
    this.recipeRepo = recipeRepo; 
    this.userRepo = userRepo;
  }

  @GetMapping
  public List<Recipe> all() { 
    return recipeRepo.findAll(); 
  }

  @GetMapping("/{id}")
  public ResponseEntity<Recipe> one(@PathVariable Long id) {
    return recipeRepo.findById(id)
      .map(ResponseEntity::ok)
      .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody Recipe recipe) {
    try {
      // Validate required fields
      if (recipe.getTitle() == null || recipe.getTitle().trim().isEmpty()) {
        return ResponseEntity.badRequest().body("Title is required");
      }
      if (recipe.getDescription() == null || recipe.getDescription().trim().isEmpty()) {
        return ResponseEntity.badRequest().body("Description is required");
      }
      if (recipe.getCookTime() == null || recipe.getCookTime() <= 0) {
        return ResponseEntity.badRequest().body("Cook time must be positive");
      }
      if (recipe.getServings() == null || recipe.getServings() <= 0) {
        return ResponseEntity.badRequest().body("Servings must be positive");
      }

      // Set author if provided
      if (recipe.getAuthor() != null && recipe.getAuthor().getId() != null) {
        Optional<User> author = userRepo.findById(recipe.getAuthor().getId());
        if (author.isPresent()) {
          recipe.setAuthor(author.get());
        } else {
          return ResponseEntity.badRequest().body("Invalid author ID");
        }
      }

      // Set default image if none provided
      if (recipe.getImage() == null || recipe.getImage().trim().isEmpty()) {
        recipe.setImage("https://images.unsplash.com/photo-1546554137-f86b9593a222?w=800&q=80&auto=format&fit=crop");
      }

      // Ensure lists are not null
      if (recipe.getIngredients() == null) {
        recipe.setIngredients(List.of());
      }
      if (recipe.getInstructions() == null) {
        recipe.setInstructions(List.of());
      }

      Recipe savedRecipe = recipeRepo.save(recipe);
      return ResponseEntity.status(HttpStatus.CREATED).body(savedRecipe);
    } catch (Exception e) {
      e.printStackTrace(); // Log the full error
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("Error creating recipe: " + e.getMessage());
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Recipe updatedRecipe) {
    try {
      // Find existing recipe
      Optional<Recipe> existingRecipeOpt = recipeRepo.findById(id);
      if (existingRecipeOpt.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      Recipe existingRecipe = existingRecipeOpt.get();

      // ===== VALIDATION =====
      if (updatedRecipe.getTitle() == null || updatedRecipe.getTitle().trim().isEmpty()) {
        return ResponseEntity.badRequest().body("Title is required");
      }
      if (updatedRecipe.getDescription() == null || updatedRecipe.getDescription().trim().isEmpty()) {
        return ResponseEntity.badRequest().body("Description is required");
      }
      if (updatedRecipe.getCookTime() == null || updatedRecipe.getCookTime() <= 0) {
        return ResponseEntity.badRequest().body("Cook time must be positive");
      }
      if (updatedRecipe.getServings() == null || updatedRecipe.getServings() <= 0) {
        return ResponseEntity.badRequest().body("Servings must be positive");
      }

      // ===== UPDATE FIELDS =====
      existingRecipe.setTitle(updatedRecipe.getTitle().trim());
      existingRecipe.setDescription(updatedRecipe.getDescription().trim());

      // Ensure lists are never null
      existingRecipe.setIngredients(
        updatedRecipe.getIngredients() != null ? updatedRecipe.getIngredients() : List.of()
      );
      existingRecipe.setInstructions(
        updatedRecipe.getInstructions() != null ? updatedRecipe.getInstructions() : List.of()
      );

      existingRecipe.setCookTime(updatedRecipe.getCookTime());
      existingRecipe.setServings(updatedRecipe.getServings());

      // Handle enums safely - they might be null
      if (updatedRecipe.getDifficulty() != null) {
        existingRecipe.setDifficulty(updatedRecipe.getDifficulty());
      }
      if (updatedRecipe.getCategory() != null) {
        existingRecipe.setCategory(updatedRecipe.getCategory());
      }

      // Update image if provided
      if (updatedRecipe.getImage() != null && !updatedRecipe.getImage().trim().isEmpty()) {
        existingRecipe.setImage(updatedRecipe.getImage().trim());
      }

      // Don't change author on update - keep original
      // (Frontend shouldn't send author in update anyway)

      Recipe savedRecipe = recipeRepo.save(existingRecipe);
      return ResponseEntity.ok(savedRecipe);
    } catch (Exception e) {
      e.printStackTrace(); // Log full error
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("Error updating recipe: " + e.getMessage());
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable Long id) {
    try {
      if (!recipeRepo.existsById(id)) {
        return ResponseEntity.notFound().build();
      }
      recipeRepo.deleteById(id);
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("Error deleting recipe: " + e.getMessage());
    }
  }
}