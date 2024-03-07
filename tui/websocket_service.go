package main

import (
	"time"

	tea "github.com/charmbracelet/bubbletea"
)

const unset = -1

type websocket_service struct {
	game_id int
	program *tea.Program
}

func (m *websocket_service) SetGameId(game_id int) {
	m.game_id = game_id
}

func (m websocket_service) Run() {
	go func() {
		for {
			pause := time.Duration(1) * time.Second
			time.Sleep(pause)

			m.program.Send(socketMsg{id: m.game_id})
		}
	}()
}

type socketMsg struct {
	id int
}
