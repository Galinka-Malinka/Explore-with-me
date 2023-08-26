FROM amazoncorretto:11-alpine-jdk

COPY stats-server-service/target/*.jar app.jar
ENV SERVER_PORT 9090
ENTRYPOINT ["java","-jar","/app.jar"]