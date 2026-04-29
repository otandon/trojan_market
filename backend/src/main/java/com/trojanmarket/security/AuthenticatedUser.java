package com.trojanmarket.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

@Getter
@AllArgsConstructor
public class AuthenticatedUser implements Principal {

    private final Integer userID;
    private final String email;
    private final String username;

    @Override
    public String getName() {
        // STOMP user destinations (/user/{name}/queue/...) resolve via Principal#getName.
        // Using userID makes the broker route private queues by numeric user identifier.
        return String.valueOf(userID);
    }
}
