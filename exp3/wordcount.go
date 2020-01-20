package main

import (
	"fmt"
	"io/ioutil"
	"os"
	"regexp"
	"strings"
)

func main() {
	data, err := ioutil.ReadFile(os.Args[1])
	if err != nil {
		panic(err)
	}
	words := strings.Fields(string(data))
	fmt.Println("num origin words:", len(words))
	var tmp []string = nil
	for _, word := range words {
		reg, err := regexp.Compile("[^a-zA-Z0-9]+")
		if err != nil {
			panic(err)
		}
		word := reg.ReplaceAllString(word, "")
		if word != "" {
			tmp = append(tmp, word)
		}
	}
	words, tmp = tmp, nil
	for _, word := range words {
		tmp = append(tmp, strings.ToLower(word))
	}
	words, tmp = tmp, nil
	fmt.Println("num filtered words:", len(words))
	wordCount := make(map[string]int)
	for _, word := range words {
		wordCount[word]++
	}
	fmt.Println("num different words:", len(wordCount))
}
