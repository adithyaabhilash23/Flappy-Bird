package com.example.game

/**
 * Handles precise mathematical intersection verification (AABB bounding box)
 * between the bird and solid boundaries like pipe bounds and ground tiles.
 */
object Collision {

    /**
     * Checks if two rectangles intersect.
     */
    fun intersects(
        x1: Float, y1: Float, w1: Float, h1: Float,
        x2: Float, y2: Float, w2: Float, h2: Float
    ): Boolean {
        return x1 < x2 + w2 &&
               x1 + w1 > x2 &&
               y1 < y2 + h2 &&
               y1 + h1 > y2
    }

    /**
     * Inspects if the bird is currently touching any hazard bounds.
     * @param bird The current state of the bird.
     * @param pipes All active columns.
     * @param birdX The fixed horizontal position of the bird (logical, normally 200f).
     * @param ceilingY Top vertical limit (normally 0f).
     * @param groundY Ground level vertical offset (where grass begins).
     * @return true if collision detected; triggers GAME_OVER.
     */
    fun checkCollision(
        bird: Bird,
        pipes: List<PipePair>,
        birdX: Float = 200f,
        ceilingY: Float = 0f,
        groundY: Float
    ): Boolean {
        // 1. Check screen floor boundary collision (bird bottom touches or exceeds floor)
        if (bird.y + bird.height >= groundY) {
            return true
        }

        // 2. Check screen ceiling boundary collision (bird top touches or goes above ceiling)
        if (bird.y <= ceilingY) {
            return true
        }

        // 3. Inspect pipe interactions
        for (pipe in pipes) {
            // A. Top column collision: extends from y = 0 to gapTop
            val hitTopPipe = intersects(
                x1 = birdX,
                y1 = bird.y,
                w1 = bird.width,
                h1 = bird.height,
                x2 = pipe.x,
                y2 = 0f,
                w2 = pipe.width,
                h2 = pipe.gapTop
            )

            // B. Bottom column collision: extends from gapTop + gapSize down to groundY
            val bottomPipeY = pipe.gapTop + pipe.gapSize
            val hitBottomPipe = intersects(
                x1 = birdX,
                y1 = bird.y,
                w1 = bird.width,
                h1 = bird.height,
                x2 = pipe.x,
                y2 = bottomPipeY,
                w2 = pipe.width,
                h2 = groundY - bottomPipeY
            )

            if (hitTopPipe || hitBottomPipe) {
                return true
            }
        }
        return false
    }
}
