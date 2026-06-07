package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.data.GameRecord
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessScreen(
    viewModel: ChessViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.gameState.collectAsState()
    val selected by viewModel.selectedSquare.collectAsState()
    val targets by viewModel.legalDestinations.collectAsState()
    val isComputingAI by viewModel.isComputingAI.collectAsState()
    val playColor by viewModel.playerColor.collectAsState()
    val gameMode by viewModel.gameMode.collectAsState()
    val p1Name by viewModel.player1Name.collectAsState()
    val p2Name by viewModel.player2Name.collectAsState()
    val aiLevel by viewModel.aiLevel.collectAsState()
    val history by viewModel.gameHistory.collectAsState()

    var showConfigDialog by remember { mutableStateOf(false) }
    var boardFlipped by remember { mutableStateOf(false) } // if true, Black is at bottom, White is at top
    var activeTab by remember { mutableStateOf(0) } // 0 = Active Game, 1 = History & Records

    val gradientBg = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E2638), // Midnight Ocean Blue Deep
            Color(0xFF0F141C)  // Dark Slate Void
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Trophy icon",
                            tint = Color(0xFFEBCB8B), // Luxury Gold Gold
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VISHWANATH'S ARENA",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = Color(0xFFECEFF4)
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1E2638)
                ),
                actions = {
                    IconButton(
                        onClick = { showConfigDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFFECEFF4)
                        )
                    }
                }
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.background(gradientBg)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Personal welcome/stats bar for Vishwanath
            VishwanathStatsCard(history = history)

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF2E3440))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabButton(
                    selected = activeTab == 0,
                    text = "Play Game",
                    icon = Icons.Default.PlayArrow,
                    onClick = { activeTab = 0 },
                    modifier = Modifier.testTag("tab_play")
                )
                TabButton(
                    selected = activeTab == 1,
                    text = "History & saves",
                    icon = Icons.Default.Info,
                    onClick = { activeTab = 1 },
                    modifier = Modifier.testTag("tab_history")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (activeTab) {
                0 -> {
                    // Active Game Content
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Game status banner
                        MatchStatusBanner(
                            state = state,
                            isComputingAI = isComputingAI,
                            p1Name = p1Name,
                            p2Name = p2Name,
                            gameMode = gameMode
                        )
                        
                        // Flip board button
                        IconButton(
                            onClick = { boardFlipped = !boardFlipped },
                            modifier = Modifier.testTag("flip_board_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Flip Board Aspect",
                                tint = Color(0xFF88C0D0)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chess Board Layer
                    ChessboardLayout(
                        board = state.board,
                        selectedSquare = selected,
                        possibleMoves = targets,
                        isCheck = state.isCheck,
                        boardFlipped = boardFlipped,
                        onSquareSelected = { r, c -> viewModel.selectSquare(r, c) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Direct Game controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { viewModel.resetGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD08770)), // Amber Orange
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .testTag("restart_game_button")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "New Game")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Game")
                        }

                        Button(
                            onClick = { showConfigDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E81AC)), // Nordic Royal Blue
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .testTag("setup_game_button")
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = "Game Setup")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Match Setup")
                        }
                    }
                }
                1 -> {
                    // Saved History Content
                    StatsAndHistoryPanel(
                        history = history,
                        onLoadGame = { record ->
                            viewModel.loadGame(record)
                            activeTab = 0 // jump to active game
                        },
                        onDeleteRecord = { id -> viewModel.deleteGameRecord(id) },
                        onClearAll = { viewModel.clearHistory() }
                    )
                }
            }
        }
    }

    if (showConfigDialog) {
        GameSetupDialog(
            currentP1 = p1Name,
            currentP2 = p2Name,
            currentMode = gameMode,
            currentClr = playColor,
            currentDiffIdx = aiLevel,
            onDismiss = { showConfigDialog = false },
            onConfirmSetups = { mode, p1, p2, color, diff ->
                viewModel.configureMatch(mode, p1, p2, color, diff)
                showConfigDialog = false
                activeTab = 0 // switch back to game tab automatically!
            }
        )
    }
}

// Compact Personal Header for Vishwanath
@Composable
fun VishwanathStatsCard(history: List<GameRecord>) {
    val totalGames = history.size
    val cleanHistory = history.filter { !it.isOngoing }
    val wins = cleanHistory.count { it.result.contains("Vishwanath") || it.result.contains("White Wins") }
    val losses = cleanHistory.count { it.result.contains("AI Bot") || it.result.contains("Bot") || it.result.contains("Black Wins") }
    val draws = cleanHistory.count { it.result.contains("Draw") || it.result.contains("Stalemate") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E3440).copy(alpha = 0.85f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF81A1C1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("V", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "Grand Arena Master",
                            color = Color(0xFF81A1C1),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Vishwanath",
                            color = Color(0xFFECEFF4),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                
                // Active Score counter Row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScoreBadge("Wins", wins, Color(0xFFA3BE8C))
                    Spacer(modifier = Modifier.width(6.dp))
                    ScoreBadge("Losses", losses, Color(0xFFBF616A))
                }
            }
        }
    }
}

@Composable
fun ScoreBadge(label: String, count: Int, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$label: $count",
                color = Color(0xFFECEFF4),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TabButton(
    selected: Boolean,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF88C0D0) else Color.Transparent,
            contentColor = if (selected) Color(0xFF2E3440) else Color(0xFFD8DEE9)
        ),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier
    ) {
        Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// Renders the board game status
@Composable
fun MatchStatusBanner(
    state: ChessGameState,
    isComputingAI: Boolean,
    p1Name: String,
    p2Name: String,
    gameMode: String
) {
    val activeTurnName = if (gameMode == "AI") {
        if (state.activeColor == PieceColor.WHITE) p1Name else p2Name
    } else {
        if (state.activeColor == PieceColor.WHITE) "$p1Name (White)" else "$p2Name (Black)"
    }

    val bannerText = when {
        state.isCheckmate -> "Checkmate! Game Over."
        state.isStalemate -> "Stalemate! Draw Match."
        state.gameWinner != null -> "${if (state.gameWinner == PieceColor.WHITE) p1Name else p2Name} wins the match!"
        isComputingAI -> "AI is analyzing moves..."
        state.isCheck -> "Check! Up next: $activeTurnName"
        else -> "Turn: $activeTurnName"
    }

    val bannerColor = when {
        state.isCheckmate || state.gameWinner != null -> Color(0xFFBF616A) // Soft Red
        state.isCheck -> Color(0xFFEBCB8B) // Soft Gold Row
        isComputingAI -> Color(0xFF81A1C1) // Soft Ocean
        else -> Color(0xFFA3BE8C) // Emerald Green
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bannerColor.copy(alpha = 0.2f))
            .border(1.dp, bannerColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        if (isComputingAI) {
            CircularProgressIndicator(
                color = bannerColor,
                modifier = Modifier
                    .size(14.dp)
                    .padding(end = 4.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(bannerColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = bannerText,
            color = Color(0xFFECEFF4),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

// Beautiful slate themes chessboard representation
@Composable
fun ChessboardLayout(
    board: Array<Array<Piece?>>,
    selectedSquare: BoardPosition?,
    possibleMoves: List<BoardPosition>,
    isCheck: Boolean,
    boardFlipped: Boolean,
    onSquareSelected: (Int, Int) -> Unit
) {
    val darkSquareColor = Color(0xFF4C566A) // Arctic deep charcoal-slate
    val lightSquareColor = Color(0xFFD8DEE9) // Arctic white/sand slate

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .border(4.dp, Color(0xFF2E3440), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val rowsRange = if (boardFlipped) 0..7 else 7 downTo 0
            val colsRange = if (boardFlipped) 7 downTo 0 else 0..7

            for (r in rowsRange) {
                Row(modifier = Modifier.weight(1f)) {
                    for (c in colsRange) {
                        val pos = BoardPosition(r, c)
                        val piece = board[r][c]
                        val isDark = (r + c) % 2 == 0

                        val baseColor = if (isDark) darkSquareColor else lightSquareColor
                        
                        // Highlights colors
                        val isSelected = selectedSquare == pos
                        val isTarget = possibleMoves.contains(pos)
                        val isCheckingKing = isCheck && piece?.type == PieceType.KING && piece.color == board[selectedSquare?.r ?: 0][selectedSquare?.c ?: 0]?.color?.opponent()

                        val squareColor = when {
                            isCheckingKing -> Color(0xFFBF616A).copy(alpha = 0.85f) // Red highlight
                            isSelected -> Color(0xFFEBCB8B).copy(alpha = 0.85f) // Amber highlight
                            isTarget -> baseColor // Draw marker on top later
                            else -> baseColor
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(squareColor)
                                .clickable { onSquareSelected(r, c) }
                                .testTag("square_${r}_${c}"),
                            contentAlignment = Alignment.Center
                        ) {
                            // Coordinate Labels (ranks / files) inside squares beautifully
                            if (c == if (boardFlipped) 7 else 0) {
                                Text(
                                    text = (r + 1).toString(),
                                    color = if (isDark) lightSquareColor.copy(alpha = 0.45f) else darkSquareColor.copy(alpha = 0.45f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(2.dp)
                                )
                            }
                            if (r == if (boardFlipped) 7 else 0) {
                                Text(
                                    text = ('A' + c).toString(),
                                    color = if (isDark) lightSquareColor.copy(alpha = 0.45f) else darkSquareColor.copy(alpha = 0.45f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(2.dp)
                                )
                            }

                            // Draws possible moves indicator dot
                            if (isTarget) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF81A1C1).copy(alpha = 0.75f))
                                        .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                )
                            }

                            // Physical piece coin representation
                            if (piece != null) {
                                val tokenBg = if (piece.color == PieceColor.WHITE) Color(0xFFFFFFFF) else Color(0xFF2E3440)
                                val tokenBorderClr = if (piece.color == PieceColor.WHITE) Color(0xFFD8DEE9) else Color(0xFFEBCB8B)
                                val textSymbolColor = if (piece.color == PieceColor.WHITE) Color(0xFF1C1B1F) else Color(0xFFECEFF4)

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(0.85f)
                                        .shadow(3.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(tokenBg)
                                        .border(2.dp, tokenBorderClr, CircleShape)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = piece.symbol(),
                                        fontSize = 28.sp,
                                        color = textSymbolColor,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Segment selection panel
@Composable
fun StatsAndHistoryPanel(
    history: List<GameRecord>,
    onLoadGame: (GameRecord) -> Unit,
    onDeleteRecord: (Int) -> Unit,
    onClearAll: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E3440).copy(alpha = 0.85f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Match Registry",
                    color = Color(0xFFECEFF4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (history.isNotEmpty()) {
                    TextButton(
                        onClick = onClearAll,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFBF616A))
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear All")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Empty histories",
                            tint = Color(0xFFECEFF4).copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No active saves or match records found.",
                            color = Color(0xFFECEFF4).copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Set up a game or play a few moves to build up logs!",
                            color = Color(0xFFECEFF4).copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(history) { record ->
                        GameHistoryItem(
                            record = record,
                            onLoad = { onLoadGame(record) },
                            onDelete = { onDeleteRecord(record.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameHistoryItem(
    record: GameRecord,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }
    val cleanDate = formatter.format(Date(record.date))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3B4252)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (record.isOngoing) Color(0xFFA3BE8C) else Color(0xFF81A1C1))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (record.isOngoing) "Ongoing Match" else "Completed",
                        color = if (record.isOngoing) Color(0xFFA3BE8C) else Color(0xFF81A1C1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${record.player1} vs ${record.player2}",
                    color = Color(0xFFECEFF4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Result: ${record.result} • Moves: ${record.moveCount}",
                    color = Color(0xFFECEFF4).copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = cleanDate,
                    color = Color(0xFFECEFF4).copy(alpha = 0.45f),
                    fontSize = 10.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onLoad,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (record.isOngoing) Color(0xFFA3BE8C) else Color(0xFF81A1C1),
                        contentColor = Color(0xFF2E3440)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("load_game_${record.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Explore Save Item",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (record.isOngoing) "Resume" else "View",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_game_${record.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFBF616A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Game Settings Config Dialog
@Composable
fun GameSetupDialog(
    currentP1: String,
    currentP2: String,
    currentMode: String,
    currentClr: PieceColor,
    currentDiffIdx: Int,
    onDismiss: () -> Unit,
    onConfirmSetups: (mode: String, player1: String, player2: String, userColor: PieceColor, difficulty: Int) -> Unit
) {
    var p1Input by remember { mutableStateOf(currentP1) }
    var p2Input by remember { mutableStateOf(currentP2) }
    var isAgainstAI by remember { mutableStateOf(currentMode == "AI") }
    var asWhite by remember { mutableStateOf(currentClr == PieceColor.WHITE) }
    var difficultyIndex by remember { mutableStateOf(currentDiffIdx) } // 1 = Easy, 2 = Medium

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Match Configuration",
                color = Color(0xFFECEFF4),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = Color(0xFF2E3440),
        shape = RoundedCornerShape(16.dp),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Mode selector tabs
                Text("Select Opponent", color = Color(0xFF81A1C1), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF3B4252))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isAgainstAI) Color(0xFF88C0D0) else Color.Transparent)
                            .clickable { isAgainstAI = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Smart Bot AI",
                            color = if (isAgainstAI) Color(0xFF2E3440) else Color(0xFFECEFF4),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isAgainstAI) Color(0xFF88C0D0) else Color.Transparent)
                            .clickable { isAgainstAI = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Pass & Play Friend",
                            color = if (!isAgainstAI) Color(0xFF2E3440) else Color(0xFFECEFF4),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // AI Difficulty slider or choice setup
                if (isAgainstAI) {
                    Text("AI Difficulty", color = Color(0xFF81A1C1), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF3B4252))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (difficultyIndex == 1) Color(0xFFEBCB8B) else Color.Transparent)
                                .clickable { difficultyIndex = 1 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Easy (Lvl 1)",
                                color = if (difficultyIndex == 1) Color(0xFF2E3440) else Color(0xFFECEFF4),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (difficultyIndex == 2) Color(0xFFEBCB8B) else Color.Transparent)
                                .clickable { difficultyIndex = 2 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Smart (Lvl 2)",
                                color = if (difficultyIndex == 2) Color(0xFF2E3440) else Color(0xFFECEFF4),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Choose Piece Color
                    Text("Play as Color", color = Color(0xFF81A1C1), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF3B4252))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (asWhite) Color(0xFFECEFF4) else Color.Transparent)
                                .clickable { asWhite = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "White (Goes First)",
                                color = if (asWhite) Color(0xFF2E3440) else Color(0xFFECEFF4),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!asWhite) Color(0xFF4C566A) else Color.Transparent)
                                .clickable { asWhite = false }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Black (Goes Second)",
                                color = if (!asWhite) Color(0xFFECEFF4) else Color(0xFFECEFF4),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Player name customization
                Text("Custom Player Names", color = Color(0xFF81A1C1), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                TextField(
                    value = p1Input,
                    onValueChange = { p1Input = it },
                    label = { Text("Player 1 Name") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF3B4252),
                        unfocusedContainerColor = Color(0xFF3B4252),
                        focusedTextColor = Color(0xFFECEFF4),
                        unfocusedTextColor = Color(0xFFECEFF4),
                        focusedLabelColor = Color(0xFF88C0D0),
                        unfocusedLabelColor = Color(0xFFD8DEE9)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("p1_name_input")
                )

                if (!isAgainstAI) {
                    TextField(
                        value = p2Input,
                        onValueChange = { p2Input = it },
                        label = { Text("Player 2 Name") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF3B4252),
                            unfocusedContainerColor = Color(0xFF3B4252),
                            focusedTextColor = Color(0xFFECEFF4),
                            unfocusedTextColor = Color(0xFFECEFF4),
                            focusedLabelColor = Color(0xFF88C0D0),
                            unfocusedLabelColor = Color(0xFFD8DEE9)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("p2_name_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmSetups(
                        if (isAgainstAI) "AI" else "LOCAL",
                        p1Input,
                        p2Input,
                        if (asWhite) PieceColor.WHITE else PieceColor.BLACK,
                        difficultyIndex
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA3BE8C)),
                modifier = Modifier.testTag("confirm_setup_button")
            ) {
                Text("Start Arena Match", color = Color(0xFF2E3440), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD8DEE9))
            ) {
                Text("Cancel")
            }
        }
    )
}
