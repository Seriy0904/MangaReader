package dev.seriy0904.mangareader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import dev.seriy0904.mangareader.TinyDB.TinyDB
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream


class MainActivity : AppCompatActivity() {
    private val bitmapArray: ArrayList<ArrayList<Bitmap>> = arrayListOf()
    private val mangaUriArray: ArrayList<Uri> = arrayListOf()
    private val mangaTitle: TextView by lazy { findViewById(R.id.mangaTitle) }
    private val scrollView: ScrollView by lazy { findViewById(R.id.scrollLayout) }
    private val nextPage: View by lazy { findViewById(R.id.nextPage) }
    private val previousPage: View by lazy { findViewById(R.id.previousPage) }
    private val mangaImage: ImageView by lazy { findViewById(R.id.manga_image) }
    private val pageTextView: TextView by lazy { findViewById(R.id.pageNumber) }
    private var page: Int = 0
    private var chapter: Int = 0
    private val selectIntent: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { imageUri: List<Uri>? ->
            if (imageUri != null) {
                val sortedList = imageUri.sortedBy {
                    it.lastPathSegment
                }
                Toast.makeText(
                    this,
                    "Подождите разорхивации, она длится около 30 секнуд",
                    Toast.LENGTH_LONG
                ).show()
                getZipArchive(sortedList)
            }
        }
    private val requestPermission: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val tiny = TinyDB(this)
        val shrUri = tiny.getListString("READING_URI")
        if (shrUri != null&&true) {
            page = tiny.getInt("READING_PAGE")
            chapter = tiny.getInt("READING_CHAPTER")
            val stringToUri = arrayListOf<Uri>()
            for (el in shrUri){
                stringToUri.add(Uri.parse(el))
            }
            Log.d("MyTag","Array: $shrUri")
            getZipArchive(stringToUri, reloadPage = true)
        }
        nextPage.setOnClickListener {
            page += 1
            setImage()
        }
        previousPage.setOnClickListener {
            page -= 1
            setImage()
        }
    }

    private fun setImage() {
        if (bitmapArray.size > chapter && page < bitmapArray[chapter].size && page >= 0) {
            mangaImage.setImageBitmap(bitmapArray[chapter][page])
            pageTextView.text = getString(R.string.page_text, page, bitmapArray[chapter].size - 1)
            pageTextView.visibility = View.VISIBLE
            scrollView.scrollTo(0, 0)
        } else if (page < 0 && chapter > 0) {
            chapter -= 1
            mangaTitle.text = File(mangaUriArray[chapter].path!!).name.substringBeforeLast('.')
            page = bitmapArray[chapter].size - 1
            setImage()
        } else if (chapter < bitmapArray.size - 1 && page > 0) {
            chapter += 1
            mangaTitle.text = File(mangaUriArray[chapter].path!!).name.substringBeforeLast('.')
            page = 0
            setImage()
        } else if (page < 0) {
            page = 0
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.selectManga -> selectIntent.launch(arrayOf("application/zip"))
            R.id.saveManga -> saveManga()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveManga() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
        ) {
            requestPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    override fun onStop() {
        if (mangaUriArray.isNotEmpty()) {
            val tiny = TinyDB(this)
            tiny.putInt("READING_PAGE", page)
            tiny.putInt("READING_CHAPTER", chapter)
            val stringUri = arrayListOf<String>()
            for (el in mangaUriArray) {
                stringUri.add(el.toString())
            }
            tiny.putListString("READING_URI", stringUri)
        }
        super.onStop()
    }

    private fun getZipArchive(
        uri: List<Uri>,
        reloadPage: Boolean = false
    ) {
        bitmapArray.clear()
        mangaUriArray.clear()
        mangaUriArray.addAll(uri)
        for (i in uri.indices) {
            val contentResolver = applicationContext.contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri[i], takeFlags)
            bitmapArray.add(arrayListOf())
            val streamFile = contentResolver.openInputStream(uri[i])
            val opts = BitmapFactory.Options()
            opts.inSampleSize = 1
            opts.inPreferredConfig = Bitmap.Config.RGB_565
            val zipFile = ZipInputStream(streamFile)
            var ze = zipFile.nextEntry
            while (ze != null) {
                val p = BitmapFactory.decodeStream(zipFile, null, opts)
                if (p != null) {
                    bitmapArray[i].add(p)
                }
                ze = zipFile.nextEntry
            }
            zipFile.close()
        }
        if (!reloadPage) page = 0
        mangaTitle.text = File(mangaUriArray[chapter].path!!).name.substringBeforeLast('.')
        setImage()
    }
}