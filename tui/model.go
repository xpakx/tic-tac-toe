package main

import (
	"fmt"

	tea "github.com/charmbracelet/bubbletea"
)

type model struct {
	board [][]string
	cursorX int
	cursorY int
	current string
	view string
	token string
	username string
	error string
	websocket websocket_service

	inputs []input
	games []gameSummary
}

func (m model) Init() tea.Cmd {
	return nil
}

func (m model) ToRegister() model {
	m.cursorX = 0
	m.cursorY = 0
	m.inputs = []input{{"Login", "", 20, false}, {"Password", "", 20, false}, {"Repeat", "", 20, false}}
	m.view = "register"
	return m
}

func (m model) ToLogin() model {
	m.cursorX = 0
	m.cursorY = 0
	m.inputs = []input{{"Login", "", 20, false}, {"Password", "", 20, false}}
	m.view = "login"
	return m
}

func (m model) ToMenu() model {
	m.cursorX = 0
	m.cursorY = 0
	m.view = "menu"
	return m
}

func (m model) ToGame(id int) model {
	m.cursorX = 0
	m.cursorY = 0
	m.view = "game"
	// TODO
	return m
}

func (m model) ToRequestForm() model {
	m.cursorX = 0
	m.cursorY = 0
	m.inputs = []input{{"Username", "", 20, false}}
	m.view = "request"
	return m
}

func (m model) ToGameList(listType string) model {
	m.cursorX = 0
	m.cursorY = 0
	m.view = listType
	return m
}

func (m model) View() string {
	s:= ""

	if len(m.error) > 0 {
		s += Red + "Error: " + m.error + Reset
		s += "\n"
	}
	switch m.view {
		case "login": 
			s += GetLoginForm(m.cursorX, m.inputs[0], m.inputs[1], false)
		case "register":
			s += GetRegisterForm(m.cursorX, m.inputs[0], m.inputs[1], m.inputs[2], false)
		case "menu":
			s += GetMenu(m.cursorX)
		case "request":
			s += GetRequestForm(m.cursorX, m.inputs[0], false)
		case "games", "archive", "requests":
			s += GetGameList(m.cursorX, m.cursorY, m.games, m.view)
		default:
			s += "Where to move?\n\n"
			s += BoardToString(m.board, m.cursorX, m.cursorY, m.current)
	}

	s += "\n\n"
	s += Red + "\nPress q to quit.\n" + Reset

	return s
}

func (m model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		return m.KeyEvent(msg)
	case authResponse:
		m.token = msg.Token
		m.username = msg.Username
		m = m.ToMenu()
	case gameResponse:
		// fmt.Println(msg.Id)
		m = m.ToMenu()
	case aiGame:
		m = m.ToGame(msg.Id)
	case gameList:
		m.games = msg.Games
		m = m.ToGameList(msg.Type)
	case serverErr:
		m.error = msg.Message
	case socketMsg:
		m.error = "Test: game id " + fmt.Sprint(msg.id)
	}
	return m, nil
}



func (m model) KeyEvent(msg tea.KeyMsg) (tea.Model, tea.Cmd) {
	for i, field := range m.inputs {
		if field.focused {
			newField, command := field.Update(msg)
			m.inputs[i] = newField
			return m, command
		}
	}

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
			m.board = initialModel(m.websocket).board;
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

	return m, nil
}
