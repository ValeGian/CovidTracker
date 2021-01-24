package it.unipi.dii.inginf.dsmt.covidtracker.log;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class CTLogger {
    private static CTLogger umLogger = new CTLogger();

    private static final String LOG_FILE = "log4j.properties";

    private CTLogger() {
        try {
            Properties loggerProperties = new Properties();
            loggerProperties.load(new FileReader(LOG_FILE));
            PropertyConfigurator.configure(loggerProperties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Logger getLogger(Class classToLog) {
        return Logger.getLogger(classToLog);
    }
}
