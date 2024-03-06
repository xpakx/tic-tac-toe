package main

import (
	"fmt"
	"os"

	tea "github.com/charmbracelet/bubbletea"
)

const apiUrl = "http://localhost:8000"

type serverErr struct {
	Error int `json:"error"`
	Message string `json:"message"`
	Status string `json:"status"`
	Errors []string `json:"errors"`
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
			} else if m.view == "register" && m.cursorX < 4 {
				m.cursorX++
			} else if m.view == "menu" && m.cursorX < 4 {
				m.cursorX++
			} else if m.view == "request" && m.cursorX < 1 {
				m.cursorX++
			} else if (m.view == "games" || m.view == "archive" || m.view == "requests") && m.cursorX < len(m.games)-1 {
				m.cursorX++
			}
		case "left", "h":
			if (m.view == "game" || m.view == "requests" || m.view == "games") && m.cursorY > 0 {
				m.cursorY--
			}
		case "right", "l":
			if m.view == "game" && m.cursorY < len(m.board[0])-1 {
				m.cursorY++
			} else if m.view == "requests" && m.cursorY < 2 && len(m.games) > 0 {
				m.cursorY++
			} else if m.view == "games" && m.cursorY < 1 && len(m.games) > 0 {
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
				return m, logIn(m.inputs[0].value, m.inputs[1].value)
			} else if m.view == "login" && m.cursorX == 3 {
				m = m.ToRegister();
			} else if m.view == "register" && (m.cursorX == 0 || m.cursorX == 1 || m.cursorX == 2)  {
				m.inputs[m.cursorX].focused = true
			} else if m.view == "register" && m.cursorX == 3 {
				return m, register(m.inputs[0].value, m.inputs[1].value, m.inputs[2].value)
			} else if m.view == "register" && m.cursorX == 4 {
				m = m.ToLogin();
			} else if m.view == "menu" {
				if m.cursorX == 0 {
					m = m.ToRequestForm()
				} else if m.cursorX == 1 { 
					return m, createAIGame(m.token)
				} else if m.cursorX == 2 { 
					return m, getGames("/request", m.token)
				} else if m.cursorX == 3 { 
					return m, getGames("", m.token)
				} else if m.cursorX == 4 { 
					return m, getGames("/archive", m.token)
				}
			} else if m.view == "request" && m.cursorX == 0 {
				m.inputs[m.cursorX].focused = true
			} else if m.view == "request" && m.cursorX == 1 {
				return m, sendRequest(m.inputs[0].value, m.token)
			} else if m.view == "requests" {
				if m.cursorY == 0 {
					m = m.ToMenu()
				} else if m.cursorY == 1 {
					return m, acceptRequest(m.games[m.cursorX].Id, true, m.token)
				} else if m.cursorY == 2 {
					return m, acceptRequest(m.games[m.cursorX].Id, false, m.token)
				}
			} else if m.view == "games" {
				if m.cursorY == 0 {
					m = m.ToMenu()
				}
			} else if m.view == "archive" {
				m = m.ToMenu()
			}
			case "r": 
			if m.view == "game" {
				m.current =  "✘"
				m.board = initialModel().board;
			}
		case "i":
			if m.view == "login" && (m.cursorX == 0 || m.cursorX == 1)  {
				m.inputs[m.cursorX].focused = true
			} else if m.view == "register" && (m.cursorX == 0 || m.cursorX == 1 || m.cursorX == 2)  {
				m.inputs[m.cursorX].focused = true
			} else if m.view == "request" && m.cursorX == 0 {
				m.inputs[m.cursorX].focused = true
			}

		}
	case authResponse:
		m.token = msg.Token
		m.username = msg.Username
		m = m.ToMenu()
	case gameResponse:
		fmt.Println(msg.Id)
		m = m.ToMenu()
	case aiGame:
		fmt.Println(msg.Id)
		m = m.ToGame(msg.Id)
	case gameList:
		m.games = msg.Games
		m = m.ToGameList(msg.Type)
	case serverErr:
		m.error = msg.Message
	}
	return m, nil
}

func main() {
	p := tea.NewProgram(initialModel())
	if _, err := p.Run(); err != nil {
		fmt.Printf("error: %v", err)
		os.Exit(1)
	}
}
