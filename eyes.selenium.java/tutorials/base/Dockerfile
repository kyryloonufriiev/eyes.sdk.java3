FROM artemqaapplitools/chrome-docker:latest

ARG version

RUN apt-get update \
    && apt-get install -y default-jdk

ENV JAVA_HOME=/usr/lib/jvm/default-java

RUN wget https://www-us.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.zip -P /tmp
RUN unzip ./tmp/apache-maven-3.6.3-bin.zip -d /opt

ENV PATH="/opt/apache-maven-3.6.3/bin:${PATH}"

COPY ./base/eyes-selenium-java*jar-with-dependencies.jar /eyes-selenium.jar
#RUN  mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=eyes-selenium.jar
RUN mvn -X install:install-file -Dfile=eyes-selenium.jar -DgroupId=com.applitools -DartifactId=eyes-selenium-java3 -Dversion=$version -Dpackaging=jar -DgeneratePom=true