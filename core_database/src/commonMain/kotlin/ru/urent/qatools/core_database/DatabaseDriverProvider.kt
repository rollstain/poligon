package ru.urent.qatools.core_database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

fun interface DatabaseDriverProvider {
    fun driver(fileName: String, schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver
}
