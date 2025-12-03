package com.flavorshare.web;

import com.flavorshare.dto.ReviewRequest;
import com.flavorshare.model.Recipe;
import com.flavorshare.model.Review;
import com.flavorshare.model.User;
import com.flavorshare.repo.RecipeRepository;
import com.flavorshare.repo.ReviewRepository;
import com.flavorshare.repo.UserRepository;
import com.flavorshare.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/recipes/{recipeId}/reviews")
@CrossOrigin(origins = "http://localhost:5173")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public ReviewController(ReviewRepository reviewRepository, RecipeRepository recipeRepository,
                           UserRepository userRepository, JwtUtil jwtUtil) {
        this.reviewRepository = reviewRepository;
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<?> getReviews(@PathVariable Long recipeId) {
        try {
            Optional<Recipe> recipeOpt = recipeRepository.findById(recipeId);
            if (recipeOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Recipe recipe = recipeOpt.get();
            List<Review> reviews = recipe.getReviews();

            return ResponseEntity.ok(reviews);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching reviews: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> addReview(
            @PathVariable Long recipeId,
            @Valid @RequestBody ReviewRequest request,
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

            // Create review
            Review review = new Review();
            review.setRating(request.getRating());
            review.setComment(request.getComment());
            review.setUser(user);
            review.setRecipe(recipe);

            review = reviewRepository.save(review);

            return ResponseEntity.status(HttpStatus.CREATED).body(review);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error adding review: " + e.getMessage());
        }
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long recipeId,
            @PathVariable Long reviewId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Extract user from token
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);

            // Find review
            Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
            if (reviewOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Review review = reviewOpt.get();

            // Check if user owns this review
            if (!review.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only delete your own reviews");
            }

            reviewRepository.delete(review);

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error deleting review: " + e.getMessage());
        }
    }
}
