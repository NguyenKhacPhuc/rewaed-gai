package com.tcp.rewaed.ui.activities

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.tcp.rewaed.R
import com.tcp.rewaed.data.models.ChatPostBody
import com.tcp.rewaed.databinding.ActivityAnywhereDialogBinding
import com.tcp.rewaed.ui.adapters.ChatListAdapter
import com.tcp.rewaed.ui.base.BaseActivity
import com.tcp.rewaed.ui.viewmodels.ChatViewModel
import com.tcp.rewaed.utils.*
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class AnywhereDialogActivity : BaseActivity(), TextToSpeech.OnInitListener {

    private val chatListAdapter by lazy {
        ChatListAdapter(mViewModel)
    }

    private lateinit var mViewBinding: ActivityAnywhereDialogBinding
    private val mViewModel: ChatViewModel by viewModels()

    private var textToSpeech: TextToSpeech? = null
    private var isNeedToSpeakAnswer: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        this.setFinishOnTouchOutside(false)
        super.onCreate(savedInstanceState)
        textToSpeech = TextToSpeech(this, this)
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d("AnywhereDialogActivity", "textToSpeech onStart")
                runOnUiThread {
                    mViewBinding.animationView.visibility = View.VISIBLE
                    mViewBinding.imgStopSpeaking.visibility = View.VISIBLE
                    mViewBinding.animationView.playAnimation()
                }
            }

            override fun onDone(utteranceId: String?) {
                Log.d("AnywhereDialogActivity", "textToSpeech onDone")
                isNeedToSpeakAnswer = false
                runOnUiThread {
                    mViewBinding.animationView.visibility = View.INVISIBLE
                    mViewBinding.imgStopSpeaking.visibility = View.INVISIBLE
                    mViewBinding.animationView.pauseAnimation()
                }
            }

            override fun onError(utteranceId: String?) {
                Log.e("ChatFragment", "textToSpeech onError")
            }

        })
        mViewBinding = DataBindingUtil.setContentView(this, R.layout.activity_anywhere_dialog)
        mViewModel.maxTokensLength =
            SharedPref.getStringPref(this, SharedPref.KEY_TOKEN_LENGTH).toInt()
        setupUI()
        observeAPICall()
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        chatListAdapter.addItems(mViewModel.chatMessageList)
        mViewBinding.apply {
            if (intent.action == Intent.ACTION_PROCESS_TEXT) {
                intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)?.let { copiedText ->
                    mViewModel.copiedText = copiedText
                    mViewModel.textCopied =
                        "It appears that you have copied:\n$copiedText\n\nWhat would you like me to do with it"
                }
            }
            ivClose.setOnClickListener {
                finish()
            }
            rvChatList.apply {
                layoutManager = LinearLayoutManager(this@AnywhereDialogActivity)
                adapter = chatListAdapter
            }

            fabVoice.setOnClickListener {
                dismissKeyboard(it)
                isNeedToSpeakAnswer = true
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                try {
                    startActivityForResult(intent, 2)
                } catch (a: ActivityNotFoundException) {
                    Toast.makeText(
                        this@AnywhereDialogActivity,
                        "Error ${a.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            fabSend.setOnClickListener {
                dismissKeyboard(it)
                if (SharedPref.getStringPref(this@AnywhereDialogActivity, SharedPref.KEY_API_KEY)
                        .isNullOrBlank()
                ) {
                    this@AnywhereDialogActivity.showToast(getString(R.string.message_add_api_key))
                    return@setOnClickListener
                }
                if (etMessage.text.toString().isNullOrBlank()) {
                    showToast(getString(R.string.message_enter_some_text))
                    return@setOnClickListener
                }
                val content =
                    if (mViewModel.copiedText.isNotBlank()) "${mViewModel.copiedText}\n${etMessage.text.toString()}" else etMessage.text.toString()
                mViewModel.chatMessageList.add(
                    ChatPostBody.Message(
                        content = content,
                        role = ChatRole.USER.name.lowercase()
                    )
                )
                chatListAdapter.addItems(mViewModel.chatMessageList)
                rvChatList.scrollToPosition(
                    chatListAdapter.itemCount.minus(1)
                )
                mViewModel.postMessage()
                etMessage.text?.clear()
                mViewModel.copiedText = ""
            }

            imgStopSpeaking.setOnClickListener {
                Log.d("ChatFragment", "textToSpeech onDone")
                textToSpeech?.stop()
                isNeedToSpeakAnswer = false
                runOnUiThread {
                    mViewBinding.animationView.visibility = View.INVISIBLE
                    mViewBinding.imgStopSpeaking.visibility = View.INVISIBLE
                    mViewBinding.animationView.pauseAnimation()
                }
            }
        }
    }

    private fun observeAPICall() {
        mViewModel.chatLiveData.observe(this, EventObserver { state ->
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
                    showToast(state.message)
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            2 -> {
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