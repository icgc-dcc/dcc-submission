dcc-genes
===

Genes model loader that reads a MongoDB dump of the Heliotrope gene model (`genes.bson`) and inserts a transformed version into the DCC `dcc-genes` MongoDB database.

Build
---

From the command line:

	mvn package

Example
---

From the command line:

	java -jar target/dcc-genes-<version>.jar -d mongodb://localhost/dcc-genes.Genes -f ~/heliotrope-bson/dump/heliotrope/genes.bson

Help
---

From the command line, type `java -jar target/dcc-genes-<version>.jar --help`:

	Usage: java -jar dcc-genes.jar [options]
	  Options:
	  * -d, --database
	       DCC mongo database uri (e.g. mongodb://localhost)
	  * -f, --file
	       Heliotrope genes.bson mongodump file (e.g. genes.bson)
	    -h, --help
	       Show help
	       Default: false
	    -v, --version
	       Show version information
	       Default: false
