package gr.kgdev.beer.utils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
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

public class BeerAuthUtils {

	public static String generateRandomSecretKey() {
		return DigestUtils.sha256Hex(UUID.randomUUID().toString());
	}
	
	public static String generateJwt(String secretKey, Integer expirationMinutes, String userId) {
		var now = Instant.now();

		var jwtClaims = new JWTClaimsSet.Builder()
				.subject(userId) // optional
				.expirationTime(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES))) // set expiration time to 1
				.build();

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
