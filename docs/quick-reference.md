# Quick Reference

## Production URLs
- **Production**: https://chickencalculator-production-production-2953.up.railway.app
- **Admin Portal**: /admin
- **Location Access**: /{slug} (password protected)
  - Calculator: /{slug}/calculator
  - Sales Data: /{slug}/sales
  - Marination History: /{slug}/history
- **Metrics**: /actuator/prometheus
- **Health**: /api/health

## Railway IDs (for MCP Commands)
```bash
Project ID: 767deec0-30ac-4238-a57b-305f5470b318
Service ID: fde8974b-10a3-4b70-b5f1-73c4c5cebbbe
Environment ID: f57580c2-24dc-4c4e-adf2-313399c855a9
Postgres ID: bbbadbce-026c-44f1-974c-00d5a457bccf
```

## Default Credentials
- **Admin**: admin@yourcompany.com
  - Password: Set via ADMIN_DEFAULT_PASSWORD env var
  - Note: Password change required on first login
- **Locations**: Each has own password
  - Default: "ChangeMe123!" (V5 migration)
  - Admins can generate/update passwords

## GitHub Repository
https://github.com/Danielalnajjar/ChickenCalculator-Production

## Deployment
- Platform: Railway (auto-deploy from main branch)
- Port: 8080 (Railway single-port constraint)
- Database: PostgreSQL 16.8