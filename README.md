# dependency-diff-tldr

This started off being heavily based on  [Jake Wharton's](https://jakewharton.com) [dependency-tree-diff](https://github.com/JakeWharton/dependency-tree-diff) project.

Initially, it started as an attempt to add a summary mode to dependency-tree-diff. However, it became apparent that a summary view should be an auxiliary tool to be used in addition to dependency-tree-diff and not a replacement for it, especially since it loses valuable information as to where and why a dependency changed. Because of the numerous changes in code, this is a separate repository for now, and there exists a bunch of common code (ex the gradle scripts, main class, rules, and some of the parsing logic and readme are the same).

This is a simple diff tool for the output of Gradle's `dependencies` task to provide a list of additions, removals, and upgraded packages.

## Why

Comparing two SuperApp releases with dependency-tree-diff, we see something
like this:

```sh
❯ dependency-tree-diff release-10431.txt master.txt | wc -l
    5058

❯ dependency-tree-diff release-10431.txt master.txt | head
```

outputs:

```diff
 +--- com.google.firebase:firebase-perf -> 19.0.9
-|    \--- com.google.firebase:firebase-config:19.2.0
-|         +--- com.google.android.gms:play-services-tasks:17.0.2 -> 17.2.0 (*)
-|         +--- com.google.firebase:firebase-abt:19.1.0
-|         |    +--- com.google.android.gms:play-services-basement:17.0.0 -> 17.4.0 (*)
-|         |    +--- com.google.firebase:firebase-common:19.3.0 -> 19.3.1 (*)
-|         |    +--- com.google.firebase:firebase-components:16.0.0 (*)
-|         |    +--- com.google.firebase:firebase-measurement-connector:18.0.0
-|         |    |    \--- com.google.android.gms:play-services-basement:17.0.0 -> 17.4.0 (*)
-|         |    \--- com.google.protobuf:protobuf-javalite:3.11.0
```

While the breakdown of this is really useful and we know exactly why every dependency has been updated, it's 5058 lines, which makes it really difficult to find out what actually changed.

Compare this to dependency-diff-tldr:

```sh
❯ java -jar build/libs/dependency-diff-tldr-r8.jar release-10431.txt master.txt | wc -l
     112

❯ java -jar build/libs/dependency-diff-tldr-r8.jar release-10431.txt master.txt | head
```

Results in the following output:

```
New Dependencies
com.careem.identity:user-profile:0.0.63
project :miniapp:thirdparty

Upgraded Dependencies
androidx.annotation:annotation-experimental:1.0.0, (upgraded from 1.0.0-rc01)
androidx.constraintlayout:constraintlayout:2.0.2, (upgraded from 2.0.1)
androidx.constraintlayout:constraintlayout-solver:2.0.2, (upgraded from 2.0.1)
com.careem.care:helpcenter:0.0.17, (upgraded from 0.0.16)
com.careem.chat:core:3.1.6, (upgraded from 3.1.4)
```

## Usage

The tool parses the output of Gradle's `dependencies` task. Specify `--configuration <name>` when running the task so that only a single tree will be shown.

```bash
$ ./gradlew :app:dependencies --configuration releaseRuntimeClasspath > old.txt
$ # Update a dependency...
$ ./gradlew :app:dependencies --configuration releaseRuntimeClasspath > new.txt
$ java -jar build/dependency-diff-tldr.jar old.txt new.txt
```

## Building

```bash
# build
./gradlew build

# run tests
./gradlew test

# run it
java -jar build/dependency-diff-tldr.jar old.txt new.txt
```

## Flags

* `-s, --side-efects` - explicitly call out the side effects of dependency
upgrades (ex upgrading Firebase here also updates it in ...)
* `-c, --collapse` - collapse a set of grouped dependencies. For example,
  passing a collapse of `com.careem.care` would result in collapsing updates
  of `com.careem.care:dep1` and `com.careem.care:dep2` to just
  `com.careem.care:*`. This flag is repeatable.

## Other Utilities

### Mismatched Dependency Finder

Sometimes, you have a library (for example, `androidx.appcompat:appcompat`)
and want to make sure that all updates that transitively use this library are
on the currently resolved version. This tool helps by listing out updates and
additions in this change set that use a version of that library that is
transitively upgraded.

For example, if our main app uses `androidx.appcompat:appcompat:1.6.1`, using
this command and passing `androidx.appcompat:appcompat` as a `-d` parameter
would highlight all transitive usages on a version that is not 1.6.1 that
resolves to 1.6.1.

```bash
java -cp build/dependency-diff-tldr.jar com.careem.gradle.dependencies.mismatched.MismatchedVersionFinder -d androidx.appcompat:appcompat old.txt new.txt
```

Would result in output like:

```
androidx.appcompat:appcompat:1.5.1:
    com.careem.kodelean:recyclerview-compose is requesting 1.5.1 instead of 1.6.1
    com.careem.kodelean:recyclerview-viewbinding is requesting 1.5.1 instead of 1.6.1
    com.careem.kodelean:viewbinding-lifecycle is requesting 1.5.1 instead of 1.6.1

androidx.appcompat:appcompat:1.3.0:
    com.careem.motcore:feature-dynamic_widget is requesting 1.3.0 instead of 1.6.1
```

Note that the `-c` flag would help simplify this output, such that running:

```bash
java -cp build/dependency-diff-tldr.jar com.careem.gradle.dependencies.mismatched.MismatchedVersionFinder -c com.careem.kodelean -d androidx.appcompat:appcompat old.txt new.txt
```

Would result in output like:

```
androidx.appcompat:appcompat:1.5.1:
    com.careem.kodelean:* is requesting 1.5.1 instead of 1.6.1

androidx.appcompat:appcompat:1.3.0:
    com.careem.motcore:feature-dynamic_widget is requesting 1.3.0 instead of 1.6.1
```

#### Flags

* `-d, --dependency` - watch a library. This flag is repeatable.
* `-c, --collapse` - collapse a set of grouped dependencies. This flag is repeatable.

### Illegal Dependencies Finder

There are often times when a certain specific version of a dependency should not be used,
due for example to it being broken or having some sort of issues. This tool checks to see
if any of a set of illegal dependencies are used, and flags their usages.

```bash
java -cp build/dependency-diff-tldr.jar com.careem.gradle.dependencies.denylist.DenyListEnforcer \
    dependencies.txt \
    -d com.careem.superapp.test:example:1.62.0 \
    -d com.careem.superapp.test:another:1.62.0
```

Would result in output like:

```
Illegal dependency: com.careem.superapp.test:example:1.62.0:
    com.careem.superapp.test:example (1.62.0)

    project:lib:base (base)
        project:lib:analytics-impl (analytics-impl)
            com.careem.superapp.test:example (1.62.0)
```

Alternatively, you can pass in `-df <illegal.txt>`, where `illegal.txt` would contain
one line per illegal dependency.

## Disclaimer

This project may contain experimental code and may not be ready for general use. Support and/or new releases may be limited.

## License

Copyright Careem, an Uber Technologies Inc. company

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
