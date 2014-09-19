ICGC DCC - Submission Reportor
===

Executive report engine for the data submission sub-system.

Build
---

	mvn -am -pl dcc-submission/dcc-submission-reporter


To see the client interface, point your browser to [http://localhost:5380/](http://localhost:5380/)

To login to SFTP type:

	sftp -P 5322 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null admin@localhost

Use the username `admin` and password `adminspasswd` to login to both HTTP and SFTP interfaces.

Debugging
---

Sometimes it is needed to debug a running instance that has been packaged into a jar to investigate packaging issues. You can attach a debugger [in your IDE](http://www.eclipsezone.com/eclipse/forums/t53459.html) by running the `jar`ed application with:

	java -Dlogback.configurationFile=../conf/logback.xml -Dlog.dir=../logs -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044 -Xmx4092m -jar ../lib/dcc-submission-server.jar external ../conf/application.conf

