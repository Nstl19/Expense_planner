package com.nastel.expenseplanner;

public class User {
    private String username;
    private String email;
    private String profilePic;


    public User() {}

    public User(String username, String email, String profilePic) {
        this.username = username;
        this.email = email;
        this.profilePic = profilePic;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getProfilePic() {
        return profilePic;
    }
}