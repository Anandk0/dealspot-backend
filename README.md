# DealSpot Backend API

Spring Boot backend for the Deal Spot rural marketplace platform.

## Tech Stack
- Java 17 + Spring Boot 3.3
- PostgreSQL
- JWT Authentication
- Cloudinary (image uploads)

## API Endpoints

### Auth
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login

### Listings
- `GET /api/listings/category/{category}` - Get listings by category
- `GET /api/listings/{id}` - Get listing details
- `GET /api/listings/recent` - Get recent listings
- `GET /api/listings/my` - Get my listings (auth required)
- `POST /api/listings` - Create listing (auth required, multipart)
- `PUT /api/listings/{id}` - Update listing (auth required)
- `DELETE /api/listings/{id}` - Delete listing (auth required)

### Search
- `GET /api/search?q=query&category=optional` - Search listings

### Favorites
- `GET /api/favorites` - Get favorites (auth required)
- `POST /api/favorites/{listingId}` - Add favorite (auth required)
- `DELETE /api/favorites/{listingId}` - Remove favorite (auth required)

### User
- `GET /api/users/me` - Get profile (auth required)
- `PUT /api/users/me` - Update profile (auth required)

### Health
- `GET /api/health` - Health check

## Environment Variables
```
DATABASE_URL=jdbc:postgresql://host:5432/dealspot
DB_USERNAME=postgres
DB_PASSWORD=password
JWT_SECRET=your-secret-key
CLOUDINARY_CLOUD_NAME=your-cloud
CLOUDINARY_API_KEY=your-key
CLOUDINARY_API_SECRET=your-secret
CORS_ORIGINS=https://your-frontend.com
PORT=8080
```
