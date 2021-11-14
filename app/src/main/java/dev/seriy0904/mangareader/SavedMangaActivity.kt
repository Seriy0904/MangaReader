package dev.seriy0904.mangareader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.seriy0904.mangareader.adapter.FilesListAdapter
import dev.seriy0904.mangareader.adapter.FilesListModel
import java.io.File

class SavedMangaActivity : AppCompatActivity() {
    private val filesRecyclerView: RecyclerView by lazy { findViewById(R.id.filesRecycler) }
    private val modelList: ArrayList<FilesListModel> = arrayListOf()
    private var selectManga = false
    private val adapter = FilesListAdapter(object : FilesListAdapter.FileClick {
        override fun onClick(position: Int) {
            val mainDir = File(
                getExternalFilesDir(null),
                "Манга"
            ).listFiles()
            if (mainDir != null) {
                if (!selectManga) startForm(mainDir[position])
                else {
                    if (intent.getBooleanExtra(
                            "Read",
                            false
                        )
                    ) {
                        AlertDialog.Builder(this@SavedMangaActivity)
                            .setCancelable(true).setTitle("Вы уже читаете мангу, уверены что хотите начать новую")
                            .setPositiveButton(
                                "Да, уверен"
                            ) { _, _ ->
                                val mainActivity = Intent(this@SavedMangaActivity, MainActivity::class.java)
                                mainActivity.putExtra("SavedImages", mainDir[modelList[0].dirParent ?: 0])
                                mainActivity.putExtra("SavedMangaChapter", position)
                                mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(mainActivity)
                            }.setNegativeButton("Нет, отмена") { dial, _ -> dial.cancel() }.show()
                    }
                    else{
                        val mainActivity = Intent(this@SavedMangaActivity, MainActivity::class.java)
                        mainActivity.putExtra("SavedImages", mainDir[modelList[0].dirParent ?: 0])
                        mainActivity.putExtra("SavedMangaChapter", position)
                        mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(mainActivity)
                    }
                }
                selectManga = true
            }
        }
    })


    private fun startForm(mainDir: File) {
        modelList.clear()
        for (i in mainDir.listFiles()) {
            var chapterCount = 0
            for (y in i.listFiles()) {
                chapterCount++
            }
            if (mainDir.name == "Манга") modelList.add(FilesListModel(i.name, "$chapterCount Глав"))
            else modelList.add(
                FilesListModel(
                    i.name,
                    "$chapterCount Страниц",
                    File(getExternalFilesDir(null), "Манга").listFiles().indexOf(mainDir)
                )
            )
        }
        val cloneModelList = modelList.clone() as ArrayList<FilesListModel>
        modelList.clear()
        modelList.addAll(cloneModelList.sortedBy { it.mangaName })
        adapter.setList(modelList)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_manga)
        filesRecyclerView.layoutManager = LinearLayoutManager(this)
        filesRecyclerView.adapter = adapter
        startForm(File(getExternalFilesDir(null), "Манга"))
    }

    override fun onBackPressed() {
        if (selectManga) {
            startForm(File(getExternalFilesDir(null), "Манга"))
            selectManga = false
        } else super.onBackPressed()
    }
}