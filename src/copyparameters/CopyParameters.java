/*
 *  Copyright:      Copyright 2018 (c) Parametric Technology GmbH
 *  Product:        PTC Integrity Lifecycle Manager
 *  Author:         Volker Eckardt, Principal Consultant ALM
 *  Purpose:        Custom Developed Code
 *  **************  File Version Details  **************
 *  Revision:       $Revision: 1.3 $
 *  Last changed:   $Date: 2018/05/18 02:18:19CET $
 */
package copyparameters;

import com.mks.api.Command;
import com.mks.api.Option;
import com.mks.api.response.APIException;
import com.mks.api.response.Response;
import copyparameters.api.APISession;
import copyparameters.api.ExceptionHandler;

/**
 *
 * @author veckardt
 */
public class CopyParameters {

   private static APISession session;

   /**
    * @param args the command line arguments
    * @throws com.mks.api.response.APIException
    */
   public static void main(String[] args) throws APIException {
      try {
         // TODO code application logic here
         session = new APISession(args, "IntegrityCopyParameters");
         log("Integrity Copy Parameters - V0.1\n----------------------------");

         String targetItemID = "";
         String sourceItemID = "";
         String copyFields = "";
         // String copyFromTypeName = "";
         // Boolean diffOnly = false;

         if (args.length != 0) {
            for (String arg : args) {
               if (arg.indexOf("--sourceItemID=") == 0) {
                  sourceItemID = arg.substring("--sourceItemID=".length(), arg.length());
                  // parCount++;
               }
               if (arg.indexOf("--targetItemID=") == 0) {
                  targetItemID = arg.substring("--targetItemID=".length(), arg.length());
                  // parCount++;
               }
               if (arg.indexOf("--copyFields=") == 0) {
                  copyFields = arg.substring("--copyFields=".length(), arg.length());
                  // parCount++;
               }
//            if (arg.indexOf("--copyFromType=") == 0) {
//               copyFromTypeName = arg.substring("--copyFromType=".length(), arg.length());
//               // parCount++;
//            }
//            if (arg.indexOf("--diff") == 0) {
//               diffOnly = true;
//               // parCount++;
//            }
            }
         }
         if (sourceItemID.isEmpty()) {
            log("Parameter '--sourceItemID' is required!");
            System.exit(4);
         }
         if (targetItemID.isEmpty()) {
            log("Parameter '--targetItemID' is required!");
            System.exit(5);
         }
         if (copyFields.isEmpty()) {
            log("Parameter '--copyFields' is required!");
            System.exit(6);
         }

         // Part 1: Read
         log("Reading Field Values '" + copyFields + "' from Item '" + sourceItemID + "' ...");
         Command cmd = new Command(Command.IM, "issues");
         cmd.addOption(new Option("fields", copyFields));
         cmd.addSelection(sourceItemID);
         Response response = session.runCommand(cmd);
         String copyField1 = response.getWorkItem(sourceItemID).getField(copyFields.split(",")[0]).getString();
         String copyField2 = "";
         if (copyFields.split(",").length > 1) {
            copyField2 = response.getWorkItem(sourceItemID).getField(copyFields.split(",")[1]).getString();
         }

         // Part 2: Write
         log("Writing Field Values '" + copyFields + "' to Item '" + targetItemID + "' ...");
         cmd = new Command(Command.IM, "editissue");
         cmd.addOption(new Option("field", copyFields.split(",")[0] + "=" + copyField1));
         if (copyFields.split(",").length > 1) {
            cmd.addOption(new Option("field", copyFields.split(",")[1] + "=" + copyField2));
         }
         cmd.addSelection(targetItemID);
         session.runCommand(cmd);

         log("Field Values '" + copyFields + "' written.");
      } catch (APIException ex) {
         ExceptionHandler eh = new ExceptionHandler(ex);
         log("ERROR: " + eh.getMessage());
         throw ex;
      }

   }

   private static void log(String text) {
      System.out.println(text);
   }

}
