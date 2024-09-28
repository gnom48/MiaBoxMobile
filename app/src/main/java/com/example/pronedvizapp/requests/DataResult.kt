package com.example.pronedvizapp.requests

sealed class DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>()
    data class Failure(val exception: Exception) : DataResult<Nothing>()
    data class Cached<T>(val data: T) : DataResult<T>()

    inline fun onSuccess(action: (T) -> Unit): DataResult<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    inline fun onFailure(action: (Exception) -> Unit): DataResult<T> {
        if (this is Failure) {
            action(exception)
        }
        return this
    }

    inline fun onCached(action: (T) -> Unit): DataResult<T> {
        if (this is Cached) {
            action(data)
        }
        return this
    }

    companion object {
        fun <T> success(value: T): DataResult<T> =
            DataResult.Success(value)

        fun failure(exception: Exception) =
            DataResult.Failure(exception)

        fun <T> cached(value: T): DataResult<T> =
            DataResult.Cached(value)
    }
}
