package edu.washington.cs.oneswarm.test.integration.oop;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.MemoryHandler;
import java.util.logging.SimpleFormatter;

import edu.washington.cs.oneswarm.f2f.ExperimentInterface;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager;

public class LogReportingExperiment implements ExperimentInterface {
    private static Logger logger = Logger.getLogger(LogReportingExperiment.class.getName());

    @Override
    public String[] getKeys() {
        return new String[] { "sow_log", "reap_log" };
    }
    
    HashMap<String,Level> elevatedLevels = new HashMap<String,Level>();
    BufferedHandler bh;

    @Override
    public void execute(String command) {
        logger.info("lre asked to execute " + command);
        String[] toks = command.toLowerCase().split("\\s+");
        if (toks[0].equals("sow_log")) {
            LogManager manager = LogManager.getLogManager();
            // Activate Handler.
            bh = new BufferedHandler();
            
            // Elevate Levels.
            for (String logger : toks) {
                Logger l = manager.getLogger(logger);
                if (l != null) {
                    elevatedLevels.put(logger, l.getLevel());
                }
                l.setLevel(Level.ALL);
                l.addHandler(bh);
            }
        } else if (toks[0].equals("reap_log")) {
            LogManager manager = LogManager.getLogManager();
            // Return Levels.
            for (Entry<String,Level> l: elevatedLevels.entrySet()) {
                Logger logger = manager.getLogger(l.getKey());
                logger.setLevel(l.getValue());
                logger.removeHandler(bh);
            }
            elevatedLevels.clear();
            
            // Flush Handler.
            String url = toks[1];
            try {
                 HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
                 conn.setDoInput(true);
                 conn.setDoOutput(true);

                 String log = bh.getBuffer();
                 OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
                 out.write(log);
                 out.close();
                 //meh.
                 conn.getInputStream().read();
            } catch (Exception e) {
                logger.warning("Log reporting to " + url + "failed: " + e.getMessage());
            }
            bh = null;
        } else {
            logger.warning("Unknown Service command: " + toks[0]);
            return;
        }
    }

    @Override
    public void load() {
        return;
    }
    
    private class BufferedHandler extends Handler {
        private StringBuilder buffer = new StringBuilder();

        @Override
        public void close() throws SecurityException {
            buffer = null;
        }

        @Override
        public void flush() {
            //None.
        }
        
        public String getBuffer() {
            if (buffer != null) {
                return buffer.toString();
            }
            return "";
        }

        @Override
        public void publish(LogRecord record) {
            Formatter f = this.getFormatter();
            if (f == null ) {
                f = new SimpleFormatter();
                this.setFormatter(f);
            }
            buffer.append(f.format(record));
        }
    }

}
