package main

import (
	"fmt"
	"strings"
)

func GetLoginForm(cursor int, username input, password input, insertMode bool) string {
	s := ""
	s += Reset + "Please log in.\n\n"
	s += Blue + "Login:    " + Reset
	if cursor == 0 {
		s += Red
	}
	s += username.value + strings.Repeat("_", username.maxLen - len(username.value)) + "\n"
	if cursor == 0 {
		s += Reset
	}


	s += Blue + "Password: " + Reset
	if cursor == 1 {
		s += Red
	}
	s += strings.Repeat("*", len(password.value)) + strings.Repeat("_", password.maxLen - len(password.value)) + "\n"
	if cursor == 1 {
		s += Reset
	}

	s +=  "\n" + strings.Repeat(" ", 30 - len("[Log in]"))
	if cursor == 2 {
		s += Red
	} else {
		s += Blue
	}
	s += "[Log in]\n"
	s += Reset

	s +=  "\n\n" + Reset + "Don't have an account? " 
	if cursor == 3 {
		s += Red
	} else {
		s += Blue
	}
	s += "Register."
	s += Reset
	return s
}

func GetRegisterForm(cursor int, username input, password input, passwordRe input, insertMode bool) string {
	s := ""
	s += Reset + "Please register.\n\n"
	s += Blue + "Login:    " + Reset
	if cursor == 0 {
		s += Red
	}
	s += username.value + strings.Repeat("_", username.maxLen - len(username.value)) + "\n"
	if cursor == 0 {
		s += Reset
	}


	s += Blue + "Password: " + Reset
	if cursor == 1 {
		s += Red
	}
	s += strings.Repeat("*", len(password.value)) + strings.Repeat("_", password.maxLen - len(password.value)) + "\n"
	if cursor == 1 {
		s += Reset
	}


	s += Blue + "Repeat:   " + Reset
	if cursor == 2 {
		s += Red
	}
	s += strings.Repeat("*", len(passwordRe.value)) + strings.Repeat("_", passwordRe.maxLen - len(passwordRe.value)) + "\n"
	if cursor == 2 {
		s += Reset
	}

	s +=  "\n" + strings.Repeat(" ", 30 - len("[Register]"))
	if cursor == 3 {
		s += Red
	} else {
		s += Blue
	}
	s += "[Register]\n"
	s += Reset

	s +=  "\n\n" + Reset + "Already have an account? " 
	if cursor == 4 {
		s += Red
	} else {
		s += Blue
	}
	s += "Log in."
	s += Reset
	return s
}


func GetMenu(cursor int) string {
	options := []string{"New Game", "vs. AI", "Requests", "Active games", "Archive"}
	s := ""
	for i, option := range options {
		if i == 2 {
			s += "\n"
		}
		if cursor == i {
			s += Red
		}
		s += option
		if cursor == i {
			s += Reset
		}
		s += "\n"
	}
	return s
}

func GetRequestForm(cursor int, username input, insertMode bool) string {
	s := ""
	s += Reset + "Enter username of the opponent.\n\n"
	s += Blue + "Username:  " + Reset
	if cursor == 0 {
		s += Red
	}
	s += username.value + strings.Repeat("_", username.maxLen - len(username.value)) + "\n"
	if cursor == 0 {
		s += Reset
	}


	s +=  "\n" + strings.Repeat(" ", 30 - len("[Send]"))
	if cursor == 1 {
		s += Red
	} else {
		s += Blue
	}
	s += "[Send]\n"
	s += Reset

	return s
}

func GetGameList(cursorX int, cursorY int, games []gameSummary, listType string) string {
	s := ""

	if len(games) == 0 || cursorX >= len(games) {
		return "[No games]" + "\n\n" +  Red + "[Menu]" + Reset + "\n"
	}

	game := games[cursorX]

	if cursorX > 0 {
		s += "↟"
	} else {
		s += " "
	}

	s += "   Game " +  fmt.Sprintf("%d", game.Id) + "   " 

	s += "\n"
	if cursorX < len(games)-1 {
		s += "↡"
	} else {
		s += " "
	}
	s += "   " + Blue + game.Username1 + Reset + " vs. " + Red + game.Username2 + Reset

	s += "\n\n"
	board := game.State
	for i := range board {
		for j := range board[i] {
			if board[i][j] == "X" {
				board[i][j] = "✘"
			} else if board[i][j] == "O" {
				board[i][j] = "○"
			} else {
				board[i][j] = " "
			}
		}
	}
	s += BoardToString(game.State, -1, -1, "")

	buttons := "[Menu]"
	if listType == "requests" {
		buttons += " [Accept]"
		buttons += " [Reject]"
	} else if listType == "games" {
		buttons += " [Go]"
	}

	s +=  "\n" + strings.Repeat(" ", 30 - len(buttons))
	if cursorY == 0 {
		s += Red
	} else {
		s += Blue
	}
	s += "[Menu]" + Reset
	if listType == "requests" {
		if cursorY == 1 {
			s += Red
		} else {
			s += Blue
		}
		s += " [Accept]" + Reset
		if cursorY == 2 {
			s += Red
		} else {
			s += Blue
		}
		s += " [Reject]" + Reset
	} else if listType == "games" {
		if cursorY == 1 {
			s += Red
		} else {
			s += Blue
		}
		s += " [Go]" + Reset
	}
	s += "\n"

	return s
}

func BoardToString(board [][]string, cursorX int, cursorY int, current string) string {
	s := "";
	for i, row := range board {
		for j := range row {
			if i == 0 && j == 0 {
				s += "╭─"
			} else if i == 0 {
				s += "┬─"
			} else if j == 0 {
				s += "├─"
			} else {
				s += "┼─"
			}
			s += "──"
			if j == len(row)-1 {
				if i == 0 {
					s += "╮"
				} else {
					s += "┤"
				}

			}
		}
		s += "\n"
		for j, field := range row {
			selected := cursorX == i && cursorY == j

			cursor := "│ " + field + " "
			color := Blue
			if field != " " {
				color = Red
			}
			if selected {
				cursor = "│ " + color + current + " " + Reset
			}

			if j == len(row)-1 {
				cursor += "│"
			}
			s += cursor
		}
		s += "\n"
	}
	for j := range board[0] {
		if j == 0 {
			s += "╰───"
		} else {
			s += "┴───"
		}
	}
	s += "╯"
	return s;
}
