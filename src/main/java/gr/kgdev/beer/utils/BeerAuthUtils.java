package gr.kgdev.beer.utils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import gr.kgdev.beer.model.exceptions.UnauthorizedException;

/**
 * Utility class providing helper methods for authentication-related operations,
 * including JWT generation, verification, and secret key creation.
 * <p>
 * This class uses HMAC SHA-256 (HS256) for signing and verifying JWT tokens.
 * All methods are stateless and thread-safe.
 */
public class BeerAuthUtils {

	/**
     * Generates a random secret key suitable for signing JWT tokens.
     * <p>
     * The secret key is created by generating a random UUID and hashing it
     * using SHA-256, producing a hexadecimal string.
     *
     * @return a randomly generated SHA-256 hexadecimal secret key
     */
	public static String generateRandomSecretKey() {
		return DigestUtils.sha256Hex(UUID.randomUUID().toString());
	}
	
	/**
     * Generates a signed JWT token for a given user.
     * <p>
     * The token uses the HS256 algorithm and includes:
     * <ul>
     *   <li>{@code sub} (subject) claim set to the provided user ID</li>
     *   <li>{@code exp} (expiration) claim based on the provided expiration time</li>
     * </ul>
     *
     * @param secretKey the secret key used to sign the JWT
     * @param expirationMinutes the token expiration time in minutes from now
     * @param userId the user identifier to set as the JWT subject
     * @return the serialized JWT token as a {@link String}
     * @throws RuntimeException if token signing fails (should not normally occur)
     */
	public static String generateJwt(String secretKey, Integer expirationMinutes, String userId) {
		return generateJwtWithClaims(secretKey, expirationMinutes, userId, Map.of());
	}
	
	public static String generateJwtWithClaims(String secretKey, Integer expirationMinutes, String userId, Map<String, Object> claims) {
		var now = Instant.now();

		var jwtBuilder = new JWTClaimsSet.Builder()
				.subject(userId)
				.claim(secretKey, userId)
				.expirationTime(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES))); // set expiration time to 1
		claims.entrySet().forEach(entry -> jwtBuilder.claim(entry.getKey(), entry.getValue()));
		var jwtClaims = jwtBuilder.build();

        var header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT)
                .build();

        var signedJWT = new SignedJWT(header, jwtClaims);

        try {
            var signer = new MACSigner(secretKey);
            signedJWT.sign(signer);

            var jwtToken = signedJWT.serialize();
            return jwtToken;
        } catch (JOSEException e) {
        	// this should never happen
            throw new RuntimeException("Could not generate jwt token", e);
        }
	}
	
	/**
     * Verifies a JWT token and returns its claims if valid.
     * <p>
     * This method performs the following checks:
     * <ul>
     *   <li>Verifies the token signature using the provided secret key</li>
     *   <li>Checks whether the token has expired</li>
     * </ul>
     *
     * @param secretKey the secret key used to verify the JWT signature
     * @param jwtToken the serialized JWT token to verify
     * @return the {@link JWTClaimsSet} extracted from the verified token
     * @throws UnauthorizedException if the token signature is invalid or the token is expired
     * @throws ParseException if the JWT token cannot be parsed
     * @throws JOSEException if a cryptographic verification error occurs
     */
	public static JWTClaimsSet verifyJwt(String secretKey, String jwtToken) throws UnauthorizedException, ParseException, JOSEException {
        var signedJWT = SignedJWT.parse(jwtToken);
        var verifier = new MACVerifier(secretKey);
        var isVerified = signedJWT.verify(verifier);
        
        if (!isVerified) {
        	throw new UnauthorizedException("Error verifying JWT token: Invalid signatue");
        }
        
        var claims = signedJWT.getJWTClaimsSet();

        if (claims.getExpirationTime() != null && claims.getExpirationTime().getTime() < System.currentTimeMillis()) {
        	throw new UnauthorizedException("Error verifying JWT token: Expired");
        }
        
		return claims;
	}
}
