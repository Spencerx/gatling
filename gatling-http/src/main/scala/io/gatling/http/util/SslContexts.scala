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

package io.gatling.http.util

import java.{ util => ju }
import java.security.SecureRandom
import javax.net.ssl._

import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._

import io.gatling.core.config.SslConfiguration
import io.gatling.http.client.SslContextsHolder

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.ssl._
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.util.ReferenceCountUtil

private[http] object SslContextsFactory {
  System.setProperty("io.netty.handler.ssl.openssl.sessionCacheClient", "true")

  private val DefaultSslSecureRandom = new SecureRandom
  private val Apn = new ApplicationProtocolConfig(
    ApplicationProtocolConfig.Protocol.ALPN,
    // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
    ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
    // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
    ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
    ApplicationProtocolNames.HTTP_2,
    ApplicationProtocolNames.HTTP_1_1
  )
}

private[gatling] final class SslContextsFactory(sslConfig: SslConfiguration, enableHostnameVerification: Boolean) extends StrictLogging {
  import SslContextsFactory._

  private val sslSessionTimeoutSeconds = sslConfig.sessionTimeout.toSeconds
  private lazy val DefaultJavaSslParameters = {
    val context = SSLContext.getInstance("TLS")
    context.init(null, null, null)
    context.getDefaultSSLParameters
  }
  private val enabledProtocols: Array[String] =
    if (sslConfig.useOpenSsl) {
      sslConfig.enabledProtocols.toArray
    } else {
      val supportedProtocols = DefaultJavaSslParameters.getProtocols.toSet
      sslConfig.enabledProtocols.toArray.filter(supportedProtocols.contains)
    }
  private val enabledCipherSuites: ju.List[String] =
    if (sslConfig.useOpenSsl) {
      sslConfig.enabledCipherSuites.asJava
    } else {
      val supportedCipherSuites = DefaultJavaSslParameters.getCipherSuites
      sslConfig.enabledCipherSuites.filter(supportedCipherSuites.contains).asJava
    }
  private val sslProvider =
    if (sslConfig.useOpenSsl) {
      if (sslConfig.useOpenSslFinalizers) {
        SslProvider.OPENSSL
      } else {
        SslProvider.OPENSSL_REFCNT
      }
    } else {
      SslProvider.JDK
    }

  private val endpointIdentificationAlgorithm = if (enableHostnameVerification) "HTTPS" else null

  def newSslContexts(http2Enabled: Boolean, perUserKeyManagerFactory: Option[KeyManagerFactory]): SslContexts = {
    val kmf = perUserKeyManagerFactory.orElse(sslConfig.keyManagerFactory)
    val tmf = sslConfig.trustManagerFactory.orElse {
      if (sslConfig.useInsecureTrustManager) {
        Some(InsecureTrustManagerFactory.INSTANCE)
      } else {
        None
      }
    }

    val sslContextBuilder = SslContextBuilder.forClient
      .sslProvider(sslProvider)
      .endpointIdentificationAlgorithm(endpointIdentificationAlgorithm)
      .secureRandom(DefaultSslSecureRandom)

    if (sslConfig.sessionCacheSize > 0) {
      sslContextBuilder.sessionCacheSize(sslConfig.sessionCacheSize)
    }

    if (sslConfig.sessionTimeout > Duration.Zero) {
      sslContextBuilder.sessionTimeout(sslSessionTimeoutSeconds)
    }

    if (enabledProtocols.nonEmpty) {
      sslContextBuilder.protocols(enabledProtocols: _*)
    }

    if (sslConfig.enabledCipherSuites.nonEmpty) {
      sslContextBuilder.ciphers(enabledCipherSuites)
    } else {
      sslContextBuilder.ciphers(null, IdentityCipherSuiteFilter.INSTANCE_DEFAULTING_TO_SUPPORTED_CIPHERS)
    }

    kmf.foreach(sslContextBuilder.keyManager)
    tmf.foreach(sslContextBuilder.trustManager)

    new SslContexts(sslContextBuilder, if (http2Enabled) Some(Apn) else None)
  }
}

private[http] final class SslContexts(sslContextBuilder: SslContextBuilder, apn: Option[ApplicationProtocolConfig])
    extends SslContextsHolder
    with AutoCloseable {

  private var loaded = false
  private lazy val (sslContext: SslContext, alpnSslContext: Option[SslContext]) = {
    loaded = true
    (
      sslContextBuilder.build,
      apn.map(sslContextBuilder.applicationProtocolConfig(_).build)
    )
  }

  override def close(): Unit =
    if (loaded) {
      ReferenceCountUtil.release(sslContext)
      alpnSslContext.foreach(ReferenceCountUtil.release)
    }

  override def getSslContext: SslContext = sslContext

  override def getAlpnSslContext: SslContext = alpnSslContext.orNull
}
