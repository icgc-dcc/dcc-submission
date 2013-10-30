ICGC DCC - Submission Server
===

Web, REST and SFTP server for the data submission sub-system. 

Build
---

	cd dcc
	mvn -am -pl dcc-submission/dcc-submission-server


Development
---

To start the server in an IDE:

	java -Dlogback.configurationFile=src/main/conf/logback.xml -Dlog.dir=target/logs org.icgc.dcc.submission.server.Main external src/test/conf/application.conf

To start the server from the command line:

	cd dcc
	mvn -am -pl dcc-submission/dcc-submission-server -DskipTests=true
	cd dcc-submission/dcc-submission-server
	mvn exec:java

To see the client interface, point your browser to [http://localhost:5380/](http://localhost:5380/)

To login to SFTP type

	sftp -P 5322 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null admin@localhost

Use the username `admin` and password `adminspasswd` to login to both HTTP and SFTP interfaces.



