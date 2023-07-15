package com.candiesgames.flappydevil.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "my_table")
data class MyEntity(
    @PrimaryKey val myBoolean: Boolean
)