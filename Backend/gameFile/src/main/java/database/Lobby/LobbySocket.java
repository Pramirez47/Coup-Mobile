package database.Lobby;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.util.JSONPObject;
import database.Chat.MessageRepository;
import database.Game.Game;
import database.Game.Player;
import database.Users.User;
import database.Users.UserRepository;
import database.Websocketconfig.WebsocketConfig;
import jakarta.persistence.Lob;
import jakarta.transaction.Transactional;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import jakarta.websocket.OnOpen;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.json.JSONObject;


@Controller      // this is needed for this to be an endpoint to springboot
@ServerEndpoint(value = "/lobby/{lobbyId}/{username}")  // this is WebSocket URL
public class LobbySocket {

    private static LobbyRepository lobbyRepository; //Lobby Repo


    private static UserRepository userRepository; // User Repo

    private Game game;

    @Autowired
    public void setUserRepository(UserRepository repo) {
        userRepository = repo;  // we are setting the static variable

    }
    @Autowired
    public void setLobbyRepository(LobbyRepository repo) {
        lobbyRepository = repo;  // we are setting the static variable
    }
    private final Logger logger = LoggerFactory.getLogger(LobbySocket.class);

    private static Map<Session, Lobby> sessionLobbyMap = new Hashtable<>(); // Associate a Sessions with Lobbys to find lobbies and to terminate lobbies
    private static Map<Session,User> sessionUserMap = new HashMap<>(); // Associate a Session with Users to find users to remove and add them

    private static Map<User,Session> userSessionMap = new HashMap<>(); // Associate a Session with Users to find users to remove and add them


    @OnOpen
    public void onOpen(Session session, @PathParam("lobbyId") int lobbyId, @PathParam("username") String username) throws IOException {
        User user = userRepository.findByUserEmail(username); // find user
        sessionUserMap.put(session, user);
        userSessionMap.put(user,session);
        if(lobbyId == 0){ //USER WANTS TO CREATE LOBBY
            Lobby newLobby = new Lobby(); // Create new Lobby
            newLobby.addUser(user); // add user
            sessionLobbyMap.put(session,newLobby); // Save Session with lobby
            lobbyRepository.save(newLobby); // Save lobby for admin use
            broadcastToSpecificLobby(newLobby.getId(), "The ID is: " + newLobby.getId());
//            broadcastToSpecificLobby(newLobby.getId(),user.getUserEmail() + " Joined the lobby");
            broadcastToSpecificUser(user.getUserEmail(),"Users: " + newLobby.getUsers());
            logger.info(newLobby.toString());
        }else{ //USER WANTS TO JOIN LOBBY
            Lobby existingLobby = lobbyRepository.findById(lobbyId); // Find Lobby By ID
            if(!existingLobby.isFull()) {
                existingLobby.addUser(user); // Add User to this Lobby
                lobbyRepository.save(existingLobby); // Save Lobby
//                broadcastToSpecificLobby(existingLobby.getId(),user.getUserEmail() + " Joined the lobby");
                for(User user1 : existingLobby.getUserArraylist()) {
                    broadcastToSpecificUser(user1.getUserEmail(),user.getUserEmail() + "Joined the lobby");
                }

                sessionLobbyMap.put(session, existingLobby);
                broadcastToSpecificUser(user.getUserEmail(),existingLobby.getUsers());
                if(existingLobby.isFull()){ //START GAME


                    /**
                     * INIT GAME
                     */
//                    broadcastToSpecificLobby(existingLobby.getId(),"lobby is full");

                    for(User user1 : existingLobby.getUserArraylist()) {
                        broadcastToSpecificUser(user1.getUserEmail(),"lobby is full");
                    }

                    List<Player> players = new ArrayList<>(4); // Create an Array list of Players

                    game = new Game(players); //Pass in Deck and Array List
                    game.initGame(existingLobby.getUserArraylist().get(0).getUserEmail(),
                            existingLobby.getUserArraylist().get(1).getUserEmail(),
                            existingLobby.getUserArraylist().get(2).getUserEmail(),
                            existingLobby.getUserArraylist().get(3).getUserEmail()); // Sends four players, see init game method

                    //For loop for normal String
//                    for(User printGameState : existingLobby.getUserArraylist()) {
//                        Player player = game.getPlayer(printGameState.getUserEmail());
//                        broadcastToSpecificUser(printGameState.getUserEmail(), game.getPlayerStats(player));
//                    }

                    for (Player printGameState : game.getPlayerArrayList()) { //Itterate through Lobby
                        Player player = game.getPlayer(printGameState.getUserEmail()); // Find Player
                        if (player != null) {
                            broadcastToSpecificUserJSON(printGameState.getUserEmail(), printGameState); //broadcast to User the Their Player JSON Object
                        }
                    }



                    /**
                     * INIT GAME
                     */


                }

            }else{
//                broadcastToSpecificLobby(existingLobby.getId(),"lobby is full");
                for(User user1 : existingLobby.getUserArraylist()) {
                    broadcastToSpecificUser(user1.getUserEmail(),"lobby is full");
                }
            }
        }
    }


    @OnMessage
    public void onMessage(Session session, String message,  @PathParam("username") String username) throws IOException {
       try {
           //Todo Make onMessage
//        JSONObject jsonpObject = new JSONObject(message);

           game.getCurrentPlayer().setCurrentMove("Income");
           game.setLastCharacterMove("Income");
           game.turn(game.getCurrentPlayer(), game.getCurrentPlayer().getCurrentMove());


           for (Player printGameState : game.getPlayerArrayList()) { //Itterate through Lobby
               if (printGameState != null) {
                   broadcastToSpecificUserJSON(printGameState.getUserEmail(), printGameState); //broadcast to User the Their Player JSON Object
               }
           }

       }catch (Exception e){
           e.printStackTrace();
       }

    }

    @OnClose
    public void onClose(Session session, @PathParam("lobbyId") int lobbyId, @PathParam("username")String username) {
        /**
         * ToDo: Make it so it closes the session for user, but not lobby
         */

        Lobby lobby = lobbyRepository.findById(userRepository.findByUserEmail(username).getLobby().getId()); //Find Lobby

        User user = userRepository.findByUserEmail(username); //Find UserName
        lobby.removeUser(user);
        sessionUserMap.remove(session);
        userSessionMap.remove(user);


        logger.info(lobby.toString());
        logger.info(lobby.toString());
        logger.info(lobby.toString());

        lobbyRepository.save(lobby); // Save Lobby
        sessionUserMap.remove(session); // Remove Users Session

        for(User user1 : lobby.getUserArraylist()) {
            broadcastToSpecificUser(user1.getUserEmail(),"THIS USER HAS LEFT:" + user.getUserEmail());
        }

        ////NEW CODE NEW CODE
        if(lobby.isEmpty()){
            sessionLobbyMap.remove(session);
            lobbyRepository.delete(lobby);
            lobbyRepository.save(lobby);
        }
        ////NEW CODE NEW CODE
    }




    private void broadcastToAllUsers(String message) { // This method broadcasts to all user
        sessionUserMap.forEach((session, user) -> {
            try {
                session.getBasicRemote().sendText(message);
            }
            catch (IOException e) {
                logger.info("Exception: " + e.getMessage().toString());
                e.printStackTrace();
            }
        });
    }

    private void broadcastToSpecificLobby(int lobbyId, String message) {
        // Iterate over the sessionLobbyMap to find sessions associated with the specific lobbyId
        sessionLobbyMap.forEach((session, lobby) -> {
            // Check if the lobby matches the lobbyId we want to broadcast to
            if (lobby.getId() == lobbyId) {
                try {
                    // Use the session to send the message
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    logger.error("Error broadcasting to specific lobby: " + e.getMessage(), e);
                }
            }
        });
    }


//    private void broadcastToSpecificUserJSON(String userEmail, Object data) {
//        ObjectMapper mapper = new ObjectMapper();
//
//        try {
//            String message = mapper.writeValueAsString(data);
//            Session userSession = getSessionByEmail(userEmail);
//            if (userSession != null) {
//                userSession.getBasicRemote().sendText(message);
//            }
//        } catch (IOException e) {
//            logger.error("Error broadcasting to specific user: " + e.getMessage(), e);
//        }
//    }


    private void broadcastToSpecificUserJSON(String userEmail, Player data) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("player", data);

        try {
            String message = mapper.writeValueAsString(wrapper);
            Session userSession = getSessionByEmail(userEmail);
            if (userSession != null) {
                userSession.getBasicRemote().sendText(message);
            }
        } catch (IOException e) {
            logger.error("Error broadcasting to specific user: " + e.getMessage(), e);
        }
    }
    private Session getSessionByEmail(String userEmail) {
        for (Map.Entry<Session, User> entry : sessionUserMap.entrySet()) { //Looks through userSessionMap
            if (entry.getValue().getUserEmail().equals(userEmail)) { //Finds Match with userEmail
                return entry.getKey(); //Returns Session
            }
        }
        return null;
    }



    private void broadcastToSpecificUser(String userEmail, String message) {
        // Iterate over the sessionUserMap to find the session associated with the userEmail
        sessionUserMap.forEach((session, user) -> {
            // Check if the user matches the userEmail we want to send a message to
            if (user.getUserEmail().equals(userEmail)) {
                try {
                    // Use the session to send the message
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    logger.error("Error broadcasting to specific user: " + e.getMessage(), e);
                }
            }
        });
    }





}
