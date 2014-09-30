ICGC DCC - Submission Generator
===

Description
---

A test data generator that generates synthetic submissions for testing purposes. The data is valid when passed through the validator (excluding script restrictions).

Build
---

From the command line:

	mvn package

Example
---
The config.yaml file is currently in src/main/conf folder

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
		-d, --dictionary
		   Path to dictionary.json file
		   Default: false
		-k, --codelist
		   Path to codelist.json file
		   Default:false		
	    -h, --help
	       Show help
	       Default: false
	    -v, --version
	       Show version information
	       Default: false
