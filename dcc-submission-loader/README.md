# ICGC DCC - Submission Loader

Submission Loader component module for the data submission sub-system.

The Submission Loader component load submission files to a database for a further analysis and reports generation.

## Libraries

The implementation uses the the following frameworks and libraries:

- [Hadoop](http://hadoop.apache.org/)
- [Spring](http://spring.io/)
- [OrientDB](http://orientdb.com/orientdb/)
- [PostgreSQL](http://www.postgresql.org/)

## Build

To compile, test and package the module, execute the following from the root of the repository:

```shell
mvn -am -pl dcc-submission/dcc-submission-loader
```

