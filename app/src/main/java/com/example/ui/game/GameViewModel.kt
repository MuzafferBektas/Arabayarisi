package com.example.ui.game

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.GameStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class PowerUpType(val label: String, val colorHex: String, val title: String) {
    SHIELD("🛡️", "#00BCD4", "Kalkan"),
    TURBO("⚡", "#F39C12", "Turbo"),
    LIFE("❤️", "#E91E63", "Can +1"),
    SLOW("🌀", "#9C27B0", "Yavaşlat")
}

data class Enemy(
    val id: Long,
    val lane: Int,
    var y: Float, // 0.0f .. 1.2f
    val speedMultiplier: Float,
    val colorHex: String
)

data class PowerUp(
    val id: Long,
    val lane: Int,
    var y: Float, // 0.0f .. 1.2f
    val type: PowerUpType
)

data class Coin(
    val id: Long,
    val lane: Int,
    var y: Float, // 0.0f .. 1.2f
    val value: Int = 1
)

data class CarModel(
    val name: String,
    val cost: Int,
    val colorHex: String,
    val speedStat: Float,
    val handlingStat: Float,
    val description: String
)

val CAR_MODELS = listOf(
    CarModel("Klasik Tofaş", 0, "#00E676", 0.008f, 0.55f, "Standart emektar araba. Güvenilir ve dengeli."),
    CarModel("Sarı Şahin", 15, "#FACC15", 0.010f, 0.65f, "Yollarda yan yan gitmek için ideal! Daha hızlı."),
    CarModel("Kırmızı Canavar", 45, "#EF4444", 0.012f, 0.75f, "Alev alan hız canavarı. Harika manevra kabiliyeti."),
    CarModel("Neon Gelecek", 100, "#C084FC", 0.015f, 0.85f, "Gelecekten gelen ultra siber süzülen prototip. Maksimum hız!")
)

data class GameParticle(
    val id: Long,
    var x: Float, // 0.0f .. 1.0f
    var y: Float, // 0.0f .. 1.0f
    val vx: Float,
    val vy: Float,
    val colorHex: String,
    var alpha: Float = 1.0f,
    var size: Float = 6f,
    var lifeTicks: Int = 35
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    private var gameJob: Job? = null
    private var idCounter = 0L

    // Repository state
    private val _stats = MutableStateFlow(GameStats())
    val stats: StateFlow<GameStats> = _stats.asStateFlow()

    // Game States (Observables)
    val isGameStarted = mutableStateOf(false)
    val isGameOver = mutableStateOf(false)
    val isPaused = mutableStateOf(false)
    val score = mutableStateOf(0)
    val speed = mutableStateOf(0.010f) // Base speed per tick (normalized)
    val lives = mutableStateOf(3)
    val playerLane = mutableStateOf(1) // 0, 1, 2, 3
    val playerX = mutableStateOf(0.375f) // Smooth sliding coordinate

    // Effects
    val shieldTimeRemaining = mutableStateOf(0)
    val slowTimeRemaining = mutableStateOf(0)
    val turboTimeRemaining = mutableStateOf(0)
    val turboCharge = mutableStateOf(0f) // 0f .. 100f
    val hitFlashTicks = mutableStateOf(0) // Temporary flashing when hit
    val screenShakeTicks = mutableStateOf(0) // Screen shaking feedback

    // Active Elements
    val enemies = mutableStateListOf<Enemy>()
    val powerups = mutableStateListOf<PowerUp>()
    val particles = mutableStateListOf<GameParticle>()
    val coinsList = mutableStateListOf<Coin>()

    // Road scroll offset for visual background movement
    val roadOffset = mutableStateOf(0f)

    init {
        val db = GameDatabase.getDatabase(application)
        repository = GameRepository(db.gameStatsDao())
        
        viewModelScope.launch {
            repository.statsFlow.collectLatest {
                _stats.value = it
            }
        }
    }

    private fun nextId(): Long = idCounter++

    fun startGame() {
        if (gameJob != null) {
            gameJob?.cancel()
            gameJob = null
        }

        // Reset state
        isGameOver.value = false
        isPaused.value = false
        isGameStarted.value = true
        score.value = 0
        lives.value = 3
        
        val selectedIndex = stats.value.selectedCarIndex
        val activeCar = CAR_MODELS.getOrElse(selectedIndex) { CAR_MODELS[0] }
        speed.value = activeCar.speedStat
        
        playerLane.value = 1
        playerX.value = 0.375f
        
        shieldTimeRemaining.value = 0
        slowTimeRemaining.value = 0
        turboTimeRemaining.value = 0
        turboCharge.value = 0f
        hitFlashTicks.value = 0
        screenShakeTicks.value = 0

        enemies.clear()
        powerups.clear()
        particles.clear()
        coinsList.clear()

        viewModelScope.launch {
            repository.incrementGamesPlayed()
        }

        runGameLoop()
    }

    private fun runGameLoop() {
        gameJob = viewModelScope.launch {
            var lastTime = System.nanoTime()
            var tickCount = 0L
            while (isGameStarted.value && !isGameOver.value) {
                if (!isPaused.value) {
                    val currentTime = System.nanoTime()
                    val elapsedNanos = currentTime - lastTime
                    lastTime = currentTime

                    val elapsedSeconds = elapsedNanos / 1_000_000_000f
                    val dt = elapsedSeconds.coerceIn(0.002f, 0.1f)
                    val timeScale = dt / 0.01666f

                    tickCount++
                    gameTick(tickCount, timeScale)
                } else {
                    lastTime = System.nanoTime()
                }
                delay(12) // High frequency loop with precise delta-time scaling for ultra-smooth 60+ FPS performance
            }
        }
    }

    fun togglePause() {
        if (isGameStarted.value && !isGameOver.value) {
            isPaused.value = !isPaused.value
        }
    }

    fun moveLeft() {
        if (isPaused.value || !isGameStarted.value || isGameOver.value) return
        if (playerLane.value > 0) {
            playerLane.value--
        }
    }

    fun moveRight() {
        if (isPaused.value || !isGameStarted.value || isGameOver.value) return
        if (playerLane.value < 3) {
            playerLane.value++
        }
    }

    private fun gameTick(tick: Long, timeScale: Float = 1.0f) {
        // 1. Smooth slide player X coordinate based on active car's custom handling!
        val selectedIndex = stats.value.selectedCarIndex
        val activeCar = CAR_MODELS.getOrElse(selectedIndex) { CAR_MODELS[0] }
        val targetX = (playerLane.value + 0.5f) / 4f
        val handlingAmt = (activeCar.handlingStat * timeScale).coerceAtMost(1.0f)
        playerX.value += (targetX - playerX.value) * handlingAmt

        // 2. Compute current speeds
        val isTurbo = turboTimeRemaining.value > 0
        val isSlow = slowTimeRemaining.value > 0
        
        val speedFactor = if (isTurbo) 1.8f else 1.0f
        val currentSpeed = speed.value * speedFactor
        val enemySpeedFactor = if (isSlow) 0.5f else 1.0f

        // Scroll background road lines
        roadOffset.value = (roadOffset.value + currentSpeed * 1000f * timeScale) % 1000f

        // 3. Update active elements positions
        enemies.forEach { enemy ->
            enemy.y += currentSpeed * enemy.speedMultiplier * enemySpeedFactor * timeScale
        }
        powerups.forEach { pu ->
            pu.y += currentSpeed * enemySpeedFactor * timeScale
        }
        coinsList.forEach { coin ->
            coin.y += currentSpeed * enemySpeedFactor * timeScale
        }
        
        // Remove off-screen items
        enemies.removeAll { it.y > 1.15f }
        powerups.removeAll { it.y > 1.15f }
        coinsList.removeAll { it.y > 1.15f }

        // 4. Update particles with translation physics (add vx/vy vectors)
        particles.forEach { p ->
            p.x += p.vx * timeScale
            p.y += p.vy * timeScale
            p.lifeTicks = maxOf(0, (p.lifeTicks - 1 * timeScale).toInt())
            p.alpha = p.lifeTicks / 35f
            p.size = maxOf(2f, p.size - 0.1f * timeScale)
        }
        particles.removeAll { it.lifeTicks <= 0 }

        // Spawn real-time exhaust trail particles from left/right exhaust pipes
        if (tick % 2L == 0L) {
            val pX = playerX.value
            val pY = 0.82f
            val pW = 0.08f
            val pH = 0.13f

            val ex1X = pX - pW / 4f
            val ex2X = pX + pW / 4f
            val exY = pY + pH / 2f

            val colorHex = if (isTurbo) "#F59E0B" else "#94A3B8" // Orange/amber flame for turbo, grey smoke otherwise
            val size = if (isTurbo) Random.nextFloat() * 6f + 4f else Random.nextFloat() * 4f + 2f
            val vyOffset = if (isTurbo) 0.010f else 0.006f

            particles.add(
                GameParticle(
                    id = nextId(),
                    x = ex1X,
                    y = exY,
                    vx = (Random.nextFloat() * 0.004f - 0.002f),
                    vy = (Random.nextFloat() * 0.003f + vyOffset),
                    colorHex = colorHex,
                    size = size,
                    lifeTicks = if (isTurbo) 25 else 18
                )
            )
            particles.add(
                GameParticle(
                    id = nextId(),
                    x = ex2X,
                    y = exY,
                    vx = (Random.nextFloat() * 0.004f - 0.002f),
                    vy = (Random.nextFloat() * 0.003f + vyOffset),
                    colorHex = colorHex,
                    size = size,
                    lifeTicks = if (isTurbo) 25 else 18
                )
            )
        }

        // 5. Counting downs for active powerups
        if (shieldTimeRemaining.value > 0) shieldTimeRemaining.value--
        if (slowTimeRemaining.value > 0) slowTimeRemaining.value--
        if (turboTimeRemaining.value > 0) {
            turboTimeRemaining.value--
            if (turboTimeRemaining.value == 0) {
                turboCharge.value = 0f
            }
        }
        if (hitFlashTicks.value > 0) hitFlashTicks.value--
        if (screenShakeTicks.value > 0) screenShakeTicks.value--

        // 6. Score progression
        val scoreGain = if (isTurbo) 3 else 1
        score.value += scoreGain

        // Speed ramp up over time (faster speedup, higher speed ceiling based on car)
        if (score.value > 0 && score.value % 250 == 0) {
            val maxSpeed = activeCar.speedStat + 0.005f
            speed.value = minOf(maxSpeed, speed.value + 0.0004f)
        }

        // 7. Spawning algorithms (slightly faster spawns for more dynamic action)
        val enemySpawnInterval = maxOf(25, 75 - score.value / 35)
        if (tick % enemySpawnInterval == 0L) {
            spawnEnemy()
        }

        val coinSpawnInterval = 45L
        if (tick % coinSpawnInterval == 0L) {
            spawnCoin()
        }

        val puSpawnInterval = maxOf(120, 240 - score.value / 75)
        if (tick % puSpawnInterval == 0L) {
            spawnPowerUp()
        }

        // 8. Collisions check
        checkCollisions()
    }

    private fun spawnEnemy() {
        val lane = Random.nextInt(4)
        // Ensure we don't spawn multiple enemies at exactly the same lane and position
        if (enemies.any { it.lane == lane && it.y < 0.25f }) return

        val colorOptions = listOf("#E74C3C", "#3498DB", "#F39C12", "#9B59B6", "#1ABC9C", "#E67E22")
        val randomColor = colorOptions.random()
        val speedMult = Random.nextFloat() * 0.4f + 1.1f // varied speed speeds

        enemies.add(
            Enemy(
                id = nextId(),
                lane = lane,
                y = -0.15f,
                speedMultiplier = speedMult,
                colorHex = randomColor
            )
        )
    }

    private fun spawnPowerUp() {
        val lane = Random.nextInt(4)
        // Don't spawn if there's already an active item nearby
        if (enemies.any { it.lane == lane && it.y < 0.2f }) return
        if (powerups.any { it.lane == lane && it.y < 0.2f }) return

        // Randomly pick a powerup type with weighted probabilities
        val roll = Random.nextFloat()
        val type = when {
            roll < 0.25f -> PowerUpType.SHIELD
            roll < 0.55f -> PowerUpType.TURBO
            roll < 0.80f -> PowerUpType.SLOW
            else -> PowerUpType.LIFE
        }

        powerups.add(
            PowerUp(
                id = nextId(),
                lane = lane,
                y = -0.1f,
                type = type
            )
        )
    }

    private fun checkCollisions() {
        val pX = playerX.value
        val pY = 0.82f
        val pW = 0.08f
        val pH = 0.13f

        // Enemy Collisions
        val currentEnemies = enemies.toList()
        for (enemy in currentEnemies) {
            val eX = (enemy.lane + 0.5f) / 4f
            val eY = enemy.y
            val eW = 0.08f
            val eH = 0.12f

            if (checkOverlap(pX, pY, pW, pH, eX, eY, eW, eH)) {
                // Collision!
                if (shieldTimeRemaining.value > 0) {
                    // Shield destroys the enemy!
                    explodeItem(eX, eY, enemy.colorHex, 15)
                    enemies.remove(enemy)
                } else if (hitFlashTicks.value == 0) {
                    // Normal hit!
                    lives.value--
                    screenShakeTicks.value = 15
                    hitFlashTicks.value = 65 // Flash/invincible for a second
                    explodeItem(eX, eY, enemy.colorHex, 20)
                    enemies.remove(enemy)

                    if (lives.value <= 0) {
                        endGame()
                        return
                    }
                }
            }
        }

        // PowerUp Collisions
        val currentPowerUps = powerups.toList()
        for (pu in currentPowerUps) {
            val puX = (pu.lane + 0.5f) / 4f
            val puY = pu.y
            val puSize = 0.08f // effective bounding box width/height

            if (checkOverlap(pX, pY, pW + 0.02f, pH + 0.02f, puX, puY, puSize, puSize)) {
                // Collect powerup!
                collectPowerUp(pu, puX, puY)
            }
        }

        // Coin Collisions
        val currentCoins = coinsList.toList()
        for (coin in currentCoins) {
            val coinX = (coin.lane + 0.5f) / 4f
            val coinY = coin.y
            val coinSize = 0.08f

            if (checkOverlap(pX, pY, pW + 0.02f, pH + 0.02f, coinX, coinY, coinSize, coinSize)) {
                collectCoin(coin, coinX, coinY)
            }
        }
    }

    private fun collectPowerUp(pu: PowerUp, x: Float, y: Float) {
        explodeItem(x, y, pu.type.colorHex, 18)
        powerups.remove(pu)

        viewModelScope.launch {
            repository.addPowerupCollected()
        }

        when (pu.type) {
            PowerUpType.SHIELD -> {
                shieldTimeRemaining.value = 350
            }
            PowerUpType.TURBO -> {
                val newCharge = minOf(100f, turboCharge.value + 40f)
                turboCharge.value = newCharge
                if (newCharge >= 100f) {
                    turboTimeRemaining.value = 220
                }
            }
            PowerUpType.LIFE -> {
                if (lives.value < 5) {
                    lives.value++
                }
            }
            PowerUpType.SLOW -> {
                slowTimeRemaining.value = 300
            }
        }
    }

    private fun explodeItem(x: Float, y: Float, colorHex: String, amount: Int) {
        for (i in 0 until amount) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speedPower = Random.nextFloat() * 0.015f + 0.005f
            particles.add(
                GameParticle(
                    id = nextId(),
                    x = x,
                    y = y,
                    vx = cos(angle) * speedPower,
                    vy = sin(angle) * speedPower,
                    colorHex = colorHex,
                    size = Random.nextFloat() * 8f + 4f,
                    lifeTicks = Random.nextInt(15) + 20
                )
            )
        }
    }

    private fun checkOverlap(
        ax: Float, ay: Float, aw: Float, ah: Float,
        bx: Float, by: Float, bw: Float, bh: Float
    ): Boolean {
        return Math.abs(ax - bx) < (aw + bw) / 2f &&
               Math.abs(ay - by) < (ah + bh) / 2f
    }

    private fun endGame() {
        isGameOver.value = true
        gameJob?.cancel()
        gameJob = null

        viewModelScope.launch {
            repository.updateHighScore(score.value)
        }
    }

    private fun spawnCoin() {
        val lane = Random.nextInt(4)
        if (enemies.any { it.lane == lane && it.y < 0.2f }) return
        if (powerups.any { it.lane == lane && it.y < 0.2f }) return
        if (coinsList.any { it.lane == lane && it.y < 0.2f }) return

        coinsList.add(
            Coin(
                id = nextId(),
                lane = lane,
                y = -0.1f
            )
        )
    }

    private fun collectCoin(coin: Coin, x: Float, y: Float) {
        explodeItem(x, y, "#FACC15", 12)
        coinsList.remove(coin)
        viewModelScope.launch {
            repository.addCoins(coin.value)
        }
    }

    fun selectCar(index: Int) {
        viewModelScope.launch {
            repository.updateSelectedCar(index)
        }
    }

    fun setControlMode(mode: Int) {
        viewModelScope.launch {
            repository.setControlMode(mode)
        }
    }

    fun buyCar(index: Int, cost: Int) {
        viewModelScope.launch {
            repository.unlockCar(index, cost)
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameJob?.cancel()
    }
}
