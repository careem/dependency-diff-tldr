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

package com.careem.gradle.dependencies.denylist

import com.careem.gradle.dependencies.common.filterMatches
import com.careem.gradle.dependencies.common.parser.Dependency
import com.careem.gradle.dependencies.common.parser.parseDependencyTree

fun illegalDependencies(dependencies: String, illegalDependencies: List<String>): Map<Dependency, List<Dependency>> {
  return if (illegalDependencies.isEmpty()) {
    emptyMap()
  } else {
    val tree = parseDependencyTree(dependencies)
    val denyListDependencies = illegalDependencies.map { it.asDependency() }
    denyListDependencies
      .map { illegal ->
        val result = tree.mapNotNull { treeDependency ->
          treeDependency.filterMatches { it.matches(illegal) }
        }
        illegal to result
      }
      .filter { (_, matches) -> matches.isNotEmpty() }
      .associateBy(
        keySelector = { it.first },
        valueTransform = { it.second }
      )
  }
}

internal fun Dependency.matches(target: Dependency) =
  artifact == target.artifact && requestedVersion == target.resolvedVersion

private fun String.asDependency(): Dependency {
  val pieces = this.split(":")
  val group = pieces[0]
  val artifact = pieces[1]
  val version = pieces[2]
  return Dependency(group, artifact, version, version)
}