package com.flavorshare.util;

import com.flavorshare.model.Recipe;
import com.flavorshare.model.User;
import com.flavorshare.repo.RecipeRepository;
import com.flavorshare.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {
  private final UserRepository userRepository;
  private final RecipeRepository recipeRepository;
  private final PasswordEncoder passwordEncoder;

  public DataLoader(UserRepository userRepository, RecipeRepository recipeRepository, 
                   PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.recipeRepository = recipeRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(String... args) {
    System.out.println("==============================================");
    System.out.println("DataLoader starting...");
    System.out.println("==============================================");
    
    try {
      long userCount = userRepository.count();
      long recipeCount = recipeRepository.count();
      
      System.out.println("Current user count: " + userCount);
      System.out.println("Current recipe count: " + recipeCount);
      
      if (userCount > 0) {
        System.out.println("Users already exist, skipping user initialization");
        System.out.println("==============================================");
        return;
      }

      System.out.println("Loading sample users with hashed passwords...");

      // Create users with HASHED passwords
      User sarah = new User("chef_sarah", "sarah@example.com", passwordEncoder.encode("password123"));
      sarah.setFullName("Chef Sarah");
      sarah = userRepository.save(sarah);
      System.out.println("✓ Created user: " + sarah.getUsername() + " (ID: " + sarah.getId() + ")");

      User mike = new User("baker_mike", "mike@example.com", passwordEncoder.encode("password123"));
      mike.setFullName("Baker Mike");
      mike = userRepository.save(mike);
      System.out.println("✓ Created user: " + mike.getUsername() + " (ID: " + mike.getId() + ")");

      User giovanni = new User("chef_giovanni", "giovanni@example.com", passwordEncoder.encode("password123"));
      giovanni.setFullName("Chef Giovanni");
      giovanni = userRepository.save(giovanni);
      System.out.println("✓ Created user: " + giovanni.getUsername() + " (ID: " + giovanni.getId() + ")");

      // Only create recipes if they don't exist
      if (recipeCount == 0) {
        System.out.println("Creating sample recipes...");
        
        Recipe recipe1 = new Recipe();
        recipe1.setTitle("Rainbow Veggie Bowl");
        recipe1.setDescription("A colorful and nutritious bowl packed with fresh vegetables, quinoa, and a tangy tahini dressing.");
        recipe1.setAuthor(sarah);
        recipe1.setCookTime(25);
        recipe1.setServings(2);
        recipe1.setDifficulty(Recipe.Difficulty.EASY);
        recipe1.setCategory(Recipe.Category.HEALTHY);
        recipe1.setIngredients(List.of(
          "Mixed greens", "Cherry tomatoes", "Avocado", "Cooked quinoa", 
          "Chickpeas", "Red cabbage", "Tahini dressing"
        ));
        recipe1.setInstructions(List.of(
          "Cook quinoa according to package directions",
          "Prep all vegetables and wash greens",
          "Whisk tahini dressing ingredients together", 
          "Assemble bowl and drizzle with dressing"
        ));
        recipe1.setImage("https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=800&q=80&auto=format&fit=crop");
        recipeRepository.save(recipe1);
        System.out.println("✓ Created recipe: " + recipe1.getTitle());

        Recipe recipe2 = new Recipe();
        recipe2.setTitle("Chocolate Chip Cookies");
        recipe2.setDescription("Soft and chewy cookies with crispy edges, loaded with chocolate chips.");
        recipe2.setAuthor(mike);
        recipe2.setCookTime(15);
        recipe2.setServings(24);
        recipe2.setDifficulty(Recipe.Difficulty.EASY);
        recipe2.setCategory(Recipe.Category.DESSERT);
        recipe2.setIngredients(List.of(
          "All-purpose flour", "Baking soda", "Salt", "Butter", "White sugar",
          "Brown sugar", "Eggs", "Vanilla extract", "Chocolate chips"
        ));
        recipe2.setInstructions(List.of(
          "Preheat oven to 375°F",
          "Cream butter and sugars until fluffy",
          "Add eggs and vanilla, mix well",
          "Gradually fold in dry ingredients",
          "Stir in chocolate chips", 
          "Bake 9-11 minutes until golden"
        ));
        recipe2.setImage("https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=800&q=80&auto=format&fit=crop");
        recipeRepository.save(recipe2);
        System.out.println("✓ Created recipe: " + recipe2.getTitle());

        Recipe recipe3 = new Recipe();
        recipe3.setTitle("Creamy Pasta Carbonara");
        recipe3.setDescription("Classic Italian pasta with silky eggs, Parmesan, and crispy pancetta.");
        recipe3.setAuthor(giovanni);
        recipe3.setCookTime(20);
        recipe3.setServings(4);
        recipe3.setDifficulty(Recipe.Difficulty.MEDIUM);
        recipe3.setCategory(Recipe.Category.MAIN_COURSE);
        recipe3.setIngredients(List.of(
          "Spaghetti", "Pancetta", "Large eggs", "Parmesan cheese", 
          "Garlic", "Black pepper", "Olive oil", "Salt"
        ));
        recipe3.setInstructions(List.of(
          "Boil pasta in salted water until al dente",
          "Crisp pancetta in large pan",
          "Whisk eggs with grated Parmesan",
          "Toss hot pasta with pancetta off heat",
          "Add egg mixture and pasta water, stirring quickly"
        ));
        recipe3.setImage("https://images.unsplash.com/photo-1621996346565-e3dbc353d2e5?w=800&q=80&auto=format&fit=crop");
        recipeRepository.save(recipe3);
        System.out.println("✓ Created recipe: " + recipe3.getTitle());
      }
      
      System.out.println("==============================================");
      System.out.println("Sample data loaded successfully!");
      System.out.println("Total users: " + userRepository.count());
      System.out.println("Total recipes: " + recipeRepository.count());
      System.out.println("==============================================");
      System.out.println("TEST CREDENTIALS:");
      System.out.println("Username: chef_sarah | Password: password123");
      System.out.println("Username: baker_mike | Password: password123");
      System.out.println("Username: chef_giovanni | Password: password123");
      System.out.println("==============================================");
      
    } catch (Exception e) {
      System.err.println("==============================================");
      System.err.println("ERROR in DataLoader:");
      System.err.println("==============================================");
      e.printStackTrace();
      System.err.println("==============================================");
    }
  }
}