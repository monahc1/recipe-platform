# FlavorShare - Recipe Sharing Platform

A full-stack recipe sharing platform that allows users to discover, create, share, and engage with culinary recipes. Built with Spring Boot and React.

## Overview

FlavorShare is a modern recipe sharing application where users can browse recipes, create their own culinary creations, leave reviews, and interact with other food enthusiasts. The platform features a clean, intuitive interface and robust backend API.

## Features

### Recipe Management
- Browse and search recipes by title or description
- Filter recipes by category (Main Course, Dessert, Healthy, Breakfast, etc.)
- View detailed recipe information including ingredients, instructions, and cooking times
- Create new recipes with images, ingredients, and step-by-step instructions
- Edit and delete your own recipes
- Upload custom recipe images

### User Interaction
- User authentication (login and signup)
- Like/unlike recipes
- Add comments and reviews to recipes
- View user profiles with activity statistics
- Track personal recipes, comments, and liked recipes

### User Interface
- Responsive design that works on desktop and mobile
- Real-time search and filtering
- Image galleries for recipes
- Profile pages with user statistics
- Clean, modern interface with smooth animations

## Tech Stack

### Backend
- Java 17+
- Spring Boot 3.x
- Spring Data JPA
- H2 Database (file-based persistence)
- Spring Web
- Spring Security
- Bean Validation

### Frontend
- React 18
- Vite
- Tailwind CSS
- Lucide React (icons)
- Fetch API for HTTP requests

## Prerequisites

- Java Development Kit (JDK) 17 or higher
- Node.js 16+ and npm
- Maven (for building the backend)

## Installation

### Backend Setup

1. Navigate to the backend directory
2. Build the project:
```bash
mvn clean install
```

3. The application uses H2 database with file-based persistence. The database file will be created automatically at `./data/flavorshare-db.mv.db`

### Frontend Setup

1. Navigate to the frontend directory
2. Install dependencies:
```bash
npm install
```

## Configuration

### Backend Configuration

The backend is configured in `application.properties`:

- Server Port: 8080
- Database: H2 (file-based at `./data/flavorshare-db`)
- H2 Console: Enabled at `/h2`
- CORS: Configured for `http://localhost:5173`

### Frontend Configuration

Create a `.env` file in the frontend directory (optional):
```
VITE_API_BASE=http://localhost:8080
```

If not specified, the frontend will default to `http://localhost:8080`.

## Running the Application

### Start the Backend
```bash
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

Access the H2 Console at: `http://localhost:8080/h2`
- JDBC URL: `jdbc:h2:file:./data/flavorshare-db`
- Username: `sa`
- Password: (leave empty)

### Start the Frontend
```bash
npm run dev
```

The frontend will start on `http://localhost:5173`

## Default Users

The application comes with three demo users:

1. Chef Sarah
   - Email: sarah@example.com
   - Password: SecurePass123!

2. Baker Mike
   - Email: mike@example.com
   - Password: BakeLife456@

3. Chef Giovanni
   - Email: giovanni@example.com
   - Password: PastaLover789#

## API Endpoints

### Recipes

- `GET /api/recipes` - Get all recipes
- `GET /api/recipes/{id}` - Get recipe by ID
- `POST /api/recipes` - Create new recipe
- `PUT /api/recipes/{id}` - Update recipe
- `DELETE /api/recipes/{id}` - Delete recipe

### Users

- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID

## Project Structure

### Backend Structure
```
src/main/java/com/flavorshare/
├── config/
│   ├── CorsConfig.java
│   └── SecurityConfig.java
├── model/
│   ├── Recipe.java
│   ├── User.java
│   ├── Review.java
│   └── Like.java
├── repo/
│   ├── RecipeRepository.java
│   ├── UserRepository.java
│   ├── ReviewRepository.java
│   └── LikeRepository.java
├── web/
│   ├── RecipeController.java
│   └── UserController.java
├── util/
│   └── DataLoader.java
└── RecipePlatformApplication.java
```

### Frontend Structure
```
src/
├── RecipePlatform.jsx (main component)
├── main.jsx (entry point)
└── index.css (styles)
```

## Data Models

### Recipe
- Title, description, image
- Ingredients list
- Instructions list
- Cook time, servings
- Difficulty level (Easy, Medium, Hard)
- Category (Main Course, Dessert, etc.)
- Author relationship

### User
- Username, email, password
- Full name, bio, profile image
- Created recipes
- Reviews and likes

### Review
- Rating (1-5)
- Comment text
- Associated recipe and user

### Like
- User and recipe relationship
- Unique constraint per user-recipe pair

## Features in Development

- Recipe rating system
- Advanced search with multiple filters
- Recipe collections/favorites
- Social features (follow users, feed)
- Recipe sharing on social media
- Nutritional information
- Recipe import from URLs

## Security Notes

This is a demonstration application. For production use:
- Implement proper authentication with JWT or OAuth
- Hash passwords using BCrypt
- Add CSRF protection
- Implement rate limiting
- Add input validation and sanitization
- Use HTTPS
- Implement proper authorization checks

## Contributing

Contributions are welcome. Please follow these steps:

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is available for educational and demonstration purposes.
