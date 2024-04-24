FROM feranto/java-maven-spring:v1
COPY . .
RUN mvn install
ENTRYPOINT ["java","-jar","/target/oauth-1.0-SNAPSHOT.jar"]