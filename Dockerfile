#
# Build stage
#
FROM maven:3.8.7-openjdk-18-slim AS build

# Install container dependencies
RUN apt-get update
RUN apt-get install dos2unix

# Copy Sources
ENV HOME=/home/app
RUN mkdir -p $HOME
WORKDIR $HOME
ADD pom.xml $HOME
RUN chmod -R 777 /home/app
RUN find /home/app -type f -print0 | xargs -0 dos2unix --

# Download dependencies
RUN mvn verify --fail-never

ADD src $HOME/src
RUN chmod -R 777 /home/app
RUN find /home/app -type f -print0 | xargs -0 dos2unix --

#Build App
#RUN mvn -f /home/app/pom.xml clean package
RUN mvn -f /home/app/pom.xml package -Dpackaging=jar

#
# Package stage
#
FROM selenium/standalone-firefox:109.0
#FROM selenium/standalone-chrome:108.0

#Copy App
COPY --from=build /home/app/target/protocolParser-0.0.1-SNAPSHOT.jar /app.jar
ADD zugang.txt /

#Run App by default
ENTRYPOINT ["java","-jar","/app.jar"]
