FROM amazoncorretto:11-alpine-jdk
COPY target/*.jar app.jar
ENV SERVER_PORT 9091
ENTRYPOINT ["java","-jar","/app.jar"]