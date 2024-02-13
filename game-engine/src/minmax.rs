use crate::board::BitBoard;

fn generate_moves(board: &BitBoard) -> i32 {
    return !(board.x | board.o) & 0b111111111;
}
