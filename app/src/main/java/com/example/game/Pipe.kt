package com.example.game

import kotlin.random.Random

/**
 * Represents a vertically aligned pair of top and bottom obstacles (pipes).
 * Contains structural variables to handle movement and whether the player cleared it.
 */
data class PipePair(
    val x: Float,                 // Horizontal left coordinate (logical coords, px)
    val gapTop: Float,            // Top-most pixel elevation of the passage gap
    val gapSize: Float = 320f,    // Vertical opening height (px)
    val width: Float = 120f,      // Pipe width (px)
    val passed: Boolean = false   // Checked to avoid duplicate score addition
) {
    companion object {
        const val WIDTH = 120f
        const val GAP_SIZE = 320f
        const val SPEED = 240f             // Horizontal travel velocity towards left (px/s)
        const val SPAWN_INTERVAL = 2.5f    // Seconds between column additions

        /**
         * Generates a PipePair on the right edge of playing field.
         * @param screenWidth The total virtual coordinate width.
         * @param playableHeight Height inside viewport excluding the grass ground panel.
         */
        fun generate(screenWidth: Float, playableHeight: Float): PipePair {
            val minGapTop = 150f
            val maxGapTop = playableHeight - GAP_SIZE - 150f
            // Generate a random gap top position between safe margins
            val gapTop = Random.nextFloat() * (maxGapTop - minGapTop) + minGapTop
            return PipePair(
                x = screenWidth,
                gapTop = gapTop,
                gapSize = GAP_SIZE,
                width = WIDTH,
                passed = false
            )
        }
    }

    /**
     * Shifts position of pipe column towards the left screen edge.
     */
    fun update(dt: Float): PipePair {
        return copy(x = x - SPEED * dt)
    }

    /**
     * Check if the pipe pair is completely out of screen viewport boundaries.
     */
    fun isOffScreen(): Boolean {
        return x + width < 0f
    }
}
