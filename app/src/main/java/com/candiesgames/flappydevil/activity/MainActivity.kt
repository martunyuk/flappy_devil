package com.candiesgames.flappydevil.activity

import android.content.Context
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.candiesgames.flappydevil.FragmentManager
import com.candiesgames.flappydevil.R
import com.candiesgames.flappydevil.fragment.GameFragment
import com.candiesgames.flappydevil.fragment.WebViewFragment
import com.candiesgames.flappydevil.room.MyDatabase
import com.candiesgames.flappydevil.room.MyEntity
import com.candiesgames.flappydevil.view_model.MainViewModel
import com.candiesgames.flappydevil.view_model.MainViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Random

class MainActivity : AppCompatActivity() {

    private lateinit var mainVM: MainViewModel
    lateinit var database: MyDatabase

    companion object {
        var mediaPlayer: MediaPlayer? = null

        fun playMusic(context: Context, audio: Int){
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, audio)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = Room.databaseBuilder(applicationContext, MyDatabase::class.java, "my_database").build()

        mainVM = ViewModelProvider(this, MainViewModelFactory(this))[MainViewModel::class.java]

        hideSystemBar()
        setNightModeNo()
        openRandomFragment()
    }

    private fun openRandomFragment() {

        val random = Random()
        val randomBoolean = random.nextBoolean()

        CoroutineScope(Dispatchers.IO).launch {
            if (database.myDao().getFirst() == null){
                val myEntity = MyEntity(randomBoolean)
                database.myDao().insert(myEntity)
            }

            checkInetConnect(database.myDao().getFirst()!!.myBoolean)
        }

    }

    private fun checkInetConnect(randomBoolean: Boolean){
        this.runOnUiThread{
            mainVM.checkForInternet()

            mainVM.isConnection.observe(this) { isConnection ->
                if (isConnection && randomBoolean) {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    FragmentManager.replaceFragment(WebViewFragment(), this@MainActivity)
                } else {
                    FragmentManager.replaceFragment(GameFragment(), this@MainActivity)
                }
            }
        }
    }

    private fun hideSystemBar() {
        WindowCompat.setDecorFitsSystemWindows(this.window, false)

        WindowInsetsControllerCompat(
            this.window,
            this.window.decorView.rootView
        ).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            this.window.decorView.setOnSystemUiVisibilityChangeListener {
                if (it != 0 && View.SYSTEM_UI_FLAG_FULLSCREEN != 0) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }

                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

        }
    }

    private fun setNightModeNo() {
        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onStart() {
        super.onStart()
        mediaPlayer?.start()
    }

}