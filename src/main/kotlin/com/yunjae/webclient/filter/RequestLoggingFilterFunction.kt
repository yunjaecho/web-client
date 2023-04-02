package com.yunjae.webclient.filter

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean
import java.lang.Math.min
import java.util.UUID.randomUUID

@Component
class RequestLoggingFilterFunction: ExchangeFilterFunction {

    private val MAX_BYTES_LOGGED = 4096

//    var log = KotlinLogging.logger {}

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
//        if (!log.isDebugEnabled()) {
//            return next.exchange(request)
//        }

        val clientRequestId: String = randomUUID().toString()

        val requestLogged = AtomicBoolean(false)
        val responseLogged = AtomicBoolean(false)

        val capturedRequestBody = StringBuilder()
        val capturedResponseBody = StringBuilder()

        val stopWatch = StopWatch()
        stopWatch.start()

        return next
            .exchange(ClientRequest.from(request).body(object : BodyInserter<Any?, ClientHttpRequest?> {
                override fun insert(req: ClientHttpRequest, context: BodyInserter.Context): Mono<Void> {
                    return request.body().insert(object : ClientHttpRequestDecorator(req) {
                        override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
                            return super.writeWith(Flux.from(body).doOnNext { data: DataBuffer ->
                                capturedRequestBody.append(
                                    extractBytes(data)
                                )
                            }) // number of bytes appended is maxed in real code
                        }
                    }, context)
                }
            }).build())
            .doOnNext { response ->
                if (!requestLogged.getAndSet(true)) {
                    println(
                        "| >>---> Outgoing request [${Pair("clientRequestId", clientRequestId)}]}]\n" +
                                "${Pair("clientRequestMethod", request.method())} " +
                                "${Pair("clientRequestUrl", request.url())}\n" +
                                "${Pair("clientRequestHeaders", request.headers().toString())}\n\n" +
                                "${Pair("clientRequestBody", capturedRequestBody.toString())}\n"
                    )
                }
            }
            .doOnError { error ->
                if (!requestLogged.getAndSet(true)) {
                    println(
                        "| >>---> Outgoing request [{${Pair("clientRequestId", clientRequestId)}}]\n${Pair("clientRequestMethod", request.method())} ${Pair("clientRequestUrl", request.url())}\n{}\n\nError: ${error.message}\n"
                    )
                }
            }
            .map { response ->
                response.mutate().body { transformer ->
                    transformer
                        .doOnNext { body -> capturedResponseBody.append(extractBytes(body)) } // number of bytes appended is maxed in real code
                        .doOnTerminate {
                            if (stopWatch.isRunning) {
                                stopWatch.stop()
                            }
                        }
                        .doOnComplete {
                            if (!responseLogged.getAndSet(true)) {
                                println(
                                    "| <---<< Response for outgoing request [${Pair("clientRequestId", clientRequestId)}] after ${Pair ("clientRequestExecutionTimeInMillis", stopWatch.totalTimeMillis)}ms\n" +
                                            "${Pair("clientResponseStatusCode", response.statusCode().value())} ${Pair ("clientResponseHeaders", response.headers().toString())}\n" +
                                            "${Pair("clientResponseBody", capturedResponseBody.toString())}\n\n"
                                )
                            }
                        }
                        .doOnError { error ->
                            if (!responseLogged.getAndSet(true)) {
                                println(
                                    "| <---<< Error parsing response for outgoing request [${Pair("clientRequestId", clientRequestId)}] after ${Pair("clientRequestExecutionTimeInMillis", stopWatch.totalTimeMillis)}ms\n${Pair("clientErrorMessage", error.message)}"
                                )
                            }
                        }
                }.build()
            }
    }

    private fun extractBytes(data: DataBuffer): String {
        val currentReadPosition = data.readPosition()
        val numberOfBytesLogged: Int = min(data.readableByteCount(), MAX_BYTES_LOGGED)
        val bytes: ByteArray = ByteArray(numberOfBytesLogged)
        data.read(bytes, 0, numberOfBytesLogged)
        data.readPosition(currentReadPosition)
        return String(bytes)
    }
}