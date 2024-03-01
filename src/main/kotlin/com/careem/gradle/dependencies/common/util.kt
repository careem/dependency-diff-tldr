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

package com.careem.gradle.dependencies.common

import com.careem.gradle.dependencies.common.parser.Dependency

internal fun Dependency.filterMatches(lambda: (Dependency) -> Boolean): Dependency? {
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

internal fun StringBuilder.writeTree(dependency: Dependency, indent: Int) {
  appendLine()
  append(" ".repeat(indent * 4))
  append(dependency.artifact)
  append(" (")
  append(dependency.resolvedVersion)
  append(")")
  dependency.children.forEach { writeTree(it, indent + 1) }
}