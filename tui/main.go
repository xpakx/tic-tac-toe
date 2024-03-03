package main

import (
	"os"
	"fmt"
	tea "github.com/charmbracelet/bubbletea"
)

type model struct {
	board [][]string
	cursorX int
	cursorY int
	current string
	// TODO: current view: list/login/register
	view string
}

func initialModel() model {
	return model{
		board:  [][]string{
			{" ", " ", " "},
			{" ", " ", " "},
			{" ", " ", " "},
		},
		current: "✘",
		view: "game",
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
	    switch msg.String() {
	    case "ctrl+c", "q":
		    return m, tea.Quit
	    case "up", "k":
		    if m.cursorX > 0 {
			    m.cursorX--
		    }
	    case "down", "j":
		    if m.cursorX < len(m.board)-1 {
			    m.cursorX++
		    }
	    case "left", "h":
		    if m.cursorY > 0 {
			    m.cursorY--
		    }
	    case "right", "l":
		    if m.cursorY < len(m.board[0])-1 {
			    m.cursorY++
		    }
	    case "enter", " ":
		    m.board[m.cursorX][m.cursorY] = m.current;
		    switch m.current {
			    case "✘": m.current = "○"
			    case "○": m.current = "✘"
		    }
	    case "r": 
		    m.current =  "✘"
		    m.board = initialModel().board;
	    }
    }
    return m, nil
}

func (m model) View() string {
	var Reset  = "\033[0m"
	var Red    = "\033[31m"

	s := "Where to move?\n\n"
	s += BoardToString(m.board, m.cursorX, m.cursorY, m.current);
	s += "\n\n"

	s += Red + "\nPress q to quit.\n" + Reset

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
