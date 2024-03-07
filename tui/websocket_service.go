package main

import (
	"fmt"
	"log"
	"os"
	"os/signal"
	"regexp"
	"time"
	"strings"
	"encoding/json"

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

func (ws websocket_service) SendChat(msg string) {
	body := "{\"message\": \"" + msg + "\"}"
	msg_length := len(body)
	path := fmt.Sprintf("/app/chat/%d", ws.game_id)
	chatMessage := fmt.Sprintf("SEND\ndestination:%s\ncontent-length:%d\n\n%s\000", path, msg_length, body)
	err := ws.Connection.WriteMessage(websocket.TextMessage, []byte(chatMessage))
	if err != nil {
		log.Println("chat:", err)
	}
}

func (ws websocket_service) SendMove(x, y int) {
	body := fmt.Sprintf(`{"x": %d, "y": %d}`, x, y)
	msg_length := len(body)
	path := fmt.Sprintf("/app/move/%d", ws.game_id)
	chatMessage := fmt.Sprintf("SEND\ndestination:%s\ncontent-length:%d\n\n%s\000", path, msg_length, body)
	err := ws.Connection.WriteMessage(websocket.TextMessage, []byte(chatMessage))
	if err != nil {
		log.Println("move:", err)
	}
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
    destination, err := extractDestination(rawMessage);
    body, err2 := extractBody(rawMessage);
    if err == nil && err2 == nil {
	    switch destination {
	    case "game":
		    var data MoveMsg
		    if err := json.Unmarshal([]byte(body), &data); err == nil {
			    m.program.Send(data)
		    } else {
			    m.program.Send(socketMsg{
				    msg: fmt.Sprintf("error: %s", string(rawMessage)),
			    })
		    }
	    case "board":
		    // TODO
	    case "chat":
		    // TODO
	    }
    }
}

func extractDestination(message string) (string, error) {
    pattern := `destination:/topic/(.*)/`
    re := regexp.MustCompile(pattern)
    matches := re.FindStringSubmatch(message)
    if len(matches) < 2 {
        return "", fmt.Errorf("destination not found in message")
    }
    return matches[1], nil
}

func extractBody(message string) (string, error) {
    start := strings.Index(message, "\n\n")
    if start == -1 {
        return "", fmt.Errorf("start of body not found in message")
    }
    start += 2 

    end := strings.Index(message, "\000")
    if end == -1 {
        return "", fmt.Errorf("end of body not found in message")
    }

    return message[start:end], nil
}

type MoveMsg struct {
    Player        string `json:"player"`
    X             int    `json:"x"`
    Y             int    `json:"y"`
    Legal         bool   `json:"legal"`
    Applied       bool   `json:"applied"`
    CurrentSymbol string `json:"currentSymbol"`
    Finished      bool   `json:"finished"`
    Drawn         bool   `json:"drawn"`
    Won           bool   `json:"won"`
    Winner        string `json:"winner"`
}
