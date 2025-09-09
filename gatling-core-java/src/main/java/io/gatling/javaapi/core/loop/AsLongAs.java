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

package io.gatling.javaapi.core.loop;

import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.StructureBuilder;
import io.gatling.javaapi.core.exec.Executable;
import io.gatling.javaapi.core.internal.Executables;
import io.gatling.javaapi.core.internal.loop.ScalaAsLongAs;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;

/**
 * Methods for defining "asLongAs" loops.
 *
 * <p>Important: instances are immutable so any method doesn't mutate the existing instance but
 * returns a new one.
 *
 * @param <T> the type of {@link StructureBuilder} to attach to and to return
 * @param <W> the type of wrapped Scala instance
 */
public interface AsLongAs<
    T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  // Gatling EL condition

  /**
   * Define a loop that will iterate as long as the condition holds true
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @return a DSL component for defining the loop content
   */
  default @NonNull On<T> asLongAs(@NonNull String condition) {
    return asLongAs(condition, UUID.randomUUID().toString());
  }

  /**
   * Define a loop that will iterate as long as the condition holds true
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component for defining the loop content
   */
  default @NonNull On<T> asLongAs(@NonNull String condition, @NonNull String counterName) {
    return asLongAs(condition, counterName, false);
  }

  /**
   * Define a loop that will iterate as long as the condition holds true
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @param exitASAP if the loop must be interrupted if the condition becomes false inside the loop
   * @return a DSL component for defining the loop content
   */
  default @NonNull On<T> asLongAs(@NonNull String condition, boolean exitASAP) {
    return asLongAs(condition, UUID.randomUUID().toString(), exitASAP);
  }

  /**
   * Define a loop that will iterate as long as the condition holds true
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @param exitASAP if the loop must be interrupted if the condition becomes false inside the loop
   * @return a DSL component for defining the loop content
   */
  default @NonNull On<T> asLongAs(
      @NonNull String condition, @NonNull String counterName, boolean exitASAP) {
    return new On<>(ScalaAsLongAs.apply(this, condition, counterName, exitASAP));
  }

  // Function condition
  /**
   * Define a loop that will iterate as long as the condition holds true
   *
   * @param condition the condition, expressed as a function
   * @return a DSL component for defining the loop content
   */
  default @NonNull On<T> asLongAs(@NonNull Function<Session, Boolean> condition) {
    return asLongAs(condition, UUID.randomUUID().toString());
  }

  /**
   * Define a loop that will iterate as long as the condition holds true
   *
   * @param condition the condition, expressed as a function
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component for defining the loop content
   */
  default @NonNull On<T> asLongAs(
      @NonNull Function<Session, Boolean> condition, @NonNull String counterName) {
    return asLongAs(condition, counterName, false);
  }

  /**
   * Define a loop that will iterate as long as the condition holds true
   *
   * @param condition the condition, expressed as a function
   * @param exitASAP if the loop must be interrupted if the condition becomes false inside the loop
   * @return a DSL component for defining the loop content
   */
  default @NonNull On<T> asLongAs(Function<Session, Boolean> condition, boolean exitASAP) {
    return asLongAs(condition, UUID.randomUUID().toString(), exitASAP);
  }

  /**
   * Define a loop that will iterate as long as the condition holds true
   *
   * @param condition the condition, expressed as a function
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @param exitASAP if the loop must be interrupted if the condition becomes false inside the loop
   * @return a DSL component for defining the loop content
   */
  default @NonNull On<T> asLongAs(
      @NonNull Function<Session, Boolean> condition,
      @NonNull String counterName,
      boolean exitASAP) {
    return new On<>(ScalaAsLongAs.apply(this, condition, counterName, exitASAP));
  }

  /**
   * A DSL component for defining the loop content
   *
   * @param <T> the type of {@link StructureBuilder} to attach to and to return
   */
  final class On<T extends StructureBuilder<T, ?>> {
    private final ScalaAsLongAs.Loop<T, ?> wrapped;

    On(ScalaAsLongAs.Loop<T, ?> wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the loop content
     *
     * @param executable the loop content
     * @param executables other chains
     * @return a new {@link StructureBuilder}
     */
    public @NonNull T on(@NonNull Executable executable, @NonNull Executable... executables) {
      return wrapped.loop(Executables.toChainBuilder(executable, executables));
    }
  }
}
