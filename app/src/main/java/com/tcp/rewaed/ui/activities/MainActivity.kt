package com.tcp.rewaed.ui.activities

import android.os.Bundle
import com.tcp.rewaed.R
import com.tcp.rewaed.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}