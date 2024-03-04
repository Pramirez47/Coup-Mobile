package onetoone.Users;

import onetoone.Friends.Friend;
import onetoone.Friends.FriendRepository;
import onetoone.Profiles.Profile;
import onetoone.Profiles.ProfileRepository;
import onetoone.Setting.Setting;
import onetoone.Setting.SettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Random;

/**
 * 
 * @author Charles Arroyo
 * 
 */ 

@RestController
public class UserController {



    @Autowired
    UserRepository userRepository; //Creating a repository(mySQL of users)

    @Autowired
    ProfileRepository profileRepository; //Creating a repository(mySQL of profiles)

    @Autowired
    FriendRepository friendRepository; // //Creating a repository(mySQL of Friends)

    @Autowired
    SettingRepository settingRepository;
    private String success = "{\"success\":true}"; //Sends a JSON boolean object named success

    private String failure = "{\"message\":\"failure\"}"; //Sends a JSON String object named message


    /**
     * Gets all users in the user repo
     * @return
     */
    @GetMapping(path = "/users")
    List<User> getAllUsers() {
        return userRepository.findAll();

    }



    /**
     * Gets a user based on unique ID
     * @param id
     * @return
     */
    @GetMapping(path = "/users/{id}")
    User getUserById(@PathVariable int id) {
        return userRepository.findById(id);
    }


    // Example method for generating a setting ID. Implementation depends on your requirements.
    private Integer generateSettingId() {
        // Generate a unique ID for the user setting. This is just a placeholder.
        // The actual implementation would depend on how you're managing IDs.
        return new Random().nextInt();
    }

    /**
     *
     * @param user
     * @return
     */
    @PostMapping(path = "/signup")
    String signUp(@RequestBody User user) {
        if (user != null) { //user is not null
            Integer settingId = generateSettingId();
            userRepository.save(user); //Create User and Save

            Setting userSetting = new Setting();
            userSetting.setId(settingId);

            // Set the userSetting on the user object
            user.setSetting(userSetting);

            return success;
        }else{ //Null
            return failure; //Return a Failure
        }
    }

    @PostMapping(path = "/addFriend")
    String addFriend(@RequestBody User user) {
        User foundUser = userRepository.findByEmailId(user.getEmailId()); // Creates a user object
            
            return success;

    }


    /**
     * Checks the repo, and allows user to sign in
     * @param user
     * @return
     */
    @PostMapping(path = "/signin")
    public String signIn(@RequestBody User user) { //sends a request body of password & username
        User foundUser = userRepository.findByEmailId(user.getEmailId()); // Creates a user object with the users email passed in
        if (foundUser != null && foundUser.getPassword().equals(user.getPassword())) {
            return success;
        }else{
           return failure;
        }
    }

    /**
     * This method finds an existing user, and updates to change username/password. This can be used for
     * a user settings
     * @param id
     * @param request
     * @return
     */

    @PutMapping("/users/{id}")
    User updateUser(@PathVariable int id, @RequestBody User request){
        User user = userRepository.findById(id);
        if(user == null)
            return null;
        userRepository.save(request);
        return userRepository.findById(id);
    }

    /**
     * This assigns a profile object to a user.
     * @param userId
     * @param profileId
     * @return
     */
    @PutMapping("/users/{userId}/profiles/{profileId}")
    String assignProfileToUser(@PathVariable int userId,@PathVariable int profileId){
        User user = userRepository.findById(userId);
        Profile profile = profileRepository.findById(profileId);
        if(user == null || profile == null) {
            return failure;
        }else {
            profile.setUser(user);
            user.setProfile(profile);
            userRepository.save(user);
            return success;
        }
    }

    /**
     * This assigns friend to a user
     * @param userId
     * @param friendId
     * @return
     */

        @PutMapping("/users/{userId}/friends/{friendId}")
        String assignFriendToUser(@PathVariable int userId,@PathVariable int friendId) {
            User user = userRepository.findById(userId);
            Friend friend = friendRepository.findById(friendId);
            if (user == null || friend == null){
                return failure;
            }else
            {
                friend.setUser(user);
                user.addFriends(friend);
                userRepository.save(user);
                return success;
            }



    }

    public User createUser(User user) {
        // Create and set the User's Setting here
        Setting setting = new Setting();
        // Initialize setting properties if needed

        user.setSetting(setting); // Associate setting with user
        setting.setUser(user); // Link back the user to the setting, if bidirectional

        settingRepository.save(setting); // Persist the setting
        return userRepository.save(user); // Save the user, which now includes the setting
    }

    /**
     * Deletes a user, can be used in the user setting ss
     * @param id
     * @return
     */
    @DeleteMapping(path = "/users/{id}")
    String deleteUser(@PathVariable int id){
        userRepository.deleteById(id);
        return success;
    }
}
