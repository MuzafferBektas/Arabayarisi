package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(private val dao: GameStatsDao) {
    val statsFlow: Flow<GameStats> = dao.getStatsFlow().map { it ?: GameStats() }

    suspend fun getStats(): GameStats {
        return dao.getStats() ?: GameStats()
    }

    suspend fun updateHighScore(score: Int) {
        val current = getStats()
        if (score > current.highScore) {
            dao.insertOrUpdate(current.copy(highScore = score))
        }
    }

    suspend fun incrementGamesPlayed() {
        val current = getStats()
        dao.insertOrUpdate(current.copy(totalGamesPlayed = current.totalGamesPlayed + 1))
    }

    suspend fun addPowerupCollected() {
        val current = getStats()
        dao.insertOrUpdate(current.copy(totalPowerupsCollected = current.totalPowerupsCollected + 1))
    }

    suspend fun addCoins(amount: Int) {
        val current = getStats()
        dao.insertOrUpdate(current.copy(coins = current.coins + amount))
    }

    suspend fun updateSelectedCar(index: Int) {
        val current = getStats()
        dao.insertOrUpdate(current.copy(selectedCarIndex = index))
    }

    suspend fun setControlMode(mode: Int) {
        val current = getStats()
        dao.insertOrUpdate(current.copy(controlMode = mode))
    }

    suspend fun unlockCar(index: Int, cost: Int) {
        val current = getStats()
        if (current.coins >= cost) {
            val unlockedList = current.unlockedCars.split(",").toMutableList()
            if (!unlockedList.contains(index.toString())) {
                unlockedList.add(index.toString())
            }
            dao.insertOrUpdate(
                current.copy(
                    coins = current.coins - cost,
                    unlockedCars = unlockedList.joinToString(","),
                    selectedCarIndex = index
                )
            )
        }
    }
}
