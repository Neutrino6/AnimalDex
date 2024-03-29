FROM feranto/java-maven-spring:v1
COPY . .
RUN mvn compile
ENTRYPOINT ["java","-jar","/target/certificate-1.0-SNAPSHOT.jar"]
