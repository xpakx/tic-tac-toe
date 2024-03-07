package main

import (
	"time"
	"regexp"
	"log"
	"os"
	"os/signal"
	"encoding/json"
	"fmt"

	tea "github.com/charmbracelet/bubbletea"
	websocket "github.com/gorilla/websocket"
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
	pattern := `^http`
	regex := regexp.MustCompile(pattern)
	url := regex.ReplaceAllString(apiUrl, "ws")
	url = url + "/play/websocket"

	c, _, err := websocket.DefaultDialer.Dial(url, nil)
	if err != nil {
		log.Fatal("dial:", err)
	}
	defer c.Close()

	topics := []string{"topic/game", "topic/board", "app/board", "topic/chat"}
	for _, topic := range topics {
		subscribeMessage := []byte(`{"action": "subscribe", "topic": "` + topic + fmt.Sprint(m.game_id) + `"}`)
		err = c.WriteMessage(websocket.TextMessage, subscribeMessage)
		if err != nil {
			log.Println("write:", err)
			return
		}
	}


	done := make(chan struct{})
	go func() {
		defer close(done)
		for {
			select {
			case <-done:
				fmt.Println("end")
				return
			default:
				_, message, err := c.ReadMessage()
				if err != nil {
					log.Println("read:", err)
					return
				}
				m.handleMessage(message)
			}
		}
	}()

	interrupt := make(chan os.Signal, 1)
	signal.Notify(interrupt, os.Interrupt)

	select {
	case <-interrupt:
		log.Println("interrupt")
	}

	err = c.WriteMessage(websocket.CloseMessage, websocket.FormatCloseMessage(websocket.CloseNormalClosure, ""))
	if err != nil {
		log.Println("write close:", err)
		return
	}
	select {
	case <-done:
	case <-time.After(time.Second):
	}

}

type socketMsg struct {
	msg string
}

func (m websocket_service) handleMessage(rawMessage []byte) {
    var message Message
    if err := json.Unmarshal(rawMessage, &message); err != nil {
        log.Println("error decoding message:", err)
        return
    }
    m.program.Send(socketMsg{
	    msg: fmt.Sprintf("received message from topic '%s': %s", message.Topic, message.Content),
    })
}

type Message struct {
    Topic   string `json:"topic"`
    Content string `json:"content"`
}
