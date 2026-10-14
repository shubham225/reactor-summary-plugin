# Reactor Summary Plugin
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.shubham225.reactor-summery?label=Gradle%20Plugin%20Portal)](https://plugins.gradle.org/plugin/io.github.shubham225.reactor-summery)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/shubham225/reactor-summary-plugin)](https://github.com/shubham225/reactor-summary-plugin/stargazers)
[![GitHub issues](https://img.shields.io/github/issues/shubham225/reactor-summary-plugin)](https://github.com/shubham225/reactor-summary-plugin/issues)
[![GitHub last commit](https://img.shields.io/github/last-commit/shubham225/reactor-summary-plugin)](https://github.com/shubham225/reactor-summary-plugin/commits)

A Gradle plugin that prints a concise summary of task outcomes for a multi-project Gradle build when the build finishes.

The summary helps you quickly see which modules succeeded, were up-to-date, were skipped, or failed—without scrolling through the entire build log.

## Features

- Prints a summary at the end of the build.
- Aggregates task outcomes by Gradle project/module.
- Shows a status for each module: `SUCCESS`, `UP-TO-DATE`, `SKIPPED`, or `FAILED`.
- Displays elapsed task time per module and total build time.
- Shows aggregate counts for modules and their statuses.
- Supports colored output, with automatic detection and an explicit configuration option.
- Registers once at the root project, so results are collected across the build.

## Example output

The exact timings and module names depend on your build.

```text
----------------------------------------------------------------------
Reactor Summary
----------------------------------------------------------------------
my-app (root) ................................ SUCCESS    [1.234 s]
core ......................................... SUCCESS    [2.345 s]
service ...................................... UP-TO-DATE [0.123 s]
integration-tests ............................ SKIPPED    [0.000 s]
----------------------------------------------------------------------
Modules: 4  2 success  1 up-to-date  1 skipped  0 failed
Total time: 4.567 s
----------------------------------------------------------------------
```

## Requirements

- A Gradle version that supports the public Flow API used by this plugin.
- A Java 11 toolchain to build the plugin.

Check the Gradle version used by your project and the compatibility of the plugin before adopting it in a production build.

## Installation

The plugin ID is `io.github.shubham225.reactor-summery`.

### If the plugin is published to the Gradle Plugin Portal

Add the plugin to the root project's `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.shubham225.reactor-summery") version "0.1.0"
}
```

For Groovy DSL (`build.gradle`):

```groovy
plugins {
    id 'io.github.shubham225.reactor-summery' version '0.1.0'
}
```

**Publishing note:** The version declaration above assumes version `0.1.0` has been published to a plugin repository configured for your build. If it has not been published yet, use the local development instructions below.

### Try it locally before publication

Clone this repository and build the plugin:

```bash
git clone https://github.com/shubham225/reactor-summary-plugin.git
cd reactor-summary-plugin
./gradlew build
```

To test it in another project before publication, publish it to Maven Local if the project is configured for that workflow, then add `mavenLocal()` to the plugin-management repositories and use the plugin's published marker coordinates. Alternatively, use Gradle's composite-build support (`includeBuild`) to include the plugin build directly. Follow Gradle's documentation for the chosen approach.

## Configuration

### Disable or enable colored output

By default, the plugin attempts to detect whether colored console output is appropriate.

You can explicitly enable colors:

```bash
./gradlew build -PreactorSummary.color=true
```

Or disable colors:

```bash
./gradlew build -PreactorSummary.color=false
```

You can also set the property in your root `gradle.properties` file:

```properties
reactorSummary.color=false
```

Valid explicit values are `true` and `false`.

## How it works

The plugin registers a shared build service to observe task-completion events, groups task results by project path, and prints the summary when the build finishes.

The status is derived from observed task results:

- `FAILED`: at least one task in the module failed.
- `SUCCESS`: at least one task executed successfully and the module had no failures.
- `UP-TO-DATE`: tasks were up-to-date or loaded from cache, with no executed, skipped, or failed task taking precedence.
- `SKIPPED`: no higher-priority status was observed.

The module timing shown is the interval between the earliest observed task start and the latest observed task end for that module. It is not necessarily the sum of individual task durations, because tasks can overlap.

## Building and testing

Run the project's verification tasks:

```bash
./gradlew build
```

On Windows, use:

```bat
gradlew.bat build
```

## Contributing

Bug reports, ideas, and pull requests are welcome. When opening an issue, include:

- Gradle version and Java version
- Operating system
- A minimal example or steps to reproduce
- Relevant build output (with secrets removed)

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
