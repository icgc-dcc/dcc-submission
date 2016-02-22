# ICGC DCC - Submission Validator

Validator library module for the data submission sub-system.

Validation is largely driven by the metadata defined in the Dictionary.

## Libraries

The implementation uses the the following frameworks and libraries:

- [Hadoop](http://hadoop.apache.org/)
- [Cascading](http://www.cascading.org/)
- [MVEL](https://en.wikisource.org/wiki/MVEL_Language_Guide)
- [Picard](http://broadinstitute.github.io/picard/)

## Build

To compile, test and package the module, execute the following from the root of the repository:

```shell
mvn -am -pl dcc-submission/dcc-submission-validator
```




