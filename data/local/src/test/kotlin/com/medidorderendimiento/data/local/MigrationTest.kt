package com.medidorderendimiento.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlin.test.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @Test fun `migration 1 to 2 preserves existing data and adds only phase 2b tables`() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(
            ApplicationProvider.getApplicationContext()).name(null).callback(object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE user_profiles (profileId TEXT NOT NULL PRIMARY KEY, marker TEXT)")
                db.execSQL("CREATE TABLE food_products (productId TEXT NOT NULL PRIMARY KEY, marker TEXT)")
                listOf("nutrition_plan_versions", "weight_measurements", "food_entries", "nutrition_diary_days").forEach {
                    db.execSQL("CREATE TABLE $it (id TEXT NOT NULL PRIMARY KEY, marker TEXT)")
                    db.execSQL("INSERT INTO $it VALUES ('row', 'preserved')")
                }
                db.execSQL("INSERT INTO user_profiles VALUES ('profile', 'preserved')")
                db.execSQL("INSERT INTO food_products VALUES ('food', 'preserved')")
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }).build())
        val db = helper.writableDatabase
        MIGRATION_1_2.migrate(db)
        assertEquals("preserved", db.query("SELECT marker FROM user_profiles WHERE profileId='profile'").use { it.moveToFirst(); it.getString(0) })
        listOf("nutrition_plan_versions", "weight_measurements", "food_entries", "nutrition_diary_days").forEach { table ->
            assertEquals("preserved", db.query("SELECT marker FROM $table WHERE id='row'").use { it.moveToFirst(); it.getString(0) })
        }
        val tables = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('favorite_foods','saved_meals','saved_meal_items','recent_foods') ORDER BY name").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        assertEquals(listOf("favorite_foods", "saved_meal_items", "saved_meals"), tables)
        helper.close()
    }

    @Test fun `migration 2 to 3 preserves phase 2 data and adds only tdee estimates`() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(
            ApplicationProvider.getApplicationContext()).name(null).callback(object : SupportSQLiteOpenHelper.Callback(2) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE user_profiles (profileId TEXT NOT NULL PRIMARY KEY, marker TEXT)")
                listOf("nutrition_plan_versions", "weight_measurements", "food_products", "food_entries", "nutrition_diary_days",
                    "favorite_foods", "saved_meals", "saved_meal_items").forEach {
                    db.execSQL("CREATE TABLE $it (id TEXT NOT NULL PRIMARY KEY, marker TEXT)")
                    db.execSQL("INSERT INTO $it VALUES ('row', 'preserved')")
                }
                db.execSQL("INSERT INTO user_profiles VALUES ('profile', 'preserved')")
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }).build())
        val db = helper.writableDatabase
        MIGRATION_2_3.migrate(db)
        listOf("user_profiles", "nutrition_plan_versions", "weight_measurements", "food_products", "food_entries",
            "nutrition_diary_days", "favorite_foods", "saved_meals", "saved_meal_items").forEach { table ->
            assertEquals("preserved", db.query("SELECT marker FROM $table LIMIT 1").use { it.moveToFirst(); it.getString(0) })
        }
        val newTables = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='tdee_estimates'").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        assertEquals(listOf("tdee_estimates"), newTables)
        helper.close()
    }
}
