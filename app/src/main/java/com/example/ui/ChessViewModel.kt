package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.GameDatabase
import com.example.data.GameRecord
import com.example.data.GameRepository
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private val db by lazy {
        Room.databaseBuilder(
            application,
            GameDatabase::class.java,
            "chess_arena_db"
        ).fallbackToDestructiveMigration().build()
    }

    private val repository by lazy {
        GameRepository(db.gameHistoryDao())
    }

    // Expose Room database history list to UI
    val gameHistory: StateFlow<List<GameRecord>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Board / game state flows
    private val _gameState = MutableStateFlow(ChessEngine.makeInitialState())
    val gameState: StateFlow<ChessGameState> = _gameState.asStateFlow()

    private val _selectedSquare = MutableStateFlow<BoardPosition?>(null)
    val selectedSquare: StateFlow<BoardPosition?> = _selectedSquare.asStateFlow()

    private val _legalDestinations = MutableStateFlow<List<BoardPosition>>(emptyList())
    val legalDestinations: StateFlow<List<BoardPosition>> = _legalDestinations.asStateFlow()

    private val _aiLevel = MutableStateFlow(2) // 1 = Easy, 2 = Medium
    val aiLevel: StateFlow<Int> = _aiLevel.asStateFlow()

    private val _isComputingAI = MutableStateFlow(false)
    val isComputingAI: StateFlow<Boolean> = _isComputingAI.asStateFlow()

    private val _playerColor = MutableStateFlow(PieceColor.WHITE) // default user: White (Vishwanath)
    val playerColor: StateFlow<PieceColor> = _playerColor.asStateFlow()

    private val _gameMode = MutableStateFlow("AI") // "AI" (Player vs Bot) or "LOCAL" (Player vs Player)
    val gameMode: StateFlow<String> = _gameMode.asStateFlow()

    private val _player1Name = MutableStateFlow("Vishwanath")
    val player1Name: StateFlow<String> = _player1Name.asStateFlow()

    private val _player2Name = MutableStateFlow("Chess Bot")
    val player2Name: StateFlow<String> = _player2Name.asStateFlow()

    private var activeRecordId: Int? = null

    init {
        // Initial setup
        resetGame()
    }

    // Configure the match setups
    fun configureMatch(mode: String, p1Name: String, p2Name: String, userColor: PieceColor, aiDiff: Int) {
        _gameMode.value = mode
        _player1Name.value = if (p1Name.isNotBlank()) p1Name else "Vishwanath"
        _player2Name.value = if (mode == "AI") "Bot Lvl $aiDiff" else if (p2Name.isNotBlank()) p2Name else "Opponent"
        _playerColor.value = userColor
        _aiLevel.value = aiDiff
        activeRecordId = null
        resetGame()
    }

    fun resetGame() {
        _gameState.value = ChessEngine.makeInitialState()
        _selectedSquare.value = null
        _legalDestinations.value = emptyList()
        _isComputingAI.value = false
        activeRecordId = null

        // Trigger AI move if Mode == AI and player chose BLACK (meaning AI is White and moves first)
        triggerAiIfNecessary()
    }

    // Select grid tiles
    fun selectSquare(r: Int, c: Int) {
        if (_isComputingAI.value) return // Block input while AI of bot makes calculation

        val clickPos = BoardPosition(r, c)
        val currentBoard = _gameState.value.board
        val clickedPiece = currentBoard[r][c]
        val activeCol = _gameState.value.activeColor

        // Ensure user is matching their respective color turn in AI mode
        if (_gameMode.value == "AI" && activeCol != _playerColor.value) {
            return // Not player's turn to click!
        }

        val selected = _selectedSquare.value

        if (selected == null) {
            // Select piece of active color
            if (clickedPiece != null && clickedPiece.color == activeCol) {
                _selectedSquare.value = clickPos
                _legalDestinations.value = ChessEngine.getLegalMoves(currentBoard, clickPos).map { it.to }
            }
        } else {
            // Deselect piece by clicking again on it
            if (selected == clickPos) {
                clearSelection()
                return
            }

            // Attempting to move
            val legalMovesForSelected = ChessEngine.getLegalMoves(currentBoard, selected)
            val move = legalMovesForSelected.find { it.to == clickPos }

            if (move != null) {
                // Execute move
                val newState = ChessEngine.executeMove(_gameState.value, move)
                _gameState.value = newState
                clearSelection()

                // Save game progress automatically
                autosaveCurrentGame()

                // Handle Turn Toggle AI bot
                triggerAiIfNecessary()
            } else {
                // User click another friendly piece -> Change Selection
                if (clickedPiece != null && clickedPiece.color == activeCol) {
                    _selectedSquare.value = clickPos
                    _legalDestinations.value = ChessEngine.getLegalMoves(currentBoard, clickPos).map { it.to }
                } else {
                    // Tap elsewhere -> Clear selection
                    clearSelection()
                }
            }
        }
    }

    private fun clearSelection() {
        _selectedSquare.value = null
        _legalDestinations.value = emptyList()
    }

    private fun triggerAiIfNecessary() {
        val state = _gameState.value
        if (state.gameWinner != null || state.isCheckmate || state.isStalemate) return

        if (_gameMode.value == "AI" && state.activeColor != _playerColor.value) {
            _isComputingAI.value = true
            viewModelScope.launch {
                val bestMove = withContext(Dispatchers.Default) {
                    // Small delay to make it look smooth and conversational
                    kotlinx.coroutines.delay(600)
                    ChessEngine.getBestMove(state, state.activeColor, depth = _aiLevel.value)
                }

                if (bestMove != null) {
                    val newState = ChessEngine.executeMove(_gameState.value, bestMove)
                    _gameState.value = newState
                    autosaveCurrentGame()
                }
                _isComputingAI.value = false
            }
        }
    }

    // Local save/load features
    private fun autosaveCurrentGame() {
        viewModelScope.launch {
            val state = _gameState.value
            val isFinished = state.gameWinner != null || state.isCheckmate || state.isStalemate
            val resultSummary = when {
                state.isCheckmate -> if (state.gameWinner == PieceColor.WHITE) "${_player1Name.value} Wins!" else "${_player2Name.value} Wins!"
                state.isStalemate -> "Stalemate!"
                isFinished -> "Draw!"
                else -> "Ongoing"
            }

            val serialized = ChessEngine.serializeBoard(state.board)

            val record = GameRecord(
                id = activeRecordId ?: 0,
                date = System.currentTimeMillis(),
                player1 = if (_playerColor.value == PieceColor.WHITE) _player1Name.value else _player2Name.value,
                player2 = if (_playerColor.value == PieceColor.WHITE) _player2Name.value else _player1Name.value,
                result = resultSummary,
                isOngoing = !isFinished,
                serializedBoard = serialized,
                activeColor = state.activeColor.name,
                diffLevel = _aiLevel.value,
                moveCount = state.moveHistory.size
            )

            val insertedId = repository.insert(record)
            if (activeRecordId == null) {
                // Keep record of the ongoing auto save ID
                activeRecordId = insertedId.toInt()
            }
        }
    }

    fun loadGame(record: GameRecord) {
        viewModelScope.launch {
            _isComputingAI.value = false
            activeRecordId = record.id

            val deserializedBoard = ChessEngine.deserializeBoard(record.serializedBoard)
            val activeCol = try { PieceColor.valueOf(record.activeColor) } catch (e: Exception) { PieceColor.WHITE }

            val inCheck = ChessEngine.isColorInCheck(deserializedBoard, activeCol)
            val legals = ChessEngine.getAllLegalMoves(deserializedBoard, activeCol)
            val isCheckmate = legals.isEmpty() && inCheck
            val isStalemate = legals.isEmpty() && !inCheck

            _gameState.value = ChessGameState(
                board = deserializedBoard,
                activeColor = activeCol,
                gameWinner = if (isCheckmate) activeCol.opponent() else null,
                isCheckmate = isCheckmate,
                isStalemate = isStalemate,
                isCheck = inCheck,
                moveHistory = emptyList() // Clear history of moves since we loaded raw state
            )

            // Setup mode details from loaded match
            val isAiGame = record.player2.startsWith("Bot") || record.player2 == "AI Bot" || record.player2.contains("Bot")
            _gameMode.value = if (isAiGame) "AI" else "LOCAL"
            _player1Name.value = record.player1
            _player2Name.value = record.player2
            _aiLevel.value = record.diffLevel
            _playerColor.value = if (isAiGame) PieceColor.WHITE else PieceColor.WHITE // default White is User

            clearSelection()

            // Trigger bot moves if loaded state has bot side due to play
            triggerAiIfNecessary()
        }
    }

    fun deleteGameRecord(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
            if (activeRecordId == id) {
                activeRecordId = null
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clear()
            activeRecordId = null
        }
    }
}
