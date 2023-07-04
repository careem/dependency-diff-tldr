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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File

@RunWith(Parameterized::class)
class FixturesTest(private val fixtureDir: File) {

  @Test
  fun testTldr() {
    val before = fixtureDir.resolve("before.txt").readText()
    val after = fixtureDir.resolve("after.txt").readText()
    val expected = fixtureDir.resolve("expected.txt").readText()
    val actual = tldr(before, after).toString(collapse = emptyList(), outputType = OutputType.PLAIN)
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun testTldrJson() {
    val before = fixtureDir.resolve("before.txt").readText()
    val after = fixtureDir.resolve("after.txt").readText()
    val expected = fixtureDir.resolve("expected_json.json").readText()
    val actual = tldr(before, after).toString(collapse = emptyList(), outputType = OutputType.JSON)
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun testSideEffects() {
    val before = fixtureDir.resolve("before.txt").readText()
    val after = fixtureDir.resolve("after.txt").readText()
    val expected = fixtureDir.resolve("expected_side_effects.txt").readText()
    val effects = upgradeEffects(before, after, emptyList())
    assertThat(effects).isEqualTo(expected)
  }

  companion object {
    @JvmStatic
    @Parameters(name = "{0}")
    fun params(): Array<File>? = File("src/test/fixtures")
      .listFiles { file -> file.isDirectory }
  }
}