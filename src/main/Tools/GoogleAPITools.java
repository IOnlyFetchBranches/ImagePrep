package Tools;



/*
handy code for whenever I'm dealing with google apis going to add mundane tasks here
 */


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.DataStoreFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class GoogleAPITools {

    public static Credential authorize(InputStream jsonIn, HttpTransport HTTP_TRANSPORT, JsonFactory JSON_FACTORY, DataStoreFactory DATA_STORE_FACTORY, List<String> SCOPES)
            throws IOException,InterruptedException {
        // Load client secrets.
        //InputStream in = jsonIn;
        GoogleClientSecrets clientSecrets=null;
        try {
            clientSecrets =
                    GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(jsonIn));
        }catch(NullPointerException npe){
            System.err.println("Error Loading Client Secrets-> "+npe.getLocalizedMessage());
        }

        if(clientSecrets==null){
            System.err.println("Unauthorized Client Secrets, Exiting!");
            System.exit(1);
        }

        // Create authorization flow with google
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();


        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved!");
        return credential;
    }
}
