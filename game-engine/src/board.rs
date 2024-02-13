#[derive(PartialEq, Eq, Debug)]
pub enum Symbol {
    X,
    O
}

pub enum Field {
    Empty,
    Occ(Symbol)
}

pub struct Board {
    pub field: [Field; 9],
}

pub struct BitBoard {
    pub x: i32,
    pub o: i32
}

impl Board{
    pub fn to_bitboard(&self) -> BitBoard {
        let mut bitboard_x = 0;
        let mut bitboard_o = 0;
        for a in &self.field {
            bitboard_x = bitboard_x << 1;
            bitboard_o = bitboard_o << 1;
            if let Field::Occ(a) = a {
                if a == &Symbol::X {
                    bitboard_x += 1;
                } else {
                    bitboard_o += 1;
                }
            } 
        }
        return BitBoard {x: bitboard_x, o: bitboard_o};
    }
}

pub struct Move {
    pub row: i32,
    pub column: i32
}

impl Move {
    pub fn to_bitboard(&self) -> Result<i32, &str> {
        let mut bitboard = 0;
        if self.row <= 0 || self.column <= 0 || self.row > 9 || self.column > 9 {
            return Err("Outside of board");
        }
        let row_offset = match self.row {
            1 => 0,
            2 => 3,
            3 => 6,
            _ => panic!("Invalid row")
        };
        let position = row_offset + (self.column-1);
        bitboard = bitboard << position;
        bitboard += 1;
        bitboard = bitboard << 9-position-1;
        return Ok(bitboard);
    }
}

pub fn is_move_legal(board: &BitBoard, mv: &i32) -> bool {
    return mv & board.x == 0 && mv & board.o == 0
}

pub fn check_win(board: &BitBoard) -> Option<Symbol> {
    if check_win_single_mask(&board.x) {
        return Some(Symbol::X);
    }
    if check_win_single_mask(&board.o) {
        return Some(Symbol::O);
    }
    return None
}

fn check_win_single_mask(board: &i32) -> bool {
    let row1 = 0b000000111;
    let row2 = row1 << 3;
    let row3 = row2 << 3;
    let diag1 = 0b100010001;
    let diag2 = 0b001010100;
    let column1 = 0b001001001;
    let column2 = column1 << 1;
    let column3 = column2 << 1;
    return board & row3 == row3 || board & row2 == row2 && board & row1 == row1 
        || board & diag1 == diag1 || board & diag2 == diag2
        || board & column1 == column1 || board & column2 == column2 || board & column3 == column3  
}

pub fn check_draw(board: &BitBoard) -> bool {
    return (board.x | board.o) == 0b111111111
}

impl BitBoard {
    pub fn apply_move(&self, mv: &i32, player: &Symbol) -> BitBoard {
        return match player {
            Symbol::X => BitBoard {x: self.x | mv, o: self.o.clone()},
            Symbol::O => BitBoard {x: self.x.clone(), o: self.o | mv},
        } 
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_move_to_bitboard_1_1() {
        let mv = Move { row: 1, column: 1 };
        assert_eq!(mv.to_bitboard(), Ok(0b100000000));
    }

    #[test]
    fn test_move_to_bitboard_2_1() {
        let mv = Move { row: 2, column: 1 };
        assert_eq!(mv.to_bitboard(), Ok(0b000100000));
    }

    #[test]
    fn test_move_to_bitboard_3_1() {
        let mv = Move { row: 3, column: 1 };
        assert_eq!(mv.to_bitboard(), Ok(0b000000100));
    }

    #[test]
    fn test_move_to_bitboard_1_2() {
        let mv = Move { row: 1, column: 2 };
        assert_eq!(mv.to_bitboard(), Ok(0b010000000));
    }

    #[test]
    fn test_move_to_bitboard_3_3() {
        let mv = Move { row: 3, column: 3 };
        assert_eq!(mv.to_bitboard(), Ok(0b000000001));
    }

    #[test]
    fn test_to_bitboard_outside_of_board() {
        let mv = Move { row: 0, column: 1 };
        assert_eq!(mv.to_bitboard(), Err("Outside of board"));
    }

    #[test]
    fn test_legal_move_legality() {
        let mv = 0b010000000;
        let x =  0b000010000;
        let o =  0b100000000;
        let board = BitBoard{x,o};
        assert_eq!(is_move_legal(&board, &mv), true);
    }

    #[test]
    fn test_illegal_move_legality_x_collision() {
        let mv = 0b000010000;
        let x =  0b000010000;
        let o =  0b100000000;
        let board = BitBoard{x,o};
        assert_eq!(is_move_legal(&board, &mv), false);
    }

    #[test]
    fn test_illegal_move_legality_o_collision() {
        let mv = 0b100000000;
        let x =  0b000010000;
        let o =  0b100000000;
        let board = BitBoard{x,o};
        assert_eq!(is_move_legal(&board, &mv), false);
    }

    #[test]
    fn test_check_win_horizontal() {
        let board_x_horizontal = BitBoard { x: 0b111000000, o: 0b000000000 };
        assert_eq!(check_win(&board_x_horizontal), Some(Symbol::X));

        let board_o_horizontal = BitBoard { x: 0b000000000, o: 0b111000000 };
        assert_eq!(check_win(&board_o_horizontal), Some(Symbol::O));
    }

    #[test]
    fn test_check_win_vertical() {
        let board_x_vertical = BitBoard { x: 0b100100100, o: 0b010010000 };
        assert_eq!(check_win(&board_x_vertical), Some(Symbol::X));

        let board_o_vertical = BitBoard { x: 0b000010000, o: 0b001001001 };
        assert_eq!(check_win(&board_o_vertical), Some(Symbol::O));
    }

    #[test]
    fn test_check_win_diagonal() {
        let board_x_diagonal1 = BitBoard { x: 0b100010001, o: 0b000001010 };
        assert_eq!(check_win(&board_x_diagonal1), Some(Symbol::X));

        let board_o_diagonal2 = BitBoard { x: 0b000100010, o: 0b001010100 };
        assert_eq!(check_win(&board_o_diagonal2), Some(Symbol::O));
    }

    #[test]
    fn test_check_win_no_win() {
        let board_no_win = BitBoard { x: 0b000010000, o: 0b001001000 };
        assert_eq!(check_win(&board_no_win), None);
    }

    #[test]
    fn test_check_draw_empty_board() {
        let board_empty = BitBoard { x: 0b000000000, o: 0b000000000 };
        assert!(!check_draw(&board_empty));
    }

    #[test]
    fn test_check_draw_partial_board() {
        let board_partial = BitBoard { x: 0b101001000, o: 0b000100010 };
        assert!(!check_draw(&board_partial));
    }

    #[test]
    fn test_check_draw_full_board() {
        let board_full = BitBoard { x: 0b111111111, o: 0b000000000 };
        assert!(check_draw(&board_full));
    }

    #[test]
    fn test_apply_move_player_x() {
        let initial_board = BitBoard { x: 0b000000000, o: 0b000000000 };
        let mv = 0b000000100;
        let updated_board = initial_board.apply_move(&mv, &Symbol::X);
        assert_eq!(updated_board.x, 0b000000100);
        assert_eq!(updated_board.o, 0b000_000_000);
    }

    #[test]
    fn test_apply_move_player_o() {
        let initial_board = BitBoard { x: 0b000000000, o: 0b000000000 };
        let mv = 0b000001000;
        let updated_board = initial_board.apply_move(&mv, &Symbol::O);
        assert_eq!(updated_board.x, 0b000000000);
        assert_eq!(updated_board.o, 0b000001000);
    }

    #[test]
    fn test_apply_move_multiple_moves() {
        let mut board = BitBoard { x: 0b000000000, o: 0b000000000 };
        let mv = 0b000000100;
        let mv2 = 0b000100000;
        board = board.apply_move(&mv, &Symbol::X).apply_move(&mv2, &Symbol::O);
        assert_eq!(board.x, 0b000000100);
        assert_eq!(board.o, 0b000100000);
    }
}
