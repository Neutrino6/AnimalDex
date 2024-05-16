FROM feranto/java-maven-spring:v1
COPY . .
COPY ./storage ./storage
RUN mvn install
EXPOSE 6040
ENTRYPOINT ["mvn", "spring-boot:run"]