# ICGC DCC - Submission Server

Web, REST and SFTP server for the data submission sub-system.

## Libraries

The submission server is comprised of the following components:

- [Jersey 2](https://jersey.java.net)
- [Jetty](https://eclipse.org/jetty/)
- [Apache Shiro](http://shiro.apache.org/)
- [Guice](https://github.com/google/guice)

The SFTP interface is implemented using [Apache MINA SSHD](https://mina.apache.org/sshd-project/index.html) using a custom [HDFS](https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-hdfs/HdfsUserGuide.html) based virtual file system.

The metadata persistence layer is implemented using:

- [Morhpia](http://mongodb.github.io/morphia)
- [MongoDB QueryDSL](http://www.querydsl.com)

## Build

To compile, test and package the module, execute the following from the root of the repository:

```shell
mvn -am -pl dcc-submission/dcc-submission-server
```

## Development

Before starting development it is necessary to download and extract a reference genome FASTA file and index:

```shell
wget http://seqwaremaven.oicr.on.ca/artifactory/simple/dcc-dependencies/org/icgc/dcc/dcc-reference-genome/GRCh37/dcc-reference-genome-GRCh37.tar.gz
tar zxf dcc-reference-genome-GRCh37.tar.gz -C /tmp
```

Ensure the FASTA file is correctly referenced from `application.conf`'s `reference.fasta` configuration element.

To start the server in an IDE:

```shell
java -Dlogback.configurationFile=src/main/conf/logback.xml -Dlog.dir=target/logs org.icgc.dcc.submission.server.Main external src/test/conf/application.conf
```

To start the server from the command line:

```shell
cd dcc
mvn -am -pl dcc-submission/dcc-submission-server -DskipTests=true
cd dcc-submission/dcc-submission-server
mvn exec:java
```

Start the server using an IDE by running:

```shell
org.icgc.dcc.submission.SubmissionMain external src/test/conf/application.conf
```

To see the client interface, point your browser to [http://localhost:5380/](http://localhost:5380/)

To login to SFTP type:

```shell
sftp -P 5322 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null admin@localhost
```

Use the username `admin` and password `adminspasswd` to login to both HTTP and SFTP interfaces.

## Debugging

Sometimes it is needed to debug a running instance that has been packaged into a jar to investigate packaging issues. You can attach a debugger [in your IDE](http://www.eclipsezone.com/eclipse/forums/t53459.html) by running the `jar`ed application with:

```java
java -Dlogback.configurationFile=../conf/logback.xml -Dlog.dir=../logs -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044 -Xmx4092m -jar ../lib/dcc-submission-server.jar external ../conf/application.conf
```

