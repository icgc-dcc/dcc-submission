ICGC DCC Data Generator
===

TODO

Build
---

From the command line:

	mvn package

Example
---
The config.yaml file is currently in the resources folder

From the command line:

	java -jar target/dcc-generator-<version>.jar -f ~/path/to/config/file

Help
---

From the command line, type `java -jar target/dcc-generator-<version>.jar --help`:

	Usage: java -jar dcc-generator-<version>.jar [options]
	  Options:
	  * -f, --file
	   	   Path to config.yaml file
		   Default: false
	    -h, --help
	       Show help
	       Default: false
	    -v, --version
	       Show version information
	       Default: false
