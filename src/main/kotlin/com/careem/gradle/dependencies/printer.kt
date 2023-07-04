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
enum class OutputType{
    PLAIN, JSON
}

fun String.toOutputType(): OutputType = when(this){
    "json" -> OutputType.JSON
    else -> OutputType.PLAIN
}


fun VersionDifferences.toString(collapse: List<String>, outputType: OutputType): String {
    return when(outputType){
        OutputType.PLAIN -> writePlain(this, collapse)
        OutputType.JSON -> writeJson(this, collapse)
    }
}

private fun writePlain(versionDifferences: VersionDifferences, collapse: List<String>): String = buildString {
    val (added, removed, upgraded) = versionDifferences
    writeList("New Dependencies", added)
    writeList("Removed Dependencies", removed, added.isNotEmpty())
    writeList("Upgraded Dependencies", collapseDependencies(upgraded, collapse),
        removed.isNotEmpty() || added.isNotEmpty())
}

private fun writeJson(versionDifferences: VersionDifferences, collapse: List<String>): String = buildString {
    val (added, removed, upgraded) = versionDifferences
    append("{")
    writeJsonObject("new_dependencies", added, trailingComma = true)
    writeJsonObject("removed_dependencies", removed,  trailingComma = true)
    writeJsonObject("upgraded_dependencies", collapseDependencies(upgraded, collapse))
    append("}")
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
                it.alternativeVersion?.let { altVersion -> append(", (changed from $altVersion)") }
            }
            append("\n")
        }
    }
}

private fun StringBuilder.writeJsonObject(key: String, list: List<VersionedDependency>, trailingComma: Boolean = false) {
    append("\"$key\":[")
    if (list.isNotEmpty()) {
        list.forEachIndexed { index, versionedDependency ->
            append(writeJsonVersionedDependency(versionedDependency))
            if(index + 1 < list.size){
                append(",")
            }
        }
    }
    append("]")
    if(trailingComma){
        append(",")
    }
}

private fun writeJsonVersionedDependency(versionedDependency: VersionedDependency): String {
    return "{\"artifact\":\"${versionedDependency.artifact}\",\"version\":\"${versionedDependency.version}\",\"other_version\":\"${versionedDependency.alternativeVersion}\"}"
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

