package com.example.nfcaudiorecorder

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private lateinit var tvStatus: TextView
    private lateinit var tvTagInfo: TextView

    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var recordingTagId: String? = null

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvTagInfo = findViewById(R.id.tvTagInfo)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            tvStatus.text = "NFC nao disponivel neste aparelho"
            return
        }
        if (nfcAdapter?.isEnabled == false) {
            tvStatus.text = "NFC desligado - ative nas configuracoes"
        }

        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        tag?.let { handleTag(it) }
    }

    private fun handleTag(tag: Tag) {
        val id = tag.id.joinToString("") { "%02X".format(it) }
        tvTagInfo.text = "Tag: $id"

        if (mediaRecorder == null) {
            recordingTagId = id
            startRecording()
        } else if (recordingTagId == id) {
            stopRecording()
        } else {
            Toast
                .makeText(this, "Gravando outra tag. Aproxime a mesma.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun outputDir(): File {
        // Android 10+ nao permite escrita direta em Downloads publico sem MediaStore,
        // entao gravamos no diretorio externo do app (acessivel via gerenciador de arquivos).
        val dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: File(filesDir, "monitor")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionsIfNeeded()
            tvStatus.text = "Permissao de microfone necessaria"
            recordingTagId = null
            return
        }

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(outputDir(), "REC_$stamp.3gp")
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            currentFile = file
            tvStatus.text = getString(R.string.status_recording)
            Log.d(TAG, "gravando -> ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "falha ao iniciar gravacao", e)
            runCatching { recorder.release() }
            mediaRecorder = null
            currentFile = null
            recordingTagId = null
            tvStatus.text = "Falha ao iniciar gravacao"
            Toast.makeText(this, "Falha ao iniciar gravacao: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopRecording() {
        val file = currentFile
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "falha ao parar gravacao", e)
        }
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null
        currentFile = null
        recordingTagId = null

        tvStatus.text = getString(R.string.status_saved)
        tvTagInfo.text = file?.let { "Salvo: ${it.name}" } ?: ""

        if (file != null && file.exists()) {
            AlertDialog.Builder(this)
                .setTitle("Audio salvo")
                .setMessage("${file.name}\n\n${file.absolutePath}\n\n${file.length() / 1024} KB")
                .setPositiveButton("OK", null)
                .show()
            Log.d(TAG, "salvo -> ${file.absolutePath}")
        }
    }

    companion object {
        private const val TAG = "NFCAudio"
    }
}
