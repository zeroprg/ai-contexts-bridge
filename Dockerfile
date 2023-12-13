# Start with a base image containing Java runtime with GraalVM version 17
FROM oracle/graalvm-ce:17

# Add Maintainer Info
LABEL maintainer="your-email@example.com"

# Add a volume pointing to /tmp
VOLUME /tmp

# Make port 8080 available to the world outside this container
EXPOSE 8080

# The application's jar file
ARG JAR_FILE=target/ai-contexts-bridge.jar

# Add the application's jar to the container
ADD ${JAR_FILE} ai-contexts-bridge.jar

# Run the jar file with /dev/./urandom as source of entropy
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/ai-contexts-bridge.jar"]
