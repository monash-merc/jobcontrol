package au.org.massive.strudel_web.jersey;

import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Form data received from the feedback form
 */
public class FeedbackDebugData {
    Map<String, Object> browser;
    String url, html, img, note;

    public static FeedbackDebugData fromJson(String json) {
        return new Gson().fromJson(json, FeedbackDebugData.class);
    }

    public byte[] getImage() {
        return Base64.decodeBase64(img.split(",")[1].getBytes());
    }

    private void appendToStringBuilder(StringBuilder sb, Object item) {
        if (item instanceof String) {
            sb.append((String) item);
        } else if (item instanceof Boolean) {
            sb.append(String.valueOf((Boolean) item));
        } else if (item instanceof List) {
            Iterator<Object> subItemIterator = ((List<Object>) item).iterator();
            appendToStringBuilder(sb, subItemIterator.next());
            while (subItemIterator.hasNext()) {
                sb.append(", ");
                appendToStringBuilder(sb, subItemIterator.next());
            }
        }
    }

    public String getBrowserInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("url: "+url+"\n");
        for (String key : browser.keySet()) {
            sb.append(key+ ": ");
            appendToStringBuilder(sb, browser.get(key));
            sb.append("\n");
        }
        return sb.toString();
    }
}
