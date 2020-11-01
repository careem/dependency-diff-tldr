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

private fun partitionDifferences(removed: Map<String, String>, added: Map<String, String>):
    Triple<List<Pair<String, String>>, List<Pair<String, String>>, List<Pair<String, String>>> {

  val additions = mutableListOf<Pair<String, String>>()
  val upgrades = mutableListOf<Pair<String, String>>()
  val removals = mutableListOf<Pair<String, String>>()

  val mutableRemovedMap = removed.toMutableMap()
  for ((artifact, version) in added) {
    val removedVersion = mutableRemovedMap[artifact]
    when {
      removedVersion != null -> {
        mutableRemovedMap.remove(artifact)
        upgrades.add(artifact to "$version, (upgraded from $removedVersion)")
      }
      else -> {
        additions.add(artifact to version)
      }
    }
  }

  for ((artifact, version) in mutableRemovedMap) {
    removals.add(artifact to version)
  }
  return Triple(
    additions.sortedBy { it.first },
    removals.sortedBy { it.first },
    upgrades.sortedBy { it.first }
  )
}

private fun StringBuilder.writeList(title: String, list: List<Pair<String, String>>, newLineBeforeTitle: Boolean = false) {
  if (list.isNotEmpty()) {
    if (newLineBeforeTitle) {
      append("\n")
    }
    append(title)
    append("\n")
    list.forEach {
      append(it.first)
      if (it.second.isNotBlank()) {
        append(":")
        append(it.second)
      }
      append("\n")
    }
  }
}