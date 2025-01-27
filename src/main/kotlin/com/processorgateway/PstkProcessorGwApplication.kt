package com.processorgateway

import org.jpos.iso.*
import org.jpos.iso.channel.ASCIIChannel
import org.jpos.iso.packager.ISO87APackager
import org.jpos.util.Logger
import org.jpos.util.SimpleLogListener
import java.io.IOException
import java.net.Socket
import java.util.*
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
      channel.disconnect()
      println("Response: $response")
      if (response.getString(39).endsWith("00")) {  // check if 39 equals 00
         println("TMK Request Successful")
         println(response.getString(53))
         val componentKey1 = "<< Your Component Key 1 >>"
         val componentKey2 = "<< Your Component Key 2 >>"

         terminalMasterKey = Util.getDecryptedTMKFromHost(response.getString(53), componentKey1, componentKey2);
         println("Terminal Master Key")
         println(terminalMasterKey);
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
