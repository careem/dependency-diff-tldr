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

package com.careem.gradle.dependencies.parser

data class Dependency(
    val group: String,
    val name: String,
    val resolvedVersion: String,
    val requestedVersion: String,
    val children: List<Dependency> = emptyList()
) {
    val artifact: String = "$group:$name"
}

fun parseDependencyTree(dependenciesFile: String): List<Dependency> {
    val dependencyLines = dependenciesFile.split("\n")
        .dropWhile { !it.startsWith("+--- ") }
        .takeWhile { it.isNotEmpty() }
    return parseDependenciesForIndent(dependencyLines, 1)
}

private fun parseDependenciesForIndent(dependencyLines: List<String>, indent: Int): List<Dependency> {
    val linesForIndent = dependencyLines
        .takeWhile { it.indexOf("--- ") >= indent }
        // remove dependency constraints, since they're not actual dependencies
        .filter { "(c)" !in it }

    return linesForIndent
        .mapIndexedNotNull { index, dependencyLine ->
            val artifactStart = dependencyLine.indexOf("--- ")
            if (artifactStart == indent) {
                parseDependency(dependencyLine, linesForIndent.safeSublistFrom(index + 1))
            } else {
                null
            }
        }
}

private fun parseDependency(dependencyLine: String, dependencyLines: List<String>): Dependency {
    val dependency = parseDependency(dependencyLine)
    val indent = dependencyLine.indexOf("--- ")
    val nextIndent = dependencyLines.firstOrNull()?.indexOf("--- ") ?: -1

    return if (indent < nextIndent) {
        // this dependency has sub-dependencies
        val children = parseDependenciesForIndent(dependencyLines, nextIndent)
        dependency.copy(children = children)
    } else {
        // leaf dependency
        dependency
    }
}

private fun parseDependency(dependencyLine: String): Dependency {
    val artifactStart = dependencyLine.indexOf("--- ")
    val artifactBase = dependencyLine.substring(artifactStart + 4)
    val noVersionInBase = artifactBase.indexOf(':') == artifactBase.lastIndexOf(':')
    val artifact = when {
        // example: project :lib:base (*)
        artifactBase.startsWith("project ") -> {
            artifactBase
        }
        // example: com.careem.superapp.lib:base -> 1.26.0 (*)
        noVersionInBase -> {
            artifactBase.substringBefore(' ')
        }
        else -> {
            artifactBase.substringBeforeLast(':')
        }
    }

    val versionPortion = dependencyLine.substringAfterLast(':')
    val requestedVersionInfo = when {
        "project :" in versionPortion -> ""
        "->" in versionPortion -> versionPortion.substringBefore(" ->")
        "(*)" in versionPortion -> versionPortion.substringBefore(" (")
        else -> versionPortion
    }

    val resolvedVersion = if ("->" in versionPortion) {
        versionPortion.substringAfter("-> ").substringBefore(" (")
    } else {
        requestedVersionInfo
    }

    return Dependency(
        group = artifact.substringBefore(':').trim(),
        name = artifact.substringAfter(':').trim(),
        resolvedVersion = resolvedVersion,
        requestedVersion = requestedVersionInfo
    )
}

private fun List<String>.safeSublistFrom(index: Int): List<String> {
    val itemCount = size
    return if (index < itemCount) {
        subList(index, itemCount)
    } else {
        emptyList()
    }
}