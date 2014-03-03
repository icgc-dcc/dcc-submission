ICGC DCC - Submission Server
===

Web, REST and SFTP server for the data submission sub-system.

Build
---

	cd dcc
	mvn -am -pl dcc-submission/dcc-submission-server


Development
---

Before starting development it is necessary to download and extract a reference genome FASTA file and index: 

	wget http://seqwaremaven.oicr.on.ca/artifactory/simple/dcc-dependencies/org/icgc/dcc/dcc-reference-genome/GRCh37/dcc-reference-genome-GRCh37.tar.gz
	tar zxf dcc-reference-genome-GRCh37.tar.gz -C /tmp

Ensure the FASTA file is correctly referenced from `application.conf`'s `reference.fasta` configuration element.

To start the server in an IDE:

	java -Dlogback.configurationFile=src/main/conf/logback.xml -Dlog.dir=target/logs org.icgc.dcc.submission.server.Main external src/test/conf/application.conf

To start the server from the command line:

	cd dcc
	mvn -am -pl dcc-submission/dcc-submission-server -DskipTests=true
	cd dcc-submission/dcc-submission-server
	mvn exec:java


Start the server using an IDE by running:

	org.icgc.dcc.submission.Main external src/test/conf/application.conf

To see the client interface, point your browser to [http://localhost:5380/](http://localhost:5380/)

To login to SFTP type:

	sftp -P 5322 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null admin@localhost

Use the username `admin` and password `adminspasswd` to login to both HTTP and SFTP interfaces.

Debugging
---

Sometimes it is needed to debug a running instance that has been packaged into a jar to investigate packaging issues. You can attach a debugger [in your IDE](http://www.eclipsezone.com/eclipse/forums/t53459.html) by running the `jar`ed application with:

	java -Dlogback.configurationFile=../conf/logback.xml -Dlog.dir=../logs -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044 -Xmx4092m -jar ../lib/dcc-submission-server.jar external ../conf/application.conf

