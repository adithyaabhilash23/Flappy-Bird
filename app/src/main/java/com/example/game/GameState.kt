package com.example.game

/**
 * Defines the main operating states of the game.
 */
enum class GameStatus {
    READY,      // Idle state: "Tap to Start", floating idle hover animation active
    PLAYING,    // Live play: Gravity applied to bird, columns rolling, collision active
    GAME_OVER   // Crash state: Game over board, bird falls to floor, restart ready
}

/**
 * Capture state of all actors and rules for a frame in a single, robust immutable data class.
 * This pattern aligns with pure Compose unidirectional flow and state management.
 */
data class GameState(
    val status: GameStatus = GameStatus.READY,
    val bird: Bird = Bird(),
    val pipes: List<PipePair> = emptyList(),
    val score: Int = 0,
    val highScore: Int = 0,
    val lastSpawnAccumulator: Float = 0f    // Tracks elapsed seconds since last column spawned
) {
    companion object {
        // Virtual Screen Boundaries
        const val VIRTUAL_WIDTH = 1000f
        const val VIRTUAL_HEIGHT = 1600f
        const val GROUND_HEIGHT = 150f      // Height of the dirty & grassy floor at the bottom
        const val PLAYABLE_HEIGHT = VIRTUAL_HEIGHT - GROUND_HEIGHT
        const val BIRD_X = 200f             // Fixed horizontal placement of bird
    }
}
