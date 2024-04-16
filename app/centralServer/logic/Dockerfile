FROM feranto/java-maven-spring:v1
COPY . .
RUN mvn install
EXPOSE 6039
ENTRYPOINT ["mvn", "spring-boot:run"]