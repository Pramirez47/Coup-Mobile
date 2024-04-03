package database.Game;

import database.Users.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Game {
    List<Player> players;
    Deck deck;

    String lastCharacterMove;
    Player currentPlayer;


    public Game(List<Player> players) {
        this.players = players;
    }

    public void initGame(String name1, String name2, String name3, String name4) {
        // Adds players
        players.add(new Player(name1, 2, false,2));
        players.add(new Player(name2, 2, false,2));
        players.add(new Player(name3, 2, false,2));
        players.add(new Player(name4, 2, false,2));
        int var = 0;
        //Shuffle
        Collections.shuffle(players); //Shuffles Player to allow fair chance for 1st move

        for (int i = 0; i < players.size(); i++) { //Assigns index to player
            Player player = players.get(i);
            player.setTurnNumber(i+1);
        }

        currentPlayer = players.get(0); //Assigns current player to first player in array.
        currentPlayer.setTurn(true);



        deck = new Deck(); // Create a Deck Object
        deck.initializeDeck(); //Initialize a deck of 15 Cards. [Duke,Duke,Duke,Captain,Captain...]
        deck.shuffle(); //Shuffle/randomize Array List of Cards
        for (Player player : players) {
            player.setCardOne(deck.drawCard());
            player.setCardTwo(deck.drawCard());
        }


    }

    public String getLastCharacterMove() {
        return lastCharacterMove;
    }

    public void setLastCharacterMove(String lastCharacterMove) {
        this.lastCharacterMove = lastCharacterMove;
    }

    public void getPlayers() {
        for (Player player : players) {
            System.out.println(player.toString());
        }
    }

    public List<Player> getPlayerArrayList() {
        return players;
    }

    public void nextTurn() {
        currentPlayer.setTurn(false); // Set their turn to false
        int CurrentPlayerIndex = (getPlayer(currentPlayer.getUserEmail()).turnNumber) % players.size(); // Find next user
        currentPlayer = players.get(CurrentPlayerIndex); // Assign player to this player
        players.get(CurrentPlayerIndex).setTurn(true); // Set turn to true
        System.out.println("The next player is: " + currentPlayer.toString()); // Print Player
    }

    public Player getPlayer(String playerName) {
        for (Player player : players) {
            if (player.getUserEmail().equals(playerName)) {
                return player; // Return the player if found
            }
        }
        return null; // Return null if player not found
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public String getDeck() {
        return deck.toString();
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
    }

//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        sb.append("Game State:\n");
//        sb.append("Current Player: ").append(currentPlayer.toString()).append("\n");
//        sb.append("Last Character Move: ").append(lastCharacterMove).append("\n");
//        sb.append("Players:\n");
//        for (Player player : players) {
//            sb.append(player.toString()).append("\n");
//        }
//        sb.append("Deck:\n").append(deck.toString());
//        return sb.toString();
//    }

    public String getPlayerStats(Player player) {
        return player.toString();
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }


    public void turn(Player player) {

    }

}

