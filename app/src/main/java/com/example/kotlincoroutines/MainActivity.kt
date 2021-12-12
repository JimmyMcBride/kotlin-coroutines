package com.example.kotlincoroutines

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(
            TAG, "Pre lifecycleScope - onCreate, running on thread: ${
                Thread.currentThread()
                    .name
            }"
        )
        lifecycleScope.launch(Dispatchers.IO) {
            Log.d(
                TAG, "LifecycleScope, context IO - running on thread: ${
                    Thread.currentThread()
                        .name
                }"
            )
            withContext(Dispatchers.Default) {
                Log.d(
                    TAG, "LifecycleScope, context DEFAULT - running on thread: ${
                        Thread.currentThread()
                            .name
                    }"
                )
            }
        }
        Log.d(
            TAG, "Post lifecycleScope - onCreate running on thread: ${
                Thread.currentThread()
                    .name
            }"
        )
    }

    companion object {
        const val TAG = "KotlinCoroutines"
    }
}