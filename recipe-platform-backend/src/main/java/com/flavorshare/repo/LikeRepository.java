package com.flavorshare.repo;

import com.flavorshare.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByUserIdAndRecipeId(Long userId, Long recipeId);
    Optional<Like> findByUserIdAndRecipeId(Long userId, Long recipeId);
}