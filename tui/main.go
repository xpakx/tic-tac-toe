package main

import (
	"fmt"
	"os"

	tea "github.com/charmbracelet/bubbletea"
)

const apiUrl = "http://localhost:8000"
const Reset  = "\033[0m"
const Red    = "\033[31m"
const Blue   = "\033[34m"

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

func main() {
	p := tea.NewProgram(initialModel())
	if _, err := p.Run(); err != nil {
		fmt.Printf("error: %v", err)
		os.Exit(1)
	}
}
