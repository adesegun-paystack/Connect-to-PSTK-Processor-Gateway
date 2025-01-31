package com.processorgateway

import org.jpos.iso.*
import org.jpos.iso.channel.ASCIIChannel
import org.jpos.util.Logger
import org.jpos.util.SimpleLogListener
import java.io.IOException
import java.net.Socket
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.util.*
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.net.ssl.SSLSocketFactory

class PstkProcessorGwApplication;
fun main(args: Array<String>) {
   println("Starting Connection to Paystack Processor Gateway!")

   val logger = Logger()
   logger.addListener(SimpleLogListener(System.out))


   // Create a Date object for the transaction
   val transactionDate = Date()

   // Get the formatted date, time, and datetime in the GMT+1 timezone
   val timeZone = TimeZone.getTimeZone("GMT+1")

   val transDate = ISODate.getDate(transactionDate, timeZone)
   val transTime = ISODate.getTime(transactionDate, timeZone)
   val transDateTime = ISODate.getDateTime(transactionDate, timeZone)

   val host = "terminal-processor-gateway.paystack.co"
   val port = 8006

   val tmkIsoMessage = Util.createTMKIsoMessage(transDate, transTime, transDateTime)

   val CHANNEL_NAME = "NIBSS-CHANNEL"

   val factory: ISOClientSocketFactory = SecureSSLSocketFactory();

   val packager = Packager()
   packager.setLogger(logger, "")


   val channel = ASCIIChannel(host, port, packager)
   channel.setLogger(logger, "")

   var terminalMasterKey: String;

   channel.setSocketFactory(factory);

   channel.setTimeout(60000);

   channel.setName(CHANNEL_NAME);

   try {
      channel.connect()
      println("Connected to $host:$port")
      channel.send(tmkIsoMessage)
      val response = channel.receive()

      println("Response: $response")
      if (response.getString(39).endsWith("00")) {  // check if 39 equals 00
         println("TMK Request Successful")
         println(response.getString(53))
         val componentKey1 = "<< Your Component Key 1 >>"
         val componentKey2 = "<< Your Component Key 2 >>"

         terminalMasterKey = Util.getDecryptedTMKFromHost(response.getString(53), componentKey1, componentKey2);
         println("Terminal Master Key")
         println(terminalMasterKey);

         // Start the TSK Process
         val tskIsoMessage = Util.createTSKIsoMessage(transDate, transTime, transDateTime)
         channel.send(tskIsoMessage)
         val tskResponse = channel.receive()
         println("Response: $tskResponse")
            if (tskResponse.getString(39).endsWith("00")) {  // check if 39 equals 00
                println("TSK Request Successful")
                val terminalSessionKey = Util.getDecryptedTSKFromHost(tskResponse.getString(53), terminalMasterKey)
                println("Terminal Session Key")
                println(terminalSessionKey)
            } else {
                println("TSK Request Failed")
                println(tskResponse.getString(39))
            }

      } else {
         response.getString(39)
      }

   } catch (e: Exception) {
      println("Error: ${e.message}")
      // e.printStackTrace()
   } finally {
      channel.disconnect()
      println("Disconnected from $host:$port")
   }
}

class SecureSSLSocketFactory : ISOClientSocketFactory {
   @Throws(IOException::class, ISOException::class)
   override fun createSocket(s: String, i: Int): Socket {
      val sslsocketfactory = SSLSocketFactory.getDefault() as SSLSocketFactory
      return sslsocketfactory
         .createSocket(s, i)
   }
}
