package com.example.game

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Handles the MVVM architecture requirements for the Flappy Bird clone.
 * Runs the smooth 60fps game loop, applies physics ticks, manages spawn intervals,
 * and handles screen tabs for jumps, start, and restart.
 */
class GameViewModel(
    context: Context
) : ViewModel() {

    private val scoreManager = ScoreManager(context)
    private val audioManager = AudioManager(context)

    private val _gameState = MutableStateFlow(
        GameState(highScore = scoreManager.getHighScore())
    )
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    init {
        // Game loop ticker is now driven safely by the Compose UI layer using LaunchedEffect
    }

    /**
     * Core update engine applied every animation frame.
     */
    fun updatePhysics(dt: Float) {
        val current = _gameState.value

        when (current.status) {
            GameStatus.READY -> {
                // Bird floats gently up and down idle; no pipe calculations
                val updatedBird = current.bird.update(dt, applyGravity = false, GameState.GROUND_HEIGHT)
                _gameState.value = current.copy(
                    bird = updatedBird,
                    pipes = emptyList(),
                    score = 0
                )
            }

            GameStatus.PLAYING -> {
                // 1. Update Bird positional coordinates with real gravity
                val updatedBird = current.bird.update(dt, applyGravity = true, GameState.PLAYABLE_HEIGHT)

                // 2. Generate and shift Pipe pairs horizontally
                val nextSpawnAccumulator = current.lastSpawnAccumulator + dt
                val finalPipes = mutableListOf<PipePair>()

                // Spawn a new column if the interval threshold is crossed
                val shouldSpawn = nextSpawnAccumulator >= PipePair.SPAWN_INTERVAL
                val spawnAcc = if (shouldSpawn) 0f else nextSpawnAccumulator

                val workingPipes = if (shouldSpawn) {
                    current.pipes + PipePair.generate(GameState.VIRTUAL_WIDTH, GameState.PLAYABLE_HEIGHT)
                } else {
                    current.pipes
                }

                var scoreGained = 0

                // Move obstacles forward, filter out redundant off-screen pipes, check scoring triggers
                workingPipes.forEach { pipe ->
                    val updatedPipe = pipe.update(dt)
                    if (!updatedPipe.isOffScreen()) {
                        // Check if bird has cleared the trailing edge of the pipe pair
                        if (!updatedPipe.passed && updatedPipe.x + updatedPipe.width < GameState.BIRD_X) {
                            finalPipes.add(updatedPipe.copy(passed = true))
                            scoreGained++
                        } else {
                            finalPipes.add(updatedPipe)
                        }
                    }
                }

                // Incremental score update
                val nextScore = current.score + scoreGained
                if (scoreGained > 0) {
                    audioManager.playPoint()
                }

                // 3. Collision Detection checking
                val hasCollided = Collision.checkCollision(
                    bird = updatedBird,
                    pipes = finalPipes,
                    birdX = GameState.BIRD_X,
                    ceilingY = 0f,
                    groundY = GameState.PLAYABLE_HEIGHT
                )

                if (hasCollided) {
                    audioManager.playHit()
                    // Instantly trigger Game Over but don't clear structures so bird can fall
                    val highVal = scoreManager.getHighScore()
                    val newHigh = nextScore > highVal
                    if (newHigh) {
                        scoreManager.updateHighScore(nextScore)
                    }

                    _gameState.value = current.copy(
                        status = GameStatus.GAME_OVER,
                        bird = updatedBird,
                        pipes = finalPipes,
                        score = nextScore,
                        highScore = scoreManager.getHighScore()
                    )
                } else {
                    _gameState.value = current.copy(
                        bird = updatedBird,
                        pipes = finalPipes,
                        score = nextScore,
                        lastSpawnAccumulator = spawnAcc
                    )
                }
            }

            GameStatus.GAME_OVER -> {
                // Bird continues falling naturally to the ground floor after collision
                val updatedBird = current.bird.update(dt, applyGravity = true, GameState.PLAYABLE_HEIGHT)
                _gameState.value = current.copy(
                    bird = updatedBird
                    // Pipes are frozen in place
                )
            }
        }
    }

    /**
     * Intercepts physical user taps on screen coordinates.
     */
    fun onScreenTap() {
        val current = _gameState.value
        when (current.status) {
            GameStatus.READY -> {
                // Transition directly to active play and kick off with an initial jump
                audioManager.playJump()
                _gameState.value = current.copy(
                    status = GameStatus.PLAYING,
                    bird = Bird().jump(),
                    pipes = emptyList(),
                    score = 0,
                    lastSpawnAccumulator = 0f
                )
            }

            GameStatus.PLAYING -> {
                // Execute quick wing flap / jump
                audioManager.playJump()
                _gameState.value = current.copy(
                    bird = current.bird.jump()
                )
            }

            GameStatus.GAME_OVER -> {
                // Tapping in Game Over resets everything to READY (idle floating state)
                _gameState.value = GameState(
                    status = GameStatus.READY,
                    bird = Bird(),
                    pipes = emptyList(),
                    score = 0,
                    highScore = scoreManager.getHighScore()
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
