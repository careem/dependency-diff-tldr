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

@file:JvmName("DenyListEnforcer")
package com.careem.gradle.dependencies.denylist

import com.careem.gradle.dependencies.common.parser.Dependency
import com.careem.gradle.dependencies.common.writeTree
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import kotlin.io.path.readText

/**
 * Finds usages of a set of absolutely unacceptable, intolerable,
 * and illicit dependencies.
 */
class DenyListEnforcerCommand : CliktCommand() {
  private val unacceptableDependencies by mutuallyExclusiveOptions(
    option(
      "-d",
      "--dependency",
      help = "A set of artifacts to ensure aren't used - ex com.dep:a:1.2.3,com.dep:b:1.3.4"
    ).multiple(),

    option("-df", help = "File containing illegal dependencies")
      .file(mustExist = true)
      .convert { it.readLines() }
  ).required()

  private val deps by argument("deps.txt").path(mustExist = true)

  override fun run() {
    val illegal = illegalDependencies(deps.readText(), unacceptableDependencies)
    if (illegal.isNotEmpty()) {
      val result = buildString {
        illegal.forEach { (dependency, usages) ->
          append("Illegal dependency: ${dependency.fullName()}:")
          usages.forEach {
            writeTree(it, 1)
            appendLine()
          }
        }
      }
      print(result)
    }
  }

  private fun Dependency.fullName(): String {
    return "$artifact:$requestedVersion"
  }
}

fun main(args: Array<String>) {
  DenyListEnforcerCommand().main(args)
}