ICGC DCC - Submission
===

Parent project of the submission modules.

Build
---

From the command line:

```shell
mvn
```
    
Modules
---

Core modules:

- [Submission Core](dcc-submission-core/README.md)
- [Submission Server](dcc-submission-server/README.md)
- [Submission Validator](dcc-submission-validator/README.md)
- [Submission Reporter](dcc-submission-reporter/README.md)
- [Submission UI](dcc-submission-ui/README.md)

Utility modules:

- [Submission Generator](dcc-submission-generator/README.md) 

Architecture
---

For a high level overview of the architecture, please see [ARCHITECTURE.md](ARCHITECTURE.md).

Deployment
---

For automated deployment of the infrastructure and software components, please consult the [dcc-cm](https://github.com/icgc-dcc/dcc-cm/blob/develop/ansible/README.md) project and execute the `submission.yml` playbook.

Changes
---
Change log for the user-facing system modules may be found in [CHANGES.md](CHANGES.md).

Copyright and License
---

Copyright and license information may be found in [LICENSE.md](LICENSE.md).
