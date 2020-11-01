# Dependency Tree Tldr

This is a fork of [Jake Wharton's](https://jakewharton.com)
[dependency-tree-diff](https://github.com/JakeWharton/dependency-tree-diff)
project.

Initially, it started as an attempt to add a summary mode to
dependency-tree-diff. However, it became apparent that a summary view should
be an auxiliary tool to be used in addition to dependency-tree-diff and not a replacement for it, especially since it loses valuable information as to where and why a dependency changed. Because of the numerous changes in code, this is a separate repository for now, and there exists a bunch of common code (ex the main class is the same, along with some parsing logic and parts of this readme).

This is a simple diff tool for the output of Gradle's `dependencies` task to
provide a list of additions, removals, and upgraded packages.


## Usage

The tool parses the output of Gradle's `dependencies` task. Specify `--configuration <name>` when running the task so that only a single tree will be shown.

```bash
$ ./gradlew :app:dependencies --configuration releaseRuntimeClasspath > old.txt
$ # Update a dependency...
$ ./gradlew :app:dependencies --configuration releaseRuntimeClasspath > new.txt
$ ./dependency-tree-tldr.jar old.txt new.txt
```

Until the above actually works, can run:

```bash
./gradlew run --args "old.txt new.txt"
```
