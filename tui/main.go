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
	username string
	error string

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

	    if res.StatusCode == 200 {
		    data := authResponse{}
		    err := json.Unmarshal([]byte(string(body)), &data)
		    if err != nil {
			    return errMsg{err}
		    }
		    return data
	    }

	    data := serverErr{}
	    json.Unmarshal([]byte(body), &data)
	    if err != nil {
		    return errMsg{err}
	    }
	    return data
    }
}

func register(username string, password string, passwordRe string) tea.Cmd {
    return func() tea.Msg {
	    c := &http.Client{Timeout: 10 * time.Second}
	    jsonBody := []byte(`{"username": "` + username + `", "password": "` + password +`", "passwordRe": "` + passwordRe + `"}`)
	    bodyReader := bytes.NewReader(jsonBody)
	    res, err := c.Post(apiUrl + "/register", "application/json", bodyReader)

	    if err != nil {
		    return errMsg{err}
	    }
	    defer res.Body.Close()
	    body, err := io.ReadAll(res.Body)
	    if err != nil {
		    return errMsg{err}
	    }

	    if res.StatusCode == 201 {
		    data := authResponse{}
		    json.Unmarshal([]byte(body), &data)
		    if err != nil {
			    return errMsg{err}
		    }
		    return data
	    }
	    data := serverErr{}
	    json.Unmarshal([]byte(body), &data)
	    if err != nil {
		    return errMsg{err}
	    }
	    return data
    }
}

type serverErr struct {
	Error int `json:"error"`
	Message string `json:"message"`
	Status string `json:"status"`
	Errors []string `json:"errors"`
}

type authResponse struct {
	Token string `json:"token"`
	Username string `json:"username"`
	ModeratorRole bool `json:"moderator_role"`
}

func sendRequest(username string, token string) tea.Cmd {
    return func() tea.Msg {
	    c := &http.Client{Timeout: 10 * time.Second}
	    jsonBody := []byte(`{"opponent": "` + username + `", "type": "USER"}`)
	    bodyReader := bytes.NewReader(jsonBody)
	    req, err := http.NewRequest(http.MethodPost, apiUrl + "/game", bodyReader)
	    if err != nil {
		    return errMsg{err}
	    }
	    req.Header.Add("Content-Type", "application/json")
	    req.Header.Add("Authorization", "Bearer " + token)
	    res, err := c.Do(req)

	    if err != nil {
		    return errMsg{err}
	    }
	    defer res.Body.Close()
	    body, err := io.ReadAll(res.Body)
	    if err != nil {
		    return errMsg{err}
	    }

	    if res.StatusCode == 201 {
		    data := gameResponse{}
		    json.Unmarshal([]byte(body), &data)
		    if err != nil {
			    return errMsg{err}
		    }
		    return data
	    }
	    data := serverErr{}
	    json.Unmarshal([]byte(body), &data)
	    if err != nil {
		    return errMsg{err}
	    }
	    return data
    }
}

func createAIGame(token string) tea.Cmd {
    return func() tea.Msg {
	    c := &http.Client{Timeout: 10 * time.Second}
	    jsonBody := []byte(`{"type": "AI"}`)
	    bodyReader := bytes.NewReader(jsonBody)
	    req, err := http.NewRequest(http.MethodPost, apiUrl + "/game", bodyReader)
	    if err != nil {
		    return errMsg{err}
	    }
	    req.Header.Add("Content-Type", "application/json")
	    req.Header.Add("Authorization", "Bearer " + token)
	    res, err := c.Do(req)

	    if err != nil {
		    return errMsg{err}
	    }
	    defer res.Body.Close()
	    body, err := io.ReadAll(res.Body)
	    if err != nil {
		    return errMsg{err}
	    }

	    if res.StatusCode == 201 {
		    data := gameResponse{}
		    json.Unmarshal([]byte(body), &data)
		    if err != nil {
			    return errMsg{err}
		    }
		    return aiGame{Id: data.Id}
	    }
	    data := serverErr{}
	    json.Unmarshal([]byte(body), &data)
	    if err != nil {
		    return errMsg{err}
	    }
	    return data
    }
}

type gameResponse struct {
	Id int `json:"id"`
}

type aiGame struct {
	Id int
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
					// requests
				} else if m.cursorX == 3 { 
					// active
				} else if m.cursorX == 4 { 
					// archive
				}
			} else if m.view == "request" && m.cursorX == 0 {
				m.inputs[m.cursorX].focused = true
			} else if m.view == "request" && m.cursorX == 1 {
				return m, sendRequest(m.inputs[0].value, m.token)
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
	case serverErr:
		m.error = msg.Message
	}
	return m, nil
}

func (m model) View() string {
	var Reset  = "\033[0m"
	var Red    = "\033[31m"

	s:= ""

	if len(m.error) > 0 {
		s += Red + "Error: " + m.error + Reset
		s += "\n"
	}

	if m.view == "login" {
		s += GetLoginForm(m.cursorX, m.inputs[0], m.inputs[1], false)
	} else if m.view == "register" {
		s += GetRegisterForm(m.cursorX, m.inputs[0], m.inputs[1], m.inputs[2], false)

	} else if m.view == "menu" {
		s += GetMenu(m.cursorX)
	} else if m.view == "request" {
		s += GetRequestForm(m.cursorX, m.inputs[0], false)
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

func GetRegisterForm(cursor int, username input, password input, passwordRe input, insertMode bool) string {
	var Reset  = "\033[0m"
	var Blue   = "\033[34m"
	var Red    = "\033[31m"

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
	var Reset  = "\033[0m"
	var Red    = "\033[31m"
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
	var Reset  = "\033[0m"
	var Blue   = "\033[34m"
	var Red    = "\033[31m"

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
