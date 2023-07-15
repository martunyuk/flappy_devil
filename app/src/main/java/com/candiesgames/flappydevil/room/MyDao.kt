package com.candiesgames.flappydevil.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MyDao {
    @Insert
    suspend fun insert(myEntity: MyEntity)

    @Query("SELECT * FROM my_table LIMIT 1")
    suspend fun getFirst(): MyEntity?
}