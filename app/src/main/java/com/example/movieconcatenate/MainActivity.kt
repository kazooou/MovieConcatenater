package com.example.movieconcatenate

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private val requestCode1 = 1
    private val requestCode2 = 2

    private val destPath: String
        get() = File(getExternalFilesDir(null), "concat.mp4").toString()
    private val moviePath1: String
        get() = File(getExternalFilesDir(null), "movie1.mp4").toString()
    private val moviePath2: String
        get() = File(getExternalFilesDir(null), "movie2.mp4").toString()

    private var concatenater: MovieConcatenater? = null

    private val coverView: View
        get() = findViewById(R.id.coverView)!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById(R.id.button) as Button
        button.setOnClickListener {
            pickVideo(requestCode1)
        }

        val button2 = findViewById(R.id.button2) as Button
        button2.setOnClickListener {


            startConcat()
        }
    }

    override fun onPause() {
        concatenater?.stopConcatenate()
        concatenater = null
        coverView.visibility = View.GONE
        super.onPause()
    }

    fun pickVideo(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "video/*"

        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == Activity.RESULT_OK) {

            val uri = data?.data ?: return
            println("uri: $uri")

            val copyTo = when(requestCode) {
                requestCode1 -> moviePath1
                requestCode2 -> moviePath2
                else -> throw Error("Invalid code: $requestCode")
            }

            contentResolver.openInputStream(uri).use { i ->
                FileOutputStream(copyTo).use { o ->
                    val buf = ByteArray(1024)
                    var len: Int
                    while (true) {
                        len = i.read(buf)
                        if(len <= 0) {
                            break
                        }
                        o.write(buf, 0, len)
                    }
                }
            }

            when(requestCode) {
                requestCode1 -> pickVideo(requestCode2)
                requestCode2 -> {
                    coverView.visibility = View.VISIBLE
                    startConcat()
                }
            }
        }
    }

    fun startConcat() {
        concatenater = MovieConcatenater(this, moviePath1!!, moviePath2!!, destPath, 640, 480) { destPath ->

            // delete copied files
            File(moviePath1).delete()
            File(moviePath2).delete()

            // insert into gallery
            insertIntoGallery(destPath)

            // start activity
            val uri = Uri.fromFile(File(destPath))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setDataAndType(uri, "video/mp4")

            startActivity(intent)
        }
    }

    fun insertIntoGallery(resultPath: String) {
        val values = ContentValues()
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        val date = System.currentTimeMillis()
        values.put(MediaStore.Video.Media.DATE_TAKEN, date)
        values.put(MediaStore.Video.Media.DATE_ADDED, date / 1000)
        values.put(MediaStore.Video.Media.DATE_MODIFIED, date / 1000)
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)

        val path: String
        val cursor = contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            path = cursor.getString(0)
            cursor.close()
        } else {
            println("failed to open path")
            return
        }

        // mkdirs
        File(path).parentFile.mkdirs()

        // transfer
        FileOutputStream(path).use { fos ->
            FileInputStream(resultPath).use { fis ->
                fis.channel.transferTo(0, fis.channel.size(), fos.channel)
            }
        }
    }
}
