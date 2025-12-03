#!/bin/bash

# FlavorShare - Phase 1: Core Fixes
# This script applies database persistence fixes, update endpoint fix, and H2 console access
# Run this from your project ROOT directory (where pom.xml is located)

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo ""
echo "================================================"
echo "  FlavorShare - Phase 1: Core Fixes"
echo "================================================"
echo ""

# Function to print colored messages
print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    print_error "pom.xml not found!"
    echo ""
    echo "Please run this script from your Spring Boot project root directory."
    echo "Example: cd /c/Users/YourName/recipe-platform-backend"
    echo "         ./apply-phase1-fixes.sh"
    exit 1
fi

print_success "Found pom.xml - correct directory"

# Check if backup directory exists, if not create it
BACKUP_DIR="backup-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"
print_info "Created backup directory: $BACKUP_DIR"

echo ""
echo "Step 1: Backing up files..."
echo "----------------------------"

# Backup RecipeController.java
if [ -f "src/main/java/com/flavorshare/web/RecipeController.java" ]; then
    cp src/main/java/com/flavorshare/web/RecipeController.java "$BACKUP_DIR/"
    print_success "Backed up RecipeController.java"
else
    print_warning "RecipeController.java not found at expected location"
fi

# Backup SecurityConfig.java
if [ -f "src/main/java/com/flavorshare/config/SecurityConfig.java" ]; then
    cp src/main/java/com/flavorshare/config/SecurityConfig.java "$BACKUP_DIR/"
    print_success "Backed up SecurityConfig.java"
else
    print_warning "SecurityConfig.java not found at expected location"
fi

echo ""
echo "Step 2: Updating Backend Files..."
echo "-----------------------------------"

# Update RecipeController.java
cat > src/main/java/com/flavorshare/web/RecipeController.java << 'EOF'
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
      e.printStackTrace();
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

      Recipe savedRecipe = recipeRepo.save(existingRecipe);
      return ResponseEntity.ok(savedRecipe);
    } catch (Exception e) {
      e.printStackTrace();
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
EOF

print_success "Updated RecipeController.java"

# Update SecurityConfig.java
cat > src/main/java/com/flavorshare/config/SecurityConfig.java << 'EOF'
package com.flavorshare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .cors(Customizer.withDefaults())
      .csrf(csrf -> csrf.disable())
      .headers(h -> h.frameOptions(f -> f.disable())) // allow H2 console in iframe
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/", "/api/**", "/h2-console/**").permitAll()
        .anyRequest().permitAll()
      );
    return http.build();
  }
}
EOF

print_success "Updated SecurityConfig.java"

echo ""
echo "Step 3: Backend Update Complete!"
echo "----------------------------------"
print_info "Backend files have been updated."
print_warning "You MUST restart Spring Boot for changes to take effect."

echo ""
echo "Step 4: Frontend Changes Required"
echo "-----------------------------------"
print_warning "The frontend requires MANUAL changes (cannot be automated safely)"
echo ""
echo "You need to edit: src/RecipePlatform.jsx"
echo ""
echo "Make these 5 changes:"
echo "1. Remove localStorage caching in useEffect (line ~141)"
echo "2. Remove localStorage from toggleLike (line ~170)"
echo "3. Replace localStorage with fetchRecipes() in handleCreateRecipe (line ~210)"
echo "4. Replace localStorage with fetchRecipes() in handleUpdateRecipe (line ~300)"
echo "5. Replace localStorage with fetchRecipes() in handleDeleteRecipe (line ~340)"
echo ""
print_info "See EXACT_CODE_CHANGES.md for detailed instructions"

echo ""
echo "================================================"
echo "  Phase 1 Backend Update: COMPLETE ✓"
echo "================================================"
echo ""
echo "NEXT STEPS:"
echo "-----------"
echo "1. Restart Spring Boot:"
echo "   $ mvn spring-boot:run"
echo ""
echo "2. Edit frontend React code:"
echo "   Follow EXACT_CODE_CHANGES.md"
echo ""
echo "3. Restart React dev server:"
echo "   $ npm run dev"
echo ""
echo "4. Test using IMPLEMENTATION_CHECKLIST.md"
echo ""
echo "5. Access H2 console:"
echo "   URL: http://localhost:8080/h2-console"
echo "   JDBC URL: jdbc:h2:file:./data/flavorshare-db"
echo "   Username: sa"
echo "   Password: (empty)"
echo ""
echo "Backup saved in: $BACKUP_DIR/"
echo ""
print_success "Phase 1 backend updates applied successfully!"
echo ""
