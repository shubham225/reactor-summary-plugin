<p align="center"><img src="docs/logo-light.svg" width="96" alt="Reactor Summary logo"></p>

<h1 align="center">Reactor Summary Plugin</h1>

<p align="center">
  A Gradle plugin that prints a Maven-style "Reactor Summary" for multi-module builds:<br>
  status and elapsed time for every module, at the end of the build.
</p>

<p align="center">
  <a href="https://plugins.gradle.org/plugin/io.github.shubham225.reactor-summary"><img alt="Gradle Plugin Portal" src="https://img.shields.io/gradle-plugin-portal/v/io.github.shubham225.reactor-summary?label=Gradle%20Plugin%20Portal"></a>
  <a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-blue.svg"></a>
  <a href="https://github.com/shubham225/reactor-summary-plugin/commits"><img alt="GitHub Last Commit" src="https://img.shields.io/github/last-commit/shubham225/reactor-summary-plugin"></a>
</p>

---

Maven prints a table at the end of a multi-module build showing which modules succeeded, failed or were skipped, and how long each took. Gradle doesn't. This plugin adds that table.

```
----------------------------------------------------------------------
Reactor Summary
----------------------------------------------------------------------
my-app (root) ..................................... UP-TO-DATE [0.010 s]
common ............................................ UP-TO-DATE [0.022 s]
core .............................................. SUCCESS    [1.473 s]
server ............................................ UP-TO-DATE [0.004 s]
web ............................................... SUCCESS    [9.377 s]
----------------------------------------------------------------------
Modules: 5  2 success  3 up-to-date  0 skipped  0 failed
Total time: 10.921 s
----------------------------------------------------------------------
```

## Features

- One line per module with its status and elapsed time
- Totals for each status, plus total build time
- Colored output, auto-detected or set explicitly
- Works with the Gradle configuration cache
- Applied once on the root project and collects results from every module
- Prints the summary on failed builds too

## Requirements

- Gradle **7.4** or newer
- Java 8 or newer to run the build (the plugin is compiled for Java 8)
- JDK 17 to build the plugin from source

## Installation

The plugin id is `io.github.shubham225.reactor-summary`.

### Option A: Gradle Plugin Portal

In the root `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.shubham225.reactor-summary") version "0.1.0"
}
```

### Option B: use the jar directly

Build the jar (see [Building from source](#building-from-source)), copy it to a `libs/` folder in your project, and add this to the **root** `build.gradle.kts`:

```kotlin
buildscript {
    dependencies {
        classpath(files("libs/reactor-summary-plugin-0.1.0.jar"))
    }
}

apply<io.github.shubham225.ReactorSummaryPlugin>()
```

Or apply it by id:

```kotlin
apply(plugin = "io.github.shubham225.reactor-summary")
```

> Apply the plugin at the top level of the root project. Don't put it inside `subprojects { }` or `allprojects { }`.

### Option C: Maven Local

Publish the plugin locally:

```bash
./gradlew publishToMavenLocal
```

In your project's `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
```

In the root `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.shubham225.reactor-summary") version "0.1.0"
}
```

### Option D: all your builds at once

Load the jar from a Gradle init script, for example `~/.gradle/init.d/reactor-summary.init.gradle.kts`:

```kotlin
initscript {
    dependencies {
        classpath(files("/absolute/path/to/reactor-summary-plugin-0.1.0.jar"))
    }
}

rootProject {
    apply<io.github.shubham225.ReactorSummaryPlugin>()
}
```

## Configuration

Set these as project properties (`-P` on the command line) or in `gradle.properties`.

| Property | Values | Default | Description |
|---|---|---|---|
| `reactorSummary.color` | `true`, `false` | auto | Force colored output on or off. In auto mode, color is on for rich consoles and off for plain consoles, when `NO_COLOR` is set, or when `TERM` is `dumb`. |
| `reactorSummary.delayMs` | milliseconds, `0` or more | `10` | Delay before the summary is printed after the build ends. See [Where the summary appears](#where-the-summary-appears). |

Examples:

```bash
./gradlew build -PreactorSummary.color=false
./gradlew build -PreactorSummary.delayMs=0
```

```properties
# gradle.properties
reactorSummary.color=true
reactorSummary.delayMs=10
```

IntelliJ's run console usually has no `TERM` set, so auto-detection may turn colors off there. Set `reactorSummary.color=true` to enable them.

## How it works

The plugin registers a Gradle build service that listens to task-completion events from every project in the build. Results are grouped by module and a status is chosen for each one:

| Status | Meaning |
|---|---|
| `FAILED` | At least one task in the module failed |
| `SUCCESS` | At least one task in the module actually ran, and none failed |
| `UP-TO-DATE` | Every task was up-to-date or restored from the build cache |
| `SKIPPED` | Tasks were skipped (for example `onlyIf` conditions or no source) |

The time shown for a module is the wall-clock time from its first task start to its last task end. With `--parallel` modules can overlap, so the times won't add up to the total build time.

Modules in which no task was scheduled don't appear in the summary.

## Where the summary appears

Gradle has no public API for running code after its own end-of-build report (the `FAILURE:` block and the `BUILD FAILED` / `BUILD SUCCESSFUL` line). The plugin prints the summary when the build finishes, after a short delay set by `reactorSummary.delayMs`.

- With `reactorSummary.delayMs=0` the summary prints immediately, which is **above** Gradle's final report.
- With a small positive delay (the default is `10` ms) the plugin tries to print it **after** that report. This depends on timing and may differ between machines, Gradle versions and consoles.
- If the summary doesn't show up, or shows up above the report when you want it below, try other values (for example `50` or `100`), or set `0` for a reliable print before the report.

## Building from source

```bash
git clone https://github.com/shubham225/reactor-summary-plugin.git
cd reactor-summary-plugin
./gradlew build
```

The jar is written to `plugin/build/libs/`. On Windows, use `gradlew.bat`.

After replacing the jar in a project, stop the daemon so Gradle reloads it:

```bash
./gradlew --stop
```

## Troubleshooting

**The plugin doesn't print anything**

- Check that the plugin is applied on the **root** project, outside `subprojects { }`.
- Check that the jar in your `libs/` folder is the freshly built one, then run `./gradlew --stop`.
- Try `-PreactorSummary.delayMs=0`. If that prints the summary, the default delay is too long for your setup, and a smaller or zero value will work.

**`No type arguments expected for fun PluginAware.apply(...)`**

The jar is not on the build script classpath. Put the `buildscript { dependencies { classpath(...) } }` block at the top level of the root `build.gradle.kts`, not inside `subprojects { }`.

**Raw codes like `[32m` appear in the output**

Your console doesn't render ANSI colors. Set `reactorSummary.color=false`.

## Contributing

Bug reports and pull requests are welcome. When opening an issue, please include:

- Gradle version and Java version
- Operating system and console (terminal, IntelliJ, CI)
- A minimal example or steps to reproduce
- The relevant build output

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).