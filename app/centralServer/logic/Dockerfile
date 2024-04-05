FROM feranto/java-maven-spring:v1
COPY . .
RUN mvn compile
ENTRYPOINT ["mvn", "spring-boot:run"]