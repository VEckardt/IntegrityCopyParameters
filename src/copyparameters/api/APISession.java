package copyparameters.api;

import com.mks.api.CmdRunner;
import com.mks.api.Command;
import com.mks.api.IntegrationPoint;
import com.mks.api.IntegrationPointFactory;
import com.mks.api.Option;
import com.mks.api.response.APIException;
import com.mks.api.response.Response;
import com.mks.api.Session;
import com.mks.api.response.WorkItem;
import com.mks.api.util.MKSLogger;
import java.util.Properties;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents the Integration Point to a server. It also contains the
 * Session object
 */
public class APISession {
   // API Version

   public static final int MAJOR_VERSION = 4;
   public static final int MINOR_VERSION = 11;
   // User's current working directory
   private static final String tmpDir = System.getProperty("java.io.tmpdir");
   // System file separator
   private static final String fs = System.getProperty("file.separator");
   // Log file location
   public static String LOGFILE = tmpDir + fs + "IntegrityCopyParameters_" + getDate() + ".log";
   // Class variables used to create an API Session
   private String hostName;
   private int port;
   private String userName;
   private String password;
   // API Specific Objects
   private IntegrationPoint integrationPoint;
   private Session apiSession;
   private CmdRunner cmdRunner;
   // Log all MKS API Commands
   private static MKSLogger logger;
   Properties loggerProps;
   public boolean initatedByIntegrity = false;

   public APISession(Map<String, String> env) {
      logger = new MKSLogger(LOGFILE);
      logger.configure(getLoggerProperties());
      logger.message("Logger initialized...");
      // logger.message("MKS API Version: " + IntegrationPointFactory.getAPIVersion());
      // Create a Local Integration Point
      logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Attempting to create local Integration Point");

      logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Host: " + env.get("MKSSI_HOST"));
      logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Port: " + env.get("MKSSI_PORT"));
      logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "User: " + env.get("MKSSI_USER"));

      hostName = env.get("MKSSI_HOST");
      if (hostName != null && !hostName.isEmpty()) {
         port = Integer.parseInt(env.get("MKSSI_PORT"));
         userName = env.get("MKSSI_USER");
         try {
            logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Before createLocalIntegrationPoint");
            integrationPoint = IntegrationPointFactory.getInstance().createLocalIntegrationPoint(MAJOR_VERSION, MINOR_VERSION);
            logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Before getCommonSession");
            apiSession = integrationPoint.getCommonSession();
            logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Before createCmdRunner");
            cmdRunner = apiSession.createCmdRunner();
            initatedByIntegrity = true;
            // cmdRunner.release();
         } catch (APIException ex) {
            logger.message(MKSLogger.ERROR, MKSLogger.LOW, ex.getLocalizedMessage());
            logger.exception(ex);
            // Logger.getLogger(APISession.class.getName()).log(Level.SEVERE, null, ex);
         } catch (NullPointerException ex) {
            logger.message(MKSLogger.ERROR, MKSLogger.LOW, ex.toString());
            logger.exception(ex);
         }
      }
   }

   /**
    * Constructor for the API Session Object
    *
    * @param args
    * @param logfileName
    * @throws APIException
    */
   public APISession(String[] args, String logfileName) throws APIException {
      // Configure the logger
      LOGFILE = tmpDir + fs + logfileName + "_" + getDate() + ".log";
      // System.out.println("APISession");
      logger = new MKSLogger(LOGFILE);
      // System.out.println("APISession");
      logger.configure(getLoggerProperties());
      logger.message("Logger initialized...");
      // logger.message("MKS API Version: " + IntegrationPointFactory.getAPIVersion());
      // Create a Local Integration Point
      logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Attempting to create local Integration Point");
      logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Attempting to create api session");
      if (setParameter(args)) {
         // if (hostName != null && !hostName.isEmpty()) {
         //     logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Attempting to create Integration Point for "+hostName+":"+port);
         //     // integrationPoint = IntegrationPointFactory.getInstance().createIntegrationPoint(hostName, port, MAJOR_VERSION, MINOR_VERSION);
         // }
         // else {
         logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Attempting to create local Integration Point");
         integrationPoint = IntegrationPointFactory.getInstance().createLocalIntegrationPoint(MAJOR_VERSION, MINOR_VERSION);
         // }
         logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Integration Point created!");
         // Auto start Integrity Client
         integrationPoint.setAutoStartIntegrityClient(true);
         // Create a common Session
         apiSession = integrationPoint.createSession(userName, password);
      } else {
         logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Attempting to create local Integration Point");
         integrationPoint = IntegrationPointFactory.getInstance().createLocalIntegrationPoint(MAJOR_VERSION, MINOR_VERSION);
         logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Integration Point created!");
         // Auto start Integrity Client
         integrationPoint.setAutoStartIntegrityClient(true);
         // Create a common Session
         apiSession = integrationPoint.getCommonSession();
      }

      logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Session created!");
      // Open a connection to the MKS Integrity Server
      logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Attempting to connect to Integrity Server");
      Command siConnect = new Command(Command.IM, "connect");
      siConnect.addOption(new Option("gui"));

      cmdRunner = apiSession.createCmdRunner();
      Response res = runCommand(siConnect);
      // Initialize class variables
      hostName = res.getConnectionHostname();
      port = res.getConnectionPort();
      userName = res.getConnectionUsername();
      cmdRunner.release();
      logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Successfully connected to "
              + hostName + ':' + port + " as user " + userName);
   }

   private Boolean setParameter(String[] args) {
      // First parse the utility arguments
      int parCount = 0;
      if (args.length != 0) {
         for (String arg : args) {
            if (arg.indexOf("--user=") == 0) {
               userName = arg.substring("--user=".length(), arg.length());
               parCount++;
            }
            if (arg.indexOf("--hostname=") == 0) {
               hostName = arg.substring("--hostname=".length(), arg.length());
               parCount++;
            }
            if (arg.indexOf("--port=") == 0) {
               port = Integer.parseInt(arg.substring("--port=".length(), arg.length()));
               parCount++;
            }
            if (arg.indexOf("--password=") == 0) {
               password = arg.substring("--password=".length(), arg.length());
               parCount++;
            }
         }
      }
      return (parCount > 0);
   }

   private Properties getLoggerProperties() {
      // Initialize logger properties
      loggerProps = new Properties();

      // Logging Categories
      loggerProps.put("mksis.logger.message.includeCategory.DEBUG", "10");
      loggerProps.put("mksis.logger.message.includeCategory.WARNING", "10");
      loggerProps.put("mksis.logger.message.includeCategory.GENERAL", "10");
      loggerProps.put("mksis.logger.message.includeCategory.ERROR", "10");
      // Output Format
      loggerProps.put("mksis.logger.message.defaultFormat", "{2}({3}): {4}");
      loggerProps.put("mksis.logger.message.format.DEBUG", "{2}({3}): {4}");
      loggerProps.put("mksis.logger.message.format.WARNING", "* * * * {2} * * * * ({3}): {4}");
      loggerProps.put("mksis.logger.message.format.ERROR", "* * * * {2} * * * * ({3}): {4}");

      return loggerProps;
   }

   public MKSLogger getLogger() {
      return logger;
   }

   /**
    * This function executes a generic API/CLI Command
    *
    * @param cmd MKS API Command Object representing a CLI command
    * @return MKS API Response Object
    * @throws APIException
    */
   public Response runCommand(Command cmd) throws APIException {
      // System.out.println("APISession 1");
      logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Attempting to run " + cmd.getApp()
              + ' ' + cmd.getCommandName());
      // System.out.println("APISession 2");
      cmdRunner = apiSession.createCmdRunner();
      cmdRunner.setDefaultHostname(hostName);
      cmdRunner.setDefaultPort(port);
      cmdRunner.setDefaultUsername(userName);
      cmdRunner.setDefaultPassword(password);
      Response res = cmdRunner.execute(cmd);
      cmdRunner.release();
      logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Command " + res.getCommandString()
              + " completed with exit code " + res.getExitCode());
      return res;
   }

   /**
    * This function executes a generic API/CLI Command impersonating another
    * user
    *
    * @param cmd MKS API Command Object representing a CLI command
    * @param impersonateUser The user to impersonate
    * @return MKS API Response Object
    * @throws APIException
    */
   public Response runCommandAs(Command cmd, String impersonateUser) throws APIException {
      logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Attempting to run " + cmd.getApp()
              + ' ' + cmd.getCommandName() + " as " + impersonateUser);
      cmdRunner = apiSession.createCmdRunner();
      cmdRunner.setDefaultHostname(hostName);
      cmdRunner.setDefaultPort(port);
      cmdRunner.setDefaultUsername(userName);
      cmdRunner.setDefaultPassword(password);
      cmdRunner.setDefaultImpersonationUser(impersonateUser);
      Response res = cmdRunner.execute(cmd);
      cmdRunner.release();
      logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Command " + res.getCommandString()
              + " completed with exit code " + res.getExitCode());
      return res;
   }

   /**
    * Get WorkItem object for the named Item ID
    *
    * @param itemID
    * @return A workitem
    */
   public WorkItem getWorkItem(String itemID, String fields) {

      Command cmd = new Command("im", "issues");
      if (!isEmpty(fields)) {
         cmd.addOption(new Option("fields", fields));
      }
      // Item #1
      cmd.addSelection(itemID);
      // WorkItem wi1 = null;
      try {
         Response response = cmdRunner.execute(cmd);
         return response.getWorkItem(itemID);
      } catch (APIException ex) {
         logger.exception(APISession.class.getName(), MKSLogger.ERROR, 1, ex);
      }
      return null;
   }

   public WorkItem getBaselines(String itemID) {
      Command cmd = new Command("im", "viewissue");
      cmd.addOption(new Option("showLabels"));
      // Item #1
      cmd.addSelection(itemID);
      // WorkItem wi1 = null;
      try {
         Response response = cmdRunner.execute(cmd);
         return response.getWorkItem(itemID);
      } catch (APIException ex) {
         logger.exception(APISession.class.getName(), MKSLogger.ERROR, 1, ex);
      }
      return null;
   }

   /**
    * Terminate the API Session and Integration Point
    *
    * @throws APIException
    * @throws IOException
    */
   public void terminate() throws APIException, IOException {

      if (null != apiSession) {
         logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "Attempting to terminate API Session");
         apiSession.release();
         logger.message(MKSLogger.DEBUG, MKSLogger.LOW, "API Session terminated!");
      }

      if (null != integrationPoint) {
         //Cannot terminate the Integration Point at this time
         //integrationPoint.release();
      }
   }

   public String getHostName() {
      return hostName;
   }

   public int getPort() {
      return port;
   }

   public String getUserName() {
      return userName;
   }

   /**
    * Returns a type workitem from the current type
    *
    * @return a workitem
    */
   public String getServerInfo() {
      return userName + "@" + hostName + ":" + port;
   }

   /**
    *
    * @return
    */
   public String getAbout() {

      Command cmd = new Command("im", "about");
      try {
         Response response = this.runCommand(cmd);
         WorkItem wi = response.getWorkItem("ci");
         // releaseCmdRunner();
         String result = wi.getField("title").getValueAsString();
         result = result + "<br>Version: " + wi.getField("version").getValueAsString();
         result = result + "<br>Patch-Level: " + wi.getField("patch-level").getValueAsString();
         result = result + "<br>API Version: " + wi.getField("apiversion").getValueAsString();

         return result;
      } catch (APIException ex) {
         Logger.getLogger(APISession.class.getName()).log(Level.SEVERE, null, ex);
      }
      return "Error in getAbout";
   }

   private void log(String text) {
      System.out.println(text);
      logger.message(MKSLogger.DEBUG, MKSLogger.LOW, text);
   }

   public void log(String text, int level) {
      String str = "          ".substring(0, level - 1);
      // if (logger.getLogFile() == null) {
      //     System.out.println("(" + level + ") " + str + string);
      // } else {
      log(str + text);
   }

   public void log(String text1, String text2) {
      System.out.println(text1 + ": " + text2);
   }
    // DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
   // Date date = new Date();

   // public String getDateInfo() {
   //     return "as of " + dateFormat.format(date) + "";
   // }
   public boolean isEmpty(String s) {
      return (s == null || s.trim().isEmpty());
   }

   public String getLogFile() {
      return LOGFILE;
   }

   private static String getDate() {
      DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
      df.setTimeZone(TimeZone.getTimeZone("CET"));
      return df.format(new Date());
   }
}
