package com.example.model

import kotlin.math.abs
import kotlin.random.Random

enum class PieceColor {
    WHITE, BLACK;
    fun opponent(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val pieceVal: Int) {
    PAWN(100),
    KNIGHT(320),
    BISHOP(330),
    ROOK(500),
    QUEEN(900),
    KING(20000)
}

data class Piece(
    val type: PieceType,
    val color: PieceColor,
    val hasMoved: Boolean = false
) {
    // Unicode symbol helper for UI representation
    fun symbol(): String {
        return when (color) {
            PieceColor.WHITE -> when (type) {
                PieceType.KING -> "♔"
                PieceType.QUEEN -> "♕"
                PieceType.ROOK -> "♖"
                PieceType.BISHOP -> "♗"
                PieceType.KNIGHT -> "♘"
                PieceType.PAWN -> "♙"
            }
            PieceColor.BLACK -> when (type) {
                PieceType.KING -> "♚"
                PieceType.QUEEN -> "♛"
                PieceType.ROOK -> "♜"
                PieceType.BISHOP -> "♝"
                PieceType.KNIGHT -> "♞"
                PieceType.PAWN -> "♟"
            }
        }
    }
}

data class BoardPosition(val r: Int, val c: Int) {
    fun isValid(): Boolean = r in 0..7 && c in 0..7
}

data class ChessMove(
    val from: BoardPosition,
    val to: BoardPosition,
    val pieceMoved: Piece,
    val pieceCaptured: Piece? = null,
    val isCastling: Boolean = false,
    val isPromotion: Boolean = false,
    val promotionType: PieceType? = null
)

// Main chess engine state descriptor
data class ChessGameState(
    val board: Array<Array<Piece?>>,
    val activeColor: PieceColor = PieceColor.WHITE,
    val gameWinner: PieceColor? = null,
    val isCheckmate: Boolean = false,
    val isStalemate: Boolean = false,
    val isCheck: Boolean = false,
    val moveHistory: List<ChessMove> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChessGameState) return false
        if (activeColor != other.activeColor) return false
        if (gameWinner != other.gameWinner) return false
        if (isCheckmate != other.isCheckmate) return false
        if (isStalemate != other.isStalemate) return false
        if (isCheck != other.isCheck) return false
        if (moveHistory != other.moveHistory) return false
        for (i in 0..7) {
            if (!board[i].contentEquals(other.board[i])) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = activeColor.hashCode()
        result = 31 * result + (gameWinner?.hashCode() ?: 0)
        result = 31 * result + isCheckmate.hashCode()
        result = 31 * result + isStalemate.hashCode()
        result = 31 * result + isCheck.hashCode()
        result = 31 * result + moveHistory.hashCode()
        result = 31 * result + board.contentDeepHashCode()
        return result
    }
}

object ChessEngine {

    const val BOARD_SIZE = 8

    fun createInitialBoard(): Array<Array<Piece?>> {
        val board = Array(BOARD_SIZE) { Array<Piece?>(BOARD_SIZE) { null } }

        // Setup Rooks
        board[0][0] = Piece(PieceType.ROOK, PieceColor.BLACK)
        board[0][7] = Piece(PieceType.ROOK, PieceColor.BLACK)
        board[7][0] = Piece(PieceType.ROOK, PieceColor.WHITE)
        board[7][7] = Piece(PieceType.ROOK, PieceColor.WHITE)

        // Setup Knights
        board[0][1] = Piece(PieceType.KNIGHT, PieceColor.BLACK)
        board[0][6] = Piece(PieceType.KNIGHT, PieceColor.BLACK)
        board[7][1] = Piece(PieceType.KNIGHT, PieceColor.WHITE)
        board[7][6] = Piece(PieceType.KNIGHT, PieceColor.WHITE)

        // Setup Bishops
        board[0][2] = Piece(PieceType.BISHOP, PieceColor.BLACK)
        board[0][5] = Piece(PieceType.BISHOP, PieceColor.BLACK)
        board[7][2] = Piece(PieceType.BISHOP, PieceColor.WHITE)
        board[7][5] = Piece(PieceType.BISHOP, PieceColor.WHITE)

        // Setup Queens
        board[0][3] = Piece(PieceType.QUEEN, PieceColor.BLACK)
        board[7][3] = Piece(PieceType.QUEEN, PieceColor.WHITE)

        // Setup Kings
        board[0][4] = Piece(PieceType.KING, PieceColor.BLACK)
        board[7][4] = Piece(PieceType.KING, PieceColor.WHITE)

        // Setup Pawns
        for (col in 0 until BOARD_SIZE) {
            board[1][col] = Piece(PieceType.PAWN, PieceColor.BLACK)
            board[6][col] = Piece(PieceType.PAWN, PieceColor.WHITE)
        }

        return board
    }

    fun makeInitialState(): ChessGameState {
        return ChessGameState(board = createInitialBoard())
    }

    // Evaluates a potential raw legal moves for a piece (ignoring check protection)
    fun getRawMoves(board: Array<Array<Piece?>>, from: BoardPosition): List<ChessMove> {
        val piece = board[from.r][from.c] ?: return emptyList()
        val moves = mutableListOf<ChessMove>()

        when (piece.type) {
            PieceType.PAWN -> {
                val direction = if (piece.color == PieceColor.WHITE) -1 else 1
                val r1 = from.r + direction
                // 1. Move forward
                if (r1 in 0..7) {
                    if (board[r1][from.c] == null) {
                        val isPromotion = (piece.color == PieceColor.WHITE && r1 == 0) || (piece.color == PieceColor.BLACK && r1 == 7)
                        moves.add(ChessMove(from, BoardPosition(r1, from.c), piece, null, isPromotion = isPromotion, promotionType = if (isPromotion) PieceType.QUEEN else null))
                        
                        // 2. Double advance
                        val r2 = from.r + (2 * direction)
                        val startRow = if (piece.color == PieceColor.WHITE) 6 else 1
                        if (from.r == startRow && board[r2][from.c] == null) {
                            moves.add(ChessMove(from, BoardPosition(r2, from.c), piece, null))
                        }
                    }
                }
                // 3. Diagonal captures
                val captCols = listOf(from.c - 1, from.c + 1)
                for (cDest in captCols) {
                    if (cDest in 0..7 && r1 in 0..7) {
                        val victim = board[r1][cDest]
                        if (victim != null && victim.color != piece.color) {
                            val isPromotion = (piece.color == PieceColor.WHITE && r1 == 0) || (piece.color == PieceColor.BLACK && r1 == 7)
                            moves.add(ChessMove(from, BoardPosition(r1, cDest), piece, victim, isPromotion = isPromotion, promotionType = if (isPromotion) PieceType.QUEEN else null))
                        }
                    }
                }
            }

            PieceType.KNIGHT -> {
                val offsets = listOf(
                    Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
                    Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
                )
                for (offset in offsets) {
                    val dest = BoardPosition(from.r + offset.first, from.c + offset.second)
                    if (dest.isValid()) {
                        val tgt = board[dest.r][dest.c]
                        if (tgt == null || tgt.color != piece.color) {
                            moves.add(ChessMove(from, dest, piece, tgt))
                        }
                    }
                }
            }

            PieceType.BISHOP -> {
                moves.addAll(getSlidingMoves(board, from, piece, listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))))
            }

            PieceType.ROOK -> {
                moves.addAll(getSlidingMoves(board, from, piece, listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))))
            }

            PieceType.QUEEN -> {
                moves.addAll(getSlidingMoves(board, from, piece, listOf(
                    Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1),
                    Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
                )))
            }

            PieceType.KING -> {
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val dest = BoardPosition(from.r + dr, from.c + dc)
                        if (dest.isValid()) {
                            val tgt = board[dest.r][dest.c]
                            if (tgt == null || tgt.color != piece.color) {
                                moves.add(ChessMove(from, dest, piece, tgt))
                            }
                        }
                    }
                }

                // Simple Castling support (simplified checks for cleaner code but robust gameplay)
                if (!piece.hasMoved) {
                    val r = from.r
                    // King's side castle
                    val rRook = board[r][7]
                    if (rRook != null && !rRook.hasMoved && rRook.type == PieceType.ROOK && rRook.color == piece.color) {
                        if (board[r][5] == null && board[r][6] == null) {
                            moves.add(ChessMove(from, BoardPosition(r, 6), piece, null, isCastling = true))
                        }
                    }
                    // Queen's side castle
                    val lRook = board[r][0]
                    if (lRook != null && !lRook.hasMoved && lRook.type == PieceType.ROOK && lRook.color == piece.color) {
                        if (board[r][1] == null && board[r][2] == null && board[r][3] == null) {
                            moves.add(ChessMove(from, BoardPosition(r, 2), piece, null, isCastling = true))
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun getSlidingMoves(
        board: Array<Array<Piece?>>,
        from: BoardPosition,
        piece: Piece,
        directions: List<Pair<Int, Int>>
    ): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        for (dir in directions) {
            var step = 1
            while (true) {
                val rDest = from.r + (dir.first * step)
                val cDest = from.c + (dir.second * step)
                val pos = BoardPosition(rDest, cDest)
                if (!pos.isValid()) break

                val tgt = board[rDest][cDest]
                if (tgt == null) {
                    moves.add(ChessMove(from, pos, piece, null))
                } else {
                    if (tgt.color != piece.color) {
                        moves.add(ChessMove(from, pos, piece, tgt))
                    }
                    break
                }
                step++
            }
        }
        return moves
    }

    // Checks if a color's king is currently in check
    fun isColorInCheck(board: Array<Array<Piece?>>, color: PieceColor): Boolean {
        // Find king
        var kingPos: BoardPosition? = null
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != null && piece.type == PieceType.KING && piece.color == color) {
                    kingPos = BoardPosition(r, c)
                    break
                }
            }
            if (kingPos != null) break
        }
        if (kingPos == null) return false

        // Check if any opponent piece can move to kingPos
        val oppColor = color.opponent()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != null && piece.color == oppColor) {
                    val raw = getRawMoves(board, BoardPosition(r, c))
                    if (raw.any { it.to == kingPos }) {
                        return true
                    }
                }
            }
        }
        return false
    }

    // Returns accurate legal moves for a given piece position, making sure standard king safety rules are checked.
    fun getLegalMoves(board: Array<Array<Piece?>>, from: BoardPosition): List<ChessMove> {
        val piece = board[from.r][from.c] ?: return emptyList()
        val raw = getRawMoves(board, from)
        val legal = mutableListOf<ChessMove>()

        for (move in raw) {
            // Simulate move
            val tempBoard = cloneBoard(board)
            tempBoard[move.to.r][move.to.c] = piece.copy(hasMoved = true)
            tempBoard[move.from.r][move.from.c] = null

            if (move.isCastling) {
                // Ensure King doesn't jump over check
                if (move.to.c == 6) { // King Side
                    val testCastleBoard = cloneBoard(board)
                    testCastleBoard[move.from.r][5] = piece.copy(hasMoved = true)
                    testCastleBoard[move.from.r][move.from.c] = null
                    if (isColorInCheck(board, piece.color) || isColorInCheck(testCastleBoard, piece.color)) {
                        continue
                    }
                } else if (move.to.c == 2) { // Queen Side
                    val testCastleBoard = cloneBoard(board)
                    testCastleBoard[move.from.r][3] = piece.copy(hasMoved = true)
                    testCastleBoard[move.from.r][move.from.c] = null
                    if (isColorInCheck(board, piece.color) || isColorInCheck(testCastleBoard, piece.color)) {
                        continue
                    }
                }
            }

            if (!isColorInCheck(tempBoard, piece.color)) {
                legal.add(move)
            }
        }
        return legal
    }

    // Get all legal moves for a given side
    fun getAllLegalMoves(board: Array<Array<Piece?>>, color: PieceColor): List<ChessMove> {
        val legal = mutableListOf<ChessMove>()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != null && piece.color == color) {
                    legal.addAll(getLegalMoves(board, BoardPosition(r, c)))
                }
            }
        }
        return legal
    }

    // Clones the 2D array representation
    fun cloneBoard(board: Array<Array<Piece?>>): Array<Array<Piece?>> {
        return Array(BOARD_SIZE) { r ->
            Array(BOARD_SIZE) { c ->
                board[r][c]?.copy()
            }
        }
    }

    // Executes a move on the given Board, returning a new FEN or Board state.
    fun executeMove(state: ChessGameState, move: ChessMove): ChessGameState {
        val boardCopy = cloneBoard(state.board)
        val movingPiece = state.board[move.from.r][move.from.c] ?: return state

        // Standard movement
        boardCopy[move.from.r][move.from.c] = null
        
        val updatedPiece = movingPiece.copy(hasMoved = true)
        if (move.isPromotion) {
            boardCopy[move.to.r][move.to.c] = Piece(move.promotionType ?: PieceType.QUEEN, movingPiece.color, true)
        } else {
            boardCopy[move.to.r][move.to.c] = updatedPiece
        }

        // Castling logic (Rearrange rook)
        if (move.isCastling) {
            val r = move.from.r
            if (move.to.c == 6) { // King side
                val rook = boardCopy[r][7]
                if (rook != null) {
                    boardCopy[r][5] = rook.copy(hasMoved = true)
                    boardCopy[r][7] = null
                }
            } else if (move.to.c == 2) { // Queen side
                val rook = boardCopy[r][0]
                if (rook != null) {
                    boardCopy[r][3] = rook.copy(hasMoved = true)
                    boardCopy[r][0] = null
                }
            }
        }

        val nextColor = state.activeColor.opponent()
        val inCheck = isColorInCheck(boardCopy, nextColor)
        val allNextMoves = getAllLegalMoves(boardCopy, nextColor)

        var gameWinner: PieceColor? = null
        var isCheckmate = false
        var isStalemate = false

        if (allNextMoves.isEmpty()) {
            if (inCheck) {
                isCheckmate = true
                gameWinner = state.activeColor // Active color has checkmated the opponent
            } else {
                isStalemate = true
            }
        }

        val updatedHistory = state.moveHistory + move

        return ChessGameState(
            board = boardCopy,
            activeColor = nextColor,
            gameWinner = gameWinner,
            isCheckmate = isCheckmate,
            isStalemate = isStalemate,
            isCheck = inCheck,
            moveHistory = updatedHistory
        )
    }

    // Simplified board conversion to/from ASCII notation string for simple Room persistence storage!
    // Format: "Pawn,WHITE,true;Rook,BLACK,false;...."
    fun serializeBoard(board: Array<Array<Piece?>>): String {
        val builder = java.lang.StringBuilder()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != null) {
                    builder.append("$r,$c,${piece.type.name},${piece.color.name},${piece.hasMoved};")
                }
            }
        }
        return builder.toString()
    }

    fun deserializeBoard(data: String): Array<Array<Piece?>> {
        val board = Array(BOARD_SIZE) { Array<Piece?>(BOARD_SIZE) { null } }
        if (data.isBlank()) return createInitialBoard()
        val tokens = data.split(";").filter { it.isNotBlank() }
        for (tok in tokens) {
            val parts = tok.split(",")
            if (parts.size >= 5) {
                val r = parts[0].toIntOrNull() ?: continue
                val c = parts[1].toIntOrNull() ?: continue
                val type = try { PieceType.valueOf(parts[2]) } catch (e: Exception) { PieceType.PAWN }
                val color = try { PieceColor.valueOf(parts[3]) } catch (e: Exception) { PieceColor.WHITE }
                val hasMoved = parts[4].toBoolean()
                if (r in 0..7 && c in 0..7) {
                    board[r][c] = Piece(type, color, hasMoved)
                }
            }
        }
        return board
    }

    // --- AI BOT SECTION ---
    // Evaluates board from the AI's perspective (maximizing color)
    private fun evaluateBoard(board: Array<Array<Piece?>>, color: PieceColor): Int {
        var score = 0
        val opp = color.opponent()

        // Square-position table multipliers for center-control incentive (simplifies to center squares)
        val posTable = arrayOf(
            intArrayOf(0,  0,  0,  0,  0,  0,  0,  0),
            intArrayOf(5, 10, 10,-20,-20, 10, 10,  5),
            intArrayOf(5, -5,-10,  0,  0,-10, -5,  5),
            intArrayOf(0,  0,  0, 20, 20,  0,  0,  0),
            intArrayOf(5,  5, 10, 25, 25, 10,  5,  5),
            intArrayOf(10,10, 20, 30, 30, 20, 10, 10),
            intArrayOf(50,50, 50, 50, 50, 50, 50, 50),
            intArrayOf(0,  0,  0,  0,  0,  0,  0,  0)
        )

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c] ?: continue
                val material = piece.type.pieceVal
                
                // Pawns evaluations or central positions
                val isPawn = piece.type == PieceType.PAWN
                val positionalBonus = if (isPawn) {
                    val actualRow = if (piece.color == PieceColor.WHITE) (7 - r) else r
                    posTable[actualRow][c]
                } else {
                    if (r in 2..5 && c in 2..5) 15 else 0 // simple reward for non-pawn pieces controlling middle
                }

                if (piece.color == color) {
                    score += material + positionalBonus
                } else {
                    score -= (material + positionalBonus)
                }
            }
        }
        return score
    }

    // Minimax search with basic Alpha-Beta pruning (Depth 2 - highly responsive and performs strong smart tactical captures)
    fun getBestMove(state: ChessGameState, color: PieceColor, depth: Int = 2): ChessMove? {
        val board = state.board
        val legalMoves = getAllLegalMoves(board, color)
        if (legalMoves.isEmpty()) return null

        // Shuffle moves to add variability and fun surprise moves for the user!
        val randomizedMoves = legalMoves.shuffled(Random(System.currentTimeMillis()))

        var bestMove: ChessMove? = null
        var bestVal = Int.MIN_VALUE

        for (move in randomizedMoves) {
            // Simulate 
            val nextBoard = cloneBoard(board)
            val movingPiece = nextBoard[move.from.r][move.from.c] ?: continue
            nextBoard[move.from.r][move.from.c] = null
            nextBoard[move.to.r][move.to.c] = movingPiece.copy(hasMoved = true)

            // Evaluate opponent replies
            val valMove = minimaxMin(nextBoard, color, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE)
            if (valMove > bestVal) {
                bestVal = valMove
                bestMove = move
            }
        }
        return bestMove
    }

    private fun minimaxMax(board: Array<Array<Piece?>>, maxColor: PieceColor, depth: Int, alpha: Int, beta: Int): Int {
        if (depth == 0) return evaluateBoard(board, maxColor)
        val legals = getAllLegalMoves(board, maxColor)
        if (legals.isEmpty()) {
            return if (isColorInCheck(board, maxColor)) -100000 else 0 // Checkmate vs draw
        }

        var currAlpha = alpha
        var maxEval = Int.MIN_VALUE
        for (move in legals) {
            val nextBoard = cloneBoard(board)
            val movingPiece = nextBoard[move.from.r][move.from.c] ?: continue
            nextBoard[move.from.r][move.from.c] = null
            nextBoard[move.to.r][move.to.c] = movingPiece.copy(hasMoved = true)

            val evaluation = minimaxMin(nextBoard, maxColor, depth - 1, currAlpha, beta)
            maxEval = maxOf(maxEval, evaluation)
            currAlpha = maxOf(currAlpha, evaluation)
            if (beta <= currAlpha) break
        }
        return maxEval
    }

    private fun minimaxMin(board: Array<Array<Piece?>>, maxColor: PieceColor, depth: Int, alpha: Int, beta: Int): Int {
        val minColor = maxColor.opponent()
        if (depth == 0) return evaluateBoard(board, maxColor)
        val legals = getAllLegalMoves(board, minColor)
        if (legals.isEmpty()) {
            return if (isColorInCheck(board, minColor)) 100000 else 0 // We like checkmating them
        }

        var currBeta = beta
        var minEval = Int.MAX_VALUE
        for (move in legals) {
            val nextBoard = cloneBoard(board)
            val movingPiece = nextBoard[move.from.r][move.from.c] ?: continue
            nextBoard[move.from.r][move.from.c] = null
            nextBoard[move.to.r][move.to.c] = movingPiece.copy(hasMoved = true)

            val evaluation = minimaxMax(nextBoard, maxColor, depth - 1, alpha, currBeta)
            minEval = minOf(minEval, evaluation)
            currBeta = minOf(currBeta, evaluation)
            if (currBeta <= alpha) break
        }
        return minEval
    }
}
