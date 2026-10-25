package com.salahmate.app.core.net

import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/** Shared JSON configuration: unknown API fields must never crash the app. */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    coerceInputValues = true
}

object HttpClients {
    fun okHttp(enableLogging: Boolean): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
        if (enableLogging) {
            builder.addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                level = okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
            })
        }
        return builder.build()
    }

    fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType()))
            .build()
}

/** Retry policy for idempotent GETs. Exponential backoff with a hard cap and jitter. */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 400,
    val maxDelayMs: Long = 4_000,
    val factor: Double = 2.0,
)

/**
 * Executes [block], retrying transient failures. Rate limiting (HTTP 429) is retried with a longer
 * delay; programming errors and HTTP 4xx responses are not retried.
 */
suspend fun <T> withRetry(policy: RetryPolicy = RetryPolicy(), block: suspend (attempt: Int) -> T): T {
    var lastError: Throwable? = null
    var delayMs = policy.initialDelayMs
    for (attempt in 1..policy.maxAttempts) {
        try {
            return block(attempt)
        } catch (e: Throwable) {
            lastError = e
            if (!isTransient(e) || attempt == policy.maxAttempts) throw e
            delay(delayMs)
            delayMs = (delayMs * policy.factor).toLong().coerceAtMost(policy.maxDelayMs)
        }
    }
    throw lastError ?: IllegalStateException("retry failed without an error")
}

internal fun isTransient(e: Throwable): Boolean = when (e) {
    is UnknownHostException, is SocketTimeoutException -> true
    is IOException -> true
    is retrofit2.HttpException -> e.code() == 429 || e.code() >= 500
    else -> false
}

/** Maps transport exceptions to the app's own error vocabulary. Never swallows the message. */
fun Throwable.toProviderError(): com.salahmate.app.data.prayer.ProviderError = when (this) {
    is UnknownHostException -> com.salahmate.app.data.prayer.ProviderError.NETWORK_UNAVAILABLE
    is SocketTimeoutException -> com.salahmate.app.data.prayer.ProviderError.TIMEOUT
    is IOException -> com.salahmate.app.data.prayer.ProviderError.NETWORK_UNAVAILABLE
    is retrofit2.HttpException -> when {
        code() == 429 -> com.salahmate.app.data.prayer.ProviderError.RATE_LIMITED
        code() >= 500 -> com.salahmate.app.data.prayer.ProviderError.SERVER_ERROR
        else -> com.salahmate.app.data.prayer.ProviderError.SERVER_ERROR
    }
    is kotlinx.serialization.SerializationException -> com.salahmate.app.data.prayer.ProviderError.MALFORMED_RESPONSE
    else -> com.salahmate.app.data.prayer.ProviderError.UNKNOWN
}
