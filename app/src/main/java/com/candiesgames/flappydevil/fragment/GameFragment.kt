package com.candiesgames.flappydevil.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.candiesgames.flappydevil.FragmentManager
import com.candiesgames.flappydevil.R
import com.candiesgames.flappydevil.activity.MainActivity
import com.candiesgames.flappydevil.databinding.FragmentGameBinding
import java.util.*

class GameFragment : Fragment() {

    private lateinit var binding: FragmentGameBinding

    private var characterYPosition = 0 // Current character's Y position
    private var characterVelocity = 0 // Current character's vertical velocity
    private val gravity = 2 // Gravity affecting the character's movement

    private val gameLoopHandler = Handler(Looper.getMainLooper())
    private val gameLoopRunnable = object : Runnable {
        override fun run() {
            updateCharacterPosition() // Update character's position
            updateObstacles() // Update obstacles' positions
            if (!checkCollision()) gameLoopHandler.postDelayed(this, 16) // Run the game loop every 16ms (roughly 60 FPS)
        }
    }

    private val obstacles = mutableListOf<LinearLayout>()
    private var obstacleGenerationTimer: Timer? = null
    private var coinGenerationTimer: Timer? = null
    private var isBottomObstacle = true
    private var gameRunning = true // Flag to track the game state
    private var speed = 10
    private val coins = mutableListOf<ImageView>()
    private var generationPeriod = 2000L
    private val gameMusic = arrayListOf(R.raw.epic1, R.raw.epic2, R.raw.epic3)

    private var coinMediaPlayer: MediaPlayer? = null

    @SuppressLint("ClickableViewAccessibility", "SourceLockedOrientationActivity")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGameBinding.inflate(inflater, container, false)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        MainActivity.playMusic(requireContext(), gameMusic.random())

        coinMediaPlayer = MediaPlayer.create(requireContext(), R.raw.coin_sound)

        binding.root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> jumpCharacter() // Start jumping when the user touches the screen
            }
            true
        }

        binding.btnPlayAgain.setOnClickListener {
            FragmentManager.replaceFragment(GameFragment(), requireActivity())
        }

        startGame()

        return binding.root
    }

    private fun startGame() {
        resetGame()
        startGameLoop()
        startObstacleGeneration()
    }

    private fun resetGame() {
        characterYPosition = 0
        characterVelocity = 0
        obstacles.clear()
        gameLoopHandler.removeCallbacksAndMessages(null)
    }

    private fun startGameLoop() {
        gameLoopHandler.post(gameLoopRunnable)
    }

    private fun startObstacleGeneration() {
        obstacleGenerationTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    generateObstacle()
                }
            }, 0, generationPeriod) // Generate obstacles every 2000 milliseconds (2 seconds)
        }

        coinGenerationTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    generateCoin()
                }
            }, (generationPeriod/1.8f).toLong(), generationPeriod)
        }
    }

    private fun generateObstacle() {
        activity?.runOnUiThread {
            val obstacle = LinearLayout(requireContext()).apply {
                setBackgroundColor(Color.WHITE)
                val displayMetrics = resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels
                val obstacleWidth = (screenWidth * 0.1).toInt()
                val obstacleHeight = (screenHeight * 0.8).toInt()
                layoutParams = ViewGroup.LayoutParams(
                    obstacleWidth,
                    obstacleHeight
                )
            }

            obstacle.translationX = binding.root.width.toFloat()
            if (isBottomObstacle) {
                obstacle.translationY = (binding.root.height/2..binding.root.height-binding.character.height).random().toFloat()
            } else {
                obstacle.translationY = (-binding.root.height/2..-binding.character.height).random().toFloat()
            }

            binding.root.addView(obstacle)
            obstacles.add(obstacle)

            isBottomObstacle = !isBottomObstacle
        }
    }

    private fun generateCoin(){
        activity?.runOnUiThread {
            val coinSize = dpToPx(50f)
            val coin = ImageView(requireContext()).apply {
                setImageResource(R.drawable.dollar)
                layoutParams = ViewGroup.LayoutParams(
                    coinSize,
                    coinSize,
                )
            }

            val coinX = binding.root.width.toFloat()
            val coinY = (0..binding.root.height - coinSize).random().toFloat()

            coin.translationX = coinX
            coin.translationY = coinY

            binding.root.addView(coin)
            coins.add(coin)
        }
    }

    private fun dpToPx(dp: Float): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (dp * scale).toInt()
    }

    private fun updateCharacterPosition() {
        if (gameRunning) { // Only update character position if the game is running
            characterVelocity += gravity // Apply gravity to the character's velocity
            characterYPosition += characterVelocity // Update the character's position based on the velocity

            val screenHeight = binding.root.height
            val characterHeight = binding.character.height
            val bottomBoundary = screenHeight - characterHeight

            if (characterYPosition < 0) {
                characterYPosition = 0
                characterVelocity = 0
            }

            if (characterYPosition > bottomBoundary) {
                characterYPosition = bottomBoundary
                characterVelocity = 0
            }

            binding.character.translationY = characterYPosition.toFloat()
        }
    }

    private fun updateObstacles() {
        val obstaclesToRemove = mutableListOf<LinearLayout>()
        val coinsToRemove = mutableListOf<ImageView>()

        for (obstacle in obstacles) {
            obstacle.translationX -= speed

            // Check if the obstacle has moved outside the left edge of the screen
            if (obstacle.translationX + obstacle.width < 0) {
                obstaclesToRemove.add(obstacle)
            }
        }

        for (coin in coins) {
            coin.translationX -= speed

            // Check if the coin has moved outside the left edge of the screen
            if (coin.translationX + coin.width < 0) {
                coinsToRemove.add(coin)
            }
        }

        for (obstacleToRemove in obstaclesToRemove) {
            binding.root.removeView(obstacleToRemove)
            obstacles.remove(obstacleToRemove)
        }

        for (coinToRemove in coinsToRemove) {
            binding.root.removeView(coinToRemove)
            coins.remove(coinToRemove)
        }
    }


    @SuppressLint("SetTextI18n")
    private fun checkCollision(): Boolean {
        val characterRect = Rect(
            binding.character.left + 100, // Adjust left boundary
            binding.character.top + characterYPosition + 20, // Adjust top boundary
            binding.character.right - 100, // Adjust right boundary
            binding.character.bottom + characterYPosition - 20 // Adjust bottom boundary
        )

        for (obstacle in obstacles) {
            val obstacleRect = Rect(
                obstacle.left + obstacle.translationX.toInt(),
                obstacle.top + obstacle.translationY.toInt(),
                obstacle.right + obstacle.translationX.toInt(),
                obstacle.bottom + obstacle.translationY.toInt()
            )

            if (Rect.intersects(characterRect, obstacleRect)) {
                stopGame()
                gameOver()
                return true
            }
        }

        // Check collision with coins
        val coinsToRemove = mutableListOf<ImageView>()
        for (coin in coins) {
            val coinRect = Rect(
                coin.left + coin.translationX.toInt() + 20, // Adjust left boundary
                coin.top + coin.translationY.toInt() + 20, // Adjust top boundary
                coin.right + coin.translationX.toInt() - 20, // Adjust right boundary
                coin.bottom + coin.translationY.toInt() - 20 // Adjust bottom boundary
            )

            if (Rect.intersects(characterRect, coinRect)) {
                coinsToRemove.add(coin)
                coinMediaPlayer?.start()
                speed++
                binding.balance.text = (binding.balance.text.toString().toInt()+1).toString()
            }
        }

        for (coinToRemove in coinsToRemove) {
            binding.root.removeView(coinToRemove)
            coins.remove(coinToRemove)
        }

        return false
    }

    private fun vibratePhone() {
        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(600, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(600)
        }
    }

    private fun gameOver(){
        binding.gameOverLayout.visibility = View.VISIBLE
        MainActivity.playMusic(requireContext(), R.raw.restless)
        vibratePhone()
    }

    private fun stopGame() {
        gameRunning = false
        gameLoopHandler.removeCallbacksAndMessages(null) // Stop the game loop
        obstacleGenerationTimer?.cancel() // Stop obstacle generation
        coinGenerationTimer?.cancel()
        speed = 0 // Freeze the obstacles
        characterVelocity = 0 // Freeze the character
    }

    private fun jumpCharacter() {
        characterVelocity = -30 // Adjust the velocity to control the height and speed of the jump
    }

    override fun onDestroyView() {
        super.onDestroyView()
        obstacleGenerationTimer?.cancel()
        gameLoopHandler.removeCallbacksAndMessages(null)
    }
}