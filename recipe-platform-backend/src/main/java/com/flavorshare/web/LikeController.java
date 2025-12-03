package com.flavorshare.web;

import com.flavorshare.model.Like;
import com.flavorshare.model.Recipe;
import com.flavorshare.model.User;
import com.flavorshare.repo.LikeRepository;
import com.flavorshare.repo.RecipeRepository;
import com.flavorshare.repo.UserRepository;
import com.flavorshare.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/recipes/{recipeId}/like")
@CrossOrigin(origins = "http://localhost:5173")
public class LikeController {

    private final LikeRepository likeRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public LikeController(LikeRepository likeRepository, RecipeRepository recipeRepository, 
                         UserRepository userRepository, JwtUtil jwtUtil) {
        this.likeRepository = likeRepository;
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<?> likeRecipe(
            @PathVariable Long recipeId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Extract user from token
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);

            // Check if user exists
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            // Check if recipe exists
            Optional<Recipe> recipeOpt = recipeRepository.findById(recipeId);
            if (recipeOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            Recipe recipe = recipeOpt.get();

            // Check if already liked
            if (likeRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
                return ResponseEntity.badRequest().body("Recipe already liked");
            }

            // Create like
            Like like = new Like();
            like.setUser(user);
            like.setRecipe(recipe);
            likeRepository.save(like);

            return ResponseEntity.ok().body("Recipe liked successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error liking recipe: " + e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> unlikeRecipe(
            @PathVariable Long recipeId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Extract user from token
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);

            // Find the like
            Optional<Like> likeOpt = likeRepository.findByUserIdAndRecipeId(userId, recipeId);
            if (likeOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Recipe not liked");
            }

            // Delete like
            likeRepository.delete(likeOpt.get());

            return ResponseEntity.ok().body("Recipe unliked successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error unliking recipe: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> checkIfLiked(
            @PathVariable Long recipeId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Extract user from token
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);

            boolean liked = likeRepository.existsByUserIdAndRecipeId(userId, recipeId);

            return ResponseEntity.ok().body(liked);

        } catch (Exception e) {
            return ResponseEntity.ok().body(false);
        }
    }
}
