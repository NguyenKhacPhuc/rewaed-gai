package com.tcp.rewaed.ui.chat

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.tcp.rewaed.R
import com.tcp.rewaed.data.models.ChatPostBody
import com.tcp.rewaed.databinding.FragmentChatBinding
import com.tcp.rewaed.ui.activities.MainActivity
import com.tcp.rewaed.ui.adapters.ChatListAdapter
import com.tcp.rewaed.ui.base.BaseFragment
import com.tcp.rewaed.ui.textdetector.ChooserActivity
import com.tcp.rewaed.ui.textdetector.TextRecognitionProcessor
import com.tcp.rewaed.ui.viewmodels.ChatViewModel
import com.tcp.rewaed.utils.*
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class ChatFragment : BaseFragment<ChatViewModel, FragmentChatBinding>(),
    TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var isNeedToSpeakAnswer: Boolean = false

    companion object {
        private const val PERMISSION_REQUESTS = 1
        private val REQUIRED_RUNTIME_PERMISSIONS =
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
    }

    private val chatListAdapter by lazy {
        ChatListAdapter(mViewModel)
    }
    override val mViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textToSpeech = TextToSpeech(activity, this)
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d("ChatFragment", "textToSpeech onStart")
                activity?.runOnUiThread {
                    mViewBinding.animationView.visibility = View.VISIBLE
                    mViewBinding.imgStopSpeaking.visibility = View.VISIBLE
                    mViewBinding.animationView.playAnimation()
                }
            }

            override fun onDone(utteranceId: String?) {
                Log.d("ChatFragment", "textToSpeech onDone")
                isNeedToSpeakAnswer = false
                activity?.runOnUiThread {
                    mViewBinding.animationView.visibility = View.INVISIBLE
                    mViewBinding.imgStopSpeaking.visibility = View.INVISIBLE
                    mViewBinding.animationView.pauseAnimation()
                }
            }

            override fun onError(utteranceId: String?) {
                Log.e("ChatFragment", "textToSpeech onError")
            }

        })
        mViewModel.maxTokensLength =
            SharedPref.getStringPref(requireContext(), SharedPref.KEY_TOKEN_LENGTH).toInt()
        chatListAdapter.addItems(mViewModel.chatMessageList)
        allowOverlayPermission()
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentChatBinding {
        return FragmentChatBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        addMenuProvider(R.menu.menu) { menuItem ->
            when (menuItem.itemId) {
                R.id.faq -> {
                    navigate(ChatFragmentDirections.actionChatFragmentToFaqFragment())
                    true
                }
                R.id.settings -> {
                    navigate(ChatFragmentDirections.actionChatFragmentToSettingsFragment())
                    true
                }
                else -> false
            }
        }
        mViewBinding.apply {
            lifecycleOwner = viewLifecycleOwner
            (activity as? AppCompatActivity)?.setSupportActionBar(layoutToolbar.toolbar)
            layoutToolbar.tvToolbarTitle.text = getString(R.string.toolbar_title_anywhere_gpt)
            rvChatList.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = chatListAdapter
            }

            fabSend.setOnClickListener {
                requireContext().dismissKeyboard(it)
                if (SharedPref.getStringPref(requireContext(), SharedPref.KEY_API_KEY)
                        .isNullOrBlank()
                ) {
                    requireContext().showToast(getString(R.string.message_add_api_key))
                    return@setOnClickListener
                }
                if (etMessage.text.toString().isNullOrBlank()) {
                    requireContext().showToast(getString(R.string.message_enter_some_text))
                    return@setOnClickListener
                }
                mViewModel.chatMessageList.add(
                    ChatPostBody.Message(
                        content = etMessage.text.toString(),
                        role = ChatRole.USER.name.lowercase()
                    )
                )
                chatListAdapter.addItems(mViewModel.chatMessageList)
                rvChatList.scrollToPosition(
                    chatListAdapter.itemCount.minus(1)
                )
                mViewModel.postMessage()
                etMessage.text?.clear()
            }

            fabImageToText.setOnClickListener {
                if (!allRuntimePermissionsGranted()) {
                    getRuntimePermissions()
                }
                val intent =
                    Intent(
                        this@ChatFragment.requireActivity(),
                        ChooserActivity::class.java
                    )
                startActivity(intent)
            }

            fabVoice.setOnClickListener {
                isNeedToSpeakAnswer = true
                requireContext().dismissKeyboard(it)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                try {
                    startActivityForResult(intent, 1)
                } catch (a: ActivityNotFoundException) {
                    Toast.makeText(activity, "Error ${a.message}", Toast.LENGTH_SHORT).show()
                }
            }

            imgStopSpeaking.setOnClickListener {
                Log.d("ChatFragment", "textToSpeech onDone")
                textToSpeech?.stop()
                isNeedToSpeakAnswer = false
                activity?.runOnUiThread {
                    mViewBinding.animationView.visibility = View.INVISIBLE
                    mViewBinding.imgStopSpeaking.visibility = View.INVISIBLE
                    mViewBinding.animationView.pauseAnimation()
                }
            }
            //Another stupid idea, but again, nobody gonna see this =))))))))))))
            etMessage.setText((this@ChatFragment.activity as MainActivity).resultReadImage)
        }
    }

    override fun observeAPICall() {
        mViewModel.chatLiveData.observe(viewLifecycleOwner, EventObserver { state ->
            when (state) {
                is State.Loading -> {
                    mViewBinding.apply {
                        pbLoading.show()
                        fabSend.invisible()
                    }
                }
                is State.Success -> {
                    if (state.data.choices.isNotEmpty()) {
                        mViewModel.chatMessageList.add(
                            ChatPostBody.Message(
                                content = state.data.choices.first().message.content,
                                role = ChatRole.ASSISTANT.name.lowercase()
                            )
                        )
                        if (isNeedToSpeakAnswer) {
                            convertTextToSpeech(state.data.choices.first().message.content)
                        }
                        chatListAdapter.addItems(mViewModel.chatMessageList)
                        mViewBinding.apply {
                            rvChatList.scrollToPosition(
                                chatListAdapter.itemCount.minus(1)
                            )
                            pbLoading.hide()
                            fabSend.show()
                        }
                    }
                }
                is State.Error -> {
                    mViewBinding.apply {
                        pbLoading.hide()
                        fabSend.show()
                    }
                    requireContext().showToast(state.message)
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            1 -> {
                if (resultCode === AppCompatActivity.RESULT_OK && null != android.R.attr.data) {
                    val result: ArrayList<String> =
                        data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)!!
                    val textOutput = result[0]
                    mViewBinding.etMessage.text?.clear()
                    mViewBinding.etMessage.text?.append(textOutput)
                    mViewBinding.fabSend.performClick()
                }
            }
        }
    }

    private fun allowOverlayPermission() {
        if (!Settings.canDrawOverlays(requireContext())) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Permission Request for \"Draw Over Other Apps\"")
            builder.setMessage("In order to provide you with the best experience, our app requires the \"Draw over other apps\" permission. This allows us to display our app's features on top of other apps. Please grant this permission to continue using our app")
            builder.setCancelable(false)
                .setPositiveButton(
                    "Ok"
                ) { dialog, which ->
                    dialog.dismiss()
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${requireContext().packageName}")
                    )
                    startActivityForResult(intent, 0)
                }
                .setNegativeButton(
                    "Cancel"
                ) { dialog, which ->
                    dialog.dismiss()
                }
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }
    }

    private fun allRuntimePermissionsGranted(): Boolean {
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission?.let {
                if (!isPermissionGranted(this@ChatFragment.requireActivity(), it)) {
                    return false
                }
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val permissionsToRequest = ArrayList<String>()
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission.let {
                if (!isPermissionGranted(this@ChatFragment.requireActivity(), it)) {
                    permissionsToRequest.add(permission)
                }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this@ChatFragment.requireActivity(),
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUESTS
            )
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("Permission", "Permission granted: $permission")
            return true
        }
        Log.i("Permission", "Permission NOT granted: $permission")
        return false
    }

    private fun convertTextToSpeech(content: String) {
        textToSpeech?.speak(content, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = textToSpeech?.setLanguage(Locale.ENGLISH)
            if (res == TextToSpeech.LANG_NOT_SUPPORTED || res == TextToSpeech.LANG_MISSING_DATA) {
                Log.e("TTS", "language not supported!")
            }
        } else {
            Log.e("TTS", "TTS Failed")
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }
}