FROM openjdk:10-jre-slim

MAINTAINER Mudassir Syed

VOLUME /tmp

ARG RELEASE_TAG
ENV RELEASE_TAG ${RELEASE_TAG}

ARG JAR_FILE
COPY target/${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]

#WORKDIR /opt

#ADD /target/CSE-Puller-0.0.1-SNAPSHOT.jar /opt/

#CMD java -jar CSE-Puller-0.0.1-SNAPSHOT.jar
