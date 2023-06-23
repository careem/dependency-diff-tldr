package com.careem.gradle.dependencies.mismatched

import com.careem.gradle.dependencies.common.parser.Dependency
import com.careem.gradle.dependencies.common.parser.filterMatches
import com.careem.gradle.dependencies.common.parser.parseDependencyTree
import com.careem.gradle.dependencies.dependencyDifferences

/**
 * Identify outdated dependencies (if any) from a list of watched libraries.
 * This method checks if any of the added or updated dependencies between old and new contain
 * an outdated version of one of the watched libraries, and informs us if so.
 */
fun mismatchedVersions(old: String, new: String, watchedLibraries: List<String>, collapse: List<String>): String {
  val (additions, _, upgrades) = dependencyDifferences(old, new)
  val total = additions + upgrades
  return if (total.isNotEmpty() && watchedLibraries.isNotEmpty()) {
    val tree = parseDependencyTree(new)
    // find the actual added or upgraded dependencies
    val upgradedDependencies = total.mapNotNull { tree.findDependency(it.artifact) }

    // let's take each watched library one at a time
    val updatesList = watchedLibraries.map { watchedArtifact ->
      watchedArtifact to upgradedDependencies.mapNotNull { dep ->
        if (dep.artifact != watchedArtifact) {
          // get any child dependencies of the added/upgraded dependency that has an older version of the watched library
          val matching =
            dep.filterMatches { it.artifact == watchedArtifact && it.resolvedVersion != it.requestedVersion }
          // whereas matching is from the root all the way down, this just finds the actual watched artifact as well
          val actualWatchedDependencyLeaf = matching?.findDependency(watchedArtifact)
          if (matching != null && actualWatchedDependencyLeaf != null) {
            dep to DependencyMatch(matching, actualWatchedDependencyLeaf)
          } else {
            null
          }
        } else {
          null
        }
      }
    }

    buildString {
      updatesList.forEach { (watchedArtifact, updates) ->
        if (updates.isNotEmpty()) {
          val groupedUpdates = updates.groupBy { it.second.match.requestedVersion }
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
              appendLine("    ${dep.artifact} is requesting ${matching.match.requestedVersion} instead of ${matching.match.resolvedVersion}")
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

data class DependencyMatch(val dependencyRoot: Dependency, val match: Dependency)

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