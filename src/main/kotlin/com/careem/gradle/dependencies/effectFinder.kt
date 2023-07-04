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

import com.careem.gradle.dependencies.common.parser.Dependency
import com.careem.gradle.dependencies.common.parser.parseDependencyTree

fun upgradeEffects(old: String, new: String, collapseKeys: List<String>): String {
    val (_, _, upgrades) = dependencyDifferences(old, new)
    return if (upgrades.isNotEmpty()) {
        val tree = parseDependencyTree(new)
        val sideEffects = upgrades
            .map { updatedDependency ->
                // these are the dependencies that were transparently upgraded due to this upgrade
                updatedDependency to tree.mapNotNull { treeDependency ->
                    treeDependency.filterMatches {
                        it.matches(updatedDependency.artifact) &&
                                it.requestedVersion != it.resolvedVersion
                    }
                }
            }
            .map { it.first to it.second.map { dep -> dep.filterMatches { match -> match.group != "project" } ?: dep } }
            .filter { it.second.isNotEmpty() }

        // side effects are grouped by the dependency that was upgraded
        // collapse dependencies with the same group with each other
        // so that the output is more readable
        val collapsedSideEffects = sideEffects
            .groupBy { it.first.group }
            .flatMap { (group, effects) ->
                if (group in collapseKeys) {
                    val upgradedDependency = effects.first().first
                    val groupedUpgradedDependency = upgradedDependency.copy(artifact = "$group:*")
                    effects.distinctBy { it.second }
                        .map { dep ->
                            val count = effects.count { it.second == dep.second }
                            if (count > 1) {
                                dep.copy(first = groupedUpgradedDependency)
                            } else {
                                dep
                            }
                        }
                } else {
                    effects
                }
            }

        buildString {
            collapsedSideEffects
                .forEachIndexed { index, effects ->
                    if (index > 0) {
                        appendLine()
                    }
                    append("Changing ${effects.first.artifact} to ${effects.first.version}${effects.first.alternativeVersion?.let { ", (changed from $it)"}} will also change:")
                    effects.second
                        .forEach { affectedDependency -> writeTree(affectedDependency, 1) }
                    appendLine()
                }
        }
    } else {
        ""
    }
}

private fun Dependency.matches(target: String) = artifact == target

private fun Dependency.filterMatches(lambda: (Dependency) -> Boolean): Dependency? {
    return if (lambda(this)) {
        this.copy(children = emptyList())
    } else {
        val children = this.children.mapNotNull { it.filterMatches(lambda) }
        if (children.isNotEmpty()) {
            this.copy(children = children)
        } else {
            null
        }
    }
}

private fun StringBuilder.writeTree(dependency: Dependency, indent: Int) {
    appendLine()
    append(" ".repeat(indent * 4))
    append(dependency.artifact)
    append(" (")
    append(dependency.resolvedVersion)
    append(")")
    dependency.children.forEach { writeTree(it, indent + 1) }
}