package dev.seriy0904.mangareader.adapter

data class FilesListModel(
    val mangaName: String,
    val mangaChapter: String,
    val dirParent: Int? = null
)
