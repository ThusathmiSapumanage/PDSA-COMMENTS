-- Create the database if it doesn't exist
-- Note: Each user should run this locally

CREATE DATABASE IF NOT EXISTS algocore_dbV2;
USE algocore_dbV2;

-- Stores all players who run game sessions.
CREATE TABLE IF NOT EXISTS Player (
    Player_Id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    Player_Name VARCHAR(45) NOT NULL,
    Player_Email VARCHAR(45) NOT NULL UNIQUE,
    Player_Password VARCHAR(255) NOT NULL,
    CONSTRAINT chk_email_min_length CHECK (CHAR_LENGTH(Player_Email) >= 3),
    CONSTRAINT chk_password_min_length CHECK (CHAR_LENGTH(Player_Password) >= 6)
);

-- Master list of supported games.
CREATE TABLE IF NOT EXISTS Game (
    Game_Id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    Game_Name VARCHAR(25) NOT NULL UNIQUE
);

-- Algorithms available for each game.
CREATE TABLE IF NOT EXISTS Algorithm (
    Algorithm_Id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    Game_Id INT UNSIGNED NOT NULL,
    Algorithm_Name VARCHAR(45) NOT NULL UNIQUE,
    FOREIGN KEY (Game_Id) REFERENCES Game(Game_Id) ON DELETE CASCADE
);

-- One row per play/run attempt by a player for a game.
CREATE TABLE IF NOT EXISTS Game_Session (
    Session_Id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    Game_Id INT UNSIGNED NOT NULL,
    Player_Id INT UNSIGNED NOT NULL,
    Created_At DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX (Created_At),
    FOREIGN KEY (Game_Id) REFERENCES Game(Game_Id),
    FOREIGN KEY (Player_Id) REFERENCES Player(Player_Id)
);

-- Captures user response data for a session.
CREATE TABLE IF NOT EXISTS Response (
    Response_Id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    Session_Id INT UNSIGNED NOT NULL,
    Response VARCHAR(50) NOT NULL,
    Is_Correct BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (Session_Id) REFERENCES Game_Session(Session_Id) ON DELETE CASCADE
);

-- Output summary for the Minimum Cost game.
CREATE TABLE IF NOT EXISTS Minimum_Cost_Game (
    Session_Id INT UNSIGNED PRIMARY KEY,
    N_Value INT UNSIGNED NOT NULL,
    Minimum_Total_Cost INT NOT NULL,
    CONSTRAINT chk_min_cost_n_range CHECK (N_Value BETWEEN 5 AND 100),
    FOREIGN KEY (Session_Id) REFERENCES Game_Session(Session_Id) ON DELETE CASCADE
);

-- Output summary for the Snake and Ladder game.
CREATE TABLE IF NOT EXISTS Snake_And_Ladder_Game (
    Session_Id INT UNSIGNED PRIMARY KEY,
    N_Value INT UNSIGNED NOT NULL,
    Minimum_Throws INT UNSIGNED NOT NULL,
    CONSTRAINT chk_snake_ladder_n_range CHECK (N_Value BETWEEN 6 AND 12),
    FOREIGN KEY (Session_Id) REFERENCES Game_Session(Session_Id) ON DELETE CASCADE
);

-- Output summary for the Traffic Simulation game.
CREATE TABLE IF NOT EXISTS Traffic_Sim_Game (
    Session_Id INT UNSIGNED PRIMARY KEY,
    Max_Flow_Value INT UNSIGNED NOT NULL,
    FOREIGN KEY (Session_Id) REFERENCES Game_Session(Session_Id) ON DELETE CASCADE
);

-- Output summary for the Knight Tour game.
CREATE TABLE IF NOT EXISTS Knight_Tour_Game (
    Session_Id INT UNSIGNED PRIMARY KEY,
    Board_Size TINYINT UNSIGNED NOT NULL,
    X_Start TINYINT UNSIGNED NOT NULL,
    Y_Start TINYINT UNSIGNED NOT NULL,
    Move_Count INT UNSIGNED NOT NULL,
    CONSTRAINT chk_knight_board_size CHECK (Board_Size IN (8, 16)),
    CONSTRAINT chk_knight_x_start_range CHECK (X_Start BETWEEN 1 AND Board_Size),
    CONSTRAINT chk_knight_y_start_range CHECK (Y_Start BETWEEN 1 AND Board_Size),
    FOREIGN KEY (Session_Id) REFERENCES Game_Session(Session_Id) ON DELETE CASCADE
);

-- Output summary for the 16 Queens game.
CREATE TABLE IF NOT EXISTS Sixteen_Queens_Game (
    Session_Id INT UNSIGNED PRIMARY KEY,
    Total_Solutions INT UNSIGNED NOT NULL,
    Solutions_Discovered INT UNSIGNED NOT NULL,
    CONSTRAINT chk_solutions CHECK (Solutions_Discovered <= Total_Solutions),
    FOREIGN KEY (Session_Id) REFERENCES Game_Session(Session_Id) ON DELETE CASCADE
);

-- Cached N-queens solution counts + benchmark timings, keyed by (board_size, queen_count).
-- Populated on first no-time-cap run; every subsequent run reads instantly from here.
CREATE TABLE IF NOT EXISTS Queens_Solution_Count (
    Board_Size TINYINT UNSIGNED NOT NULL,
    Queen_Count TINYINT UNSIGNED NOT NULL,
    Total_Solutions INT UNSIGNED NOT NULL,
    Sequential_Time_Ms DECIMAL(10,3) UNSIGNED NOT NULL,
    Threaded_Time_Ms DECIMAL(10,3) UNSIGNED NOT NULL,
    Created_At TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (Board_Size, Queen_Count),
    CONSTRAINT chk_qsc_queens CHECK (Queen_Count >= 1 AND Queen_Count <= Board_Size)
);

-- Stores all snakes/ladders configured for a session.
CREATE TABLE IF NOT EXISTS Snake_And_Ladder_Item (
    Session_Id INT UNSIGNED NOT NULL,
    Start_Cell SMALLINT UNSIGNED NOT NULL,
    End_Cell SMALLINT UNSIGNED NOT NULL,
    Path_Type ENUM('SNAKE', 'LADDER') NOT NULL,
    PRIMARY KEY (Session_Id, Start_Cell),
    CONSTRAINT chk_not_same_cell CHECK (Start_Cell <> End_Cell),
    CONSTRAINT chk_cells_positive CHECK (Start_Cell > 0 AND End_Cell > 0),
    CONSTRAINT chk_path_direction CHECK (
        (Path_Type = 'SNAKE' AND Start_Cell > End_Cell) OR
        (Path_Type = 'LADDER' AND Start_Cell < End_Cell)
    ),
    FOREIGN KEY (Session_Id) REFERENCES Snake_And_Ladder_Game(Session_Id) ON DELETE CASCADE
);

-- Graph edges and capacities for traffic simulation.
CREATE TABLE IF NOT EXISTS Roads (
    Session_Id INT UNSIGNED NOT NULL,
    Start_Node VARCHAR(1) NOT NULL,
    End_Node VARCHAR(1) NOT NULL,
    Capacity INT UNSIGNED NOT NULL,
    PRIMARY KEY (Session_Id, Start_Node, End_Node),
    CONSTRAINT chk_road_nodes CHECK (Start_Node <> End_Node),
    CONSTRAINT chk_road_capacity_range CHECK (Capacity BETWEEN 5 AND 15),
    FOREIGN KEY (Session_Id) REFERENCES Traffic_Sim_Game(Session_Id) ON DELETE CASCADE
);

-- Ordered move list produced by Knight Tour.
CREATE TABLE IF NOT EXISTS Knight_Move (
    Session_Id INT UNSIGNED NOT NULL,
    Algorithm_Id INT UNSIGNED NOT NULL,
    Step_No SMALLINT UNSIGNED NOT NULL,
    X_Value TINYINT UNSIGNED NOT NULL,
    Y_Value TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (Session_Id, Algorithm_Id, Step_No),
    FOREIGN KEY (Session_Id) REFERENCES Knight_Tour_Game(Session_Id) ON DELETE CASCADE,
    FOREIGN KEY (Algorithm_Id) REFERENCES Algorithm(Algorithm_Id)
);

-- Stores each 16-Queens solution discovered/generated.
CREATE TABLE IF NOT EXISTS Solution (
    Session_Id INT UNSIGNED NOT NULL,
    Solution_Id INT UNSIGNED NOT NULL,
    Is_Discovered BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (Session_Id, Solution_Id),
    FOREIGN KEY (Session_Id) REFERENCES Sixteen_Queens_Game(Session_Id) ON DELETE CASCADE
);

-- Queen coordinates for a given solution layout.
CREATE TABLE IF NOT EXISTS Queen_Location (
    Session_Id INT UNSIGNED NOT NULL,
    Solution_Id INT UNSIGNED NOT NULL,
    X_Value TINYINT UNSIGNED NOT NULL,
    Y_Value TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (Session_Id, Solution_Id, X_Value, Y_Value),
    FOREIGN KEY (Session_Id, Solution_Id) REFERENCES Solution(Session_Id, Solution_Id) ON DELETE CASCADE
);

-- Tracks algorithm execution metrics per session.
CREATE TABLE IF NOT EXISTS Algorithm_Execute (
    Session_Id INT UNSIGNED NOT NULL,
    Algorithm_Id INT UNSIGNED NOT NULL,
    Execution_Time_MS DECIMAL(10,3) UNSIGNED NOT NULL,
    Output_Result ENUM('SUCCESS', 'FAILURE', 'TIMEOUT', 'ERROR') NOT NULL,
    Max_Flow_Result INT NULL,
    PRIMARY KEY (Session_Id, Algorithm_Id),
    FOREIGN KEY (Session_Id) REFERENCES Game_Session(Session_Id) ON DELETE CASCADE,
    FOREIGN KEY (Algorithm_Id) REFERENCES Algorithm(Algorithm_Id)
);

-- Record user input as the users path.
CREATE TABLE IF NOT EXISTS Knight_Tour_Response_Move (
    Response_Id INT UNSIGNED NOT NULL,
    Step_No SMALLINT UNSIGNED NOT NULL,
    X_Value TINYINT UNSIGNED NOT NULL,
    Y_Value TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (Response_Id, Step_No),
    CONSTRAINT fk_ktrm_response
        FOREIGN KEY (Response_Id) REFERENCES Response(Response_Id) ON DELETE CASCADE
);
