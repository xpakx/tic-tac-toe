package main

import (
	"bytes"
	"encoding/json"
	"io"
	"net/http"
	"time"
	"fmt"

	tea "github.com/charmbracelet/bubbletea"
)

type gameSummary struct {
    Id int `json:"id"`
    State [][]string `json:"currentState"`
    LastMoveRow int `json:"lastMoveRow"`
    LastMoveColumn int `json:"lastMoveColumn"`
    Type string `json:"Type"`

    Finished bool `json:"finished"`
    Won bool `json:"won"`
    Lost bool `json:"lost"`
    Drawn bool `json:"drawn"`

    Username1 string `json:"username1"`
    Username2 string `json:"username2"`
    UserStarts bool `json:"userStarts"`
    CurrentSymbol string `json:"currentSymbol"`
}

type gameList struct {
	Games []gameSummary
	Type string
}

type gameResponse struct {
	Id int `json:"id"`
}

type aiGame struct {
	Id int
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

func getGames(endpoint string, token string) tea.Cmd {
    return func() tea.Msg {
	    c := &http.Client{Timeout: 10 * time.Second}
	    req, err := http.NewRequest(http.MethodGet, apiUrl + "/game" + endpoint, nil)
	    if err != nil {
		    return errMsg{err}
	    }
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

	    if res.StatusCode == 200 {
		    var data []gameSummary
		    json.Unmarshal([]byte(body), &data)
		    if err != nil {
			    return errMsg{err}
		    }
		    requestType := "games" 
		    if endpoint == "/archive" {
			    requestType = "archive" 
		    } else if endpoint == "/request" {
			    requestType = "requests" 
		    }
		    return gameList{Games: data, Type: requestType}
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

func acceptRequest(gameId int, accept bool, token string) tea.Cmd {
    return func() tea.Msg {
	    c := &http.Client{Timeout: 10 * time.Second}
	    acceptString := "false"
	    if accept {
		    acceptString = "true"
	    }
	    jsonBody := []byte(`{"accepted": "` + acceptString + `"}`)
	    bodyReader := bytes.NewReader(jsonBody)
	    req, err := http.NewRequest(http.MethodPost, apiUrl + "/game/" + fmt.Sprint(gameId) + "/request", bodyReader)
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

	    if res.StatusCode == 200 {
		    var data bool;
		    json.Unmarshal([]byte(body), &data)
		    if err != nil {
			    return errMsg{err}
		    }
		    fmt.Println(data)
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
