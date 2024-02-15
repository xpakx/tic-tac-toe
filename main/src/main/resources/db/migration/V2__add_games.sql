CREATE TABLE game (
   id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,

   accepted BOOLEAN DEFAULT FALSE NOT NULL,
   current_state VARCHAR(255) NOT NULL,
   type SMALLINT NOT NULL,
   finished BOOLEAN DEFAULT FALSE NOT NULL,
   won BOOLEAN DEFAULT FALSE NOT NULL,
   lost BOOLEAN DEFAULT FALSE NOT NULL,
   drawn BOOLEAN DEFAULT FALSE NOT NULL,
   last_move VARCHAR(255),
   last_move_at TIME,
   started_at TIME,

   user_id BIGINT NOT NUll,
   opponent_id BIGINT,
   CONSTRAINT pk_game PRIMARY KEY (id),
   CONSTRAINT fk_user_id
      FOREIGN KEY(user_id)
      REFERENCES account(id),
   CONSTRAINT fk_opponent_id
      FOREIGN KEY(opponent_id)
      REFERENCES account(id)
);