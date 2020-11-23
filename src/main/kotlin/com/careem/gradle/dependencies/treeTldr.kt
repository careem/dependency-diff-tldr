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
    .dropWhile { !it.startsWith("+--- ") }
    .takeWhile { it.isNotEmpty() }
    .map {
      val artifactStart = it.indexOf("--- ")
      val artifactBase = it.substring(artifactStart + 4)
      val noVersion = artifactBase.indexOf(':') == artifactBase.lastIndexOf(':')
      val artifact = when {
        artifactBase.startsWith("project ") -> {
          artifactBase
        }
        noVersion -> {
          artifactBase.substringBefore(' ')
        }
        else -> {
          artifactBase.substringBeforeLast(':')
        }
      }
      val versionInfo = it.substringAfterLast(':')
      val canonicalVersionInfo = when {
        "project" in artifact -> ""
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