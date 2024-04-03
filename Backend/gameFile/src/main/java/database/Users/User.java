package database.Users;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import database.Lobby.Lobby;
import jakarta.persistence.*;
import database.Stats.Stat;
import jakarta.persistence.*;
import database.FriendRequest.FriendRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import java.util.Objects;

/**
 * 
 * @author Charles Arroyo
 * @author Bo Oo
 * 
 */ 

@Entity
public class User {
     /*
     * The annotation @ID marks the field below as the primary key for the table created by springboot
     * The @GeneratedValue generates a value if not already present, The strategy in this case is to start from 1 and increment for each table
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @Column(unique = true)
    private String userEmail;
    private boolean ifActive;

    private String password;

    private boolean active;



//    private boolean friendRequest = false;
//
//    private boolean isOnline;




    /*
     * @OneToOne creates a relation between the current entity/table(Laptop) with the entity/table defined below it(User)
     * cascade is responsible propagating all changes, even to children of the class Eg: changes made to laptop within a user object will be reflected
     * in the database (more info : https://www.baeldung.com/jpa-cascade-types)
     * @JoinColumn defines the ownership of the foreign key i.e. the user table will have a field called laptop_id
     */

    @OneToMany(mappedBy = "requestedUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FriendRequest> receivedFriendRequests = new ArrayList<>();

    @OneToMany(mappedBy = "requestingUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FriendRequest> sentFriendRequests = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "stat_id")
    @JsonManagedReference
    private Stat stat;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "lobby_id")
    private Lobby lobby;




    public User(String name, String userEmail,int id ,String password ,int UniqueID) {
        this.name = name;
        this.userEmail = userEmail;
        this.ifActive = true;
        this.id = id;
        this.password = password;
    }

    public User() {

    }

    // =============================== Getters and Setters for each field ================================== //

    public int getId(){
        return id;
    }

    public String getPassword(){
        return password;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getUserEmail(){
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public boolean getIsActive(){
        return ifActive;
    }

    public void setIfActive(boolean ifActive){
        this.ifActive = ifActive;
    }


    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", ifActive=" + ifActive +
                '}';
    }


    public void sendFriendRequest(User targetUser) {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setRequestingUser(this);
        friendRequest.setRequestedUser(targetUser);
        sentFriendRequests.add(friendRequest);
    }

    public void removeFriendRequest(FriendRequest friendRequest) {
        sentFriendRequests.remove(friendRequest);
        receivedFriendRequests.remove(friendRequest);
    }

    public List<FriendRequest> getReceivedFriendRequests() {
        return receivedFriendRequests;
    }

    public List<FriendRequest> getSentFriendRequests() {
        return sentFriendRequests;
    }


    public List<String> getFriendRequestEmails() {
        return receivedFriendRequests.stream()
                .map(fr -> fr.getRequestingUser().getUserEmail())
                .collect(Collectors.toList());
    }

    public Stat getStat(){
        return stat;
    }
    public void setStat(Stat stat) {
        this.stat = stat;
        stat.setUser(this); // Ensure the bidirectional link is established
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return Objects.equals(userEmail, user.userEmail); // Assuming userEmail is a unique identifier.
    }

    @Override
    public int hashCode() {
        return Objects.hash(userEmail);
    }


    public Lobby getLobby() {
        return lobby;
    }

    public void setLobby(Lobby lobby) {
        this.lobby = lobby;
    }

    public boolean isActive() {
        return active;
    }

    @Transactional
    public void setActive(boolean active) {
        this.active = active;
    }

}



