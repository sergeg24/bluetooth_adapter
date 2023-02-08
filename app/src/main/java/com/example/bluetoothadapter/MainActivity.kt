package com.example.bluetoothadapter

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.bluetoothadapter.data.DataModel
import com.example.bluetoothadapter.databinding.ActivityMainBinding
import com.example.bluetoothadapter.fragment.SearchListFragment

class MainActivity : AppCompatActivity() {

    private val dataModel: DataModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            toast(this, "«Ваше устройство не поддерживает Bluetooth BLE и будет выключено»")
            finish();
            return
        }

        val bManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bAdapter = bManager.adapter

        supportFragmentManager.beginTransaction().replace(R.id.fragment, DeviceListFragment(bAdapter)).commit()
        supportFragmentManager.beginTransaction().replace(R.id.fragment2, SearchListFragment(bAdapter)).commit()

        binding.apply {
            dataModel.clickMenuF1.observe(this@MainActivity) {
                DrawerLayout.openDrawer(GravityCompat.START)
            }
            //Навигация выплывающего меню
            NavigationView.setNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_settings -> {
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                        DrawerLayout.closeDrawer(GravityCompat.START)
                    }
                }
                true
            }
        }
    }
}