ICGC DCC Data Generator
===

Description
---

A test data generator that generates artificial data for testing purposes. The data is valid when passed through the validator.
Build
---

From the command line:

	mvn package

Example
---
The config.yaml file is currently in the resources folder

From the command line:

	java -jar target/dcc-generator-<version>.jar -c ~/path/to/config/file

Help
---

From the command line, type `java -jar target/dcc-generator-<version>.jar --help`:

	Usage: java -jar dcc-generator-<version>.jar [options]
	  Options:
	  * -c, --config
	   	   Path to config.yaml file
		   Default: false
	    -h, --help
	       Show help
	       Default: false
	    -v, --version
	       Show version information
	       Default: false
