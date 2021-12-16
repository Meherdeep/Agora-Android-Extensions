package io.agora.agora_android_uikit

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

import io.agora.agorauikit_android.*
import io.agora.rtc2.Constants
import com.synervoz.voicefilters.AgoraExtensionPropertyInterface
import com.synervoz.voicefilters.SynervozVoiceFilter
import com.synervoz.voicefilters.SynervozVoiceFilterExtensionManager
import com.synervoz.voicefilters.VoiceEffectsComponentConfiguration


@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity(){
    var agView: AgoraVideoViewer? = null
    var synervozVoiceFilter = SynervozVoiceFilter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            agView = AgoraVideoViewer(
                    this, AgoraConnectionData("my-app-id", extensionName = listOf(SynervozVoiceFilterExtensionManager.EXTENSION_NAME)),
                agoraSettings = this.settingsWithExtraButtons()
            )
        } catch (e: Exception) {
            print("Could not initialise AgoraVideoViewer. Check your App ID is valid.")
            print(e.message)
            return
        }
        val set = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

        this.addContentView(agView, set)

        // Check that the camera and mic permissions are accepted before attempting to join
        if (AgoraVideoViewer.requestPermissions(this)) {
            initSynervozVoiceFilter().apply {
                agView!!.join("test", role = Constants.CLIENT_ROLE_BROADCASTER)
            }
        } else {
            val joinButton = Button(this)
            joinButton.text = "Allow Camera and Microphone, then click here"
            joinButton.setOnClickListener(View.OnClickListener {
                // When the button is clicked, check permissions again and join channel
                // if permissions are granted.
                if (AgoraVideoViewer.requestPermissions(this)) {
                    (joinButton.parent as ViewGroup).removeView(joinButton)
                    initSynervozVoiceFilter().apply {
                        agView!!.join("test", role=Constants.CLIENT_ROLE_BROADCASTER)
                    }
                }
            })
            joinButton.setBackgroundColor(Color.GREEN)
            joinButton.setTextColor(Color.RED)
            this.addContentView(joinButton, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 300))
        }
    }

    fun initSynervozVoiceFilter() {
        agView?.agkit?.enableExtension(SynervozVoiceFilterExtensionManager.EXTENSION_VENDOR_NAME, SynervozVoiceFilterExtensionManager.EXTENSION_AUDIO_FILTER_NAME, true)

        synervozVoiceFilter.agoraExtensionPropertyInterface =
            AgoraExtensionPropertyInterface { vendorName, filterName, propertyName, propertyValue ->
                agView?.agkit?.setExtensionProperty(vendorName, filterName, propertyName, propertyValue)
            }
    }

    fun settingsWithExtraButtons(): AgoraSettings {
        var voiceFilters = arrayOf("Echo", "Reverb", "Flanger", "Pitch Shift", "Reset Reverb")
        var aa = ArrayAdapter(this, android.R.layout.simple_spinner_item, voiceFilters)
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val agoraSettings = AgoraSettings()

        val enableSynervozButton = AgoraButton(this)
        enableSynervozButton.clickAction = {
            it.isSelected = !it.isSelected
            enableSynervozButton.setImageResource( if (it.isSelected) android.R.drawable.star_off else android.R.drawable.star_on)
            it.background.setTint(if (it.isSelected) Color.GRAY else Color.GREEN)
            this.agView?.agkit?.enableExtension(SynervozVoiceFilterExtensionManager.EXTENSION_VENDOR_NAME, SynervozVoiceFilterExtensionManager.EXTENSION_AUDIO_FILTER_NAME, it.isSelected)
        }
        enableSynervozButton.setImageResource(android.R.drawable.star_on)
        enableSynervozButton.background.setTint(Color.GREEN)

        val echoFilterButton = AgoraButton(this)
        echoFilterButton.clickAction = {
            it.isSelected = !it.isSelected
            echoFilterButton.setImageResource(android.R.drawable.stat_sys_speakerphone)
            it.background.setTint(if (it.isSelected) Color.GREEN else Color.GRAY)

            showToast(message = "Echo filter " + if(it.isSelected) "enabled" else "disabled")
            synervozVoiceFilter.enableEcho(it.isSelected)
        }
        echoFilterButton.setImageResource(android.R.drawable.stat_sys_speakerphone)

        val reverbFilterButton = AgoraButton(this)
        reverbFilterButton.clickAction = {
            it.isSelected = !it.isSelected
            it.background.setTint(if (it.isSelected) Color.GREEN else Color.GRAY)

            showToast(message = "Reverb filter " + if(it.isSelected) "enabled" else "disabled")
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
            synervozVoiceFilter.enableReverb(it.isSelected)
        }
        reverbFilterButton.setImageResource(android.R.drawable.stat_notify_sync)

        val pitchShiftFilterButton = AgoraButton(this)
        pitchShiftFilterButton.clickAction = {
            it.isSelected = !it.isSelected
            it.background.setTint(if (it.isSelected) Color.GREEN else Color.GRAY)

            showToast(message = "Pitch Shift filter " + if(it.isSelected) "enabled" else "disabled")
            synervozVoiceFilter.enablePitchShift(it.isSelected)
        }
        pitchShiftFilterButton.setImageResource(android.R.drawable.stat_notify_chat)

        val flangerFilterButton = AgoraButton(this)
        flangerFilterButton.clickAction = {
            it.isSelected = !it.isSelected
            it.background.setTint(if (it.isSelected) Color.GREEN else Color.GRAY)

            showToast(message = "Flanger filter " + if(it.isSelected) "enabled" else "disabled")
            synervozVoiceFilter.enableFlanger(it.isSelected)
        }
        flangerFilterButton.setImageResource(android.R.drawable.stat_notify_sdcard_usb)

        agoraSettings.extraButtons = mutableListOf(enableSynervozButton, echoFilterButton, reverbFilterButton, pitchShiftFilterButton, flangerFilterButton)

        agoraSettings.enabledButtons = mutableSetOf(AgoraSettings.BuiltinButton.END)

        return agoraSettings
    }

    private fun showToast(context: Context = applicationContext, message: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(context, message, duration).show()
    }

}