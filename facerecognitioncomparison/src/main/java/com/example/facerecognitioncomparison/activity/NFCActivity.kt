package com.example.facerecognitioncomparison.activity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.FormatException
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.example.facerecognitioncomparison.databinding.ActivityNfcactivityBinding
import com.example.facerecognitioncomparison.utils.WritableTag
import java.io.File

class NFCActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNfcactivityBinding
    private var adapter: NfcAdapter? = null
    var tag: WritableTag? = null
    var tagId: String? = null
    var nfcid: String = ""
    val txtNfc: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNfcactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initNfcAdapter()
    }
    fun initNfcAdapter() {
        val nfcManager = getSystemService(Context.NFC_SERVICE) as NfcManager
        adapter = nfcManager.defaultAdapter
    }


    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
    }

    override fun onPause() {
        disableNfcForegroundDispatch()
        super.onPause()
    }

    private fun enableNfcForegroundDispatch() {
        try {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val flag =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                else 0
            val nfcPendingIntent = PendingIntent.getActivity(this, 0, intent, flag)
            adapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
        } catch (ex: IllegalStateException) {
            Log.e(getTag(), "Error enabling NFC foreground dispatch", ex)
        }
    }

    private fun disableNfcForegroundDispatch() {
        try {
            adapter?.disableForegroundDispatch(this)
        } catch (ex: IllegalStateException) {
            Log.e(getTag(), "Error disabling NFC foreground dispatch", ex)
        }
    }

    private fun getTag() = "MainActivity"

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tagFromIntent = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        try {
            tag = tagFromIntent?.let { WritableTag(it) }
        } catch (e: FormatException) {
            Log.e(getTag(), "Unsupported tag tapped", e)
            return
        }
        tagId = tag!!.tagId
        returnValue(tagId.toString())
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMsgs != null) {
                onTagTapped()
            }
        }
    }


    private fun returnValue(str: String) : String{
        var buffer = ""

        for (index in str.length - 1 downTo 1 step 2) {
            buffer += str[index - 1]
            buffer += str[index] + ""
        }
        buffer = buffer.trim()
        buffer = buffer.takeLast(8)
        nfcid = buffer.toLong(16).toString()

        nfcid = when (nfcid.length) {
            9 -> {
                "0$nfcid"
            }
            8 -> {
                "00$nfcid"
            }
            7 -> {
                "000$nfcid"
            }
            else -> {
                nfcid
            }
        }
        binding.nfcId.text = nfcid
        val nfcId = nfcid
        Log.d("check NFCID", nfcId)
        if (isNfcIdFolderExists(nfcId)) {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("nfcid", nfcId)
            startActivity(intent)
        } else {
            val intent = Intent(this, AddFaceActivity::class.java)
            intent.putExtra("nfcid", nfcId)
            startActivity(intent)
        }
        Log.d("nfc", nfcid)
        txtNfc?.text = nfcid
        return  nfcid
    }

    private fun onTagTapped() {}

    private fun isNfcIdFolderExists(nfcId: String): Boolean {
        // Get the directory path for "face recognition" in the external files directory
        val imagesDirectory = File(getExternalFilesDir(null), "images")

        // Create the directory if it doesn't exist
        if (!imagesDirectory.exists()) {
            return false
        }

        // Check if the "nfc_id" folder exists
        val nfcIdDirectory = File(imagesDirectory, nfcId)
        return nfcIdDirectory.exists()
    }

}