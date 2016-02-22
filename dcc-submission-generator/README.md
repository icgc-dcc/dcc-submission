# ICGC DCC - Submission Generator

A test data generator utility that generates synthetic submissions for testing purposes. The data is valid when passed through the validator (excluding script restrictions).

## Build

To compile, test and package the module, execute the following from the root of the repository:

```shell
mvn -am -pl dcc-submission/dcc-submission-generator
```

## Usage

The `config.yaml` file is currently in `src/main/conf` folder

From the command line:

```shell
java -jar target/dcc-generator-<version>.jar -c ~/path/to/config/file
```

# Help

From the command line, type `java -jar target/dcc-generator-<version>.jar --help`:

```shell
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
```
