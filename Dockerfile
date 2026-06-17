#
# Build stage — compile, test and assemble the runnable distribution with JDK 25.
#
FROM eclipse-temurin:25-jdk AS build

ENV APP_HOME=/home/app
WORKDIR $APP_HOME

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

RUN ./gradlew --no-daemon clean test installDist

#
# Runtime stage — Firefox + geckodriver for scraping, plus the JDK 25 copied from the build stage
# (the selenium image is Ubuntu-based and has no openjdk-25 package).
#
FROM selenium/standalone-firefox:109.0

COPY --from=build /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"

COPY --from=build /home/app/build/install/protocolParser /opt/protocolParser
COPY zugang.txt /zugang.txt

WORKDIR /
ENTRYPOINT ["/opt/protocolParser/bin/protocolParser"]
