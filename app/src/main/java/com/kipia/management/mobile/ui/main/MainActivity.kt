package com.kipia.management.mobile.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kipia.management.mobile.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        println("KIPiA Management Mobile успешно запущено!")
    }
}