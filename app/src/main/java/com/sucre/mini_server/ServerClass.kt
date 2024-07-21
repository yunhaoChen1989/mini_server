package com.sucre.mini_server


import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.sucre.mini_server.http.FileProcess
import com.sucre.mini_server.http.Header
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Server class for socket
 * yunhao chen
 * July 20, 2024
 */
class ServerClass() :Thread(){
    lateinit var context:Context
    lateinit var serverSocket: ServerSocket
    lateinit var inputStream: InputStream
    lateinit var  outputStream: OutputStream
    lateinit var socket: Socket


    /**
     * override run method for thread
     */
    override fun run() {
        //user might click start button twice
        try {
            serverSocket = ServerSocket(1989)
        }catch (ex: IOException){
            ex.printStackTrace()
            return
        }
        //define buffer
        val buffer = ByteArray(1024)
        var byte: Int
        Log.i("Server_class", "waiting")
        //keep waiting for connection
        while (true) {
            socket = serverSocket.accept()
            inputStream =socket.getInputStream()
            outputStream = socket.getOutputStream()
            //read first buffer
            byte = inputStream.read(buffer)
            //set time for time out
            var now=System.currentTimeMillis()

            while(byte>0){
                //time out
                if(System.currentTimeMillis()-now>30000){
                    Log.i("Server_class","timeout")
                    break
                }
                //convert buffer to string
                val tmpMeassage = String(buffer,0,byte)
                var response : ByteArray = ByteArray(0)

                try {
                    //define header class
                    var header = Header("200 OK","text/html","")

                    if (tmpMeassage !=null) {

                        Log.i("Server_class", "$tmpMeassage")
                        // Extract URL from request
                        val matcher = isFound("(POST|GET)\\s+(.*?)\\s+HTTP/1.1",tmpMeassage)
                        if (matcher!=null) {
                            val url = matcher.groupValues[2]
                            //when it's GET method
                            if(matcher.groupValues[1]=="GET"){
                                //index page
                                if (url == "/") {
                                    //return index.html in asset folder
                                    response = header.makeResponse(readFile("html/index.html"))
                                //listFile API
                                } else if (isFound("\\/listFiles\\?.*", url)!=null) {
                                    //get folder in constant
                                    val folder = File(Constant.listenPath)
                                    //list all files in the folder
                                    val files = folder.listFiles()

                                    //get page number in url
                                    var responseFiles = mutableListOf<String>()
                                    var page = isFound("page=([^&]+)", url)?.groupValues?.get(1)?.toInt()
                                    var pageSize = isFound(("pageSize=([^&]+)"), url)?.groupValues?.get(1)?.toInt()

                                    //add all files in the list
                                    if (files != null) {
                                        for (file in files) {
                                            //only files with name and not folder
                                            if (file.isFile && !file.name.equals("")) {
                                                responseFiles.add("\""+URLEncoder.encode(file.name)+"\"")
                                            }
                                        }
                                    }
                                    //reverse the list for show the newest file first
                                    responseFiles.reverse()

                                    //user must provide page information
                                    if (page != null && pageSize != null) {
                                        //page index
                                        var start = page * pageSize
                                        //if user scroll too much return empty list
                                        if (start >= responseFiles.size) {
                                            response = header.makeResponse(mutableListOf<String>().toString())
                                        } else {
                                            //if page size over the size of the list, only return up to size of the list
                                            //also means it's reached the end of all page
                                            var end =
                                                if ((start + pageSize) > responseFiles.size) responseFiles.size - 1 else (start + pageSize)
                                            header.contentType = "application/json"
                                            //make response between page number
                                            response = header.makeResponse(responseFiles.subList(start, end).toString())
                                        }

                                    } else {
                                        //user didn't provide page information
                                        header.headCode = "500 Internal Server Error"
                                        response = header.makeResponse("error")
                                    }
                                //download page
                                }else if(isFound("\\/download\\?.*", url)!=null) {
                                    //get file name from url
                                    var fileName = isFound("fileName=([^&]+)", url)?.groupValues?.get(1)
                                    fileName = URLDecoder.decode(fileName) //decode file name
                                    if (fileName != null) {
                                        Log.i("Server_class", Constant.listenPath + fileName)
                                        //check whether file exists
                                        val file = File(Constant.listenPath + fileName)
                                        if (file.exists()) {
                                            //read the file
                                            val byteArray = file.readBytes()
                                            header.contentType = "application/force-download"
                                            header.fileName = fileName
                                            //make response and return
                                            response = header.makeResponse(byteArray)
                                        } else {
                                            //file not found
                                            header.headCode = "404 Not Found"
                                            response = header.makeResponse("")
                                        }
                                    } else {
                                        //user not providing file name in the url
                                        header.headCode = "404 Not Found"
                                        response = header.makeResponse("")
                                    }
                                //file page API, only shows the image
                                }else if(isFound("\\/file\\?.*", url)!=null){
                                    //get file name in url
                                    var fileName = isFound("fileName=([^&]+)", url)?.groupValues?.get(1)
                                    //decode file name
                                    fileName = URLDecoder.decode(fileName)
                                    Log.i("Server_class", Constant.listenPath + fileName)
                                    //check whether file exists
                                    val file = File(Constant.listenPath + fileName)
                                    if (file.exists()) {
                                        var byteArray : ByteArray = ByteArray(0)
                                        //check if file type is image
                                        if(fileName.lowercase().endsWith("jpg")||fileName.lowercase().endsWith("jpeg")||fileName.lowercase().endsWith("png")){
                                            val format = if(fileName.lowercase().endsWith("png")) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                                            val quality = 10 // Quality from 0 to 100
                                            //compress the image before response
                                            byteArray = compressImageToByteArray(Constant.listenPath + fileName, format, quality)
                                        }else{
                                            //not image, only show the icon image, as thumbnail
                                            byteArray = readFileByte("html/img/files.png")
                                        }
                                        //make header and response
                                        header.contentType = "image/png"
                                        header.fileName=""
                                        response = header.makeResponse(byteArray)
                                    } else {
                                        //file not found
                                        header.headCode = "404 Not Found"
                                        response = header.makeResponse("")
                                    }
                                //not index, not API, surface for resource in asset folder
                                } else if (isExists(url.substring(1))) {
                                    //change content type if the file is css
                                    if (url.lowercase().endsWith(".css")) header.contentType = "text/css"
                                    response = header.makeResponse(readFile(url.substring(1)))
                                } else {
                                    //unknown url, return 404
                                    Log.i("Server_class", "404")
                                    header.headCode = "404 Not Found"
                                    response = header.makeResponse("")
                                }
                                //response the browser
                                write(response)
                                break
                            //post method for upload API
                            }else if(matcher.groupValues[1]=="POST"){
                                //upload API
                                if(url=="/upload") {
                                    //define file class
                                    var fileProcess = FileProcess(Constant.listenPath)
                                    //send out buffer and inputStream to write file the storage
                                    var fileName = fileProcess.handleFileUpload(inputStream, buffer, byte)
                                    if(fileName!=""){
                                        //file upload success
                                        header.headCode="200 OK"
                                        header.contentType="application/json"
                                        fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                                        //return jason response
                                        response = header.makeResponse("{\"data\":\"$fileName, success uploaded\"}")
                                    }else{
                                        //upload fail, return error response
                                        header.headCode="200 OK"
                                        header.contentType="application/json"
                                        fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                                        response = header.makeResponse("{\"data\":\"$fileName, upload failed\", \"errorMessage\":\"$fileName, upload failed\"}")

                                    }
                                    //response the browser
                                    write(response)
                                    break
                                }
                            }
                        } else {
                            //not get/post method, return error
                            header.headCode = "404 Not Found"
                            response =  header.makeResponse("error")
                            write(response)
                            break
                        }
                    }else{
                        //not message read from user,break and listen again
                        break
                    }
                } catch (ex: IOException) {
                    Log.e("Server_class",ex.message.toString())
                    ex.printStackTrace()
                }
            }
            //close everything
            socket.close()
            outputStream.close()
            inputStream.close()

        }
    }

    /**
     * write function for write response to browser
     */
    fun write(byteArray: ByteArray){
        try {
            //Log.i("Server_class","${byteArray.toString()} sending")
            outputStream.write(byteArray + "\n\n".toByteArray())
        }catch (ex:IOException){
            ex.printStackTrace()
        }
    }

    /**
     * read file in asset folder
     */
    fun readFile(fileName: String):String{
        val assetManager: AssetManager = context.getAssets()
        return assetManager.open(fileName).reader().readText()
    }

    /**
     * read file in asset folder return bytes
     */
    fun readFileByte(fileName: String):ByteArray{
        val assetManager: AssetManager = context.getAssets()
        return assetManager.open(fileName).readBytes()
    }

    /**
     * check whether file exists in asset folder
     */
    fun isExists(fileName:String):Boolean{
        try{
            val assetManager: AssetManager = context.getAssets()
            assetManager.open(fileName)
            return true
        }catch (ex: IOException){
            return false
        }
    }

    /**
     * use regex expression to find pattern in string
     */
    fun isFound(regex:String, data:String): MatchResult? {
        val pattern = Regex(regex)
        return pattern.find(data)
    }

    /**
     * function to compress imagess
     */
    fun compressImageToByteArray(imageFilePath: String, format: Bitmap.CompressFormat, quality: Int): ByteArray {
        // Load the image from the file path
        val bitmap = BitmapFactory.decodeFile(imageFilePath)

        // Create an output stream to hold the compressed image data
        val byteArrayOutputStream = ByteArrayOutputStream()

        // Compress the bitmap into the output stream
        bitmap.compress(format, quality, byteArrayOutputStream)

        // Convert the output stream to a byte array
        return byteArrayOutputStream.toByteArray()
    }

}