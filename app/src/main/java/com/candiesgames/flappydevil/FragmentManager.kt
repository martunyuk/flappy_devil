package com.candiesgames.flappydevil

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

object FragmentManager {
    fun replaceFragment(
        fragment: Fragment?,
        activity: Activity,
    ) {
        val fragmentManager = (activity as AppCompatActivity).supportFragmentManager
        val fragmentTransition = fragmentManager.beginTransaction()
        fragmentTransition.replace(R.id.frame_layout, fragment!!)
        fragmentTransition.commit()
    }
}