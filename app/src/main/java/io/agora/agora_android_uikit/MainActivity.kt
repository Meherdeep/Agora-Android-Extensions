package io.agora.agora_android_uikit

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

import io.agora.agorauikit_android.*
import io.agora.rtc2.Constants
import io.agora.rtc2.IMediaExtensionObserver
import com.synervoz.voicefilters.AgoraExtensionPropertyInterface
import com.synervoz.voicefilters.SynervozVoiceFilter
import com.synervoz.voicefilters.SynervozVoiceFilterExtensionManager
import com.synervoz.voicefilters.VoiceEffectsComponentConfiguration


@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity(), IMediaExtensionObserver {

    var agView: AgoraVideoViewer? = null
    var synervozVoiceFilter = SynervozVoiceFilter()
    var effectEnabled: Boolean = true
    var echoEnabled: Boolean = false
    var reverbEnabled: Boolean = false
    var pitchShiftEnabled: Boolean = false
    var flangerEnabled: Boolean = false
    private val TAG = "AgoraUIKit-Synervoz"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        synervozVoiceFilter.agoraExtensionPropertyInterface =
            AgoraExtensionPropertyInterface { vendorName, filterName, propertyName, propertyValue ->
                agView?.agkit?.setExtensionProperty(vendorName, filterName, propertyName, propertyValue)
            }

        try {
            agView = AgoraVideoViewer(
                this, AgoraConnectionData("my-app-id", extensionName = listOf(SynervozVoiceFilterExtensionManager.EXTENSION_NAME), iMediaExtensionObserver = this),
                agoraSettings = this.settingsWithExtraButtons()
            )
        } catch (e: Exception) {
            print("Could not initialise AgoraVideoViewer. Check your App ID is valid.")
            print(e.message)
            return
        }
        val set = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

        this.addContentView(agView, set)

        agView?.agkit?.enableExtension(
            SynervozVoiceFilterExtensionManager.EXTENSION_VENDOR_NAME,
            SynervozVoiceFilterExtensionManager.EXTENSION_AUDIO_FILTER_NAME,
            true
        )

        // Check that the camera and mic permissions are accepted before attempting to join
        if (AgoraVideoViewer.requestPermissions(this)) {
            agView!!.join("test", role = Constants.CLIENT_ROLE_BROADCASTER)
        } else {
            val joinButton = Button(this)
            joinButton.text = "Allow Camera and Microphone, then click here"
            joinButton.setOnClickListener(View.OnClickListener {
                // When the button is clicked, check permissions again and join channel
                // if permissions are granted.
                if (AgoraVideoViewer.requestPermissions(this)) {
                    (joinButton.parent as ViewGroup).removeView(joinButton)
                    agView!!.join("test", role=Constants.CLIENT_ROLE_BROADCASTER)
                }
            })
            joinButton.setBackgroundColor(Color.GREEN)
            joinButton.setTextColor(Color.RED)
            this.addContentView(joinButton, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 300))
        }
    }

    fun enableEffectTapped() {

        agView?.agkit?.setExtensionProperty(
            SynervozVoiceFilterExtensionManager.EXTENSION_VENDOR_NAME,
            SynervozVoiceFilterExtensionManager.EXTENSION_AUDIO_FILTER_NAME,
            "apiSecret",
            "<--YOUR API SECRET-->"
        )

        agView?.agkit?.setExtensionProperty(
            SynervozVoiceFilterExtensionManager.EXTENSION_VENDOR_NAME,
            SynervozVoiceFilterExtensionManager.EXTENSION_AUDIO_FILTER_NAME,
            "apiKey",
            "<--YOUR API KEY-->"
        )

        effectEnabled = !effectEnabled
        val result = agView?.agkit?.enableExtension(
            SynervozVoiceFilterExtensionManager.EXTENSION_VENDOR_NAME,
            SynervozVoiceFilterExtensionManager.EXTENSION_AUDIO_FILTER_NAME,
            effectEnabled
        )

        Log.d(TAG, "[enableExtension] result = $result")
    }

    fun echoTapped() {
        echoEnabled = !echoEnabled
        synervozVoiceFilter.enableEcho(echoEnabled)
    }

    fun pitchShiftTapped() {
        pitchShiftEnabled = !pitchShiftEnabled
        synervozVoiceFilter.enablePitchShift(pitchShiftEnabled)
    }

    fun flangerTapped() {
        flangerEnabled = !flangerEnabled
        synervozVoiceFilter.enableFlanger(flangerEnabled)
    }

    fun reverbTapped() {
        reverbEnabled = !reverbEnabled

        synervozVoiceFilter.reverbVoiceConfig =
            VoiceEffectsComponentConfiguration.ReverbVoiceEffectConfiguration().apply {
                dry = 1.0f
                wet = 1.0f
                mix = 1.0f
                width = 1.0f
                damp = 0.5f
                roomSize = 1.0f
                preDelay = 100.0f
                lowCut = 5.0f
            }
        synervozVoiceFilter.enableReverb(reverbEnabled)
    }

    fun settingsWithExtraButtons(): AgoraSettings {
        val agoraSettings = AgoraSettings()

        val agEnableSynervozFilter = AgoraButton(this)
        agEnableSynervozFilter.clickAction = {
            agEnableSynervozFilter.setImageResource(
                if (effectEnabled) android.R.drawable.star_on else android.R.drawable.star_off
            )
            it.background.setTint(if (it.isSelected) Color.GREEN else Color.GRAY)
            enableEffectTapped()
        }
        agEnableSynervozFilter.setImageResource(android.R.drawable.star_on)
        agEnableSynervozFilter.background.setTint(Color.GREEN)

        val agEchoEffect = AgoraButton(this)
        agEchoEffect.clickAction = {
            echoTapped()
            it.background.setTint(if (echoEnabled) Color.GREEN else Color.GRAY)
        }
        agEchoEffect.setImageResource(android.R.drawable.stat_sys_speakerphone)

        val agPitchShiftEffect = AgoraButton(this)
        agPitchShiftEffect.clickAction = {
            pitchShiftTapped()
            it.background.setTint(if (pitchShiftEnabled) Color.GREEN else Color.GRAY)
        }
        agPitchShiftEffect.setImageResource(android.R.drawable.picture_frame)

        val agFlangerEffect = AgoraButton(this)
        agFlangerEffect.clickAction = {
            flangerTapped()
            it.background.setTint(if (flangerEnabled) Color.GREEN else Color.GRAY)
        }
        agFlangerEffect.setImageResource(android.R.drawable.arrow_up_float)

        val agReverbEffect = AgoraButton(this)
        agReverbEffect.clickAction = {
            reverbTapped()
            it.background.setTint(if (reverbEnabled) Color.GREEN else Color.GRAY)
        }
        agFlangerEffect.setImageResource(android.R.drawable.arrow_down_float)

        agoraSettings.extraButtons = mutableListOf(agEnableSynervozFilter, agEchoEffect, agPitchShiftEffect, agFlangerEffect, agReverbEffect)

        agoraSettings.enabledButtons = mutableSetOf(AgoraSettings.BuiltinButton.END, AgoraSettings.BuiltinButton.MIC)

        return agoraSettings
    }

    override fun onEvent(p0: String?, p1: String?, p2: String?, p3: String?) {
        TODO("Not yet implemented")
    }
}