package au.org.massive.strudel_web.gwt.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Created by jrigby on 10/08/2015.
 */
public abstract class AjaxUtils {
    protected static <T extends JavaScriptObject> void getData(String url, final AsyncCallback<T> callback) {
        try {
            new RequestBuilder(RequestBuilder.GET, url).sendRequest(null, new RequestCallback() {

                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (response.getStatusCode() == 200) {
                        callback.onSuccess(JsonUtils.<T>safeEval(response.getText()));
                    } else {
                        callback.onFailure(new RequestException());
                    }
                }

                @Override
                public void onError(Request request, Throwable throwable) {
                    callback.onFailure(throwable);
                }

            });
        } catch (RequestException e) {
            callback.onFailure(e);
        }
    }
}
