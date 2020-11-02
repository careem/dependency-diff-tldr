package com.careem.gradle.dependencies

fun tldr(old: String, new: String): String {
  val oldPaths = mapize(old)
  val newPaths = mapize(new)

  val removedMap = mapDifference(oldPaths, newPaths)
  val addedMap = mapDifference(newPaths, oldPaths)
  val (added, removed, upgraded) = partitionDifferences(removedMap, addedMap)

  return buildString {
    writeList("New Dependencies", added)
    writeList("Removed Dependencies", removed, added.isNotEmpty())
    writeList("Upgraded Dependencies", upgraded, removed.isNotEmpty() || added.isNotEmpty())
  }
}

private fun mapize(deps: String): Map<String, String> {
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
      artifact to canonicalVersionInfo
    }
    .toMap()
}

private fun <K, V> mapDifference(from: Map<K, V>, to: Map<K, V>): Map<K, V> {
  val result = mutableMapOf<K, V>()
  for ((key, value) in from) {
    val valueInTo = to[key]
    if (valueInTo == null || valueInTo != value) {
      result[key] = value
    }
  }
  return result
}

data class VersionedDependency(val artifact: String, val version: String)
data class VersionDifferences(
  val additions: List<VersionedDependency>,
  val removals: List<VersionedDependency>,
  val upgrades: List<VersionedDependency>
)

private fun partitionDifferences(
  removed: Map<String, String>,
  added: Map<String, String>
): VersionDifferences {
  val additions = mutableListOf<VersionedDependency>()
  val upgrades = mutableListOf<VersionedDependency>()
  val removals = mutableListOf<VersionedDependency>()

  val mutableRemovedMap = removed.toMutableMap()
  for ((artifact, version) in added) {
    val removedVersion = mutableRemovedMap[artifact]
    when {
      removedVersion != null -> {
        mutableRemovedMap.remove(artifact)
        upgrades.add(
          VersionedDependency(artifact, "$version, (upgraded from $removedVersion)"))
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