@file:JvmName("DependencyTreeTldr")

package com.careem.gradle.dependencies

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  val help = "-h" in args || "--help" in args
  if (help || args.size != 2) {
    System.err.println("Usage: dependency-tree-tldr old.txt new.txt")
    if (!help) {
      exitProcess(1)
    }
    return
  }

  val old = Paths.get(args[0]).readText()
  val new = Paths.get(args[1]).readText()

  print(tldr(old, new))
}

private fun Path.readText(charset: Charset = StandardCharsets.UTF_8): String {
  return Files.readAllBytes(this).toString(charset)
}