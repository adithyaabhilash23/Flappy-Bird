package com.example.game

/**
 * Represents the player-controlled Bird in the game.
 * Stores modern game state such as coordinates, velocity, and rotation angle.
 */
data class Bird(
    val y: Float = 600f,         // Vertically centered at startup (logical coords)
    val vy: Float = 0f,          // Current vertical velocity (px/s)
    val width: Float = 50f,      // Fixed hitbox width (px)
    val height: Float = 50f,     // Fixed hitbox height (px)
    val rotation: Float = 0f     // Render angle from -25 (diving up) to 90 (nosedive)
) {
    companion object {
        const val WIDTH = 50f
        const val HEIGHT = 50f
        const val GRAVITY = 1800f          // px/s^2
        const val JUMP_VELOCITY = -650f     // px/s (upwards is negative Y)
        const val TERMINAL_VELOCITY = 900f  // Max falling speed
    }

    /**
     * Triggers a flap/jump, overriding the vertical velocity to go upwards.
     */
    fun jump(): Bird {
        return copy(vy = JUMP_VELOCITY)
    }

    /**
     * Updates physics for a single frame base on delta time.
     * @param dt Elapsed time in seconds.
     * @param applyGravity Set to false during READY state.
     * @param groundY The vertical roof of the floor.
     */
    fun update(dt: Float, applyGravity: Boolean, groundY: Float): Bird {
        if (!applyGravity) {
            // Idle float animation during Ready State (floating up and down gently)
            val time = System.currentTimeMillis() / 200.0
            val floatOffset = Math.sin(time).toFloat() * 1.5f
            return copy(
                y = 600f + floatOffset * 15f,
                vy = 0f,
                rotation = 0f
            )
        }

        // Apply constant gravity
        val nextVy = (vy + GRAVITY * dt).coerceAtMost(TERMINAL_VELOCITY)
        val nextY = (y + nextVy * dt).coerceAtMost(groundY - height).coerceAtLeast(-100f)

        // Interpolate rotation pitch based on velocity:
        // Going up (negative velocity) -> rotate up (-25 degrees)
        // Falling down (positive velocity) -> rotate down (up to 95 degrees)
        val targetRotation = if (nextVy < 0f) {
            // Quick curve going up
            val ratio = (nextVy / JUMP_VELOCITY).coerceIn(0f, 1f)
            -25f * ratio
        } else {
            // Gentle then rapid nosedive when falling
            val ratio = (nextVy / TERMINAL_VELOCITY).coerceIn(0f, 1f)
            val angle = -25f + (90f - (-25f)) * ratio
            angle.coerceIn(-25f, 90f)
        }

        return copy(
            y = nextY,
            vy = nextVy,
            rotation = targetRotation
        )
    }
}
