# ICGC DCC - Submission EGA

EGA integration library module for the data submission sub-system.

## Build

To compile, test and package the module, execute the following from the root of the repository:

```shell
mvn -am -pl dcc-submission/dcc-submission-ega
```

## Import

The EGA Importer can be triggered by hitting a spring boot actuator endpoint. 
```aidl
<>:8081/import
```
The actuator endpoint runs on a different port from the REST API. 

