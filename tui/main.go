package main

import (
	"fmt"
	"os"
	"log"

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

func initialModel(websocket *websocket_service) model {
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
		websocket: websocket,
	}
}

func main() {
	w := websocket_service{game_id: unset}
	p := tea.NewProgram(initialModel(&w))
	w.SetProgram(p)
	defer func() {
		// TODO
		if w.Connection != nil {
			if err := w.Connection.Close(); err != nil {
				log.Println("error closing WebSocket connection:", err)
			} else {
				log.Println("closed WebSocket connection")
			}
		} else {
			log.Println("No WebSocket connection")
		}
	}()
	if _, err := p.Run(); err != nil {
		fmt.Printf("error: %v", err)
		os.Exit(1)
	}
}
