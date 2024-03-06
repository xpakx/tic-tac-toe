package main

import (
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
