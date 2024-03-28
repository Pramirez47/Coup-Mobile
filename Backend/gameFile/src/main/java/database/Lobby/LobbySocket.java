package database.Lobby;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import jakarta.websocket.OnOpen;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller      // this is needed for this to be an endpoint to springboot
@ServerEndpoint(value = "/lobby/{lobbyId}/{username}")  // this is WebSocket URL
public class LobbySocket {
    private static LobbyRepository lobbyRepository;

    private final Logger logger = LoggerFactory.getLogger(LobbySocket.class);

    private static Map<Session, Integer> sessionLobbyMap = new Hashtable<>(); // Maps Session to Id's

    private static Map<Integer,Session> lobbySessionmap = new Hashtable<>(); // Maps Session to Id's
    private static Map<Session, String> sessionUsernamemap = new Hashtable<>(); // Maps Session to Users
    private static Map<String, Session> userSessionmap = new Hashtable<>(); // Maps Session to Users


    @Autowired
    public void setLobbyRepository(LobbyRepository repo) {
        lobbyRepository = repo;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("lobbyId") int lobbyId, @PathParam("username") String username) throws IOException {
        if (lobbyId == 0) {  // If no lobbyId was provided, this user wants to create a new lobby.
            Lobby newLobby = new Lobby(); // Create New Lobby
            boolean notFull = newLobby.addUser(username); // Try to add the user to the lobby, returns false if full
            if(notFull){
                logger.info("User: {} connected to the WebSocket", username);
                lobbyRepository.save(newLobby);
                sessionLobbyMap.put(session, newLobby.getId());
                sessionUsernamemap.put(session, username);
                broadcast("The lobby ID is: " + newLobby.getId());
                broadcast(username + " has joined the lobby");
                broadcast("Is the lobby full?: " + newLobby.isFull());

                StringBuilder users = new StringBuilder("Users in all lobbies: ");
                if(newLobby.getUser1() != null){
                    users.append(newLobby.getUser1());
                }
                if(newLobby.getUser2() != null){
                    users.append(newLobby.getUser2());
                }
                if(newLobby.getUser3() != null){
                    users.append(newLobby.getUser3());
                }
                if(newLobby.getUser4() != null){
                    users.append(newLobby.getUser4());
                }
                broadcast(users.toString());
                }else{
                broadcast("Lobby Full");
            }
        } else {
            // If a lobbyId was provided, the user wants to join an existing lobby.
            Lobby existingLobby = lobbyRepository.findById(lobbyId); // Find lobby by ID, provided by user
            if (existingLobby != null) { //found lobby
              boolean notFull =  existingLobby.addUser(username); // Add the user to the lobby.
                if(notFull) {
                    logger.info("User: {} connected to the WebSocket", username);
                    sessionLobbyMap.put(session, lobbyId); //Why would you do it again?
                    sessionUsernamemap.put(session, username); // Put this new user into the session User
                    broadcast(username + " has joined the lobby");
                    lobbyRepository.save(existingLobby); // Update the existing lobby for the add^
                    if(existingLobby.isFull()){
                        broadcast("lobby is now full");
                    }else{
                        broadcast("Not full");
                    }
                    // Send a message to all clients in the lobby to indicate that this user has joined.
                    broadcast(username + " has joined the lobby, The ID is " +lobbyId);
                    StringBuilder users = new StringBuilder("Users in lobbies: ");
                    if(existingLobby.getUser1() != null){
                        users.append(" " + existingLobby.getUser1());
                    }
                    if(existingLobby.getUser2() != null){
                        users.append(" " + existingLobby.getUser2());
                    }
                    if(existingLobby.getUser3() != null){
                        users.append(" " + existingLobby.getUser3());
                    }
                    if(existingLobby.getUser4() != null){
                        users.append(" " + existingLobby.getUser4());
                    }
                    broadcast(users.toString());
                }else{
                    broadcast("Lobby Full");
                }
            } else {
                // If the lobby doesn't exist or is full
                session.getBasicRemote().sendText("Lobby does not exist");
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Lobby is full or does not exist"));
                return;
            }
        }
        // Store the user's session and associate it with the lobbyId.
      //  sessionLobbyMap.put(session, lobbyId);
    }


    @OnClose
    public void onClose(Session session) throws IOException {
        logger.info("Entered into Close");

        // Remove the user connection information
        String username = sessionUsernamemap.get(session); //get username by session
        Integer lobbyId = sessionLobbyMap.get(session); //get lobbyId by session


        // If username or lobbyId is null, return
        if (username == null || lobbyId == null) {
            return;
        }
        // Remove the user from the lobby
        Lobby lobby = lobbyRepository.findById(lobbyId);
        if (lobby != null) {
            lobby.removeUser(username);
            lobbyRepository.save(lobby);
            // Broadcast the message to other users in the lobby
            String message = username + " left the lobby " + lobbyId;
            broadcast(message, lobbyId);
            if(lobby.getUser1() == null){
                lobbyRepository.delete(lobby);
                lobbyRepository.save(lobby);
                sessionLobbyMap.remove(session);
            }
        }else{
//            lobbyRepository.deleteById(lobbyId);
//            lobbyRepository.delete(lobby);
//            lobbyRepository.save(lobby);
        }

        // Remove the session information
//        sessionLobbyMap.remove(session);
        sessionUsernamemap.remove(session);
    }

    private void broadcast(String message) {
        sessionLobbyMap.forEach((session, username) -> {
            try {
                session.getBasicRemote().sendText(message);
            }
            catch (IOException e) {
                logger.info("Exception: " + e.getMessage().toString());
                e.printStackTrace();
            }
        });
    }

    private void broadcast(String message, Integer lobbyId) {
        sessionLobbyMap.forEach((session, sessionLobbyId) -> {
            if (sessionLobbyId.equals(lobbyId)) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    logger.error("Error broadcasting message to session: " + session.getId(), e);
                }
            }
        });
    }

//    @OnClose
//    public void onClose(Session session) throws IOException {
//        logger.info("Entered into Close");
//        // remove the user connection information
//        String username = sessionUsernamemap.get(session);
//        Integer lobbyId = sessionLobbyMap.get(session);
////        lobbyRepository.deleteById(lobbyId);
//        String message = username + " left the lobby" + lobbyId;
//        broadcast(message);
//        sessionLobbyMap.remove(session);
//        sessionUsernamemap.remove(session);
////
////        if(lobbyId != null){
////            Lobby lobby = lobbyRepository.findById(lobbyId);
////            if(lobby != null){
////                lobby.removeUser(username);
////                lobbyRepository.save(lobby);
////                if(lobbyRepository.findById(lobbyId).isEmpty()){
////                    lobbyRepository.delete(lobbyRepository.findById(lobbyId));
////                    lobbyRepository.save(lobby);
////                }
////            }
////        }
//    }

//    @OnClose
//    public void onClose(Session session) throws IOException {
//        logger.info("Entered into Close");
//
//        // Remove the user connection information
//        String username = sessionUsernamemap.get(session);
//        Integer lobbyId = sessionLobbyMap.get(session);
//
//        // If username or lobbyId is null, return
//        if (username == null || lobbyId == null) {
//            return;
//        }
//
//        // Remove the user from the lobby
//        Lobby lobby = lobbyRepository.findById(lobbyId);
//        if (lobby != null) {
//            lobby.removeUser(username);
//            lobbyRepository.save(lobby);
//
//            // Broadcast the message to other users in the lobby
//            String message = username + " left the lobby " + lobbyId;
//            broadcast(message, lobbyId);
//        }
//
//        // Remove the session information
//        sessionLobbyMap.remove(session);
//        sessionUsernamemap.remove(session);
//    }




}
