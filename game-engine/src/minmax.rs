use crate::board::{BitBoard, Symbol, check_win, check_draw};

fn generate_moves(board: &BitBoard) -> i32 {
    return !(board.x | board.o) & 0b111111111;
}

pub fn min_max_decision(board: &BitBoard, player: &Symbol) -> i32 {
    let moves = generate_moves(board);
    let mut mask = 0b000000001;
    let next_player = match player {
        Symbol::X => Symbol::O,
        Symbol::O => Symbol::X,
    };
    let mut best_move = 0;
    let mut best_result = -1;
    for _ in 0..9 {
        if moves & mask != 0 {
            let new_board = board.apply_move(&mask, player);
            let min = min_value(&new_board, &next_player);
            if min > best_result {
                best_result = min;
                best_move = mask;
            }
        }
        mask = mask << 1;
    }
    return best_move;
}

fn max_value(board: &BitBoard, player: &Symbol) -> i8 {
    if check_win(board).is_some() {
        return -1;
    }
    if check_draw(board) {
        return 0;
    }
    let moves = generate_moves(board);
    let next_player = match player {
        Symbol::X => Symbol::O,
        Symbol::O => Symbol::X,
    };
    let mut best_result = -1;
    let mut mask = 0b000000001;
    for _ in 0..9 {
        if moves & mask != 0 {
            let new_board = board.apply_move(&mask, player);
            let min = min_value(&new_board, &next_player);
            if min > best_result {
                best_result = min;
            }
        }
        mask = mask << 1;
    }
    return best_result;
}

fn min_value(board: &BitBoard, player: &Symbol) -> i8 {
    if check_win(board).is_some() {
        return 1;
    }
    if check_draw(board) {
        return 0;
    }
    let moves = generate_moves(board);
    let next_player = match player {
        Symbol::X => Symbol::O,
        Symbol::O => Symbol::X,
    };
    let mut best_result = 1;
    let mut mask = 0b000000001;
    for _ in 0..9 {
        if moves & mask != 0 {
            let new_board = board.apply_move(&mask, player);
            let max = max_value(&new_board, &next_player);
            if max < best_result {
                best_result = max;
            }
        }
        mask = mask << 1;
    }
    return best_result;
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

    #[test]
    fn test_min_max_decision_win_in_1_1() {
        let board = BitBoard { x: 0b000101000, o: 0b110000001 };
        // ╭───┬───┬───╮
        // │ o │ o │   │
        // ├───┼───┼───┤
        // │ x │   │ x │
        // ├───┼───┼───┤
        // │   │   │ o │
        // ╰───┴───┴───╯
        let player = Symbol::X;
        let best_move = min_max_decision(&board, &player);
        assert_eq!(best_move, 0b000010000);
    }

    #[test]
    fn test_min_max_decision_win_in_1_2() {
        let board = BitBoard { x: 0b010100110, o: 0b001001000 };
        // ╭───┬───┬───╮
        // │   │ x │ o │
        // ├───┼───┼───┤
        // │ x │   │ o │
        // ├───┼───┼───┤
        // │ x │ x │   │
        // ╰───┴───┴───╯
        let player = Symbol::O;
        let best_move = min_max_decision(&board, &player);
        assert_eq!(best_move, 0b000000001);
    }
}
