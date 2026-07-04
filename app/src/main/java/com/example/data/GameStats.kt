package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_stats")
data class GameStats(
    @PrimaryKey val id: Int = 1,
    val highScore: Int = 0,
    val totalGamesPlayed: Int = 0,
    val totalPowerupsCollected: Int = 0,
    val coins: Int = 0,
    val selectedCarIndex: Int = 0,
    val unlockedCars: String = "0",
    val controlMode: Int = 0 // 0: Dokunmatik (Sol/Sağ), 1: Kaydırma (Swipe), 2: Ekran Butonları
)
