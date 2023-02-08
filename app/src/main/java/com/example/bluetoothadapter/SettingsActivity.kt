package com.example.bluetoothadapter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.bluetoothadapter.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    companion object {
        const val TAG_MUSIC = "settingCheckMusic"
        const val TAG_VIBRO = "settingCheckVibro"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("SETTINGS", MODE_PRIVATE)
        val editPreferences = sharedPreferences.edit()

        binding.apply {

            btnMainPrevActivity.setOnClickListener {
                finish()
            }

            settingCheckMusic.isChecked = sharedPreferences.getBoolean(TAG_MUSIC, false)

            settingCheckMusic.setOnCheckedChangeListener { _, isChecked ->
                editPreferences.putBoolean(TAG_MUSIC, isChecked).apply()
            }

            settingCheckVibro.isChecked = sharedPreferences.getBoolean(TAG_VIBRO, false)

            settingCheckVibro.setOnCheckedChangeListener { _, isChecked ->
                editPreferences.putBoolean(TAG_VIBRO, isChecked).apply()
            }
        }
    }
}