package main


import (
	tea "github.com/charmbracelet/bubbletea"
)
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
	} else if key == "backspace" {
		if len(i.value) > 0 {
			i.value = i.value[:len(i.value)-1]
		}
	}
	return i, nil
}
