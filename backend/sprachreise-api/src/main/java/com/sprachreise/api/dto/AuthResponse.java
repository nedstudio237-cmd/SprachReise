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
    private String trainerStatus;    // PENDING | APPROVED | REJECTED (null for non-trainers)
    private Long   assignedTrainerId; // for LEARNER role

    public AuthResponse(String accessToken, String refreshToken,
                        Long id, String email, String firstName, String lastName,
                        String role, String photoUrl) {
        this(accessToken, refreshToken, id, email, firstName, lastName, role, photoUrl, null, null);
    }

    public AuthResponse(String accessToken, String refreshToken,
                        Long id, String email, String firstName, String lastName,
                        String role, String photoUrl, String trainerStatus) {
        this(accessToken, refreshToken, id, email, firstName, lastName, role, photoUrl, trainerStatus, null);
    }

    public AuthResponse(String accessToken, String refreshToken,
                        Long id, String email, String firstName, String lastName,
                        String role, String photoUrl, String trainerStatus, Long assignedTrainerId) {
        this.accessToken        = accessToken;
        this.refreshToken       = refreshToken;
        this.id                 = id;
        this.email              = email;
        this.firstName          = firstName;
        this.lastName           = lastName;
        this.role               = role;
        this.photoUrl           = photoUrl;
        this.trainerStatus      = trainerStatus;
        this.assignedTrainerId  = assignedTrainerId;
    }

    public String getAccessToken()      { return accessToken; }
    public String getRefreshToken()     { return refreshToken; }
    public Long   getId()               { return id; }
    public String getEmail()            { return email; }
    public String getFirstName()        { return firstName; }
    public String getLastName()         { return lastName; }
    public String getRole()             { return role; }
    public String getPhotoUrl()         { return photoUrl; }
    public String getTrainerStatus()    { return trainerStatus; }
    public Long   getAssignedTrainerId(){ return assignedTrainerId; }
}
