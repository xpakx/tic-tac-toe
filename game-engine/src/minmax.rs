use crate::board::BitBoard;

fn generate_moves(board: &BitBoard) -> i32 {
    return !(board.x | board.o) & 0b111111111;
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_generate_moves_empty_board() {
        let board_empty = BitBoard { x: 0b000000000, o: 0b000000000 };
        let moves_bitmask = generate_moves(&board_empty);
        assert_eq!(moves_bitmask, 0b111111111);
    }

    #[test]
    fn test_generate_moves_partial_board() {
        let board_partial = BitBoard { x: 0b101001000, o: 0b010100000 };
        let moves_bitmask = generate_moves(&board_partial);
        assert_eq!(moves_bitmask, 0b000010111);
    }

    #[test]
    fn test_generate_moves_full_board() {
        let board_full = BitBoard { x: 0b111111111, o: 0b000000000 };
        let moves_bitmask = generate_moves(&board_full);
        assert_eq!(moves_bitmask, 0b000000000);
    }
}
