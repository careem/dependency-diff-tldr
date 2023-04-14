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

@file:JvmName("DependencyTreeTldr")

package com.careem.gradle.dependencies

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import kotlin.io.path.readText

class Tldr : CliktCommand(name = "dependency-diff-tldr") {
  private val sideEffects by option(
    "-s",
    "--side-effects",
    help = "Print out any side effects of upgrading the dependencies."
  ).flag()
  private val collapse: List<String> by option(
    "-c",
    "--collapse",
    help = "Collapse packages with a matching group under a group.*. Collapsing " +
        "will only occur if all version numbers match. (ex --collapse com.careem.ridehail --collapse com.careem.now)."
  ).multiple()

  private val old by argument("old.txt").path(mustExist = true)
  private val new by argument("new.txt").path(mustExist = true)

  override fun run() {
    val oldContents = old.readText()
    val newContents = new.readText()
    print(tldr(oldContents, newContents, collapse))
    if (sideEffects) {
      val upgradeEffects = upgradeEffects(oldContents, newContents, collapse)
      if (upgradeEffects.isNotEmpty()) {
        println()
        println("Upgrade Side Effects")
        print(upgradeEffects)
      }
    }
  }
}

fun main(args: Array<String>) {
  Tldr().main(args)
}