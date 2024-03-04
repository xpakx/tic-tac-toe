package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"strings"
	"time"

	tea "github.com/charmbracelet/bubbletea"
)

const apiUrl = "http://localhost:8000"

type model struct {
	board [][]string
	cursorX int
	cursorY int
	current string
	view string
	token string

	inputs []input
}

type input struct {
	title string
	value string
	maxLen int
	focused bool
}

func (i input) Update(msg tea.KeyMsg) (input, tea.Cmd) {
	key := msg.String()
	if key == "esc" || key == "enter" {
		i.focused = false
	} else if len(key) == 1 {
		if len(i.value) < i.maxLen {
			i.value += key;
		}
	} else if key == "ctrl+c" {
		return i, tea.Quit
	}
	return i, nil
}

func logIn(username string, password string) tea.Cmd {
    return func() tea.Msg {
	    c := &http.Client{Timeout: 10 * time.Second}
	    jsonBody := []byte(`{"username": "` + username + `", "password": "` + password +`"}`)
	    bodyReader := bytes.NewReader(jsonBody)
	    res, err := c.Post(apiUrl + "/authenticate", "application/json", bodyReader)

	    if err != nil {
		    return errMsg{err}
	    }
	    defer res.Body.Close()
	    body, err := io.ReadAll(res.Body)
	    if err != nil {
		    return errMsg{err}
	    }

	    data := authResponse{}
	    if res.StatusCode == 200 {
		    json.Unmarshal([]byte(body), &data)
	    }

	    return authMsg{res.StatusCode, data}
    }
}

type authMsg struct {
	status int
	body authResponse
}

type authResponse struct {
	token string
	username string
	moderator_role bool
}

type errMsg struct{ err error }

func (e errMsg) Error() string { return e.err.Error() }

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
		inputs: []input{{"Login", "", 20, false}, {"Password", "", 20, false}},
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
		for i, field := range m.inputs {
			if field.focused {
				newField, command := field.Update(msg)
				m.inputs[i] = newField
				return m, command
			}
		}
	}
	switch msg := msg.(type) {
	case tea.KeyMsg:
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
				m.inputs[m.cursorX].focused = true
			} else if m.view == "login" && m.cursorX == 2 {
				// login
				fmt.Print("log")
				return m, logIn(m.inputs[0].value, m.inputs[1].value)
			} else if m.view == "login" && m.cursorX == 3 {
				// to register
				fmt.Print("to reg")
			}
			case "r": 
			if m.view == "game" {
				m.current =  "✘"
				m.board = initialModel().board;
			}
		case "i":
			if m.view == "login" && (m.cursorX == 0 || m.cursorX == 1)  {
				m.inputs[m.cursorX].focused = true
			}

		}
	case authMsg:
		fmt.Print(msg.status)
		fmt.Print(msg.body.token)
		fmt.Print(msg.body.username)
		fmt.Print(msg.body.moderator_role)
	}
	return m, nil
}

func (m model) View() string {
	var Reset  = "\033[0m"
	var Red    = "\033[31m"

	s:= ""

	if m.token == "" {
		s += GetLoginForm(m.cursorX, m.inputs[0], m.inputs[1], false)

	} else {

		s += "Where to move?\n\n"
		s += BoardToString(m.board, m.cursorX, m.cursorY, m.current)
	}

	s += "\n\n"
	s += Red + "\nPress q to quit.\n" + Reset

	return s
}

func GetLoginForm(cursor int, username input, password input, insertMode bool) string {
	var Reset  = "\033[0m"
	var Blue   = "\033[34m"
	var Red    = "\033[31m"

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
