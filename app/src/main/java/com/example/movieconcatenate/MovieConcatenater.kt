package com.example.movieconcatenate

import android.content.Context
import android.media.MediaCodecInfo
import org.m4m.*
import org.m4m.android.AndroidMediaObjectFactory
import org.m4m.android.AudioFormatAndroid
import org.m4m.android.VideoFormatAndroid

class MovieConcatenater(val context: Context,
                        val moviePath1: String,
                        val moviePath2: String,
                        val destPath: String,
                        val outputWidth: Int,
                        val outputHeight: Int,
                        var onComplete: ((destPath: String)->Unit)?) : IProgressListener {

    private var composer: MediaComposer? = null

    private val videoMimeType = "video/avc"
    private var videoBitRateInKBytes = 5000
    private val videoFrameRate = 30

    private val videoIFrameInterval = 1

    private val audioMimeType = "audio/mp4a-latm"
    private val audioBitRate = 96 * 1024

    init {
        startConcatenate()
    }

    private fun startConcatenate() {

        if(composer != null) {
            println("Already started")
            return
        }

        val factory = AndroidMediaObjectFactory(context)
        val composer = org.m4m.MediaComposer(factory, this)
        this.composer = composer

        composer.addSourceFile(moviePath1)
        composer.setTargetFile(destPath)

        val (audioFormat, videoFormat) = getMediaInfo(context, Uri(moviePath1))

        configureAudioEncoder(composer, audioFormat)
        configureVideoEncoder(composer, outputWidth, outputHeight)

        composer.addSourceFile(moviePath2)

        println("composer start")
        composer.start()
    }

    fun stopConcatenate() {
        /*
            Even though MediaComposer#stop is called,
            IProgressListener#onMediaDone will be executed.
         */
        onComplete = null

        composer?.stop()
    }

    private fun getMediaInfo(context: Context, uri: Uri): Pair<AudioFormat, VideoFormat> {
        val mediaFileInfo = org.m4m.MediaFileInfo(AndroidMediaObjectFactory(context))
        mediaFileInfo.uri = uri
        val audioFormat = mediaFileInfo.audioFormat as AudioFormat
        val videoFormat = mediaFileInfo.videoFormat as VideoFormat
        return Pair(audioFormat, videoFormat)
    }

    private fun configureVideoEncoder(mediaComposer: MediaComposer, width: Int, height: Int) {

        val videoFormat = VideoFormatAndroid(videoMimeType, width, height)

        videoFormat.videoBitRateInKBytes = videoBitRateInKBytes
        videoFormat.videoFrameRate = videoFrameRate
        videoFormat.videoIFrameInterval = videoIFrameInterval

        mediaComposer.targetVideoFormat = videoFormat
    }

    private fun configureAudioEncoder(mediaComposer: MediaComposer, audioFormat: AudioFormat) {

        val aFormat = AudioFormatAndroid(audioMimeType, audioFormat.audioSampleRateInHz, audioFormat.audioChannelCount)

        aFormat.audioBitrateInBytes = audioBitRate
        aFormat.audioProfile = MediaCodecInfo.CodecProfileLevel.AACObjectLC

        mediaComposer.targetAudioFormat = aFormat
    }

    // region IProgressListener
    override fun onMediaStart() {
        println("onMediaStart")
    }

    override fun onMediaProgress(progress: Float) {
        println("onMediaProgress: $progress")
    }

    override fun onMediaDone() {
        println("onMediaDone")
        composer = null
        onComplete?.invoke(destPath)
    }

    override fun onMediaPause() {
        println("onMediaPause")
    }

    override fun onMediaStop() {
        println("onMediaStop")
    }

    override fun onError(exception: Exception?) {
        throw(exception!!)
    }
    // endregion
}