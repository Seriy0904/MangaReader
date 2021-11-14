package dev.seriy0904.mangareader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.core.net.toUri
import dev.seriy0904.mangareader.TinyDB.TinyDB
import java.io.File
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.util.zip.ZipInputStream


class MainActivity : AppCompatActivity() {
    private val bitmapArray: ArrayList<ArrayList<Bitmap>> = arrayListOf()
    private val pathArray: ArrayList<ArrayList<String>> = arrayListOf()
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
                getZipArchive(sortedList)
            }
        }
    private val requestPermission: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
        ) {
            requestPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        nextPage.setOnClickListener {
            page += 1
            setImage()
        }
        previousPage.setOnClickListener {
            page -= 1
            setImage()
        }
        val tiny = TinyDB(this)
        val shrUri = tiny.getListString("READING_URI_ZIP")
        val savedImageSerializable = intent.getSerializableExtra("SavedImages")
        if (savedImageSerializable != null) {
            val savedImageFileMainDir = (savedImageSerializable as File).listFiles().sortedBy { it.name }
            for (i in savedImageFileMainDir) {
                mangaUriArray.add(i.toUri())
                pathArray.add(arrayListOf())
                for (y in i.listFiles().sortedBy { it.name }) {
                    pathArray.last().add(y.path)
                }
            }
            chapter = intent.getIntExtra("SavedMangaChapter", 0)
            mangaTitle.text = File(mangaUriArray[chapter].path!!).name.substringBeforeLast('.')
            Log.d("MyTag","uri finished")
            bitmapLazyLoader(chapter)
            setImage()

        } else if (shrUri != null && false) {
            val stringToUri = arrayListOf<Uri>()
            try {
                for (el in shrUri) {
                    val streamFile = contentResolver.openInputStream(Uri.parse(el))
                    stringToUri.add(Uri.parse(el))
                }
                page = tiny.getInt("READING_PAGE")
                chapter = tiny.getInt("READING_CHAPTER")
                getZipArchive(stringToUri, reloadPage = true)
            } catch (e: FileNotFoundException) {
                Log.d("MyTag", "Eroor loading")
            }
        }
    }

    private fun setImage() {
//        if (reload) {
//            if (uriArray.size > chapter && page < uriArray[chapter].size && page >= 0) {
//                mangaImage.setImageURI(uriArray[chapter][page])
////                Glide.with(this).load(uriArray[chapter][page]).dontTransform().into(mangaImage)
//                pageTextView.text =
//                    getString(R.string.page_text, page, uriArray[chapter].size - 1)
//                pageTextView.visibility = View.VISIBLE
//                scrollView.scrollTo(0, 0)
//            } else if (page < 0 && chapter > 0) {
//                chapter -= 1
//                mangaTitle.text = File(mangaUriArray[chapter].path!!).name.substringBeforeLast('.')
//                page = uriArray[chapter].size - 1
//                setImage()
//            } else if (chapter < uriArray.size - 1 && page > 0) {
//                chapter += 1
//                mangaTitle.text = File(mangaUriArray[chapter].path!!).name.substringBeforeLast('.')
//                page = 0
//                setImage()
//            } else if (page < 0) {
//                page = 0
//            } else {
//                page = uriArray[chapter].size - 1
//            }
//        } else {
            if (bitmapArray.size > chapter && page < bitmapArray[chapter].size && page >= 0) {
                mangaImage.setImageBitmap(bitmapArray[chapter][page])
                pageTextView.text =
                    getString(R.string.page_text, page, bitmapArray[chapter].size - 1)
                pageTextView.visibility = View.VISIBLE
                scrollView.scrollTo(0, 0)
            } else if (page < 0 && chapter > 0) {
                chapter -= 1
                if(bitmapArray[chapter].size<1) bitmapLazyLoader(chapter)
                mangaTitle.text = File(mangaUriArray[chapter].path!!).name.substringBeforeLast('.')
                page = bitmapArray[chapter].size - 1
                setImage()
            } else if (chapter < bitmapArray.size - 1 && page > 0) {
                chapter += 1
                if(bitmapArray[chapter].size<1) bitmapLazyLoader(chapter)
                mangaTitle.text = File(mangaUriArray[chapter].path!!).name.substringBeforeLast('.')
                page = 0
                setImage()
            } else if (page < 0) {
                page = 0
            } else {
                page = bitmapArray[chapter].size - 1
            }
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.selectManga -> selectIntent.launch(arrayOf("application/zip"))
            R.id.savedManga -> {
                val savedMangas = Intent(this, SavedMangaActivity::class.java)
                if (bitmapArray.size > 0) savedMangas.putExtra("Read", true)
                startActivity(savedMangas)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
//        if (mangaUriArray.isNotEmpty()) {
//                val tiny = TinyDB(this)
//                tiny.putInt("READING_PAGE", page)
//                tiny.putInt("READING_CHAPTER", chapter)
//                tiny.putBoolean("READING_SAVED", false)
//                val stringUri = arrayListOf<String>()
//                for (el in mangaUriArray) {
//                    stringUri.add(el.toString())
//                }
//                tiny.putListString("READING_URI_ZIP", stringUri)
//            }
//            else {
//                val tiny = TinyDB(this)
//                tiny.putInt("READING_PAGE", page)
//                tiny.putInt("READING_CHAPTER", chapter)
//                tiny.putBoolean("READING_SAVED", true)
//                val stringUri = arrayListOf<String>()
//                for (el in mangaUriArray) {
//                    stringUri.add(el.toString())
//                }
//                tiny.pu("READING_URI_ZIP", stringUri)
//            }
//        }
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
                    saveImage(p, ze.name, File(uri[i].path).name)
                    Log.d("MyTag", "Pos: $i")
                }
                ze = zipFile.nextEntry
            }
            streamFile?.close()
            zipFile.close()
        }
        if (!reloadPage) page = 0
        mangaTitle.text = File(mangaUriArray[chapter].path!!).name.substringBeforeLast('.')
        setImage()
    }

    private fun saveImage(bmp: Bitmap, fileName: String, chapterName: String) {
        val storage = File(getExternalFilesDir(null), "Манга")
        val mangaName = chapterName.substringBeforeLast(" Т")
        val chapterWord = chapterName.substringAfter("$mangaName ").substringBeforeLast('.')
        val mangaDir = File(storage, mangaName)
        mangaDir.mkdir()
        val chapterDir = File(mangaDir, chapterWord)
        chapterDir.mkdir()
        val file = File(chapterDir, fileName)
        file.writeBytes(bmp.convertToByteArray())
    }

    private fun Bitmap.convertToByteArray(): ByteArray {
        val size = this.byteCount
        val buffer = ByteBuffer.allocate(size)
        val bytes = ByteArray(size)
        this.copyPixelsToBuffer(buffer)
        buffer.rewind()
        buffer.get(bytes)
        return bytes
    }

    private var doubleBackToExitPressedOnce = false
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Нажмите еще раз для выхода", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            doubleBackToExitPressedOnce = false
        }, 2000)
    }
    private fun bitmapLazyLoader(chapterPos:Int){
        if(pathArray.size>0){
            Log.d("MyTag","Bitmap lazy:$chapterPos")
            if (bitmapArray.size < 1) {
                for(i in mangaUriArray.indices){
                    bitmapArray.add(arrayListOf())
                }
                Log.d("MyTag","bitmapArray.add")
            }
            val opts = BitmapFactory.Options()
            opts.inSampleSize = 1
            opts.inPreferredConfig = Bitmap.Config.RGB_565
            for (i in pathArray[chapterPos]) {
                bitmapArray[chapterPos].add(BitmapFactory.decodeFile(i, opts))
            }
        }
    }
}