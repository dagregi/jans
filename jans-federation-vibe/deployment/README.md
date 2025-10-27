# Jans Federation Vibe - Deployment Guide

This guide provides comprehensive instructions for deploying and running the Jans Federation Vibe application using Docker.

## 📋 Prerequisites

Before deploying the application, ensure you have the following installed:

- **Docker** (version 20.10 or higher)
- **Docker Compose** (version 2.0 or higher)
- **Git** (for cloning the repository)
- **Maven** (version 3.6 or higher) - for local development

### Installing Prerequisites

#### Docker Installation
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install docker.io docker-compose

# macOS (using Homebrew)
brew install docker docker-compose

# Windows
# Download Docker Desktop from https://www.docker.com/products/docker-desktop
```

#### Verify Installation
```bash
docker --version
docker-compose --version
```

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone <repository-url>
cd jans-federation-vibe
```

### 2. Build the Application
```bash
# Build the Docker image
./deployment/scripts/build.sh
```

### 3. Start the Application
```bash
# Start all services
./deployment/scripts/start.sh
```

### 4. Verify Deployment
```bash
# Check application health
./deployment/scripts/health-check.sh

# Check database setup
curl http://localhost:8080/database/stats
curl http://localhost:8080/database/health
```

## 🐳 Docker Deployment

### Using Docker Compose (Recommended)

The easiest way to deploy the application is using Docker Compose:

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f federation

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Using Docker Commands

#### Build the Image
```bash
docker build -f deployment/Dockerfile -t jans-federation-vibe:latest .
```

#### Run the Container
```bash
docker run -d \
  --name jans-federation-vibe \
  -p 8080:8080 \
  -e JAVA_OPTS="-Xmx512m -Xms256m" \
  jans-federation-vibe:latest
```

#### Stop the Container
```bash
docker stop jans-federation-vibe
docker rm jans-federation-vibe
```

## 📁 Deployment Structure

```
deployment/
├── Dockerfile                 # Docker image definition
├── docker-compose.yml        # Docker Compose configuration
├── config/                   # Configuration files
│   ├── application.properties
│   ├── log4j2.xml
│   └── nginx.conf
└── scripts/                  # Deployment scripts
    ├── build.sh              # Build script
    ├── start.sh              # Start script
    ├── stop.sh               # Stop script
    └── health-check.sh       # Health check script
```

## 🗄️ Database Setup

The application automatically creates database tables and populates sample data on first startup.

### Automatic Database Initialization

When the application starts for the first time, it will:

1. **Create Database Tables**: All required tables for federation operations
2. **Populate Sample Data**: Test entities, trust marks, and configurations
3. **Create Database Views**: For easy querying and statistics
4. **Set Up Indexes**: For optimal performance

### Sample Data Included

The database is automatically populated with:

- **Federation Metadata**: Basic federation configuration
- **Entity Configurations**: Sample OpenID Providers and Relying Parties
- **Trust Mark Issuers**: Sample trust mark issuing authorities
- **Trust Mark Profiles**: Basic, Advanced, and Enterprise trust levels
- **Trust Marks**: Sample trust marks for test entities

### Database Endpoints

- `GET /database/health` - Check database connectivity
- `GET /database/stats` - View database statistics
- `POST /database/migrate` - Run database migrations manually

### Manual Database Setup

If you need to manually set up the database:

```bash
# Start only the database
docker-compose up -d postgres

# Run database setup
./deployment/scripts/setup-db.sh

# Check database status
curl http://localhost:8080/database/stats
```

## 🔧 Configuration

### Environment Variables

The application can be configured using the following environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `FEDERATION_ISSUER` | `https://federation.example.com` | Federation issuer URL |
| `FEDERATION_AUDIENCE` | `federation` | Federation audience |
| `JAVA_OPTS` | `-Xmx512m -Xms256m` | JVM options |
| `SERVER_PORT` | `8080` | Application port |

### Configuration Files

#### application.properties
```properties
# Federation settings
federation.issuer=https://federation.example.com
federation.audience=federation
federation.name=Jans Federation Vibe

# Security settings
federation.security.jwt.algorithm=RS256
federation.security.jwt.expiration=3600

# Trust mark settings
federation.trustmark.default.expiration=86400
```

#### nginx.conf
```nginx
# Reverse proxy configuration
upstream federation {
    server federation:8080;
}

server {
    listen 80;
    location / {
        proxy_pass http://federation;
    }
}
```

## 🌐 API Endpoints

Once deployed, the application provides the following endpoints:

### Core Federation Endpoints
- `GET /.well-known/openid-federation` - Entity configuration
- `GET /federation/metadata` - Federation metadata
- `POST /federation/validate-trust-chain` - Trust chain validation

### Trust Mark Endpoints
- `GET /federation/trust-marks` - Get trust marks
- `POST /federation/issue-trust-mark` - Issue trust mark
- `GET /federation/trust-mark-issuers` - Get trust mark issuers

### Database Endpoints
- `GET /database/health` - Database health check
- `GET /database/stats` - Database statistics
- `POST /database/migrate` - Run database migrations

### Health Check
- `GET /federation/metadata` - Application health check

## 📊 Monitoring and Logging

### View Logs
```bash
# View application logs
docker-compose logs -f federation

# View all service logs
docker-compose logs -f

# View logs with timestamps
docker-compose logs -f -t federation
```

### Log Files
- Application logs: `/app/logs/federation.log`
- Rolling logs: `/app/logs/federation-rolling.log`
- Access logs: Available through nginx

### Health Monitoring
```bash
# Check application health
curl http://localhost:8080/federation/metadata

# Check Docker container health
docker ps
```

## 🔒 Security Considerations

### Production Deployment

1. **SSL/TLS Configuration**
   ```bash
   # Generate SSL certificates
   openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
     -keyout deployment/config/ssl/key.pem \
     -out deployment/config/ssl/cert.pem
   ```

2. **Environment Variables**
   ```bash
   # Set secure environment variables
   export FEDERATION_ISSUER=https://your-federation.com
   export FEDERATION_AUDIENCE=your-audience
   ```

3. **Network Security**
   ```yaml
   # In docker-compose.yml
   networks:
     federation-network:
       driver: bridge
       internal: true  # For internal network
   ```

### Security Headers
The nginx configuration includes security headers:
- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- `X-XSS-Protection: 1; mode=block`
- `Strict-Transport-Security`

## 🛠️ Troubleshooting

### Common Issues

#### Application Won't Start
```bash
# Check Docker logs
docker-compose logs federation

# Check if port is available
netstat -tulpn | grep 8080

# Restart services
docker-compose restart federation
```

#### Database Connection Issues
```bash
# Check database container
docker-compose ps postgres

# Check database logs
docker-compose logs postgres

# Test database connection
docker-compose exec postgres psql -U federation -d federation
```

#### Memory Issues
```bash
# Increase JVM memory
export JAVA_OPTS="-Xmx1g -Xms512m"
docker-compose up -d
```

### Debug Mode
```bash
# Enable debug logging
export LOGGING_LEVEL=DEBUG
docker-compose up -d
```

## 📈 Scaling

### Horizontal Scaling
```yaml
# In docker-compose.yml
services:
  federation:
    deploy:
      replicas: 3
    ports:
      - "8080-8082:8080"
```

### Load Balancing
```nginx
# nginx.conf
upstream federation {
    server federation:8080;
    server federation:8081;
    server federation:8082;
}
```

## 🧪 Testing

### Unit Tests
```bash
# Run tests locally
mvn test

# Run tests in Docker
docker-compose exec federation mvn test
```

### Integration Tests
```bash
# Test API endpoints
curl -X GET http://localhost:8080/federation/metadata
curl -X GET "http://localhost:8080/.well-known/openid-federation?iss=https://example.com"
```

### Load Testing
```bash
# Install Apache Bench
sudo apt-get install apache2-utils

# Run load test
ab -n 1000 -c 10 http://localhost:8080/federation/metadata
```

## 📚 Additional Resources

- [OpenID Federation 1.0 Specification](https://openid.net/specs/openid-federation-1_0.html)
- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Nginx Configuration](https://nginx.org/en/docs/)

## 🤝 Support

For issues and questions:
1. Check the logs: `docker-compose logs federation`
2. Run health check: `./deployment/scripts/health-check.sh`
3. Review configuration files in `deployment/config/`
4. Check Docker and Docker Compose versions

## 📝 License

This project is licensed under the same terms as the Janssen project.
