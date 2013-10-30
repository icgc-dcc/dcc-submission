ICGC DCC - Submission Server
===

Web, REST and SFTP server for the data submission sub-system. 

Build
---

	mvn


Development
---

To start the server in an IDE:

	java -Dlogback.configurationFile=src/main/conf/logback.xml -Dlog.dir=target/logs org.icgc.dcc.submission.server.Main external src/test/conf/application.conf

To start the server from the command line:

	cd dcc/dcc-submission-server
	mvn exec:java

To see the client interface, point your browser to [http://localhost:5380/](http://localhost:5380/)



