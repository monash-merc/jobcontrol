package au.org.massive.strudel_web;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import au.org.massive.strudel_web.ssh.CertAuthInfo;

import com.google.gson.Gson;

/**
 * Provides key pair generation and certificate signing services
 * 
 * @author jrigby
 *
 */
public class KeyService {
	
	private static Settings settings = Settings.getInstance();
	
	private KeyService() {
		
	}
	
	/**
	 * Generates an RSA key pair
	 * 
	 * @return a key pair
	 */
	private static KeyPair generateKeyPair() {
		
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", new BouncyCastleProvider());
			kpg.initialize(2048, SecureRandom.getInstance("SHA1PRNG", "SUN"));
			return kpg.generateKeyPair();
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Converts a key that's subclassed from {@link RSAKey} to a string representation
	 * @param key
	 * @return String representation of the key
	 */
	public static <E extends RSAKey> String keyToString(E key) {
		String header = "", footer = "", keyString = "";
		if (key instanceof RSAPrivateKey) {
			// Return the private key in PEM format
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			OutputStreamWriter out = new OutputStreamWriter(buf);
			JcaPEMWriter pemWriter = new JcaPEMWriter(out);
			try {
				pemWriter.writeObject((RSAPrivateKey) key);
				pemWriter.close();
				out.close();
				keyString = new String(buf.toByteArray());
				buf.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else if (key instanceof RSAPublicKey) {
			// Return the public key in SSH format (suitable for authorized_keys)
			header = "ssh-rsa ";
			footer = " jobcontrol@"+System.currentTimeMillis();
			byte[] keyBytes;
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			RSAPublicKey rsaPubKey = (RSAPublicKey) key;
	        DataOutputStream dos = new DataOutputStream(buf);
			try {
				dos.write(new byte[] {0, 0, 0, 7, 's', 's', 'h', '-', 'r', 's', 'a'});
				dos.writeInt(rsaPubKey.getPublicExponent().toByteArray().length);
				dos.write(rsaPubKey.getPublicExponent().toByteArray());
				dos.writeInt(rsaPubKey.getModulus().toByteArray().length);
				dos.write(rsaPubKey.getModulus().toByteArray());
				dos.flush();
				buf.flush();
				keyBytes = buf.toByteArray();
				dos.close();
				buf.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			keyString = new String(Base64.encodeBase64(keyBytes));
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(header);
		sb.append(keyString);
		sb.append(footer);
		
		return sb.toString();
	}
	
	/**
	 * Registers a public key with the ssh authz server and returns a certificate
	 * @param oauthAccessToken
	 * @return certificate
	 * @throws OAuthSystemException
	 * @throws OAuthProblemException
	 * @throws UnauthorizedException
	 */
	public static CertAuthInfo registerKey(String oauthAccessToken) throws OAuthSystemException, OAuthProblemException, UnauthorizedException {
		Gson gson = new Gson();
		KeyPair kp = generateKeyPair();
		
		OAuthClientRequest apiRequest = new OAuthBearerClientRequest(settings.getSSHAPIEndpoint())
			.setAccessToken(oauthAccessToken)
			.buildQueryMessage();
		for (String headerKey : apiRequest.getHeaders().keySet()) {
			System.out.println(headerKey + ": "+apiRequest.getHeader(headerKey));
		}
		apiRequest.setHeader(OAuth.HeaderType.CONTENT_TYPE, "application/json");
		
		Map<String,String> data = new HashMap<String,String>();
		data.put("public_key", keyToString((RSAPublicKey) kp.getPublic()));
		
		apiRequest.setBody(gson.toJson(data));
		
		OAuthClient client = new OAuthClient(new URLConnectionClient());
		OAuthResourceResponse apiResponse = client.resource(apiRequest, OAuth.HttpMethod.POST, OAuthResourceResponse.class);
		
		// This happens when the user is no longer authorised
		if (apiResponse.getResponseCode() == 401) {
			throw new UnauthorizedException();
		}
		
		@SuppressWarnings("unchecked")
		Map<String,String> certificateResponse = (Map<String,String>)gson.fromJson(apiResponse.getBody(), HashMap.class);
		
		return new CertAuthInfo(certificateResponse.get("user"), certificateResponse.get("certificate"), keyToString((RSAPrivateKey) kp.getPrivate()));
		
	}
}
