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

package io.gatling.core.action

import scala.runtime.LongRef

import io.gatling.commons.util.Clock
import io.gatling.core.session.{ Expression, LoopBlock, Session }
import io.gatling.core.stats.StatsEngine

private final class Loop(
    continueCondition: Expression[Boolean],
    counterName: String,
    exitASAP: Boolean,
    timeBased: Boolean,
    statsEngine: StatsEngine,
    clock: Clock,
    override val name: String,
    next: Action
) extends Action {
  private[this] var innerLoop: Action = _

  private[core] def initialize(loopNext: Action): Unit = {
    val counterIncrement = (session: Session) =>
      if (session.contains(counterName)) {
        session.incrementCounter(counterName)
      } else if (timeBased) {
        session.enterTimeBasedLoop(counterName, continueCondition, next, exitASAP, clock.nowMillis)
      } else {
        session.enterLoop(counterName, continueCondition, next, exitASAP)
      }

    innerLoop = new InnerLoop(continueCondition, loopNext, counterIncrement, name + "-inner", statsEngine, next)
  }

  override def execute(session: Session): Unit =
    BlockExit.mustExit(session) match {
      case Some(blockExit) => blockExit.exitBlock(statsEngine, clock.nowMillis)
      case _               => innerLoop ! session
    }
}

final class InnerLoop(
    continueCondition: Expression[Boolean],
    loopNext: Action,
    counterIncrement: Session => Session,
    val name: String,
    val statsEngine: StatsEngine,
    val next: Action
) extends ChainableAction {
  private[this] val lastUserIdThreadLocal = ThreadLocal.withInitial(() => LongRef.zero())

  private[this] def getAndSetLastUserId(session: Session): Long = {
    val lastUserIdRef = lastUserIdThreadLocal.get()
    val prev = lastUserIdRef.elem
    lastUserIdRef.elem = session.userId
    prev
  }

  /**
   * Evaluates the condition and if true executes the first action of loopNext else it executes next
   *
   * @param session
   *   the session of the virtual user
   */
  def execute(session: Session): Unit = {
    val incrementedSession = counterIncrement(session)
    val lastUserId = getAndSetLastUserId(session)

    if (LoopBlock.continue(continueCondition, incrementedSession)) {
      if (incrementedSession.userId == lastUserId) {
        // except if we're running only one user per core, it's very likely we're hitting an empty loop
        // let's dispatch so we don't spin
        val eventLoop = session.eventLoop
        if (!eventLoop.isShutdown) {
          eventLoop.execute(() => loopNext ! incrementedSession)
        }
      } else {
        loopNext ! incrementedSession
      }
    } else {
      val newSession = incrementedSession.blockStack match {
        case LoopBlock(counterName) :: tail => incrementedSession.exitLoop(counterName, tail)
        case blockStack =>
          logger.error(s"Loop $name called exitLoop on Session $incrementedSession but stack $blockStack head isn't a Loop Block, please report.")
          incrementedSession
      }

      next ! newSession
    }
  }
}
