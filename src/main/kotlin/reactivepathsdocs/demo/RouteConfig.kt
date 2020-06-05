package reactivepathsdocs.demo

import kotlinx.coroutines.reactive.asFlow
import org.springdoc.core.GroupedOpenApi
import org.springdoc.core.annotations.RouterOperation
import org.springdoc.core.annotations.RouterOperations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.reactive.function.BodyInserters.fromValue
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Flux
import java.time.Duration


@Component
class NumberHandler {
    private fun numbers() = Flux.range(1, 10).delayElements(Duration.ofMillis(300))
    private fun letters() = ('a'..'z').asSequence()

    @Bean
    @RouterOperation(beanClass = NumberHandler::class, beanMethod = "getNumbers")
    fun getNumbers() = router {
        GET("/numbers") {
            ServerResponse
                    .ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(numbers())
        }
    }

    @Bean
    @RouterOperation(beanClass = NumberHandler::class, beanMethod = "getLetters")
    fun getLetters() = router {
        GET("/letters") { ServerResponse.ok().body(fromValue(letters())) }
    }
}

@Component
class ApiHandler {

    suspend fun getEmojis(req: ServerRequest): ServerResponse {
        return ok()
                .contentType(MediaType.TEXT_HTML)
                .bodyAndAwait(Flux.just("😆", "∑", "π").asFlow())
    }

    suspend fun getData(req: ServerRequest): ServerResponse {

        data class JsonData(val id: Long = 1323L,
                            var name: String = "John",
                            var lastname: String = "Doe")

        return ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyAndAwait(Flux.just(JsonData(), JsonData(id = 2323, name = "Mia", lastname = "Turner")).asFlow())
    }
}


@Configuration
class OpenApiConfig {

    @Bean
    fun handlerGroupAPI(): GroupedOpenApi {
        val paths = arrayOf("/api/**")
        return GroupedOpenApi.builder().group("api").pathsToMatch(*paths).build()
    }

    @Bean
    fun numbersGroupAPI(): GroupedOpenApi {
        val paths = arrayOf("/numbers", "/letters")
        return GroupedOpenApi.builder().group("ROOT").pathsToMatch(*paths).build()
    }

}

@Configuration
class RouteConfig {

    @Bean
    @RouterOperations(
            value = [
                RouterOperation(path = "/api/data", beanClass = ApiHandler::class, beanMethod = "getData", method = [RequestMethod.GET]),
                RouterOperation(path = "/api/emojis", beanClass = ApiHandler::class, beanMethod = "getEmojis", method = [RequestMethod.GET]
                )
            ]
    )
    fun routes(numberHandler: NumberHandler,
               apiHandler: ApiHandler): RouterFunction<ServerResponse> = coRouter {
        "/api".nest {
            GET("/data", apiHandler::getData)
            GET("/emojis", apiHandler::getEmojis)
        }
    }

}
