package com.example.game

import android.content.Context
import android.content.SharedPreferences

/**
 * Handles high score local persistence using SharedPreferences.
 * Provides synchronized loading, checking, and updating triggers.
 */
class ScoreManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "flappy_bird_game_prefs", 
        Context.MODE_PRIVATE
    )

    /**
     * Returns the currently saved high score.
     */
    fun getHighScore(): Int {
        return prefs.getInt(KEY_HIGH_SCORE, 0)
    }

    /**
     * Inspects the current score and updates SharedPreferences if surpassed.
     * @return true if a new high score is established, otherwise false.
     */
    fun updateHighScore(currentScore: Int): Boolean {
        val currentHigh = getHighScore()
        if (currentScore > currentHigh) {
            prefs.edit().putInt(KEY_HIGH_SCORE, currentScore).apply()
            return true
        }
        return false
    }

    companion object {
        private const val KEY_HIGH_SCORE = "key_high_score"
    }
}
