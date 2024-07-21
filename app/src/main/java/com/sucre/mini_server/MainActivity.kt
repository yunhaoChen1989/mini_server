package com.sucre.mini_server


import android.R.attr
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.util.regex.Pattern

/**
 * main activity
 * yunhao chen
 * July 20, 2024
 */
class MainActivity : AppCompatActivity() {
    private val PICK_FILE_REQUEST_CODE = 2
    val context:Context=this
    lateinit var startButton: Button
    lateinit var selectButton: Button
    lateinit var ipView: TextView
    lateinit var folder: EditText

    /**
     * override onCreate function
     * define click event, text view, button.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //create button and text view instance
        startButton=findViewById(R.id.startButton)
        selectButton=findViewById(R.id.file)
        ipView=findViewById(R.id.textViewIp)
        folder=findViewById(R.id.folder)

        //show ip address and instruction
        ipView.setText("1,click select button to select any file under the folder you would like to share.\n" +
                "2, click start button to start the server.\n" +
                "3,Open the browser in any other device and using follow IP address to connect:" + getLocalIpAddress()+":1989\n " +
                "4,make sure other devices are under the same wifi or hotspot.\n")
        //set start button click event
        startButton.setOnClickListener() {
            var text = "server started"
            if(Constant.listenPath==""){
                text="please select folder first"
            }else{
                //start thread for server class
                var server=ServerClass()
                //pass out the activity context for asset path and file access
                server.context=context.applicationContext
                server.start()
            }
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
        //set select folder button event
        selectButton.setOnClickListener() {
            //call file selector
            openFileSelector()
        }
    }

    /**
     * file selector intent function
     */
    private fun openFileSelector() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"  // You can set a specific type like "image/*" for images
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        //set function after user selected file
        startActivityForResult(Intent.createChooser(intent, "Select a file"), PICK_FILE_REQUEST_CODE)
    }

    /**
     * override function for user selected file information
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            //get path from uri
            data?.data?.let { uri: Uri ->
                //call function to get path
                val filePath = getPathFromUri(this, uri)?.let { extractPath(it) }
                if (filePath != null) {
                    //set path to constant class
                    Constant.listenPath=filePath
                    Toast.makeText(this, "File Selected: $filePath", Toast.LENGTH_LONG).show()
                    folder.setText(filePath)
                } else {
                    Toast.makeText(this, "Failed to get file path", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * extract path from absolute path
     * between two /
     */
    private fun extractPath(filePath:String):String?{
        val regex = "/(.+)/"
        //set pattern with regex and find string in pattern
        val pattern = Pattern.compile(regex)
        var match = pattern.matcher(filePath)
        if(match.find()){
            return match.group(0)//return result found
        }
        return null
    }

    /**
     * get path from uri and context
     */
    private fun getPathFromUri(context: Context, uri: Uri): String? {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            //document and download folders are work under the same code on my phone
            //need more phone to test this function
            if (isExternalStorageDocument(uri) || isDownloadsDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                return split[1]
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val type = split[0]
                var contentUri: Uri? = null
                when (type) {
                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    /**
     * extract column data in uri
     */
    private fun getDataColumn(context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor = uri?.let { context.contentResolver.query(it, projection, selection, selectionArgs, null) }
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * to tell whether is external storage path
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }
    /**
     * to tell whether is download path
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }
    /**
     * to tell whether is media path
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * function to get local ip
     */
    private fun getLocalIpAddress(): String? {
        try {
            val interfaces: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                val addresses: List<InetAddress> = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress) {
                        val ipAddress: String = address.hostAddress
                        // Check if the address is IPv4
                        if (ipAddress.indexOf(':') < 0) {
                            return ipAddress
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }
}