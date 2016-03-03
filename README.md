# ICGC DCC - Submission

Parent project of the submission system which is responsible for accepting and validating ICGC clinical and experimental data files submitted to the DCC.

## Setup

Follow the setup instructions in each of the system modules before attempting to build the top level project.

## Build

To compile, test and package the system, execute the following from the root of the repository:

```shell
mvn
```

## Modules

The following modules comprise the backend of the system:

- [Submission Core](dcc-submission-core/README.md)
- [Submission Server](dcc-submission-server/README.md)
- [Submission Validator](dcc-submission-validator/README.md)
- [Submission Loader](dcc-submission-loader/README.md)

The following module is the user interface to the system:

- [Submission UI](dcc-submission-ui/README.md)

The following are utility modules related to submissions:

- [Submission Generator](dcc-submission-generator/README.md)

For detailed information on each module, please consult the above `README.md`s.

## Architecture

For a high level overview of the architecture, please see [ARCHITECTURE.md](ARCHITECTURE.md).

## Installation

For automated deployment and installation of the infrastructure and software components, please consult the [dcc-cm](https://github.com/icgc-dcc/dcc-cm/blob/develop/ansible/README.md) project and execute the `submission.yml` playbook.

## Changes

Change log for the user-facing system modules may be found in [CHANGES.md](CHANGES.md).

## License

Copyright and license information may be found in [LICENSE.md](LICENSE.md).
