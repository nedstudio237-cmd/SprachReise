package com.sprachreise.api.dto;

public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String photoUrl;

    public AuthResponse(String accessToken, String refreshToken,
                        Long id, String email, String firstName, String lastName,
                        String role, String photoUrl) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.photoUrl = photoUrl;
    }

    public String getAccessToken()  { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public Long   getId()           { return id; }
    public String getEmail()        { return email; }
    public String getFirstName()    { return firstName; }
    public String getLastName()     { return lastName; }
    public String getRole()         { return role; }
    public String getPhotoUrl()     { return photoUrl; }
}
