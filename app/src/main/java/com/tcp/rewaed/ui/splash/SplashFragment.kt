package com.tcp.rewaed.ui.splash

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tcp.rewaed.BuildConfig
import com.tcp.rewaed.R
import com.tcp.rewaed.databinding.FragmentSplashBinding
import com.tcp.rewaed.ui.base.BaseFragment
import com.tcp.rewaed.ui.viewmodels.SplashState
import com.tcp.rewaed.ui.viewmodels.SplashViewModel
import com.tcp.rewaed.utils.navigate
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashFragment : BaseFragment<SplashViewModel, FragmentSplashBinding>() {

    override val mViewModel: SplashViewModel by viewModels()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSplashBinding {
        return FragmentSplashBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        mViewBinding.apply {
            lifecycleOwner = viewLifecycleOwner
            tvAppVersion.text = getString(R.string.label_app_version, BuildConfig.VERSION_NAME)
        }
    }

    override fun initializeObserver() {
        mViewModel.splashStateLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is SplashState.SplashScreen -> {
                    navigate(SplashFragmentDirections.actionSplashFragmentToChatFragment())
                }
            }
        }
    }

}