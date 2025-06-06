/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.core.stats.message

import org.scalacheck.Gen.alphaStr
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class MessageEventSpec extends AnyFlatSpecLike with Matchers with ScalaCheckDrivenPropertyChecks {
  "MessageEvent.apply" should "return Start when passing 'START'" in {
    MessageEvent("START") shouldBe MessageEvent.Start
  }

  it should "return End when passing 'END'" in {
    MessageEvent("END") shouldBe MessageEvent.End
  }

  it should "throw an IllegalArgumentException on any other string" in {
    forAll(alphaStr.suchThat(s => s != "START" && s != "END")) { string =>
      an[IllegalArgumentException] should be thrownBy MessageEvent(string)
    }
  }
}
