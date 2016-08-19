# DCC Submission - Docker Image

Uber container with all DCC Submission Server dependencies installed, including a single node hadoop cluster.

## Building

In the directory of the the `DockerFile`

```shell
docker build -t dcc-submission .
```

## Running

```shell
docker run -it -p 27017:27017 -p 8020:8020 -p 8021:8021 -p 50070:50070 -p 50010:50010 -p 50020:50020 -p 50075:50075 -p 50030:50030 -p 50060:50060 dcc-submission
```
