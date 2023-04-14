@file:JvmName("MismatchedVersionFinder")
package com.careem.gradle.dependencies.mismatched

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import kotlin.io.path.readText

class MismatchedVersionFinderCommand : CliktCommand() {
  private val dependencies: List<String> by option(
    "-d",
    "--dependency",
    help = "A set of artifacts to watch dependency mismatches for."
  ).multiple()

  private val collapse: List<String> by option(
    "-c",
    "--collapse",
    help = "Collapse packages with a matching group under a group.*. Collapsing " +
        "will only occur if all version numbers match. (ex --collapse com.careem.ridehail --collapse com.careem.now)."
  ).multiple()

  private val old by argument("old.txt").path(mustExist = true)
  private val new by argument("new.txt").path(mustExist = true)

  override fun run() {
    print(mismatchedVersions(old.readText(), new.readText(), dependencies, collapse))
  }
}

fun main(args: Array<String>) {
  MismatchedVersionFinderCommand().main(args)
}