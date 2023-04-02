package com.yunjae.webclient

import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import reactor.core.publisher.Mono

@SpringBootApplication
class WebClientApplication(private val webClient: WebClient) {

    @Bean
    fun initRun() = ApplicationRunner {
        val restTemplate = RestTemplate()
        val res: Map<String, Map<String, Double>> = restTemplate.getForObject("https://open.er-api.com/v6/latest", Map::class.java) as Map<String, Map<String, Double>>
        println(res)

//        val client = WebClient.create("https://open.er-api.com")
//        val res2: MutableMap<*, *>? = webClient
//            .get()
////            .uri("https://open.er-api.com/v6/latest")
//            .uri("http://localhost:9090/test")
//            .retrieve().bodyToMono(MutableMap::class.java)
//            .block()
//
//        println(res2)

//        val httpServiceProxyFactory = HttpServiceProxyFactory
//            .builder(WebClientAdapter.forClient(webClient))
//            .build()

//        val erApi = httpServiceProxyFactory.createClient(ErApi::class.java)
//        val res3 = erApi.getLatest()
//        println(res3)

    }
}

@RestController
class Test(private val webClient: WebClient) {
    @GetMapping("/test")
    fun test(): ResponseEntity<Map<String, String>> {
        val mapOf = mapOf("aaa" to "bbbb")
        return ResponseEntity(mapOf, null, HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/test2")
    fun test2(): Mono<Map<*, *>> {
        val mapOf = mapOf<String, Any>("Id" to 78912, "Customer" to "Jason Sweet", "Quantity" to 1, "Price" to 18.00)

        return webClient.post()
            .uri("https://reqbin.com/echo/post/json")
            .bodyValue(mapOf)
            .retrieve()
            .bodyToMono(Map::class.java)

    }
}

interface ErApi {
    @GetExchange("/v6/latest")
    fun getLatest(): MutableMap<*, *>?
}


fun main(args: Array<String>) {
    runApplication<WebClientApplication>(*args)
}
