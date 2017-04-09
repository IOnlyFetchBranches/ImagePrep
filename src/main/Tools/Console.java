package Tools;


import javafx.application.Platform;
import javafx.scene.control.TextArea;
import java.io.IOException;
import java.io.OutputStream;

/*basic console out class by stack overflow user assylias ,
though the code was pretty simple and I could make my own version, his is the most stream lined
in terms of implementation and as such has been saved to my Tools (With a few modifications)
*/

public class Console extends OutputStream {

    private static boolean isAlive;
    private TextArea output;

    public Console(TextArea ta) {
        isAlive=true;
        this.output = ta;
    }
    public static void terminate(){
        isAlive=false;
    }
    public boolean getAlive(){
        return isAlive;
    }

    public void append(String text){
        Platform.runLater( ()->output.appendText(text) );//better method to write strings
    }


    @Override
    public void write(int i) throws IOException {
        output.appendText(String.valueOf((char) i));
    }



}