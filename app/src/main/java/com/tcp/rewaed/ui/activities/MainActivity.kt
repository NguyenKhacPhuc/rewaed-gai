package com.tcp.rewaed.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.tcp.rewaed.R
import com.tcp.rewaed.ui.base.BaseActivity
import com.tcp.rewaed.ui.textdetector.TextRecognitionProcessor
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setNavigationGraph()
    }

    //This is stupid but no one gonna read it anyway =))))))
    var resultReadImage = ""
    private fun setNavigationGraph() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_main_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.nav_main_graph)
        val startDestination =
            if (intent.getBooleanExtra(TextRecognitionProcessor.IS_FROM_TEXT_REG, false)) {
                resultReadImage =
                    intent.getStringExtra(TextRecognitionProcessor.TEXT_REG_VALUE).toString()
                R.id.chatFragment
            } else R.id.splashFragment
        navGraph.setStartDestination(startDestination)
        navController.graph = navGraph
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val bundle = Bundle().apply {
            putBoolean("open_by_service", true)
        }
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_main_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(R.id.chatFragment, bundle)
    }
}