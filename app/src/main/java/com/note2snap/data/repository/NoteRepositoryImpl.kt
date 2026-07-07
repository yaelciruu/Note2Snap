package com.note2snap.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import com.note2snap.ccl.ConnectedComponentLabeler
import com.note2snap.ccl.RegionType
import com.note2snap.core.util.ImageStorage
import com.note2snap.data.local.NoteDao
import com.note2snap.data.local.NoteElementEntity
import com.note2snap.data.local.NoteEntity
import com.note2snap.preprocessing.WhiteboardPreprocessor
import com.note2snap.recognition.HandwritingRecognizer
import com.note2snap.recognition.MockHandwritingRecognizer
import com.note2snap.structuring.ElementKind
import com.note2snap.structuring.NoteBlock
import com.note2snap.structuring.NoteStructuringEngine
import com.note2snap.structuring.StructuredElement
import com.note2snap.structuring.StructuredNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val appContext: Context,
    private val recognizer: HandwritingRecognizer = MockHandwritingRecognizer()
) : NoteRepository {

    private val imageStorage = ImageStorage(appContext)
    private val preprocessor = WhiteboardPreprocessor()
    private val labeler = ConnectedComponentLabeler()
    private val structuringEngine = NoteStructuringEngine()

    override suspend fun processAndSaveWhiteboard(imageFilePath: String, title: String): Long {
        val sourceFile = File(imageFilePath)

        val preprocessed = preprocessor.process(sourceFile)
        val regions = labeler.label(preprocessed.binarizedBitmap)
        val textRegions = regions.filter { it.type != RegionType.NON_TEXT }
        val nonTextRegions = regions.filter { it.type == RegionType.NON_TEXT }

        val recognizedLines = recognizer.recognize(textRegions)

        val structuredNote = structuringEngine.structure(
            recognizedLines = recognizedLines,
            nonTextRegions = nonTextRegions,
            sourceImageWidth = preprocessed.originalWidth,
            sourceImageHeight = preprocessed.originalHeight
        )

        val noteEntity = NoteEntity(
            title = title,
            createdAtEpochMillis = System.currentTimeMillis(),
            sourceImagePath = sourceFile.absolutePath,
            sourceImageWidth = preprocessed.originalWidth,
            sourceImageHeight = preprocessed.originalHeight
        )
        val noteId = noteDao.insertNote(noteEntity)

        var orderIndex = 0
        val elementEntities = mutableListOf<NoteElementEntity>()
        for (element in structuredNote.allElementsInReadingOrder) {
            val diagramPath = element.diagramBitmap?.let { bitmap ->
                imageStorage.saveBitmap(bitmap, filePrefix = "DIAGRAM").absolutePath
            }
            elementEntities.add(
                NoteElementEntity(
                    noteId = noteId,
                    kind = element.kind.name,
                    text = element.text,
                    confidence = element.confidence,
                    diagramImagePath = diagramPath,
                    normalizedX = element.normalizedX,
                    normalizedY = element.normalizedY,
                    normalizedWidth = element.normalizedWidth,
                    normalizedHeight = element.normalizedHeight,
                    blockIndex = element.blockIndex,
                    orderIndex = orderIndex++
                )
            )
        }
        noteDao.insertElements(elementEntities)

        return noteId
    }

    override fun observeAllNotes(): Flow<List<NoteSummary>> =
        noteDao.observeAllNotesWithElements().map { list ->
            list.map { noteWithElements ->
                val entity = noteWithElements.note
                NoteSummary(
                    id = entity.id,
                    title = entity.title,
                    createdAtEpochMillis = entity.createdAtEpochMillis,
                    sourceImagePath = entity.sourceImagePath
                )
            }
        }

    override suspend fun getStructuredNote(noteId: Long): StructuredNote? {
        val noteWithElements = noteDao.getNoteWithElements(noteId) ?: return null
        val note = noteWithElements.note
        val elements = noteWithElements.elements
            .sortedBy { it.orderIndex }
            .map { entity ->
                StructuredElement(
                    elementId = entity.id,
                    kind = ElementKind.valueOf(entity.kind),
                    text = entity.text,
                    confidence = entity.confidence,
                    diagramBitmap = entity.diagramImagePath?.let { path ->
                        BitmapFactory.decodeFile(path)
                    },
                    normalizedX = entity.normalizedX,
                    normalizedY = entity.normalizedY,
                    normalizedWidth = entity.normalizedWidth,
                    normalizedHeight = entity.normalizedHeight,
                    blockIndex = entity.blockIndex
                )
            }

        val blocks = elements.groupBy { it.blockIndex }
            .toSortedMap()
            .map { (index, blockElements) -> NoteBlock(index = index, elements = blockElements) }

        return StructuredNote(
            blocks = blocks,
            sourceImageWidth = note.sourceImageWidth,
            sourceImageHeight = note.sourceImageHeight
        )
    }

    override suspend fun deleteNote(noteId: Long) {
        noteDao.deleteNote(noteId)
    }

    override suspend fun updateElementText(elementId: Long, newText: String) {
        noteDao.updateElementText(elementId, newText)
    }
}