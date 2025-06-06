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

//
// Copyright (c) 2018 AsyncHttpClient Project. All rights reserved.
//
// This program is licensed to you under the Apache License Version 2.0,
// and you may not use this file except in compliance with the Apache License Version 2.0.
// You may obtain a copy of the Apache License Version 2.0 at
//     http://www.apache.org/licenses/LICENSE-2.0.
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the Apache License Version 2.0 is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the Apache License Version 2.0 for the specific language governing permissions and
// limitations there under.
//

package io.gatling.http.client.oauth;

import io.gatling.http.client.Param;
import io.gatling.shared.util.StringBuilderPool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class Params {

  private final List<Param> parameters = new ArrayList<>();

  public Params add(String key, String value) {
    parameters.add(new Param(key, value));
    return this;
  }

  public void reset() {
    parameters.clear();
  }

  String sortAndConcat() {
    // then sort them (AFTER encoding, important)
    Collections.sort(parameters);

    // and build parameter section using pre-encoded pieces:
    StringBuilder encodedParams = StringBuilderPool.DEFAULT.get();
    for (Param param : parameters) {
      encodedParams.append(param.getName()).append('=').append(param.getValue()).append('&');
    }
    int length = encodedParams.length();
    if (length > 0) {
      encodedParams.setLength(length - 1);
    }
    return encodedParams.toString();
  }
}
