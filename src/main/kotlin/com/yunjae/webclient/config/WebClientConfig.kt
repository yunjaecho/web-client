package com.yunjae.webclient.config

import com.yunjae.webclient.filter.RequestLoggingFilterFunction
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import org.reactivestreams.Publisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.NettyDataBufferFactory
import org.springframework.http.HttpStatus
import org.springframework.http.client.reactive.ClientHttpRequest
import org.springframework.http.client.reactive.ClientHttpRequestDecorator
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.LoggingCodecSupport
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.client.*
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.util.concurrent.atomic.AtomicBoolean


@Configuration
class WebClientConfig(private val requestLoggingFilterFunction: RequestLoggingFilterFunction) {
    @Bean
    fun webclient(): WebClient {
        val exchangeStrategies: ExchangeStrategies = ExchangeStrategies.builder()
            .codecs { configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024 * 50) }
            .build()
        exchangeStrategies
            .messageWriters().stream()
            .filter { obj: Any? -> LoggingCodecSupport::class.java.isInstance(obj) }
            .forEach { writer -> (writer as LoggingCodecSupport).setEnableLoggingRequestDetails(true) }

        return WebClient.builder()
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient
                        .create()
                )
            )
            .exchangeStrategies(exchangeStrategies)
            .filter(requestLoggingFilterFunction)
            .filter(ExchangeFilterFunction.ofResponseProcessor { response ->
                Mono.just(ClientResponse.from(response).statusCode(HttpStatus.OK).body(response.body(
                    BodyExtractors.toDataBuffers())).build())
//
            })
            .build()
    }

    private fun traceRequest(clientRequest: ClientRequest, buffer: DataBuffer) {
        val byteBuf: ByteBuf = NettyDataBufferFactory.toByteBuf(buffer)
        val bytes = ByteBufUtil.getBytes(byteBuf)
        // do some tracing e.g. new String(bytes)
        println(bytes?.toString())
    }
}