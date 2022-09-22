/*
* Copyright Careem, an Uber Technologies Inc. company
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.careem.gradle.dependencies

fun tldr(old: String, new: String, collapse: List<String>): String {
  val oldDependencies = extractDependencies(old)
  val newDependencies = extractDependencies(new)

  val addedDependencies = newDependencies - oldDependencies
  val removedDependencies = oldDependencies - newDependencies
  val (added, removed, upgraded) = partitionDifferences(removedDependencies, addedDependencies)

  return buildString {
    writeList("New Dependencies", added)
    writeList("Removed Dependencies", removed, added.isNotEmpty())
    writeList("Upgraded Dependencies", collapseDependencies(upgraded, collapse),
      removed.isNotEmpty() || added.isNotEmpty())
  }
}

private fun extractDependencies(deps: String): Set<VersionedDependency> {
  return deps.split('\n')
    .asSequence()
    .dropWhile { !it.startsWith("+--- ") }
    .takeWhile { it.isNotEmpty() }
    .map {
      val artifactStart = it.indexOf("--- ")
      it.substring(artifactStart + 4)
    }
    // ignore added or removed local project modules
    .filter { !it.startsWith("project ") }
    .map { artifactBase ->
      val noVersion = artifactBase.indexOf(':') == artifactBase.lastIndexOf(':')
      val artifact = when {
        noVersion -> {
          artifactBase.substringBefore(' ')
        }
        else -> {
          artifactBase.substringBeforeLast(':')
        }
      }
      val versionInfo = artifactBase.substringAfterLast(':')
      val canonicalVersionInfo = when {
        "->" in versionInfo -> versionInfo.substringAfter("-> ").substringBefore(' ')
        "(*)" in versionInfo || "(c)" in versionInfo -> versionInfo.substringBefore(" (")
        else -> versionInfo
      }
      VersionedDependency(artifact, canonicalVersionInfo)
    }
    .toSet()
}

private fun collapseDependencies(
  dependencies: List<VersionedDependency>,
  collapses: List<String>
): List<VersionedDependency> {
  val add = mutableSetOf<VersionedDependency>()
  val remove = mutableSetOf<VersionedDependency>()

  collapses.forEach { collapse ->
    val matchingToCollapse = dependencies.filter { it.artifact.startsWith(collapse) }
    val versions = matchingToCollapse.map { it.version }.toSet().size
    if (versions == 1) {
      remove.addAll(matchingToCollapse)
      add.add(matchingToCollapse[0].copy(artifact = collapse))
    }
  }

  return if (add.isNotEmpty()) {
    (dependencies - remove) + add
  } else {
    dependencies
  }
}

data class VersionedDependency(val artifact: String, val version: String)
data class VersionDifferences(
  val additions: List<VersionedDependency>,
  val removals: List<VersionedDependency>,
  val upgrades: List<VersionedDependency>
)

private fun partitionDifferences(
  removed: Set<VersionedDependency>,
  added: Set<VersionedDependency>
): VersionDifferences {
  val additions = mutableListOf<VersionedDependency>()
  val upgrades = mutableListOf<VersionedDependency>()
  val removals = mutableListOf<VersionedDependency>()

  val mutableRemovedMap = removed.map { it.artifact to it.version }.toMap(mutableMapOf())
  for ((artifact, version) in added) {
    val removedVersion = mutableRemovedMap[artifact]
    when {
      removedVersion != null -> {
        mutableRemovedMap.remove(artifact)
        upgrades.add(
          VersionedDependency(artifact, "$version, (changed from $removedVersion)"))
      }
      else -> {
        additions.add(VersionedDependency(artifact, version))
      }
    }
  }

  for ((artifact, version) in mutableRemovedMap) {
    removals.add(VersionedDependency(artifact, version))
  }
  return VersionDifferences(
    additions.sortedBy { it.artifact },
    removals.sortedBy { it.artifact },
    upgrades.sortedBy { it.artifact }
  )
}

private fun StringBuilder.writeList(title: String, list: List<VersionedDependency>, newLineBeforeTitle: Boolean = false) {
  if (list.isNotEmpty()) {
    if (newLineBeforeTitle) {
      append("\n")
    }
    append(title)
    append("\n")
    list.forEach {
      append(it.artifact)
      if (it.version.isNotBlank()) {
        append(":")
        append(it.version)
      }
      append("\n")
    }
  }
}