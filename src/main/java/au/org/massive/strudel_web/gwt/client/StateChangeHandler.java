package au.org.massive.strudel_web.gwt.client;

/**
 * Created by jrigby on 10/08/2015.
 */
public interface StateChangeHandler<T extends State> {
    void onStateChange(T oldState, T newState);
}
