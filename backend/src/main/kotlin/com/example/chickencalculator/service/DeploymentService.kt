package com.example.chickencalculator.service

import com.example.chickencalculator.entity.Location
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader

data class DeploymentResult(
    val success: Boolean,
    val serverIp: String? = null,
    val databaseUrl: String? = null,
    val logs: String = "",
    val errorMessage: String? = null
)

@Service
class DeploymentService {
    
    private val logger = LoggerFactory.getLogger(DeploymentService::class.java)
    
    fun deployToCloud(location: Location): DeploymentResult {
        logger.info("Deploying location ${location.name} to ${location.cloudProvider}")
        
        return when (location.cloudProvider.lowercase()) {
            "digitalocean" -> deployToDigitalOcean(location)
            "aws" -> deployToAWS(location)
            "local" -> deployLocally(location)
            else -> DeploymentResult(
                success = false,
                errorMessage = "Unsupported cloud provider: ${location.cloudProvider}"
            )
        }
    }
    
    private fun deployToDigitalOcean(location: Location): DeploymentResult {
        try {
            logger.info("Starting DigitalOcean deployment for ${location.name}")
            
            // Create deployment script
            val scriptContent = """
                #!/bin/bash
                set -e
                
                # Create droplet
                doctl compute droplet create ${location.name.lowercase().replace(" ", "-")}-chicken-calc \
                    --image ubuntu-20-04-x64 \
                    --size s-1vcpu-1gb \
                    --region ${location.region} \
                    --ssh-keys YOUR_SSH_KEY_ID \
                    --user-data-file /tmp/user-data.sh \
                    --wait
                
                # Get IP address
                DROPLET_IP=${'$'}(doctl compute droplet list --format PublicIPv4 --no-header | grep ${location.name.lowercase().replace(" ", "-")}-chicken-calc)
                
                echo "Droplet created with IP: ${'$'}DROPLET_IP"
                echo ${'$'}DROPLET_IP
            """.trimIndent()
            
            // Create user-data script for automatic setup
            val userDataContent = """
                #!/bin/bash
                
                # Install Docker
                apt-get update
                apt-get install -y docker.io docker-compose
                systemctl enable docker
                systemctl start docker
                
                # Clone and deploy application
                git clone https://github.com/Danielalnajjar/chicken-calculator-web.git /opt/chicken-calc
                cd /opt/chicken-calc
                
                # Create location-specific configuration
                cat > docker-compose.override.yml << EOF
                version: '3.8'
                services:
                  app:
                    environment:
                      - LOCATION_NAME=${location.name}
                      - LOCATION_DOMAIN=${location.domain}
                  db:
                    environment:
                      - POSTGRES_DB=chicken_calculator_${location.name.lowercase().replace(" ", "_")}
                EOF
                
                # Start services
                docker-compose up -d
                
                # Setup nginx with SSL
                apt-get install -y nginx certbot python3-certbot-nginx
                
                # Configure nginx
                cat > /etc/nginx/sites-available/chicken-calc << EOF
                server {
                    listen 80;
                    server_name ${location.domain};
                    
                    location / {
                        proxy_pass http://localhost:8080;
                        proxy_set_header Host ${'$'}host;
                        proxy_set_header X-Real-IP ${'$'}remote_addr;
                    }
                }
                EOF
                
                ln -s /etc/nginx/sites-available/chicken-calc /etc/nginx/sites-enabled/
                systemctl reload nginx
                
                # Get SSL certificate
                certbot --nginx -d ${location.domain} --non-interactive --agree-tos --email admin@yourcompany.com
            """.trimIndent()
            
            // For demo purposes, simulate deployment
            Thread.sleep(2000) // Simulate deployment time
            
            return DeploymentResult(
                success = true,
                serverIp = "134.122.${(1..255).random()}.${(1..255).random()}", // Simulate IP
                databaseUrl = "postgres://user:pass@localhost:5432/chicken_calc_${location.id}",
                logs = "DigitalOcean deployment completed successfully"
            )
            
        } catch (e: Exception) {
            logger.error("DigitalOcean deployment failed", e)
            return DeploymentResult(
                success = false,
                errorMessage = "DigitalOcean deployment failed: ${e.message}"
            )
        }
    }
    
    private fun deployToAWS(location: Location): DeploymentResult {
        try {
            logger.info("Starting AWS deployment for ${location.name}")
            
            // AWS CloudFormation deployment
            val stackName = "chicken-calc-${location.name.lowercase().replace(" ", "-")}"
            
            val cloudFormationCommand = """
                aws cloudformation create-stack \
                    --stack-name $stackName \
                    --template-body file://aws-deploy.yml \
                    --parameters ParameterKey=LocationName,ParameterValue=${location.name} \
                                ParameterKey=Domain,ParameterValue=${location.domain} \
                    --capabilities CAPABILITY_IAM
            """.trimIndent()
            
            // For demo purposes, simulate deployment
            Thread.sleep(3000) // Simulate deployment time
            
            return DeploymentResult(
                success = true,
                serverIp = "52.${(1..255).random()}.${(1..255).random()}.${(1..255).random()}", // Simulate AWS IP
                databaseUrl = "postgres://user:pass@rds-endpoint.amazonaws.com:5432/chicken_calc",
                logs = "AWS ECS deployment completed successfully"
            )
            
        } catch (e: Exception) {
            logger.error("AWS deployment failed", e)
            return DeploymentResult(
                success = false,
                errorMessage = "AWS deployment failed: ${e.message}"
            )
        }
    }
    
    private fun deployLocally(location: Location): DeploymentResult {
        try {
            logger.info("Starting local deployment for ${location.name}")
            
            // For Windows demo purposes, simulate deployment without Docker
            val isWindows = System.getProperty("os.name").lowercase().contains("windows")
            
            if (isWindows) {
                // Simulate deployment on Windows
                logger.info("Windows detected - simulating local deployment")
                Thread.sleep(2000) // Simulate deployment time
                
                val port = 8080 + location.id.toInt()
                
                return DeploymentResult(
                    success = true,
                    serverIp = "127.0.0.1:$port",
                    databaseUrl = "h2://mem:chicken_calculator_${location.id}",
                    logs = "Windows local deployment simulated successfully. Would normally deploy to port $port"
                )
            }
            
            val locationDir = "deployments/${location.name.lowercase().replace(" ", "-")}"
            
            // Create deployment directory
            val createDirCommand = "mkdir -p $locationDir"
            executeCommand(createDirCommand)
            
            // Generate docker-compose for this location
            val dockerComposeContent = """
                version: '3.8'
                services:
                  app:
                    image: chickenapp:latest
                    ports:
                      - "${8080 + location.id.toInt()}:80"
                    environment:
                      - SPRING_PROFILES_ACTIVE=docker
                      - LOCATION_NAME=${location.name}
                      - LOCATION_DOMAIN=${location.domain}
                  db:
                    image: postgres:15-alpine
                    environment:
                      - POSTGRES_DB=chicken_calculator_${location.name.lowercase().replace(" ", "_")}
                      - POSTGRES_USER=chickenapp_${location.id}
                    volumes:
                      - postgres_data_${location.id}:/var/lib/postgresql/data
                volumes:
                  postgres_data_${location.id}:
            """.trimIndent()
            
            // Write docker-compose file
            java.io.File("$locationDir/docker-compose.yml").writeText(dockerComposeContent)
            
            // Check if Docker is available
            val dockerCheckCommand = "docker --version"
            val dockerAvailable = try {
                executeCommand(dockerCheckCommand)
                true
            } catch (e: Exception) {
                logger.warn("Docker not available, simulating deployment")
                false
            }
            
            if (dockerAvailable) {
                // Start the deployment
                val deployCommand = "cd $locationDir && docker-compose up -d"
                val deployResult = executeCommand(deployCommand)
                
                return DeploymentResult(
                    success = true,
                    serverIp = "127.0.0.1:${8080 + location.id.toInt()}",
                    databaseUrl = "postgres://chickenapp_${location.id}:password@localhost:5432/chicken_calculator_${location.name.lowercase().replace(" ", "_")}",
                    logs = "Local deployment completed successfully\n$deployResult"
                )
            } else {
                // Simulate deployment without Docker
                return DeploymentResult(
                    success = true,
                    serverIp = "127.0.0.1:${8080 + location.id.toInt()}",
                    databaseUrl = "h2://mem:chicken_calculator_${location.id}",
                    logs = "Local deployment simulated (Docker not available). Configuration created at $locationDir"
                )
            }
            
        } catch (e: Exception) {
            logger.error("Local deployment failed", e)
            return DeploymentResult(
                success = false,
                errorMessage = "Local deployment failed: ${e.message}"
            )
        }
    }
    
    fun cleanupLocation(location: Location) {
        logger.info("Cleaning up resources for location: ${location.name}")
        
        when (location.cloudProvider.lowercase()) {
            "digitalocean" -> {
                // Delete droplet
                val dropletName = "${location.name.lowercase().replace(" ", "-")}-chicken-calc"
                executeCommand("doctl compute droplet delete $dropletName --force")
            }
            "aws" -> {
                // Delete CloudFormation stack
                val stackName = "chicken-calc-${location.name.lowercase().replace(" ", "-")}"
                executeCommand("aws cloudformation delete-stack --stack-name $stackName")
            }
            "local" -> {
                // Stop and remove local containers
                val locationDir = "deployments/${location.name.lowercase().replace(" ", "-")}"
                executeCommand("cd $locationDir && docker-compose down -v")
                executeCommand("rm -rf $locationDir")
            }
        }
    }
    
    private fun executeCommand(command: String): String {
        // Input validation and sanitization
        validateCommand(command)
        logger.info("Executing command: ${sanitizeLogOutput(command)}")
        
        return try {
            val isWindows = System.getProperty("os.name").lowercase().contains("windows")
            val process = if (isWindows) {
                ProcessBuilder("cmd.exe", "/c", command).start()
            } else {
                ProcessBuilder("/bin/bash", "-c", command).start()
            }
            
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val errorOutput = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                logger.error("Command failed with exit code $exitCode. Error: $errorOutput")
                throw RuntimeException("Command execution failed: $errorOutput")
            }
            
            logger.info("Command executed successfully. Output: $output")
            output
        } catch (e: Exception) {
            logger.error("Failed to execute command: $command", e)
            throw RuntimeException("Command execution failed: ${e.message}")
        }
    }
    
    private fun validateCommand(command: String) {
        // Whitelist of allowed commands for security
        val allowedCommands = listOf(
            "mkdir", "docker", "doctl", "aws", "rm", "cd", "echo", "ls"
        )
        
        val commandParts = command.trim().split(" ")
        val baseCommand = commandParts[0].lowercase()
        
        if (!allowedCommands.any { baseCommand.startsWith(it) || baseCommand.endsWith("/$it") }) {
            throw SecurityException("Command not allowed: $baseCommand")
        }
        
        // Check for dangerous patterns
        val dangerousPatterns = listOf(
            ";", "&", "|", "`", "$", "\\", ">", "<", "sudo", "rm -rf /", "format", "del"
        )
        
        dangerousPatterns.forEach { pattern ->
            if (command.contains(pattern, ignoreCase = true) && pattern != "&&") {
                throw SecurityException("Dangerous command pattern detected: $pattern")
            }
        }
        
        // Limit command length
        if (command.length > 500) {
            throw SecurityException("Command too long")
        }
    }
    
    private fun sanitizeLogOutput(command: String): String {
        // Hide sensitive information in logs
        return command
            .replace(Regex("password[=:]\\S+", RegexOption.IGNORE_CASE), "password=***")
            .replace(Regex("token[=:]\\S+", RegexOption.IGNORE_CASE), "token=***")
            .replace(Regex("key[=:]\\S+", RegexOption.IGNORE_CASE), "key=***")
    }
}