package ru.urent.qatools.core_database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

internal class JdbcDatabaseDriverProvider(private val directory: File) : DatabaseDriverProvider {

    override fun driver(fileName: String, schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
        directory.mkdirs()
        val properties = Properties()
        properties.setProperty(FOREIGN_KEYS_PROPERTY, true.toString())
        return JdbcSqliteDriver(
            url = "$JDBC_SQLITE_PREFIX${File(directory, fileName).absolutePath}",
            properties = properties,
            schema = schema,
        )
    }

    private companion object {
        const val JDBC_SQLITE_PREFIX = "jdbc:sqlite:"
        const val FOREIGN_KEYS_PROPERTY = "foreign_keys"
    }
}
