package com.processorgateway

import org.jpos.iso.*
import org.jpos.iso.channel.ASCIIChannel
import org.jpos.iso.packager.ISO87APackager
import java.io.IOException
import java.net.Socket
import java.util.*
import javax.net.ssl.SSLSocketFactory


class PstkProcessorGwApplication

fun main(args: Array<String>) {
   System.out.println("Starting Connection to Paystack Processor Gateway!")

   // Create a Date object for the transaction
   val transactionDate = Date()

   // Get the formatted date, time, and datetime in the GMT+1 timezone
   val timeZone = TimeZone.getTimeZone("GMT+1")

   val transDate = ISODate.getDate(transactionDate, timeZone)
   val transTime = ISODate.getTime(transactionDate, timeZone)
   val transDateTime = ISODate.getDateTime(transactionDate, timeZone)

   val host = "terminal-processor-gateway.paystack.co"
   val port = 8006

   val tmkIsoMessage = createTMKIsoMessage(transDate, transTime, transDateTime)

   val CHANNEL_NAME = "NIBSS-CHANNEL"

   var factory: ISOClientSocketFactory = SecureSSLSocketFactory();

   val channel = ASCIIChannel(host, port, ISO87APackager())

   channel.setSocketFactory(factory);

   channel.setTimeout(60000);

   channel.setName(CHANNEL_NAME);

   try {
      channel.connect()
      System.out.println("Connected to $host:$port")
      channel.send(tmkIsoMessage)
      val response = channel.receive()
      channel.disconnect()
      println("Response: $response")

   } catch (e: Exception) {
      System.out.println("Error: ${e.message}")
      // e.printStackTrace()
   } finally {
      channel.disconnect()
      System.out.println("Disconnected from $host:$port")
   }
}


fun createTMKIsoMessage(transDate: String, transTime: String, transDateTime: String): ISOMsg {
    val isoMessage = ISOMsg()
    isoMessage.setMTI("0800")
    isoMessage.set(3, "9A0000")
    isoMessage.set(7, transDateTime)
    isoMessage.set(11, generateStan())
    isoMessage.set(12, transTime)
    isoMessage.set(13, transDate)
    isoMessage.set(41, "2PSTAD61")
    return isoMessage
}

fun generateStan(): String {
   val random = Random()
   val stan = random.nextInt(999999)
   return ISOUtil.zeropad(stan.toString(), 6)
}

class SecureSSLSocketFactory : ISOClientSocketFactory {
   @Throws(IOException::class, ISOException::class)
   override fun createSocket(s: String, i: Int): Socket {
      val sslsocketfactory = SSLSocketFactory.getDefault() as SSLSocketFactory
      return sslsocketfactory
         .createSocket(s, i)
   }
}
