package main

import (
	"fmt"
	"log"
	"os"
	"os/signal"
	"regexp"
	"time"

	tea "github.com/charmbracelet/bubbletea"
	websocket "github.com/gorilla/websocket"
)

const unset = -1

type websocket_service struct {
	game_id int
	program *tea.Program
	Connection *websocket.Conn
}

func (m *websocket_service) SetGameId(game_id int) {
	m.game_id = game_id
}

func (m *websocket_service) SetProgram(program *tea.Program) {
	m.program = program
}


func (ws *websocket_service) ConnectWS() {
	pattern := `^http`
	regex := regexp.MustCompile(pattern)
	url := regex.ReplaceAllString(apiUrl, "ws")
	url = url + "/play/websocket"

	c, _, err := websocket.DefaultDialer.Dial(url, nil)
	if err != nil {
		log.Fatal("dial:", err)
	}
	// defer c.Close() // TODO

	ws.Connection = c
}

func (ws *websocket_service) Connect(token string) {
	connectMessage := fmt.Sprintf("CONNECT\nToken:%s\naccept-version:%s\nheart-beat:%s\n\n\000", token, "1.2,1.1,1.0", "20000,0")
	err := ws.Connection.WriteMessage(websocket.TextMessage, []byte(connectMessage))
	if err != nil {
		log.Fatal("write:", err)
	}
}


func (ws *websocket_service) Subscribe() {
	topics := []string{"/topic/game", "/topic/board", "/app/board", "/topic/chat"}
	for _, topic := range topics {
		subscribeMessage := fmt.Sprintf("SUBSCRIBE\nid:sub-0\ndestination:%s/%d\n\n\000", topic ,ws.game_id);
		err := ws.Connection.WriteMessage(websocket.TextMessage, []byte(subscribeMessage))
		if err != nil {
			log.Println("write:", err)
			return
		}
	}
}

func (ws *websocket_service) Run() {
	done := make(chan struct{})
	go func() {
		defer close(done)
		for {
			select {
			case <-done:
				fmt.Println("end")
				return
			default:
				_, message, err := ws.Connection.ReadMessage()
				if err != nil {
					log.Println("read:", err)
					return
				}
				ws.handleMessage(string(message))
			}
		}
	}()

	interrupt := make(chan os.Signal, 1)
	signal.Notify(interrupt, os.Interrupt)

	select {
	case <-interrupt:
		log.Println("interrupt")
	}

	err := ws.Connection.WriteMessage(websocket.CloseMessage, websocket.FormatCloseMessage(websocket.CloseNormalClosure, ""))
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

func (m websocket_service) handleMessage(rawMessage string) {
    log.Println(rawMessage)
    m.program.Send(socketMsg{
	    msg: fmt.Sprintf("received message: %s", string(rawMessage)),
    })
}
