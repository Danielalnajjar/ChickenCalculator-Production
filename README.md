# ChickenCalculator Production System

A multi-tenant marination calculator system for Wok to Walk restaurants, helping kitchen staff calculate optimal chicken marination quantities based on projected sales.

## 🚀 Production Status
- **URL**: https://chickencalculator-production-production-2953.up.railway.app
- **Version**: 1.0.0
- **Status**: ✅ Fully Operational
- **Monitoring**: Sentry 7.14.0 Active

## 📋 Features
- Multi-location support with independent authentication
- Marination calculations for Soy, Teriyaki, and Turmeric
- Sales data tracking and historical analysis
- Admin portal for location management
- WCAG 2.1 AA accessibility compliant
- Real-time error monitoring with Sentry

## 🛠 Tech Stack
- **Backend**: Spring Boot 3.2.0 + Kotlin 1.9.20
- **Frontend**: React 18.2.0 + TypeScript 4.9.5
- **Database**: PostgreSQL 16.8
- **Deployment**: Railway (auto-deploy from main)
- **Monitoring**: Sentry + Prometheus

## 🚦 Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- Maven 3.8+

### Local Development
```bash
# Backend
cd backend
mvn spring-boot:run -Dspring.profiles.active=dev

# Frontend
cd frontend
npm install
npm start

# Admin Portal
cd admin-portal
npm install
npm start
```

### Deployment
```bash
git push origin main  # Auto-deploys to Railway
```

## 📚 Documentation
- **Claude Code Guide**: See `CLAUDE.md` for AI assistance context
- **Architecture**: `docs/architecture/overview.md`
- **API Reference**: `docs/api-reference.md`
- **Development**: `docs/development-workflow.md`
- **Deployment**: `docs/deployment-guide.md`
- **Known Issues**: `KNOWN_ISSUES.md`

## 🔐 Security
- JWT authentication with httpOnly cookies
- CSRF protection
- BCrypt password hashing
- Multi-tenant data isolation
- Rate limiting on authentication

## 📊 Current Metrics
- **Test Coverage**: ~30% (target: 80%)
- **Response Time**: p95 < 200ms
- **Error Rate**: < 0.1%
- **Uptime**: 99.9%

## 🤝 Contributing
1. Check `KNOWN_ISSUES.md` for areas needing work
2. Follow existing code patterns
3. Ensure tests pass
4. Monitor Sentry after deployment

## 📝 License
Private - Wok to Walk Internal Use Only

## 👥 Team
- Development: Daniel Alnajjar
- AI Assistant: Claude Code

---

For detailed technical documentation, see the `/docs` directory.