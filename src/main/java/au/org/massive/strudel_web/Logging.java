package au.org.massive.strudel_web;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by jason on 11/03/2016.
 */
public abstract class Logging {
    public static final Logger accessLogger = LogManager.getLogger("access_logger");
}
