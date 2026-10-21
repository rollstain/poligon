package ru.urent.qatools.core_entities

sealed interface RequestResult<out T> {
    data class Success<out T>(val data: T) : RequestResult<T>

    data class Error(val failure: RequestFailure) : RequestResult<Nothing>
}

sealed interface RequestFailure {
    data object NoConnection : RequestFailure

    data object SessionExpired : RequestFailure

    data object InvalidCredentials : RequestFailure

    data class Http(val statusCode: Int) : RequestFailure

    data object InvalidResponse : RequestFailure
}
