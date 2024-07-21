package com.sucre.mini_server.http


import android.util.Log
import java.net.URLEncoder
import java.util.*

/**
 * header class for response
 * yunhao chen
 * July 20, 2024
 */
data class Header(var headCode:String,
                  var contentType: String,
                  var fileName: String,
    ){

    /**
     * function takes string to make response header
     */
    fun makeResponse(data:String):ByteArray{
        var length = data.length
        var head:String="HTTP/1.1 $headCode\n" +
                "Access-Control-Allow-Origin: *\n" +
                "Connection: Keep-Alive\n" +
                "content-length: $length\n" +
                "Content-Type: $contentType; charset=utf-8;\n\n"

        return head.toByteArray() + data.toByteArray()
    }

    /**
     * function takes bytearray to make response header
     */
    fun makeResponse(data:ByteArray):ByteArray{
        var length = data.size
        var disposition = if(fileName==("")) "" else "Content-Disposition: attachment; filename=\"$fileName\"\n"
        var head:String="HTTP/1.1 $headCode\n" +
                "Access-Control-Allow-Origin: *\n" +
                "Connection: Keep-Alive\n" +
                "content-length: $length\n" +
                "$disposition" +
                "Content-Type: $contentType; charset=utf-8;\n\n"
        Log.i("Server_class", head)
        return head.toByteArray() + data
    }
}
