package main

import (
	"fmt"
	"os"
	"strings"

	tea "github.com/charmbracelet/bubbletea"
)

type model struct {
	board [][]string
	cursorX int
	cursorY int
	current string
	// TODO: current view: list/login/register
	view string
	token string

	login string
	password string
	insertMode bool
}

func initialModel() model {
	return model{
		board:  [][]string{
			{" ", " ", " "},
			{" ", " ", " "},
			{" ", " ", " "},
		},
		current: "✘",
		view: "login",
		token: "",
		login: "",
		password: "",
		insertMode: false,
	}
}

type pair struct {
	x int
	y int
}

func (m model) Init() tea.Cmd {
	return nil
}

func (m model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		if m.insertMode {
			key := msg.String()
			if len(key) == 1 {
				if m.view == "login" && m.cursorX == 0 && len(m.login) < 20 {
					m.login += key;
				} else if m.view == "login" && m.cursorX == 1 && len(m.password) < 20 {
					m.password += key;
				}

			} else {
				switch msg.String() {
				case "ctrl+c":
					return m, tea.Quit
				case "esc", "enter":
					m.insertMode = false
				}
			}
		} else {
			switch msg.String() {
			case "ctrl+c", "q":
				return m, tea.Quit
			case "up", "k":
				if m.cursorX > 0 {
					m.cursorX--
				}
			case "down", "j":
				if m.view == "game" && m.cursorX < len(m.board)-1 {
					m.cursorX++
				} else if m.view == "login" && m.cursorX < 3 {
					m.cursorX++
				}
			case "left", "h":
				if m.view == "game" && m.cursorY > 0 {
					m.cursorY--
				}
			case "right", "l":
				if m.view == "game" && m.cursorY < len(m.board[0])-1 {
					m.cursorY++
				}
			case "enter", " ":
				if m.view == "game" {
					m.board[m.cursorX][m.cursorY] = m.current;
					switch m.current {
						case "✘": m.current = "○"
						case "○": m.current = "✘"
					}
				} else if m.view == "login" && (m.cursorX == 0 || m.cursorX == 1)  {
					m.insertMode = true
				}
				case "r": 
				if m.view == "game" {
					m.current =  "✘"
					m.board = initialModel().board;
				}
			case "i":
				if m.view == "login" && !m.insertMode && (m.cursorX == 0 || m.cursorX == 1)  {
					m.insertMode = true
				}

			}
		}
	}
	return m, nil
}

func (m model) View() string {
	var Reset  = "\033[0m"
	var Red    = "\033[31m"

	s:= ""

	if m.token == "" {
		s += GetLoginForm(m.cursorX, m.login, m.password, false)

	} else {

		s += "Where to move?\n\n"
		s += BoardToString(m.board, m.cursorX, m.cursorY, m.current)
	}

	s += "\n\n"
	s += Red + "\nPress q to quit.\n" + Reset

	return s
}

func GetLoginForm(cursor int, username string, password string, insertMode bool) string {
	var Reset  = "\033[0m"
	var Blue   = "\033[34m"
	var Red    = "\033[31m"

	s := ""
	s += "Please log in.\n\n"
	s += Blue + "Login:    " + Reset
	if cursor == 0 {
		s += Red
	}
	s += username + strings.Repeat("_", 20 - len(username)) + "\n"
	if cursor == 0 {
		s += Reset
	}


	s += Blue + "Password: " + Reset
	if cursor == 1 {
		s += Red
	}
	s += strings.Repeat("*", len(password)) + strings.Repeat("_", 20 - len(password)) + "\n"
	if cursor == 1 {
		s += Reset
	}

	s +=  "\n\nDon't have an account? " 
	if cursor == 2 {
		s += Red
	} else {
		s += Blue
	}
	s += "Register."
	s += Reset
	return s
}


func BoardToString(board [][]string, cursorX int, cursorY int, current string) string {
	var Reset  = "\033[0m"
	var Blue   = "\033[34m"
	var Red    = "\033[31m"

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

func main() {
	p := tea.NewProgram(initialModel())
	if _, err := p.Run(); err != nil {
		fmt.Printf("error: %v", err)
		os.Exit(1)
	}
}
