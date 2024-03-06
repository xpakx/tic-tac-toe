package main

import (
	"bytes"
	"encoding/json"
	"io"
	"net/http"
	"time"

	tea "github.com/charmbracelet/bubbletea"
)

type authResponse struct {
	Token string `json:"token"`
	Username string `json:"username"`
	ModeratorRole bool `json:"moderator_role"`
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
