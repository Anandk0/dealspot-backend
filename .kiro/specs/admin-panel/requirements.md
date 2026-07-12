# Requirements Document

## Introduction

This document specifies the requirements for an Admin Panel and Role-Based Authorization system for Deal Spot (ಡೀಲ್ ಸ್ಪಾಟ್), a rural marketplace platform for Karnataka villages. The system introduces a hierarchical role model (SUPER_ADMIN, ADMIN, CHECKER, USER) with a dedicated admin interface for platform management, listing moderation, user management, revenue analytics, platform ads, and configurable settings. The admin panel is accessible via a protected `/admin/*` route in the frontend.

## Glossary

- **Authorization_Service**: The backend component responsible for evaluating role-based access control decisions for API endpoints
- **Admin_Panel**: The frontend interface accessible at `/admin/*` routes, providing management capabilities to authorized roles
- **Dashboard_Service**: The backend component that aggregates and serves platform metrics and analytics data
- **Moderation_Service**: The backend component responsible for listing review workflows including approval, rejection, and flagging
- **User_Management_Service**: The backend component responsible for managing user accounts, roles, and account status
- **Revenue_Service**: The backend component responsible for aggregating payment data and generating revenue reports
- **Ads_Service**: The backend component responsible for managing platform-posted listings and featured/promoted content
- **Settings_Service**: The backend component responsible for managing configurable platform parameters
- **SUPER_ADMIN**: A role with full platform control including revenue management, settings configuration, user management, and role assignment
- **ADMIN**: A role that can moderate listings, view revenue data, ban users, and post platform ads
- **CHECKER**: A role that can review pending listings (approve/reject) and flag suspicious content
- **USER**: The default marketplace user role with no administrative privileges
- **Listing_Status**: The state of a listing, one of: PENDING, ACTIVE, REJECTED, FLAGGED, REMOVED, SOLD, EXPIRED
- **User_Status**: The state of a user account, one of: ACTIVE, SUSPENDED, BANNED

## Requirements

### Requirement 1: Role Hierarchy and Assignment

**User Story:** As a platform operator, I want a hierarchical role system enforced across the application, so that each user has precisely scoped access to platform capabilities.

#### Acceptance Criteria

1. THE Authorization_Service SHALL enforce exactly four roles in the following hierarchy: SUPER_ADMIN > ADMIN > CHECKER > USER
2. WHEN a new user registers, THE Authorization_Service SHALL assign the USER role by default
3. WHEN a SUPER_ADMIN assigns a role to a user, THE Authorization_Service SHALL permit assignment of ADMIN, CHECKER, or USER roles
4. WHEN an ADMIN assigns a role to a user, THE Authorization_Service SHALL permit assignment of CHECKER or USER roles only
5. IF a user with CHECKER or USER role attempts to assign roles, THEN THE Authorization_Service SHALL reject the request with a 403 Forbidden response
6. THE Authorization_Service SHALL store the role as a single value on the User entity

### Requirement 2: API Endpoint Authorization

**User Story:** As a platform operator, I want all admin API endpoints protected by role checks, so that unauthorized users cannot access administrative functions.

#### Acceptance Criteria

1. WHEN an unauthenticated request is made to any `/api/admin/**` endpoint, THE Authorization_Service SHALL return a 401 Unauthorized response
2. WHEN an authenticated USER makes a request to any `/api/admin/**` endpoint, THE Authorization_Service SHALL return a 403 Forbidden response
3. WHEN a CHECKER makes a request to endpoints outside `/api/admin/moderation/**`, THE Authorization_Service SHALL return a 403 Forbidden response
4. WHEN an ADMIN or SUPER_ADMIN makes a request to any `/api/admin/**` endpoint, THE Authorization_Service SHALL permit access
5. WHEN a CHECKER makes a request to `/api/admin/moderation/**` endpoints, THE Authorization_Service SHALL permit access
6. THE Authorization_Service SHALL evaluate role permissions on every request using the role stored in the authenticated user's JWT claims

### Requirement 3: Admin Dashboard Metrics

**User Story:** As an administrator, I want a dashboard showing key platform metrics at a glance, so that I can monitor platform health and business performance.

#### Acceptance Criteria

1. WHEN an ADMIN or SUPER_ADMIN requests dashboard data, THE Dashboard_Service SHALL return revenue totals for today, this week, this month, this year, and all-time
2. WHEN an ADMIN or SUPER_ADMIN requests dashboard data, THE Dashboard_Service SHALL return the total registered user count and the count of users with ACTIVE status
3. WHEN an ADMIN or SUPER_ADMIN requests dashboard data, THE Dashboard_Service SHALL return counts of listings grouped by Listing_Status (ACTIVE, PENDING, REJECTED, FLAGGED)
4. WHEN an ADMIN or SUPER_ADMIN requests dashboard data, THE Dashboard_Service SHALL return the total number of completed contact unlock transactions
5. WHEN an ADMIN or SUPER_ADMIN requests dashboard data, THE Dashboard_Service SHALL return listing counts grouped by category
6. WHEN an ADMIN or SUPER_ADMIN requests dashboard data, THE Dashboard_Service SHALL return the total cumulative page view count across all listings

### Requirement 4: Listing Moderation Workflow

**User Story:** As a checker, I want to review new listings before they become visible to buyers, so that the platform maintains quality and trust.

#### Acceptance Criteria

1. WHEN a user creates a new listing, THE Moderation_Service SHALL set the listing status to PENDING
2. WHILE a listing has PENDING status, THE Admin_Panel SHALL NOT display the listing in public search results or category pages
3. WHEN a CHECKER or ADMIN approves a pending listing, THE Moderation_Service SHALL change the listing status to ACTIVE
4. WHEN a CHECKER or ADMIN rejects a pending listing, THE Moderation_Service SHALL change the listing status to REJECTED and store the rejection reason
5. WHEN a CHECKER or ADMIN flags an active listing, THE Moderation_Service SHALL change the listing status to FLAGGED and store the flag reason
6. WHEN an ADMIN or SUPER_ADMIN removes a flagged listing, THE Moderation_Service SHALL change the listing status to REMOVED
7. WHEN a CHECKER requests the moderation queue, THE Moderation_Service SHALL return all listings with PENDING status ordered by creation date ascending (oldest first)
8. WHEN a CHECKER requests the moderation queue, THE Moderation_Service SHALL support pagination with a configurable page size

### Requirement 5: User Management

**User Story:** As an administrator, I want to view and manage user accounts, so that I can enforce platform policies and manage the team.

#### Acceptance Criteria

1. WHEN an ADMIN or SUPER_ADMIN requests the user list, THE User_Management_Service SHALL return paginated user records
2. WHEN an ADMIN or SUPER_ADMIN filters the user list by role, status, or district, THE User_Management_Service SHALL return only matching user records
3. WHEN an ADMIN or SUPER_ADMIN suspends a user, THE User_Management_Service SHALL set the user's status to SUSPENDED and prevent the user from creating new listings or making purchases
4. WHEN an ADMIN or SUPER_ADMIN bans a user, THE User_Management_Service SHALL set the user's status to BANNED and prevent the user from logging in
5. WHEN an ADMIN or SUPER_ADMIN views a specific user's profile, THE User_Management_Service SHALL return the user's listing history and payment transaction history
6. IF a SUSPENDED or BANNED user attempts to create a listing, THEN THE Authorization_Service SHALL reject the request with a 403 Forbidden response and a message indicating the account restriction

### Requirement 6: Revenue and Transaction Analytics

**User Story:** As a SUPER_ADMIN, I want detailed revenue reports and transaction history, so that I can track business performance and identify payment issues.

#### Acceptance Criteria

1. WHEN a SUPER_ADMIN or ADMIN requests transaction history, THE Revenue_Service SHALL return paginated payment order records including Razorpay order ID, amount, status, user, listing, and timestamp
2. WHEN a SUPER_ADMIN or ADMIN requests revenue reports, THE Revenue_Service SHALL return revenue totals grouped by day, week, or month based on the requested granularity
3. WHEN a SUPER_ADMIN or ADMIN requests failed payment data, THE Revenue_Service SHALL return all payment orders with FAILED status
4. WHEN a SUPER_ADMIN requests a revenue export, THE Revenue_Service SHALL generate a CSV file containing all payment order records within the specified date range
5. THE Revenue_Service SHALL calculate revenue using only payment orders with PAID status

### Requirement 7: Platform Ads and Featured Listings

**User Story:** As an administrator, I want to post listings on behalf of the platform and manage featured content, so that I can promote platform services and highlight quality listings.

#### Acceptance Criteria

1. WHEN an ADMIN or SUPER_ADMIN creates a platform ad listing, THE Ads_Service SHALL create a listing with a platform-owned flag and bypass the PENDING moderation status
2. WHEN an ADMIN or SUPER_ADMIN marks a listing as featured, THE Ads_Service SHALL set the featured flag and the featured expiry timestamp on the listing
3. WHEN the featured expiry timestamp is reached, THE Ads_Service SHALL remove the featured flag from the listing
4. WHEN the Admin_Panel displays featured listings management, THE Ads_Service SHALL return all currently featured listings with their expiry dates
5. THE Admin_Panel SHALL display platform ad listings with a distinct visual indicator in search results and category pages

### Requirement 8: Platform Settings Management

**User Story:** As a SUPER_ADMIN, I want to configure platform parameters without code changes, so that I can adjust pricing, categories, and promotional content dynamically.

#### Acceptance Criteria

1. WHEN a SUPER_ADMIN updates the contact unlock price, THE Settings_Service SHALL persist the new price and all subsequent payment orders SHALL use the updated amount
2. WHEN a SUPER_ADMIN adds a new category, THE Settings_Service SHALL make the category available for listing creation immediately
3. WHEN a SUPER_ADMIN disables a category, THE Settings_Service SHALL prevent new listings in that category while preserving existing listings
4. WHEN a SUPER_ADMIN edits a category name or icon, THE Settings_Service SHALL update the category metadata without affecting existing listings in that category
5. WHEN a SUPER_ADMIN manages home page banners, THE Settings_Service SHALL support adding, reordering, and removing banner entries with an image URL and optional link target
6. IF an ADMIN or CHECKER attempts to modify platform settings, THEN THE Authorization_Service SHALL reject the request with a 403 Forbidden response

### Requirement 9: Admin Panel Frontend Access Control

**User Story:** As a platform operator, I want the admin panel frontend to be accessible only to authorized roles, so that regular users cannot access or view administrative interfaces.

#### Acceptance Criteria

1. WHEN a user with USER role navigates to any `/admin/*` route, THE Admin_Panel SHALL redirect the user to the home page
2. WHEN a user with CHECKER role navigates to `/admin/*` routes other than `/admin/moderation`, THE Admin_Panel SHALL redirect the user to `/admin/moderation`
3. WHEN a user with ADMIN or SUPER_ADMIN role navigates to `/admin`, THE Admin_Panel SHALL display the dashboard with all accessible sections
4. THE Admin_Panel SHALL render navigation menu items based on the authenticated user's role, showing only sections the user has access to
5. WHEN an unauthenticated user navigates to any `/admin/*` route, THE Admin_Panel SHALL redirect the user to the login page
