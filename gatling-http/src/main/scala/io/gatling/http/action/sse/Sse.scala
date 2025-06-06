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

package io.gatling.http.action.sse

import io.gatling.commons.validation.Validation
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session._
import io.gatling.http.check.sse.SseMessageCheck
import io.gatling.http.request.builder.sse.SseConnectRequestBuilder

import io.netty.handler.codec.http.HttpMethod

object Sse {
  private val DefaultSseName = SessionPrivateAttributes.generatePrivateAttribute("http.sse").expressionSuccess

  def apply(requestName: Expression[String]): Sse = apply(requestName, DefaultSseName)

  def apply(requestName: Expression[String], sseName: Expression[String]): Sse = new Sse(requestName, sseName)

  def checkMessage(name: String): SseMessageCheck = SseMessageCheck(name, Nil, Nil)

  def processUnmatchedMessages(f: (List[SseInboundMessage], Session) => Validation[Session]): ActionBuilder =
    processUnmatchedMessages(DefaultSseName, f)

  def processUnmatchedMessages(sseName: Expression[String], f: (List[SseInboundMessage], Session) => Validation[Session]): ActionBuilder =
    new SseProcessUnmatchedInboundMessagesBuilder(sseName, f)
}

final class Sse(requestName: Expression[String], sseName: Expression[String]) {
  def sseName(sseName: Expression[String]): Sse = new Sse(requestName, sseName)

  def get(url: Expression[String]): SseConnectRequestBuilder = SseConnectRequestBuilder(requestName, HttpMethod.GET, url, sseName)

  def post(url: Expression[String]): SseConnectRequestBuilder = SseConnectRequestBuilder(requestName, HttpMethod.POST, url, sseName)

  def setCheck: SseSetCheckBuilder = SseSetCheckBuilder(requestName, sseName, Nil)

  def close: SseCloseBuilder = SseCloseBuilder(requestName, sseName)
}
