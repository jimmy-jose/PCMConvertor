package app.jimmy.pcmconvertor
/**
 * @author Jimmy
 * Created on 18/5/18.
 */
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

import android.os.Handler
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

class AudioTrackPlayer {
    private var pathAudio: String? = null
    private var audioPlayer: AudioTrack? = null
    private var mThread: Thread? = null
    private var bytesread = 0
    private var ret = 0
    private var size = 0L
    private var `in`: FileInputStream? = null
    private var byteData: ByteArray? = null
    private val count = 512 * 1024 // 512 kb
    private var isPlay = true
    private var isLooping = false

    private val mLopingRunnable = Runnable { play() }

    fun prepare(pathAudio: String) {
        this.pathAudio = pathAudio
        mHandler = Handler()
    }

    fun play() {
        stop()

        isPlay = true
        bytesread = 0
        ret = 0
        if (pathAudio == null)
            return

        audioPlayer = createAudioPlayer()
        if (audioPlayer == null) return
        audioPlayer!!.play()

        mThread = Thread(PlayerProcess())
        mThread!!.start()
    }

    private fun createAudioPlayer(): AudioTrack? {
        val intSize = android.media.AudioTrack.getMinBufferSize(96000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT)
        val audioTrack = AudioTrack(AudioManager.STREAM_MUSIC, 96000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT, intSize, AudioTrack.MODE_STREAM)
        if (audioTrack == null) {
            Log.d("TCAudio", "audio track is not initialised ")
            return null
        }

        var file: File? = null
        file = File(pathAudio)

        byteData = ByteArray(count)
        try {
            `in` = FileInputStream(file)

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        size = file!!.length()
        return audioTrack
    }

    private inner class PlayerProcess : Runnable {

        override fun run() {
            while (bytesread < size && isPlay) {
                if (Thread.currentThread().isInterrupted) {
                    break
                }
                try {
                    ret = `in`!!.read(byteData, 0, count)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                if (ret != -1) { // Write the byte array to the track
                    audioPlayer!!.write(byteData!!, 0, ret)
                    bytesread += ret
                } else
                    break
            }
            try {
                `in`!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            if (audioPlayer != null) {
                if (audioPlayer!!.state != AudioTrack.PLAYSTATE_STOPPED) {
                    audioPlayer!!.stop()
                    audioPlayer!!.release()
                    mThread = null
                }
            }
            if (isLooping && isPlay) mHandler!!.postDelayed(mLopingRunnable, 100)
        }
    }

    fun setLooping() {
        isLooping = !isLooping
    }

    fun pause() {

    }

    fun stop() {
        isPlay = false
        if (mThread != null) {
            mThread!!.interrupt()
            mThread = null
        }
        if (audioPlayer != null) {
            audioPlayer!!.stop()
            audioPlayer!!.release()
            audioPlayer = null
        }
    }

    fun reset() {

    }

    companion object {
        private var mHandler: Handler? = null
    }
}