package com.candiesgames.flappydevil.view_model

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import java.util.*

class MainViewModel(private val context: Context) : ViewModel() {

    private val _isConnection = MutableLiveData<Boolean>()
    val isConnection: LiveData<Boolean> = _isConnection

    // Check Internet connection
    @SuppressLint("MissingPermission")
    fun checkForInternet() {
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        _isConnection.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connManager.getNetworkCapabilities(connManager.activeNetwork)
            networkCapabilities != null
        } else {
            val activeNetwork = connManager.activeNetworkInfo
            activeNetwork?.isConnectedOrConnecting == true && activeNetwork.isAvailable
        }
    }

}