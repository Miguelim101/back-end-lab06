package edu.eci.dosw.todo.dto;

import java.util.Objects;

/**
 * tokenType
 * accessToken
 * expiresIn (segundos)
 */
public class TokenResponse {

    private String tokenType = "Bearer";
    private String accessToken;
    private long expiresIn;

    // Constructors
    public TokenResponse (String accessToken, long expiresIn){
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }

    public String getTokenType() { return this.tokenType; }
    public String getAccessToken() { return this.accessToken; }
    public long getExpiresIn() { return this.expiresIn; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenResponse that = (TokenResponse) o;

        return Objects.equals(tokenType, that.tokenType) &&
                Objects.equals(accessToken, that.accessToken) &&
                expiresIn == that.expiresIn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenType, accessToken, expiresIn);
    }
}
