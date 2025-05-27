[![Codefresh build status]( https://g.codefresh.io/api/badges/pipeline/regnosysops/FINOS%2Frune-common?type=cf-1)]( https://g.codefresh.io/public/accounts/regnosysops/pipelines/new/667e8e2a5151928589be94bf)

# Rune Common

Rune Common is a java library that is utilised by [Rune Code Generators](https://github.com/REGnosys/rosetta-code-generators) and models expressed in the [Rune DSL](https://github.com/finos/rune-dsl) the main `Rune` project.

## Installation

You will need Java 11 SDK installed and referenced by the JAVA_HOME environment variable.
You will need [Maven](http://maven.apache.org/) and [Git](https://git-scm.com/) installed and configured in your environment.
[Fork and clone](https://help.github.com/articles/fork-a-repo) the project in your own workspace. Then run the first build:

``` sh
mvn clean install
```

## Usage example

Use the transform data structures in your project, e.g., `TestPackModel` or `PipelineModel`.

## Development setup

Add the library to your project using as a maven dependency as shown below:

``` xml
<dependency>
    <groupId>com.regnosys</groupId>
    <artifactId>rosetta-common</artifactId>
    <version>0.0.0-SNAPSHOT</version>
</dependency>
```

## Contributing
For any questions, bugs or feature requests please open an [issue](https://github.com/REGnosys/rosetta-common/issues)
For anything else please send an email to {project mailing list}.

To submit a contribution:
1. Fork it (<https://github.com/REGnosys/rosetta-common/fork>)
2. Create your feature branch (`git checkout -b feature/fooBar`)
3. Read our [contribution guidelines](.github/CONTRIBUTING.md) and [Community Code of Conduct](https://www.finos.org/code-of-conduct)
4. Commit your changes (`git commit -am 'Add some fooBar'`)
5. Push to the branch (`git push origin feature/fooBar`)
6. Create a new Pull Request

_NOTE:_ Commits and pull requests to FINOS repositories will only be accepted from those contributors with an active, executed Individual Contributor License Agreement (ICLA) with FINOS OR who are covered under an existing and active Corporate Contribution License Agreement (CCLA) executed with FINOS. Commits from individuals not covered under an ICLA or CCLA will be flagged and blocked by the FINOS Clabot tool (or [EasyCLA](https://community.finos.org/docs/governance/Software-Projects/easycla)). Please note that some CCLAs require individuals/employees to be explicitly named on the CCLA.

* Unsure if you are covered under an existing CCLA? Email [help@finos.org](mailto:help@finos.org)*


## Get in touch with the Rune Common Team

Get in touch with the Rune Common team by creating a [GitHub issue](https://github.com/REGnosys/rune-common/issues/new) and labelling it with "help wanted".

We encourage the community to get in touch via the [FINOS Slack](https://www.finos.org/blog/finos-announces-new-community-slack).

## License

Copyright 2024 REGnosys

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
