package com.careem.gradle.dependencies.mismatched

import com.careem.gradle.dependencies.dependencyDifferences
import com.careem.gradle.dependencies.common.parser.Dependency
import com.careem.gradle.dependencies.common.parser.parseDependencyTree

fun mismatchedVersions(old: String, new: String, watchedLibraries: List<String>, collapse: List<String>): String {
  val (additions, _, upgrades) = dependencyDifferences(old, new)
  val total = additions + upgrades
  return if (total.isNotEmpty() && watchedLibraries.isNotEmpty()) {
    val tree = parseDependencyTree(new)
    val upgradedDependencies = total.mapNotNull { tree.findDependency(it.artifact) }
    val updatesList = watchedLibraries.map { watchedArtifact ->
      watchedArtifact to upgradedDependencies.mapNotNull { dep ->
        val matching = dep.findDependency(watchedArtifact)
        if (matching != null && matching.requestedVersion != matching.resolvedVersion) {
          dep to matching
        } else {
          null
        }
      }
    }

    buildString {
      updatesList.forEach { (watchedArtifact, updates) ->
        if (updates.isNotEmpty()) {
          val groupedUpdates = updates.groupBy { it.second.requestedVersion }
            .map { (version, deps) ->
              val groupedVersionedDependencies = deps.groupBy { it.first.group }
                .flatMap { (group, deps) ->
                  if (deps.size > 1 && group in collapse) {
                    val firstDependency = deps.first()
                    val groupedDep = firstDependency.first.copy(name = "*")
                    listOf(groupedDep to firstDependency.second)
                  } else {
                    deps
                  }
                }
              version to groupedVersionedDependencies
            }

          groupedUpdates.forEach { (version, deps) ->
            appendLine("$watchedArtifact:$version:")
            deps.forEach { (dep, matching) ->
              appendLine("    ${dep.artifact} is requesting ${matching.requestedVersion} instead of ${matching.resolvedVersion}")
            }
            appendLine()
          }
        }
      }
    }
  } else {
    ""
  }
}

private fun List<Dependency>.findDependency(artifactId: String): Dependency? {
  return firstNotNullOfOrNull { it.findDependency(artifactId) }
}

private fun Dependency.findDependency(artifactId: String): Dependency? {
  return if (artifactId == artifact) {
    this
  } else {
    children.firstNotNullOfOrNull { it.findDependency(artifactId) }
  }
}