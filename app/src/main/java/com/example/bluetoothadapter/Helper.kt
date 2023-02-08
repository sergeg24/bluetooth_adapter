package com.example.bluetoothadapter

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

const val TAG = "MyLog"

fun toast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

fun Fragment.vibrateOrSound() {
    try {

        val sharedPreferences = requireContext().getSharedPreferences("SETTINGS", AppCompatActivity.MODE_PRIVATE)
        val music = sharedPreferences.getBoolean(SettingsActivity.TAG_MUSIC, false)
        val vibro = sharedPreferences.getBoolean(SettingsActivity.TAG_VIBRO, false)

        if (music) {
            val soundPlayer = MediaPlayer.create(requireContext(), R.raw.music)
            soundPlayer.setVolume(0.02f, 0.02f);
            soundPlayer.start()
        }

        if (vibro) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                (requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            else
                activity?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        }

    } catch (e: SecurityException) {}
}