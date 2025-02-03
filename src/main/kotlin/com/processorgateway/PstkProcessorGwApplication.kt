package com.processorgateway

import org.jpos.iso.ISOClientSocketFactory
import org.jpos.iso.ISODate
import org.jpos.iso.ISOException
import org.jpos.iso.channel.ASCIIChannel
import org.jpos.util.Logger
import org.jpos.util.SimpleLogListener
import java.io.IOException
import java.net.Socket
import java.util.*
import javax.net.ssl.SSLSocketFactory

class PstkProcessorGwApplication

fun main(args: Array<String>) {
   println("Starting Connection to Paystack Processor Gateway!")

   /*
   Every terminal has to initiate the Key Exchange message before it can do anything else.
   These would provide the keys that would be used to both encrypt the other message types being sent.

   Key Exchange Involves the following:
       1. Terminal Master Key (TMK)
       2. Terminal Session Key (TSK)
       3. Terminal Pin Key (TPK)
       4. Terminal Parameters
       5. AID
       6. CAPK
   */


   val logger = Logger().apply {
      addListener(SimpleLogListener(System.out))
   }

   val transactionDate = Date()
   val timeZone = TimeZone.getTimeZone("GMT+1")

   val transDate = ISODate.getDate(transactionDate, timeZone)
   val transTime = ISODate.getTime(transactionDate, timeZone)
   val transDateTime = ISODate.getDateTime(transactionDate, timeZone)

   val host = "terminal-processor-gateway.paystack.co"
   val port = 8006
   val terminalId = "2PSTAD61"
   val channelName = "NIBSS-CHANNEL"
   val timeout = 60000

   val factory: ISOClientSocketFactory = SecureSSLSocketFactory()
   val packager = Packager().apply { setLogger(logger, "") }
   val channel = setupChannel(host, port, packager, factory, channelName, timeout, logger)

   try {
      channel.connect()
      println("Connected to $host:$port")

      val terminalMasterKey = initiateKeyExchange(channel, transDate, transTime, transDateTime, terminalId)
      if (terminalMasterKey != null) {
         val terminalSessionKey = getSessionKey(channel, transDate, transTime, transDateTime, terminalId, terminalMasterKey)
         if (terminalSessionKey != null) {
           getTerminalPinKey(channel, transDate, transTime, transDateTime, terminalId)
           initiateParamDownload(channel, transDate, transTime, transDateTime, terminalId, terminalSessionKey, packager)
           initiateCAPKDownload(channel, transDate, transTime, transDateTime, terminalId, terminalSessionKey, packager)
           initiateAIDDownload(channel, transDate, transTime, transDateTime, terminalId, terminalSessionKey, packager)

            println("Key Exchange Successful")
         }
      }
   } catch (e: Exception) {
      println("Error: ${e.message}")
   } finally {
      channel.disconnect()
      println("Disconnected from $host:$port")
   }
}

fun setupChannel(
   host: String, port: Int, packager: Packager, factory: ISOClientSocketFactory,
   channelName: String, timeout: Int, logger: Logger
): ASCIIChannel {
   return ASCIIChannel(host, port, packager).apply {
      setLogger(logger, "")
      setSocketFactory(factory)
      setTimeout(timeout)
      setName(channelName)
   }
}

fun initiateKeyExchange(
   channel: ASCIIChannel, transDate: String, transTime: String, transDateTime: String, terminalId: String
): String? {
   val tmkIsoMessage = Util.createTMKIsoMessage(transDate, transTime, transDateTime, terminalId)
   channel.send(tmkIsoMessage)
   val response = channel.receive()

   println("Response: $response")
   return if (response.getString(39).endsWith("00")) {
      println("TMK Request Successful")
      val encryptedTMK = response.getString(53)
      val componentKey1 = "<< component key 1 >>"
      val componentKey2 = "<< component key 2 >>"
      Util.getDecryptedTMKFromHost(encryptedTMK, componentKey1, componentKey2).also {
         println("Terminal Master Key: $it")
      }
   } else {
      println("TMK Request Failed: ${response.getString(39)}")
      null
   }
}

fun getSessionKey(
   channel: ASCIIChannel, transDate: String, transTime: String, transDateTime: String,
   terminalId: String, terminalMasterKey: String
): String? {
   val tskIsoMessage = Util.createTSKIsoMessage(transDate, transTime, transDateTime, terminalId)
   channel.send(tskIsoMessage)
   val tskResponse = channel.receive()

   println("Response: $tskResponse")
   if (tskResponse.getString(39).endsWith("00")) {
      println("TSK Request Successful")
      val terminalSessionKey = Util.getDecryptedTSKFromHost(tskResponse.getString(53), terminalMasterKey)
      println("Terminal Session Key: $terminalSessionKey")
      return terminalSessionKey;
   } else {
      println("TSK Request Failed: ${tskResponse.getString(39)}")
   }
   return null;
}

fun getTerminalPinKey(
   channel: ASCIIChannel, transDate: String, transTime: String, transDateTime: String,
   terminalId: String
) {
   val tpkIsoMessage = Util.createTPKIsoMessage(transDate, transTime, transDateTime, terminalId)
   channel.send(tpkIsoMessage)
   val tpkResponse = channel.receive()

   println("Response: $tpkResponse")
   if (tpkResponse.getString(39).endsWith("00")) {
      println("TPK Request Successful")
      println(tpkResponse.getString(39))
   } else {
      println("TSK Request Failed: ${tpkResponse.getString(39)}")
   }
}

fun initiateParamDownload(
   channel: ASCIIChannel, transDate: String, transTime: String, transDateTime: String,
   terminalId: String, terminalSessionKey: String, packager: Packager
) {

   val tParamDownloadIsoMessage = Util.createParamDownloadIsoMessage(transDate, transTime, transDateTime, terminalId, terminalSessionKey, packager)
   channel.send(tParamDownloadIsoMessage)
   val tParamDownloadResponse = channel.receive()

   println("Response: $tParamDownloadResponse")
   if (tParamDownloadResponse.getString(39).endsWith("00")) {
      println("TPK Request Successful")
      println(tParamDownloadResponse.getString(39))
   } else {
      println("TPK Request Failed: ${tParamDownloadResponse.getString(39)}")
   }
}

fun initiateCAPKDownload(
   channel: ASCIIChannel, transDate: String, transTime: String, transDateTime: String,
   terminalId: String, terminalSessionKey: String, packager: Packager
) {
   val tCAPKDownloadIsoMessage = Util.createCAPKDownloadIsoMessage(transDate, transTime, transDateTime, terminalId, terminalSessionKey, packager)
   channel.send(tCAPKDownloadIsoMessage)
   val tCAPKDownloadResponse = channel.receive()

   println("Response: $tCAPKDownloadResponse")
   if (tCAPKDownloadResponse.getString(39).endsWith("00")) {
      println("CAPK Request Successful")
      println(tCAPKDownloadResponse.getString(39))
   } else {
      println("CAPK Request Failed: ${tCAPKDownloadResponse.getString(39)}")
   }
}



fun initiateAIDDownload(
   channel: ASCIIChannel, transDate: String, transTime: String, transDateTime: String,
   terminalId: String, terminalSessionKey: String, packager: Packager
) {
   val tAIDDownloadIsoMessage = Util.createAIDDownloadIsoMessage(transDate, transTime, transDateTime, terminalId, terminalSessionKey, packager)
   channel.send(tAIDDownloadIsoMessage)
   val tAIDDownloadResponse = channel.receive()

   println("Response: $tAIDDownloadResponse")
   if (tAIDDownloadResponse.getString(39).endsWith("00")) {
      println("AID Request Successful")
      println(tAIDDownloadResponse.getString(39))
   } else {
      println("AID Request Failed: ${tAIDDownloadResponse.getString(39)}")
   }
}


class SecureSSLSocketFactory : ISOClientSocketFactory {
   @Throws(IOException::class, ISOException::class)
   override fun createSocket(host: String, port: Int): Socket {
      return (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(host, port)
   }
}

