package ru.urent.qatools.core_network

class StandUnavailableException(cause: Throwable) : Exception(cause)

class SessionExpiredException : Exception()

class UnexpectedResponseException(val statusCode: Int, val path: String) : Exception("HTTP $statusCode: $path")
