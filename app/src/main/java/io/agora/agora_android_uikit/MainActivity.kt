package io.agora.agora_android_uikit

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

import io.agora.agorauikit_android.*
import io.agora.rtc2.Constants
import io.agora.extension.ExtensionManager


@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity(){
    var agView: AgoraVideoViewer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            agView = AgoraVideoViewer(
                    this, AgoraConnectionData("my-app-id", extensionName = listOf(ExtensionManager.EXTENSION_NAME)),
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

        agView?.agkit?.setExtensionProperty(
            ExtensionManager.EXTENSION_VENDOR_NAME, ExtensionManager.EXTENSION_AUDIO_FILTER_NAME,
            "API-KEY",
            "API-SECRET")
    }



    fun settingsWithExtraButtons(): AgoraSettings {
        val agoraSettings = AgoraSettings()

        val agBosePinPointButton = AgoraButton(this)
        agBosePinPointButton.clickAction = {
            it.isSelected = !it.isSelected
            agBosePinPointButton.setImageResource(
                if (it.isSelected) android.R.drawable.star_on else android.R.drawable.star_off
            )
            it.background.setTint(if (it.isSelected) Color.GREEN else Color.GRAY)
            this.agView?.agkit?.enableExtension(ExtensionManager.EXTENSION_VENDOR_NAME, ExtensionManager.EXTENSION_AUDIO_FILTER_NAME, it.isSelected)
        }
        agBosePinPointButton.setImageResource(android.R.drawable.star_off)

        agoraSettings.extraButtons = mutableListOf(agBosePinPointButton)

        return agoraSettings
    }

}