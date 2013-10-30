ICGC DCC - Submission Server
===

Web, REST and SFTP server for the data submission sub-system.

Build
---

	mvn


Development
---

Start the server using mvn

	cd dcc/dcc-submission-server
	mvn exec:java


Start the server using an IDE by running

    org.icgc.dcc.submission.Main external src/test/conf/application.conf

To see the client interface, point your browser to [http://localhost:5380/](http://localhost:5380/)



