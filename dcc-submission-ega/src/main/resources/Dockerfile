FROM dcc-submission-base:v1
MAINTAINER ICGC <dcc-support@icgc.org>

WORKDIR /

RUN mkdir -p /srv/dcc-submission-ega/lib
RUN mkdir -p /srv/dcc-submission-ega/conf

COPY dcc-submission-ega.jar /srv/dcc-submission-ega/lib
COPY application.yml /srv/dcc-submission-ega/conf

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/srv/dcc-submission-ega/lib/dcc-submission-ega.jar", "--spring.config.location=/srv/dcc-submission-ega/conf/"]
