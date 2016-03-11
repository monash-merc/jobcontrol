package au.org.massive.strudel_web.ssh;

import java.util.concurrent.TimeUnit;

/**
 * Contains certificate details for SSH auth
 *
 * @author jrigby
 */
public class CertAuthInfo {
    private final String userName;
    private final String certificate;
    private final String privateKey;
    private final Long createdTime;

    public CertAuthInfo(String userName, String certificate, String privateKey) {
        super();
        this.userName = userName;
        this.certificate = certificate;
        this.privateKey = privateKey;
        createdTime = System.currentTimeMillis();
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

    public Long getCreatedTime() {
        return createdTime;
    }

    public Long timeSinceCreated() {
        return System.currentTimeMillis() - getCreatedTime();
    }

    public String formattedTimeSinceCreated() {
        Long timeSinceCreated = timeSinceCreated();
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(timeSinceCreated),
                TimeUnit.MILLISECONDS.toMinutes(timeSinceCreated) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeSinceCreated)),
                TimeUnit.MILLISECONDS.toSeconds(timeSinceCreated) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeSinceCreated)));
    }
}
