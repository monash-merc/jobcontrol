package au.org.massive.strudel_web.ssh;

/**
 * Contains certificate details for SSH auth
 * @author jrigby
 *
 */
public class CertAuthInfo {
	private final String userName;
	private final String certificate;
	private final String privateKey;
	
	public CertAuthInfo(String userName, String certificate, String privateKey) {
		super();
		this.userName = userName;
		this.certificate = certificate;
		this.privateKey = privateKey;
	}

	public String getUserName() {
		return userName;
	}

	public String getCertificate() {
		return certificate;
	}

	public String getPrivateKey() {
		return privateKey;
	}
}
