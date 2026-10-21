package ru.urent.qatools.testing_stand

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties
import ru.urent.qatools.core_database.DatabaseDriverProvider

object InMemoryDatabaseDriverProvider : DatabaseDriverProvider {
    private const val FOREIGN_KEYS_PROPERTY = "foreign_keys"

    override fun driver(fileName: String, schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
        val properties = Properties()
        properties.setProperty(FOREIGN_KEYS_PROPERTY, true.toString())
        return JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, properties = properties, schema = schema)
    }
}
