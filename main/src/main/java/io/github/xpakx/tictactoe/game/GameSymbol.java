package io.github.xpakx.tictactoe.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameSymbol {
    X("x"),
    O("o"),
    Empty("?");

    private final String symbol;
}
