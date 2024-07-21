package com.sucre.mini_server.http


import android.util.Log
import com.sucre.mini_server.Constant
import java.io.*
/**
 * file process class for receive user uploaded file
 * yunhao chen
 * July 20, 2024
 */
class FileProcess(var filePath:String) {

    /**
     * handle file upload packet
     * @input: inputStream of the socket
     * @buffer: first read buffer of the inputStream
     * @byte: length of the buffer
     */
    fun handleFileUpload( input: InputStream, buffer: ByteArray, byte:Int):String {
        var byte=byte
        var boundaryBytes: ByteArray = ByteArray(0) //for file boundary string in byte array
        var fileOutputStream : FileOutputStream? = null
        var boundaryString :String ="" //for file boundary string in string
        var fileName: String = ""
        var bytesRead: Int

        //convert buffer to string
        var line = String(buffer,0,byte)
        //util get all information
        while(true){
            Log.i("Server_class", line)
            //find boundary string in header
            var regex=Regex("boundary=(.*?)\\r\\n")
            var boundary = regex.find(line)
            if(boundary !=null){
                boundaryString = boundary.groupValues[1]
                boundaryBytes = boundaryString.toByteArray()
            }
            //find filename in header body
            regex= Regex("filename=\"(.*?)\"")
            val pattern = regex.find(line)
            if(pattern!=null){
                fileName=pattern.groupValues[1]
                //create file after getting file name
                fileOutputStream = FileOutputStream(File(filePath + fileName))
                Log.i("Server_class", filePath+fileName)
            }
            //got enough information, break the loop
            if(fileName!="" && boundaryString!="") {
                break
            }else{
                //packet is not complete enough to get boundary and file name
                //read buffer again
                bytesRead = input.read(buffer)
                byte = bytesRead
                line = String(buffer,0,byte)
            }
        }

        //after collected boundary and file name
        if (boundaryBytes != null && fileOutputStream != null) {
            //before read again, make sure there is no file byte data in last buffer
            var lastByte = getFileByte(buffer,byte)
            if(lastByte.size > 0){
                //write first part of the file in buffer we read before
                fileOutputStream.write(lastByte, 0, lastByte.size)
            }
            //keep read rest of the file
            while (true) {
                bytesRead = input.read(buffer)
                if (bytesRead == -1) break
                //check if we reach the end of the file
                val index = indexOf(buffer, boundaryBytes, bytesRead)
                if (index != -1) {
                    fileOutputStream.write(buffer, 0, index - 4) // Remove the last CRLF
                    break
                } else {
                    //is middle of the file, keep writing
                    fileOutputStream.write(buffer, 0, bytesRead)
                }
            }

            fileOutputStream.close()
        }
        return fileName
    }

    /**
     * search bytearray in buffer
     */
    fun indexOf(buffer: ByteArray, target: ByteArray, bytesRead: Int): Int {
        for (i in 0 until bytesRead - target.size + 1) {
            var found = true
            for (j in target.indices) {
                if (buffer[i + j] != target[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }

    /**
     * search bytearray from backward
     */
    fun indexOfRev(buffer: ByteArray, target: ByteArray, bytesRead: Int): Int {
        for (i in (bytesRead - target.size) downTo 0) {
            var found = true
            for (j in target.indices) {
                if (buffer[i + j] != target[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }

    /**
     * get the file bytearray in the header
     */
    fun getFileByte(buffer: ByteArray,byte:Int): ByteArray{
        //packet are using two CRLF to separate header and file bytearray
        val endCode :String ="\r\n\r\n"
        val endByte :ByteArray = endCode.toByteArray()
        //find the last two CRLF if buffer
        val index = indexOfRev(buffer, endByte, byte)
        if(index != -1){
            //only copy the file array
            return buffer.copyOfRange(index+4, byte)
        }else{
            return ByteArray(0)
        }
    }
}