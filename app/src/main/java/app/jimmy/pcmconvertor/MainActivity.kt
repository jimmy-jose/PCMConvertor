package app.jimmy.pcmconvertor

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*


class MainActivity : AppCompatActivity() {
//    private var test = true
    private val WRITE_PERMISSION = 1010
    private val TAG = MainActivity::class.java.simpleName
    private var togglePlayState = true
    var a = AudioTrackPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_PERMISSION)
        }else{
            convert()
        }

        toggleState.setOnClickListener(object :View.OnClickListener{
            override fun onClick(v: View?) {
                togglePlayState()
            }

        })
    }

    fun togglePlayState(){
        if(togglePlayState){
            a.stop()

        }else{
            a.play()
        }

        togglePlayState = !togglePlayState

    }


    fun convert(){
        val fileBeforConversion = File(Environment.getExternalStorageDirectory().absolutePath+"/newFile.mp3")
        Log.i(TAG," "+fileBeforConversion.absolutePath)

        val fileAfterConversion = File(Environment.getExternalStorageDirectory().absolutePath+"/convertedFile.pcm")

        val intTest = File(Environment.getExternalStorageDirectory().absolutePath+"/intTest.pcm")

        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(fileBeforConversion.absolutePath)

        Log.i(TAG,mediaExtractor.trackCount.toString())

        val format = mediaExtractor.getTrackFormat(0)
        mediaExtractor.selectTrack(0)

        val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val name = mediaCodecList.findDecoderForFormat(format)
        val mediaCodec = MediaCodec.createByCodecName(name)
        Log.i(TAG,"name "+name)
        val fos = FileOutputStream(fileAfterConversion)
        val fos1 = FileOutputStream(intTest)
//        val outputStreamWriter = OutputStreamWriter(fos1)
        //region async
        mediaCodec.setCallback(object : MediaCodec.Callback(){
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                Log.d(TAG,"onInputBufferAvailable")
                val decoderInputBuffer = codec.getInputBuffer(index)
                val size = mediaExtractor.readSampleData(decoderInputBuffer, 0)
                val presentationTime = mediaExtractor.getSampleTime()
                Log.d(TAG, "audio extractor: returned buffer of size $size")
                Log.d(TAG, "audio extractor: returned buffer for time $presentationTime")
                if (size < 0) {
                    codec.queueInputBuffer(
                            index,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    Log.d(TAG, "audio extractor: EOS")
                }else{
                    codec.queueInputBuffer(
                            index,
                            0,
                            size,
                            mediaExtractor.sampleTime,
                            0)
                    mediaExtractor.advance()
                }
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat?) {
                Log.d(TAG, "New format " + mediaCodec.getOutputFormat())
            }

            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                Log.d(TAG, "onOutputBufferAvailable")
                val buffer = mediaCodec.getOutputBuffer(index)
                val b = ByteArray(info.size - info.offset)
                val a = buffer.position()
                buffer.get(b)
                buffer.position(a)
                mediaCodec.releaseOutputBuffer(index, true)
                Log.i(TAG,"byte data: "+b.contentToString())
                fos.write(b,0,info.size-info.offset)
//                if(test) {
//                    outputStreamWriter.write(b.contentToString())
//                    test = false
//                }
            }


            override fun onError(codec: MediaCodec?, e: MediaCodec.CodecException?) {
                Log.d(TAG,"onError")
            }

        })
        mediaCodec.configure(format,null,null,0)
        Log.d(TAG,"output format :"+mediaCodec.outputFormat)
        val numChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        Log.d(TAG,"number of channels:"+numChannels)
        mediaCodec.start()
        a.prepare(intTest.absolutePath)
        //region endregion
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == WRITE_PERMISSION){
            Log.i(TAG,"Permission granted!!");
            convert()
        }
    }
}
