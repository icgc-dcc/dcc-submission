# ICGC DCC - Submission

Parent project of the submission system which is responsible for accepting and validating ICGC clinical and experimental data files submitted to the DCC.

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/29bb5857a70d4861b46cbcc94d569009)](https://www.codacy.com/app/icgc-dcc/dcc-portal?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=icgc-dcc/dcc-portal&amp;utm_campaign=Badge_Grade)

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

The following module is the user interface to the system:

- [Submission UI](dcc-submission-ui/README.md)

The following are static files for use in any project:

- [Submission Resources](dcc-submission-resources/README.md)

The following are utility modules related to submissions:

- [Submission Loader](dcc-submission-loader/README.md)
- [Submission Generator](dcc-submission-generator/README.md)

The following module is a project testing libraries:

- [Submission Test](dcc-submission-test/README.md)

For detailed information on each module, please consult the above `README.md`s.

## Architecture

For a high level overview of the architecture, please see [ARCHITECTURE.md](ARCHITECTURE.md).

## Installation

For automated deployment and installation of the infrastructure and software components, please consult the [dcc-cm](https://github.com/icgc-dcc/dcc-cm/blob/develop/ansible/README.md) project and execute the `submission.yml` playbook.

## Changes

Change log for the user-facing system modules may be found in [CHANGES.md](CHANGES.md).

## License

Copyright and license information may be found in [LICENSE.md](LICENSE.md).
