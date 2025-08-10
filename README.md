# 🐔 Chicken Calculator - Multi-Tenant Restaurant Management System

## 🎯 Overview

A comprehensive restaurant management system for chicken inventory and marination planning with multi-location support and admin portal.

## 🌟 Features

### Core Calculator
- **Smart Marination Planning** - Calculate optimal raw chicken amounts based on inventory and sales projections
- **4-Day Sales Window** - Plan ahead with emergency priority handling  
- **Multiple Chicken Types** - Soy, Teriyaki, and Turmeric with different portion sizes and yield factors
- **End-of-Day Processing** - Account for already-marinated chicken

### Multi-Tenant System
- **Location Management** - Deploy and manage multiple restaurant locations
- **Cloud Deployment** - DigitalOcean, AWS, and local deployment support
- **Admin Portal** - Centralized management dashboard
- **Real-time Monitoring** - Live deployment status and health checks

### Technology Stack
- **Backend**: Spring Boot 3.2 + Kotlin
- **Frontend**: React 18 + TypeScript + Tailwind CSS  
- **Database**: PostgreSQL (production) / H2 (development)
- **Deployment**: Docker + Railway/Render/Fly.io
- **Authentication**: Admin portal with role-based access

## 🚀 Quick Deploy

### Option 1: Railway (Recommended)
1. Fork/clone this repository
2. Go to [Railway.app](https://railway.app)
3. Click "Deploy from GitHub repo"
4. Select this repository
5. Railway auto-deploys everything!

**Your app will be available at:**
- Main App: `https://your-app.railway.app/`
- Admin Portal: `https://your-app.railway.app/admin`
- Login: `admin@yourcompany.com` / `admin123`

### Option 2: Local Development
```bash
# Backend
cd backend
mvn spring-boot:run

# Admin Portal  
cd admin-portal
npm install
npm start

# Main Frontend
cd frontend
npm install  
npm start
```

## 🏗️ Architecture

### Backend (Spring Boot)
- **Controllers**: REST API endpoints for calculator, admin, and data management
- **Services**: Business logic for calculations, deployments, and location management
- **Entities**: JPA models for locations, sales data, marination logs, and admin users
- **Repositories**: Data access layer with custom queries

### Frontend (React)
- **Main App**: Customer-facing chicken calculator interface
- **Admin Portal**: Multi-location management dashboard
- **Components**: Reusable UI components with TypeScript
- **Services**: API integration and data management

### Database Schema
- **locations**: Multi-tenant location management
- **sales_data**: Historical sales records  
- **marination_log**: Daily marination tracking
- **admin_users**: Admin authentication and roles

## 🔧 Configuration

### Environment Variables
```env
DATABASE_URL=postgresql://username:password@host:port/database
SPRING_PROFILES_ACTIVE=production  
PORT=8080
```

### Production Settings
- **Database**: PostgreSQL with connection pooling
- **Security**: HTTPS, CORS, environment variable secrets
- **Monitoring**: Health checks, actuator endpoints
- **Logging**: Structured logging for production debugging

## 🛡️ Security

- **Authentication**: Admin portal with secure login
- **Authorization**: Role-based access control
- **CORS**: Configured for production domains
- **Environment Variables**: All secrets externalized
- **Database**: Parameterized queries, no SQL injection

## 📊 Business Logic

### Chicken Constants
- **Soy**: 100g portions, 3000g/pan, 0.73 yield factor
- **Teriyaki**: 160g portions, 3200g/pan, 0.88 yield factor
- **Turmeric**: 160g portions, 1500g/pan, 0.86 yield factor

### Calculation Strategy  
1. **4-Day Planning Window** - Always calculate through Day 3
2. **Emergency Priority** - Address Day 0-1 shortfalls first
3. **Proportional Distribution** - Optimize when raw chicken is limited
4. **Pan Rounding** - 30% threshold for practical operations

## 🎮 Admin Portal Features

### Dashboard
- Location overview and statistics
- Real-time deployment monitoring  
- Health status indicators
- Revenue and transaction tracking

### Location Management
- Create new restaurant locations
- Deploy to cloud providers (DigitalOcean, AWS, Local)
- Monitor deployment status
- Manage location settings

### Multi-Tenant Architecture
- Isolated data per location
- Custom domains for each location
- Manager-specific access controls
- Centralized admin oversight

## 📱 API Endpoints

### Calculator API
- `POST /api/calculator/calculate` - Main marination calculation
- `GET /api/calculator/has-sales-data` - Check data availability

### Admin API  
- `POST /api/admin/auth/login` - Admin authentication
- `GET /api/admin/locations` - List all locations
- `POST /api/admin/locations` - Create new location
- `DELETE /api/admin/locations/{id}` - Delete location
- `GET /api/admin/stats` - Dashboard statistics

### Data Management
- `GET /api/sales-data` - Sales history
- `POST /api/sales-data` - Add sales record
- `GET /api/marination-log` - Marination history
- `POST /api/marination-log` - Log marination entry

## 🔄 Development Workflow

1. **Local Development**: Use H2 database and development profiles
2. **Testing**: Unit tests with Maven, integration tests for APIs  
3. **Staging**: Deploy to Railway/Render staging environment
4. **Production**: Deploy to Railway with PostgreSQL database

## 📈 Scaling & Performance

- **Database**: Connection pooling, query optimization
- **Caching**: Application-level caching for calculations
- **CDN**: Static asset delivery via Railway/Render CDN
- **Monitoring**: Application metrics and error tracking

## 🆘 Support & Documentation

- **Health Check**: `/actuator/health` - Application status
- **API Docs**: Embedded API documentation  
- **Logs**: Structured logging for debugging
- **Monitoring**: Built-in metrics and alerting

## 📄 License

This project is proprietary software for restaurant management.

---

**Built with ❤️ for efficient restaurant operations**