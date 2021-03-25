package io.agora.agorauikit_android

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import io.agora.rtc.Constants
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.BeautyOptions
import io.agora.rtc.video.VideoEncoderConfiguration
import java.util.logging.Level
import java.util.logging.Logger


interface AgoraVideoViewerDelegate {
    
}

@ExperimentalUnsignedTypes
open class AgoraVideoViewer: FrameLayout {

    enum class Style {
        GRID, FLOATING, COLLECTION
    }
    /// Gets and sets the role for the user. Either `.audience` or `.broadcaster`.
    public var userRole: Int = Constants.CLIENT_ROLE_BROADCASTER
        set(value: Int) {
            field = value
            this.agkit.setClientRole(value)
        }

    var controlContainer: ButtonContainer? = null
    var camButton: AgoraButton? = null
    var micButton: AgoraButton? = null
    var flipButton: AgoraButton? = null
    var beautyButton: AgoraButton? = null
    var screenShareButton: AgoraButton? = null

    companion object {}
    var remoteUserIDs: MutableSet<Int> = mutableSetOf()
    var userVideoLookup: MutableMap<Int, AgoraSingleVideoView> = mutableMapOf()
    val userVideosForGrid: Map<Int, AgoraSingleVideoView>
        get() {
            return if (this.style == Style.FLOATING) {
                this.userVideoLookup.filterKeys { it == this.overrideActiveSpeaker ?: this.activeSpeaker ?: this.userID }
            } else if (this.style == Style.GRID) {
                this.userVideoLookup
            } else {
                emptyMap()
            }
        }

    open val beautyOptions: BeautyOptions
        get() {
            val beautyOptions = BeautyOptions()
            beautyOptions.smoothnessLevel = 1f
            beautyOptions.rednessLevel = 0.1f
            return beautyOptions
        }

    val collectionViewVideos: Map<Int, AgoraSingleVideoView>
    get() {
        return if (this.style == Style.FLOATING) {
            return this.userVideoLookup
        } else {
            emptyMap()
        }
    }

    public var userID: Int = 0
    var activeSpeaker: Int? = null
    private val newHandler = AgoraVideoViewerHandler(this)

    fun addUserVideo(userId: Int): AgoraSingleVideoView {
        this.userVideoLookup[userId]?.let { remoteView ->
            return remoteView
        }
        val remoteVideoView = AgoraSingleVideoView(this.context, userId, this.agoraSettings.colors.micFlag, this.agkit)
        remoteVideoView.canvas.renderMode = this.agoraSettings.videoRenderMode
        this.agkit.setupRemoteVideo(remoteVideoView.canvas)
//        this.agkit.setRemoteVideoRenderer(remoteVideoView.uid, remoteVideoView.textureView)
        this.userVideoLookup[userId] = remoteVideoView
        if (this.activeSpeaker == null) {
            this.activeSpeaker = userId
        }
        this.reorganiseVideos()
        return remoteVideoView
    }

    fun removeUserVideo(uid: Int, reogranise: Boolean = true) {
        val userSingleView = this.userVideoLookup[uid] ?: return
//        val canView = userSingleView.hostingView ?: return
        this.agkit.muteRemoteVideoStream(uid, true)
        userSingleView.canvas.view = null
        this.userVideoLookup.remove(uid)

        this.activeSpeaker.let {
            if (it == uid) this.setRandomSpeaker()
        }
        if (reogranise) {
            this.reorganiseVideos()
        }
    }

    internal fun setRandomSpeaker() {
        this.activeSpeaker = this.userVideoLookup.keys.shuffled().firstOrNull { it != this.userID }
    }

    public var overrideActiveSpeaker: Int? = null
        set(newValue) {
            val oldValue = this.overrideActiveSpeaker
            field = newValue
            if (field != oldValue) {
                this.reorganiseVideos()
            }
        }

    internal fun addLocalVideo(): AgoraSingleVideoView? {
        if (this.userID == 0 || this.userVideoLookup.containsKey(this.userID)) {
            return this.userVideoLookup[this.userID]
        }
        val vidView = AgoraSingleVideoView(this.context, 0, this.agoraSettings.colors.micFlag, this.agkit)
        vidView.canvas.renderMode = this.agoraSettings.videoRenderMode
         this.agkit.setupLocalVideo(vidView.canvas)
        this.userVideoLookup[this.userID] = vidView
        this.reorganiseVideos()
        return vidView
    }


    var connectionData: AgoraConnectionData
    public constructor(
        context: Context, connectionData: AgoraConnectionData,
        style: Style = Style.FLOATING,
        agoraSettings: AgoraSettings = AgoraSettings(),
        delegate: AgoraVideoViewerDelegate? = null,
    ): super(context) {
        this.connectionData = connectionData
        this.style = style
        this.agoraSettings = agoraSettings
        this.delegate = delegate
//        this.setBackgroundColor(Color.BLUE)
        initAgoraEngine()
        this.addView(this.backgroundVideoHolder, ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT))
        this.addView(this.floatingVideoHolder, ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, 200))
        this.floatingVideoHolder.setBackgroundColor(this.agoraSettings.colors.floatingBackgroundColor)
        this.floatingVideoHolder.background.alpha = this.agoraSettings.colors.floatingBackgroundAlpha
    }

    private fun initAgoraEngine() {
        this.agkit = RtcEngine.create(context, connectionData.appId, this.newHandler)
        agkit.enableAudioVolumeIndication(1000, 3, true)
        agkit.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        agkit.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
        agkit.enableVideo()
        agkit.setVideoEncoderConfiguration(VideoEncoderConfiguration())
    }
//    constructor(context: Context) : super(context)
    /// Delegate for the AgoraVideoViewer, used for some important callback methods.
    public var delegate: AgoraVideoViewerDelegate? = null

    var floatingVideoHolder: RecyclerView = RecyclerView(context)
    var backgroundVideoHolder: RecyclerView = RecyclerView(context)
    /// Settings and customisations such as position of on-screen buttons, collection view of all channel members,
    /// as well as agora video configuration.
    public var agoraSettings: AgoraSettings = AgoraSettings()

    /// Style and organisation to be applied to all the videos in this AgoraVideoViewer.
    public var style: Style
        set(value: Style) {
            val oldValue = field
            field = value
            if (oldValue != value) {
//                this.backgroundVideoHolder.visibility = if (value == Style.COLLECTION) INVISIBLE else VISIBLE
                this.reorganiseVideos()
            }
        }

    public lateinit var agkit: RtcEngine

    /// VideoControl

    fun setupAgoraVideo() {
        if (this.agkit.enableVideo() < 0) {
//            AgoraVideoViewer.agoraPrint(.error, message: "Could not enable video")
            return
        }
        if (this.controlContainer == null) {
            this.addVideoButtons()
        }
        this.agkit.setVideoEncoderConfiguration(this.agoraSettings.videoConfiguration)
    }

    fun leaveChannel(): Int {
        val channelName = this.connectionData.channel
        if (channelName == null) {
            return 0
        }
        this.agkit.setupLocalVideo(null)
        if (this.userRole == Constants.CLIENT_ROLE_BROADCASTER) {
            this.agkit.stopPreview()
        }
        this.activeSpeaker = null
        (this.context as Activity).runOnUiThread {
            this.remoteUserIDs.forEach { this.removeUserVideo(it, false) }
            this.remoteUserIDs = mutableSetOf()
            this.userVideoLookup = mutableMapOf()
            this.reorganiseVideos()
            this.controlContainer?.visibility = INVISIBLE
        }

        val leaveChannelRtn = this.agkit.leaveChannel()
        if (leaveChannelRtn >= 0) {
            this.connectionData.channel = null
        }
        return leaveChannelRtn
    }

    fun join(channel: String, role: Int, fetchToken: Boolean, uid: Int? = null) {
        if (fetchToken) {
            this.agoraSettings.tokenURL?.let { tokenURL ->
                AgoraVideoViewer.Companion.fetchToken(
                    tokenURL, channel, uid ?: this.userID,
                    object : TokenCallback {
                        override fun onSuccess(token: String) {
                            this@AgoraVideoViewer.connectionData.appToken = token
                            this@AgoraVideoViewer.join(channel, token, role, uid)
                        }
                        override fun onError(error: TokenError) {
                            Logger.getLogger("AgoraUIKit", "Could not get token: ${error.name}")
                        }
                    }
                )
            }
            return
        }
        this.join(channel, this.connectionData.appToken, role, uid)
    }

    fun join(channel: String, token: String? = null, role: Int? = null, uid: Int? = null) {
        if (role == Constants.CLIENT_ROLE_BROADCASTER) {
            AgoraVideoViewer.requestPermissions(this.context)
        }
        if (this.connectionData.channel != null) {
            if (this.connectionData.channel == channel) {
                // already in this channel
                return
            }
            val leaveChannelRtn = this.leaveChannel()
            if (leaveChannelRtn < 0) {
                // could not leave channel
                Logger.getLogger("AgoraUIKit").log(Level.WARNING, "Could not leave channel: $leaveChannelRtn")
            } else {
                this.join(channel, token, role, uid)
            }
            return
        }
        role?.let {
            if (it != this.userRole) {
                this.userRole = it
            }
        }
        uid?.let {
            this.userID = it
        }
        this.setupAgoraVideo()
        this.agkit.joinChannel(token, channel, null, this.userID)
    }

}