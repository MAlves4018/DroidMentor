package data.local.room

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {

    private val context =
        InstrumentationRegistry.getInstrumentation().targetContext

    private val databaseName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DroidMentorDatabase::class.java
    )

    @Test
    fun version1SchemaIsValid() {

        // Create database using the exported version 1 schema
        helper.createDatabase(databaseName, 1).close()

        val database = Room.databaseBuilder(
            context,
            DroidMentorDatabase::class.java,
            databaseName
        ).build()

        try {
            val sql = database.openHelper.writableDatabase

            assertEquals(
                setOf(
                    "id",
                    "createdAtMillis",
                    "updatedAtMillis",
                    "isActive"
                ),
                getColumns(sql, "conversations")
            )

            assertEquals(
                setOf(
                    "id",
                    "conversationId",
                    "role",
                    "content",
                    "sequence",
                    "createdAtMillis"
                ),
                getColumns(sql, "messages")
            )

            // Verify cascade deletion
            sql.query("PRAGMA foreign_key_list(messages)").use {
                    cursor ->
                var hasCascade = false

                while (cursor.moveToNext()) {
                    val table = cursor.getString(
                        cursor.getColumnIndexOrThrow("table")
                    )
                    val action = cursor.getString(
                        cursor.getColumnIndexOrThrow("on_delete")
                    )

                    if (table == "conversations" &&
                        action.equals("CASCADE", ignoreCase = true)
                    ) {
                        hasCascade = true
                    }
                }

                assertTrue(hasCascade)
            }

        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun getColumns(
        database: SupportSQLiteDatabase,
        table: String
    ): Set<String> {

        val columns = mutableSetOf<String>()

        database.query("PRAGMA table_info($table)").use {
                cursor ->
            while (cursor.moveToNext()) {
                columns.add(
                    cursor.getString(
                        cursor.getColumnIndexOrThrow("name")
                    )
                )
            }
        }

        return columns
    }
}