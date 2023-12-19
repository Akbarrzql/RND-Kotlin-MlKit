package com.example.facerecognitioncomparison.utils

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.util.Log
import java.io.IOException
import java.util.*


class WritableTag @Throws(FormatException::class) constructor(tag: Tag) {
    private val NFC_A = NfcA::class.java.canonicalName
    private val NDEF = Ndef::class.java.canonicalName
    private val NDEF_FORMATABLE = NdefFormatable::class.java.canonicalName

    private var nfcA: NfcA? = null
    private val ndef: Ndef?
    private var ndefFormatable: NdefFormatable? = null

    val tagId: String?
        get() {
            if (ndef != null) {
                val idnfc = ndef.tag.id

                return bytesToHexString(idnfc)
            } else if (ndefFormatable != null) {
                 return bytesToHexString(ndefFormatable!!.tag.id)
            }else if (nfcA != null){
                val idnfc_a = nfcA!!.tag.id
                return bytesToHexString(nfcA!!.tag.id)
            }
            return null
        }
    init {

        val technologies = tag.techList
        val tagTechs = Arrays.asList(*technologies)
        if (tagTechs.contains(NDEF)) {
            Log.i("WritableTag", "contains ndef")
            ndef = Ndef.get(tag)
            ndefFormatable = null
        } else if (tagTechs.contains(NDEF_FORMATABLE)) {
            Log.i("WritableTag", "contains ndef_formatable")
            ndefFormatable = NdefFormatable.get(tag)
            ndef = null
        } else if (tagTechs.contains(NFC_A)){
            nfcA = NfcA.get(tag)
            ndef = null
        }
        else {
            throw FormatException("Tag doesn't support ndef")

        }
    }

    @Throws(IOException::class, FormatException::class)
    fun writeData(
        tagId: String,
        message: NdefMessage,
        messageA : ByteArray
    ): Boolean {
        if (tagId != tagId) {
            return false
        }
        if (ndef != null) {
            ndef.connect()
            if (ndef.isConnected) {
                ndef.writeNdefMessage(message)
                return true
            }
        } else if (ndefFormatable != null) {
            ndefFormatable!!.connect()
            if (ndefFormatable!!.isConnected) {
                ndefFormatable!!.format(message)
                return true
            }
        }
        else if (nfcA != null){
            nfcA!!.connect()
            if (nfcA!!.isConnected) {
                nfcA!!.transceive(messageA)
                return true
            }

        }
        return false
    }
    @Throws(IOException::class)
    private fun close() {
        ndef?.close() ?: ndefFormatable?.close()
    }
    companion object {
        fun bytesToHexString(src: ByteArray): String? {
            if (ByteUtils.isNullOrEmpty(src)) {
                return null
            }
            val sb = StringBuilder()
            for (b in src) {
                sb.append(String.format("%02X", b))
            }
            return sb.toString()

        }
    }
}





