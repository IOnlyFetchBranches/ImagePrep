package Tools;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import javafx.scene.control.Alert;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

//the underscore is to keep the "l" from running into the I, makes for some pretty confusing references. I know it may not be proper conventions
public class Gmail_Interface {
    private static  List<String> SCOPES=
            Arrays.asList(GmailScopes.GMAIL_COMPOSE); //Ask Google for permission to compose messages and send

    //declare our global vars
    private static HttpTransport toGoogle;
    private static JsonFactory factory;
    private static Credential token;
    private static String name;
    private static Multipart multipart; //our message

    private static DataStoreFactory STORE;
    static {
        try {
            java.io.File gmailCreds=new java.io.File(System.getProperty("user.home"),".credentials/gmailCreds");
            gmailCreds.deleteOnExit();
            STORE = new FileDataStoreFactory(gmailCreds);
        }catch (IOException ioe){
            System.out.println("Error Creating Data Store for GMAIL CREDS!");
        }
    }


    private Console logOut;


    //constructor here
    public Gmail_Interface(String appName, HttpTransport transportToGoogle, InputStream jIn, JsonFactory jsonFactory, Console logOut){
        toGoogle=transportToGoogle;
        factory=jsonFactory;
        try {
            token = GoogleAPITools.authorize(jIn, transportToGoogle, jsonFactory, STORE, SCOPES);

        }catch (IOException | InterruptedException ioe){
            System.err.println("Error With Authorization of Gmail!");
        }
        name=appName;
        this.logOut=logOut;

        if(logOut != null){
            logOut.append("Binded to console!");
        }



        //our interface is now ready to handle requests between other apps and gmail
    }



    private static Gmail getGmailService() {
        return new Gmail.Builder(toGoogle, factory, token)
                .setApplicationName(name)
                .build();
    }

    public static Message newMultiMessage(List<String> to,String from,String subject, String content, List<java.io.File> attachments) throws MessagingException{

        Properties mailProperties=new Properties();
        Session mailSession=Session.getDefaultInstance(mailProperties,null);

        //create RAW email (not encoded and thus not sendable thru gmail)

        MimeMessage email=new MimeMessage(mailSession);

        //this is going to have atachments so It will need to be a MULTIPART (a MIME type as per google documentation);
        multipart=new MimeMultipart();

        //this is the body of our message
        BodyPart bodyPart=new MimeBodyPart();
        bodyPart.setContent(content,"text/plain"); //([whut],[MIME Type])
        multipart.addBodyPart(bodyPart); //add to our multipart which is in essence, our email



        //add the files
        try {
            for(java.io.File file: attachments)
            attachFile(file,multipart);

        }catch (Exception e){
            JOptionPane.showMessageDialog(null,"Error Attaching File -> "+e.getLocalizedMessage(),"Exception Caught",JOptionPane.ERROR_MESSAGE);
        }

        //set email attributes
        email.setContent(multipart);
        email.setSender(new InternetAddress(from));


        Address[] allTo=new Address[to.size()];
        int index=0;

        for(String person: to){
            allTo[index]=new InternetAddress(person);
            index++;
        }

        email.setRecipients(javax.mail.Message.RecipientType.TO,allTo); //set who gets it lol

        //turn it into a message (If a simple message google also has createMessageWithEmail to translate in one step
        //to do this we first have to follow a few steps

        //first create a ByteArrayOutput

        ByteArrayOutputStream baos=new ByteArrayOutputStream();

        //next write out email to the baos using google's already created method
        try {
            email.writeTo(baos);
        }catch(IOException ioe){
            System.err.println("Error writing email to Stream -> "+ioe.getLocalizedMessage());
        }

        byte[] messageInBytes= baos.toByteArray(); //now we have the message in Bytes!

        //encoding happens now
        String message64= Base64.encodeBase64URLSafeString(messageInBytes); //again google provides a very simple method, thanks Google!
        //Finally create a new Message Object

        Message emailMessage=new Message();
        //set the [Raw] message as the email byte stream we encoded using googles method
        emailMessage.setRaw(message64);

        return emailMessage;
    }

    private static boolean sendingMessage=false;

    public void sendMessage(Message email){


        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ReImage Prep @SAEM Technical Support");
        alert.setHeaderText(" Authenticating G-Mail");
        String s = "After this you may be asked to authorize G-Mail access... \n " +
                "This is only to send back my generated logs, and comments to SAEM!";
        alert.setContentText(s);

        alert.showAndWait();


        //create gmail instance
        Gmail gmail=getGmailService();

        Alert alert1 = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ReImage Prep @SAEM Technical Support");
        alert.setHeaderText(" Authenticated!");
        String s1 = "Connected to Google Successfully!";
        alert.setContentText(s1);

        alert.showAndWait();


        sendingMessage=true;

        new Thread(()->{
            while(sendingMessage) {
                System.out.println("Sending Message");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    System.err.println(ie.getLocalizedMessage());
                }
            }
        }).start();
        //send out message here
        try {
            gmail.users().messages().send("me", email).execute();


            sendingMessage=false;

        }catch(IOException ioe){
            JOptionPane.showMessageDialog(null,"error -> "+ioe.getLocalizedMessage(),"lol",JOptionPane.INFORMATION_MESSAGE);
        System.err.println(ioe.getLocalizedMessage());
        }
        sendingMessage=false;

    }

    private static void attachFile(java.io.File attachment, Multipart body) throws MessagingException{

        DataSource source = new FileDataSource(attachment);
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(attachment.getName());
        multipart.addBodyPart(messageBodyPart);
    }

}
