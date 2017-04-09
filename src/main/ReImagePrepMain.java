
import Tools.*;
import Tools.Console;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.gmail.model.Message;
import com.google.common.collect.Lists;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;

import javax.mail.MessagingException;
import javax.swing.*;


import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;

@SuppressWarnings("ALL")
public class ReImagePrepMain extends Application {

    //ALWAYS MAKE THE APPLICATION NAME FIRST
    private static final String APP_NAME="SAEM:ReImage-Prep";


    //then define a directory to store your files
    private static final java.io.File DATA_STORE_DIR=new java.io.File(
            System.getProperty("user.home"),".credentials/driveCreds"
    );
    //Remeber system.getproperty

    //create data store factory to handle credential storage

    private static FileDataStoreFactory DATA_STORE_FACTORY;
    private static final JsonFactory JSON_FACTORY= JacksonFactory.getDefaultInstance();
    private static HttpTransport TRANSPORT;
    private static Credential credential;

    //define scopes for permissions
    private static final List<String> SCOPES =
            Arrays.asList(DriveScopes.DRIVE_FILE,DriveScopes.DRIVE);

    static{
        try {
            //init the transport and data store
            TRANSPORT= GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);

        }catch(Exception e){
            System.out.println(e.getLocalizedMessage());
        }
    } //declare Transport and Data Store

    private static Drive drive; //our global drive, not created yet
    private static java.io.File fileZip; //for later as i need to pass it to the DriveHandler Task

    private static  boolean uploadInProgress=false; //will need for my upload progress checker thread; (Will likely consolodate);

    //Declare this global so i can hide and show it whenever
    private static Stage consoleWindow;


    private static Drive getDrive() throws IOException{
        InputStream jIn= ReImagePrepMain.class.getResourceAsStream("client_secret.json");

        //choose to keep or discard credentials (debug)


        /*int choice =JOptionPane.showConfirmDialog(null,"Save session? (Typically no)","Save Session?",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
        if(choice==JOptionPane.NO_OPTION){
            File temp=new File(DATA_STORE_DIR.getAbsolutePath()+"\\StoredCredential");
            temp.deleteOnExit();
        }
        */





        //load credential using my auth method
        credential=null;
        try {
             credential = GoogleAPITools.authorize(jIn, TRANSPORT, JSON_FACTORY, DATA_STORE_FACTORY, SCOPES);
        }catch(Exception e){
            System.err.println("\n\n Error has occured ->"+e.getLocalizedMessage());
        }
        //start a drive instance and return it (Using Drive.builder)
        return new Drive.Builder(TRANSPORT,JSON_FACTORY,credential)
                .setApplicationName(APP_NAME).build();

    }




    public static void main(String[] args){
        launch(args);
    }

    public void start(Stage primaryStage){

        try{


           //Begin User GUI
            boolean ready=false;
            BorderPane root=new BorderPane();



            //create console and console stage;
            TextArea consoleText=new TextArea();
            //style
            consoleText.setWrapText(true);
            consoleText.setEditable(false);
            consoleText.setPrefSize(400,400);
            consoleText.setStyle("-fx-text-fill: midnightblue");




            Console consoleOut=new Console(consoleText); //create custom "console output stream"

            PrintStream ps=new PrintStream(consoleOut);
            //set output channels for system Out and Error messages
            System.setOut(ps);
            System.setErr(ps);

            consoleWindow=new Stage();
            Pane  consolePane=new Pane();
            consolePane.getChildren().add(consoleText); //finally add text area
            Scene consoleScene=new Scene(consolePane);
            consoleWindow.setScene(consoleScene);



            System.out.println("Nothing here yet....");

            //debug mode?
            boolean DEBUG_MODE=false;
            if(DEBUG_MODE){
                consoleWindow.show();


                        secondProccess(consoleOut);

                JOptionPane.showMessageDialog(null,"Done");

                System.exit(0);

            } //debug instructions yadayada

            Button start=new Button("Click Here To Begin!");
            start.setStyle("-fx-background-color: black;-fx-text-fill: white;-fx-border-color: white");
            root.setBottom(start);

            Label intro=new Label("Hello, welcome to the Preperation Tool! \n" +
                    "This program will gather all your files and upload them to your Google Drive,\n" +
                    "as well as take a list of your printers, software and at the end if there's anything we missed, " +
                    "such as any special software you may need in the future or a new printer coming that isn't hooked up at the moment..." +
                    "\n You can let us know then");
            intro.setTextAlignment(TextAlignment.CENTER);
            intro.setStyle("-fx-background-color: black; -fx-wrap-text: true ;-fx-border-color: white; -fx-text-fill: white");
            root.setCenter(intro);

            root.setStyle("-fx-background-color: black");


            root.setPrefSize(250,250);
            Scene introScene=new Scene(root);
            primaryStage.setScene(introScene);

            primaryStage.setResizable(false);
            primaryStage.setOnCloseRequest((t) -> System.exit(0));
            primaryStage.setTitle("ReImage Prep @SAEM Technical Support");
            primaryStage.show();

            consoleWindow.setOnCloseRequest((t) -> System.exit(0));
            consoleWindow.setTitle("Log -> ReImage Prep @SAEM Technical Support");






            start.setOnAction((t)->{
                try {
                    consoleWindow.show();
                    System.out.print("Starting....");
                    JOptionPane.showMessageDialog(null,"You will be notified to sign into your Drive account. \n" +
                            "Don't worry! This is a simple authorization request securely between you and Google... \n " +
                           "Please note there is a 60 sec timeout, the app will open your default browser! ",
                            "Begin... @SAEM Technical Support-ReImage Prep",JOptionPane.INFORMATION_MESSAGE);
                    primaryStage.hide();
                    System.out.println("I'll display things here to keep you updated... \n");
                    startProccess(consoleOut);
                }catch(InterruptedException | IOException ie ){
                    System.out.println("Error -> "+ ie.getLocalizedMessage());
                }
            });







        }catch(Exception ioe){
            System.out.println("Exception Occured -> " +ioe.getLocalizedMessage());
        }
    }

    private static boolean authenticated=false;

    private static void startProccess(Console c) throws IOException, InterruptedException{
        //time out thread

        Thread timeoutThread=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(60000);
                    if(!authenticated) {
                        JOptionPane.showMessageDialog(null, "Timeout Error (Or Default Browser Not Responding...) ! For security reasons the program" +
                                " will now Exit! \n Please Restart...", "Authentication Timeout",JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    }

                }catch(Exception e){
                    System.out.println(e.getLocalizedMessage());
                }

            }

        });

        timeoutThread.setDaemon(true);
        timeoutThread.start();

        //call drive, hopefully credentials are valid
        drive=getDrive();
        //for our timeout thread
        authenticated=true;
        //now we have our drive instance
        if(drive !=null) {

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success @SAEM Technical Support");
            alert.setHeaderText("Drive Succesfully Linked, Click Ok to Begin!");
            String s = "" +
                    "Note: When a stage Completes, I'll pop up and let you know! " +
                    "\\n Most work will be done in the background! \"";
            alert.setContentText(s);
            alert.initOwner(consoleWindow);
            alert.showAndWait();
        }






        consoleWindow.hide();

        //I really should put this into a method right?? I Will do this in the future!! lol
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ReImage Prep @SAEM Technical Support");
        alert.setHeaderText(" Begin Proccessing File");
        String s = "I will now gather your files from Desktop and Downloads! \n" +
                "Note: Document Files should be stored on Network Drive!";
        alert.setContentText(s);
        alert.initOwner(consoleWindow);
        alert.showAndWait();


        //time to start the actual work
        //gather files and store to common dir .output
        gatherUserFiles();


        //Files should be copied by this point
        //zip the files
        String home=System.getProperty("user.home")+"\\.output";
        java.io.File homePath=new java.io.File(System.getProperty("user.home")+"\\.output");
        homePath.deleteOnExit();
        java.io.File fileZip=new java.io.File(home+"\\files.zip");
        DirTools.zipDir(homePath,fileZip,null);


        //now begin upload to drive

        Alert alert1 = new Alert(Alert.AlertType.INFORMATION);
        alert1.setTitle("ReImage Prep @SAEM Technical Support");
        alert1.setHeaderText("File Processing Complete");
        alert1.setContentText("I will now upload to a zip to a folder on your Google Drive! ->" +drive.getBaseUrl());
        alert1.initOwner(consoleWindow);
        alert1.showAndWait();
        uploadInProgress=true;
        consoleWindow.setTitle("Uploading... Not Frozen!");
        consoleWindow.show();
        handleUpload(new File(System.getProperty("user.home")).getName(), fileZip, c);


        //end of start sequence advance to secondary

    }

    //second set of instructions for program to run through, executed by the Drive upload after its over and the thread done.

    private static void secondProccess(Console c) throws IOException,InterruptedException {
        //since we should not have to deal with the google api here I will not use java.io.File but rather just File
        consoleWindow.setTitle("Log -> ReImage Prep @SAEM Technical Support");
        c.append("\n Logging Programs... \n");

        //Gather Printers, output to file
        PrintQuery printService = new PrintQuery();

        java.io.File printLog = new java.io.File(System.getProperty("user.home"), "printers.txt");

        //utilize my class to generate a printer log
        printService.printList(printLog);

        //Create of Program Files!

        //get root drive
        File root = new File(System.getProperty("user.dir"));

        String mainDrive = root.getAbsolutePath().substring(0, 2);
        //will only work on 64 bit for now, unless i decide to throw in a safe guard
        String programFiles64Path = mainDrive + "\\Program Files";
        String programFiles32Path = mainDrive + "\\Program Files (x86)";
        //paths have been defined

        //create files
        File files64 = new File(programFiles64Path);
        File files32 = new File(programFiles32Path);

        //test existence;

        if (!files32.exists()) {
            Platform.runLater(() -> System.out.println("Detected 32 Bit System"));
            files32 = files64; //to set them both the same
        }

        File programLog = new File(System.getProperty("user.home"), "programList.txt");

        if (files64.exists() && files32.exists()) {
            c.append(" \n Programs Directory Found... \n");
            if (!(files64.getAbsolutePath().equalsIgnoreCase(files32.getAbsolutePath()))) {

                //Run simple method to print out this to my log File
                logDir(files64, programLog, true);
                logDir(files32, programLog, false);

            } else if (files64.getAbsolutePath().equalsIgnoreCase(files32.getAbsolutePath())) {
                //this is for 32 Bit env
                logDir(files32, programLog, true);
            }
        }



        /* Fun fact: Time to explain myself...
        This time i have opted on a class that creates and stores the dmail instance and i make calls through that class
        versus what i've done earlier by directly instancing the drive in this class this is done because i originally started this project
        off my curiousity for google Drive and googles neat API's in general, and this ended up turning into a lot more
        As I write this i already have a lot of plans for what i want from this, I'll trn it into a backup app later on likely;
        Gmail is a secondary feature and as such i want it to be seperate from the main code, it's not quite a factory pattern
        but given google has a whole extra layer of abstraction added in to create their Google App object Abstractions. It may as
        well be. All Instances of Gmail will be created through the gmail interface, although at the moment everything is static
        and it only supports one user, in the future this may change.
         */
        InputStream jIn = ReImagePrepMain.class.getResourceAsStream("client_secret.json");

        Gmail_Interface mailbox = new Gmail_Interface(APP_NAME, TRANSPORT, jIn, JSON_FACTORY, c);
        List<String> sel = new ArrayList<>();

        //define agents here, besure to add their email below!
        sel.add("Marcus Joachim");
        sel.add("Brett Bacon");
        sel.add("John Swett");

        String result="";
        boolean noAgent=true;

        do{

        Dialog<String> dialog = new ChoiceDialog<String>(sel.get(0), sel);

        dialog.initOwner(consoleWindow);
        dialog.setHeaderText("Please Choose your Agent!");

        dialog.setTitle("Choose Agent @SAEM Technical Support");

        Optional<String> choice = dialog.showAndWait();

        if (choice.isPresent()) {
            noAgent=false;
            if(choice.get().equalsIgnoreCase("Marcus Joachim")) {
               result = "Demarcus-Joachim@georgiasouthern.edu";
            }
            if(choice.get().equalsIgnoreCase("Brett Bacon")) {
                result = "bbacon@georgiasouthern.edu";
            }
            if(choice.get().equalsIgnoreCase("John Swett")) {
                result = "jswett@georgiasouthern.edu";
            }


        }
        else{
            Alert noAgentError=new Alert(Alert.AlertType.ERROR);
            noAgentError.setTitle("Choose Agent @SAEM Technical Support");
            noAgentError.setContentText("Please Choose an Agent!");
            noAgentError.setResizable(false);
            noAgentError.initOwner(consoleWindow);
            noAgentError.setHeaderText("Attention!");
        }
    }while(noAgent);




        //only dealing with single results right now but i designed the interface to work with multiple so it takes in array lists :// will fix that one day
        List<String> to=new ArrayList<>();
        to.add(result);

        List<java.io.File> attachments=new ArrayList<>();
        attachments.add(programLog);
        attachments.add(printLog);

        try {
            String userText="Nothing here...";

            Dialog<String> dialog1=new TextInputDialog();
            dialog1.setTitle("Anything that I missed? @SAEM Technical Support");

            dialog1.setResizable(false);
            dialog1.initOwner(consoleWindow);

            dialog1.setHeaderText("If there is anything, such as... \nPrograms you don't have now but need in the future... \n A Printer you dont have now, but will soon... \n " +
                    "Comment to send us or Folder i may have missed...");


            Optional<String> text=dialog1.showAndWait();

            if(text.isPresent()){
                userText=text.get();
            }




            Message message=mailbox.newMultiMessage(to,"llmuzical@gmail.com","test",
                "Results for user-> "+ new File(System.getProperty("user.home")).getName() + " Attached You Will find the Programs and Printers! \n" +
                        "If There were any comments from the user they will be displayed below.... \n\n "+ userText,attachments);

            mailbox.sendMessage(message);

        }catch(MessagingException me){
            System.err.println("\n Messaging error ->"+me.getLocalizedMessage());
        }

        Alert alert1 = new Alert(Alert.AlertType.INFORMATION);
        alert1.setTitle("ReImage Prep @SAEM Technical Support");
        alert1.setHeaderText("All Done!");
        alert1.setContentText("Thank You, This is the end of the Process, an Agent may email you Soon!");
        alert1.initOwner(consoleWindow);
        alert1.showAndWait();

        System.exit(0);


















    }

    private static int dots=0;

    private static void handleUpload(String usrPath,File file,Console c) throws IOException, InterruptedException{

        file.deleteOnExit();

        System.out.println(usrPath +"<- Current User, creating folder \""+usrPath+" Backup\" ID->");
        //define folder locally with metadata;
        com.google.api.services.drive.model.File folderMetadata=new com.google.api.services.drive.model.File();
        folderMetadata.setName(usrPath+" Backup");
        folderMetadata.setDescription("Back up files for ->"+usrPath);

        //remember tis MIME type tells google what it is pretty much, the one for folder is
        //very special, (application)/vnd.google-apps.folder)
        folderMetadata.setMimeType("application/vnd.google-apps.folder");

        //create Folder (drive).(grab files).(create)(what?).(setFields)("what should i expect this to have) [id]).(action)[execute]
        com.google.api.services.drive.model.File driveUserFolder=drive.files().create(folderMetadata).setFields("id").execute();

        //get id IMPORTANT
        String ID=driveUserFolder.getId();

        System.out.println("Folder Created");

        //declare File now
        com.google.api.services.drive.model.File zipMetadata=new com.google.api.services.drive.model.File();

        zipMetadata.setName(usrPath+"-backup.zip");
        zipMetadata.setDescription("Created on "+ new GregorianCalendar().get(Calendar.DAY_OF_WEEK_IN_MONTH));
        zipMetadata.setMimeType("application/zip");
        zipMetadata.setParents(Collections.singletonList(
                ID)); // I once read about the singleton design, idk Documentation says use singleton list


        FileContent zipContent=new FileContent("application/zip",file);

        //now that the metadata is set lets add it
        System.out.println("\n Upload Ready");
        Thread.sleep(1000);



        //start a thread to update the Console while uploading
        Task uploadProgressHandler=new Task() {
            @Override
            protected Object call() throws Exception {
                do{

                    Platform.runLater(()->System.out.println("Uploading..."));
                    Thread.sleep(1000);

                } while(uploadInProgress);
                return null;
            }
        };

        Thread uploadProccessHandlerThread=new Thread(uploadProgressHandler);
        uploadProccessHandlerThread.setDaemon(true);
        uploadProccessHandlerThread.start();
         //Execute upload
        com.google.api.services.drive.model.File zipOutToDrive= drive.files().create(zipMetadata,zipContent).setFields("id").execute();
        uploadInProgress=false; //end upload status

        System.out.println("Success");



        Thread.sleep(2000);
        try {
            JOptionPane.showMessageDialog(null,"Upload Complete, A browser window will open to Drive for you to verify!",
                    "Success... @SAEM Technical Support",JOptionPane.INFORMATION_MESSAGE);

            //open drive
            Desktop.getDesktop().browse(new URI("https://drive.google.com"));


            //begin second stage!
            secondProccess(c);

        }catch(URISyntaxException urse){
            System.out.println("\n "+urse.getLocalizedMessage());
        }




    }

    private static void copyOut(java.io.File f, String userFolderType) throws IOException{
        System.out.print(".");
        dots++;
        if(dots ==10){
            System.out.println("\n");
            dots=0;
        }
        java.io.File OUTPUT_DIRECTORY=null;
        if(userFolderType.equalsIgnoreCase("downloads")) {
            OUTPUT_DIRECTORY = new java.io.File(System.getProperty("user.home"), "\\.output\\Downloads");

        }
        if(userFolderType.equalsIgnoreCase("desktop")) {
            OUTPUT_DIRECTORY = new java.io.File(System.getProperty("user.home"), "\\.output\\Desktop");

        }

        if(f !=null) {
            if (f.isDirectory()) {
                assert OUTPUT_DIRECTORY != null; //use assert to catch a null file, although I've coded for it this is to get rid of the warning lol
                FileUtils.copyDirectoryToDirectory(f, OUTPUT_DIRECTORY);
            } else {
                assert OUTPUT_DIRECTORY != null;
                FileUtils.copyFileToDirectory(f, OUTPUT_DIRECTORY);
            }
        }




    }
    private static void gatherUserFiles() throws IOException{

        //add more directories
        java.io.File DOWNLOAD_DIRECTORY=new java.io.File(System.getProperty("user.home"),"\\Downloads");
        java.io.File DESKTOP_DIRECTORY=new java.io.File(System.getProperty("user.home"),"\\Desktop");


        File[] downloadFiles= DOWNLOAD_DIRECTORY.listFiles();
        File[] desktopFiles= DESKTOP_DIRECTORY.listFiles();
        System.out.print("Loaded Dirs");
        System.out.println("\nCopying...");


        if(downloadFiles==null || downloadFiles.length==0){
            System.out.println("No Files found in ->"+DOWNLOAD_DIRECTORY.getAbsolutePath());

        }
        else {
            for (File f : downloadFiles) {
                //System.out.println("\n"+f.getName());
                copyOut(f,"downloads");
            }
        }
        if(desktopFiles==null || desktopFiles.length==0){
            System.out.println("No Files found in ->"+DOWNLOAD_DIRECTORY.getAbsolutePath());

        }
        else {
            for (File f : desktopFiles) {
                //System.out.println("\n"+f.getName()); //debug method
                copyOut(f,"desktop");
            }
        }
    }
    private static void logDir(File fileIn,File fileOut,boolean isFirstRun) throws IOException{
        //verify it is a directory;
        if(!fileIn.isDirectory())
            throw new IOException("Not a Directory for dirLog!");

        //we know what this is by now lol;
        PrintWriter logger=new PrintWriter(fileOut);
        //give header
        if(isFirstRun)
            logger.println("<----------Begin Log---------->");

        //"actual" content
        logger.println("");

        //actual content
        for (File f: fileIn.listFiles()){
            logger.println(f.getName()+ "\n Path->"+ f.getAbsolutePath()+"\n");
        }
        logger.close();
    }





}
