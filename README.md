# dependency-diff-tldr

This is a fork of [Jake Wharton's](https://jakewharton.com) [dependency-tree-diff](https://github.com/JakeWharton/dependency-tree-diff) project.

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
$ ./dependency-diff-tldr.jar old.txt new.txt
```

Until the above actually works, can run:

```bash
./gradlew build
java -jar build/libs/dependency-diff-tldr-r8.jar old.txt new.txt
```
