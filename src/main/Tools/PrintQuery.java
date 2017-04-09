package Tools;


import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PrintQuery implements Serializable{

    //working with the Printservice and Printservice lookup lass in javax.
   private static PrintService[] printService;


    public PrintQuery(){
        printService=PrintServiceLookup.lookupPrintServices(null,null);
    }


    public PrintService[] gather(){
        return printService;
    }

    public void printList(File printerOutput) throws IOException{
        FileOutputStream fileOut=new FileOutputStream(printerOutput);
        PrintWriter pw=new PrintWriter(fileOut);

        //header
        pw.println("Begin List of Printers... \n\n");

        //print printers
        for(PrintService printer : printService){
            pw.println("Printer Found->"+printer.toString());
        }

        pw.close();

    }
    public List<String> returnPrintersAsString() throws IOException{
        List<String> list=new ArrayList<>();
        for(PrintService printer : printService){
           list.add(printer.toString());
        }

        return list;
    }



}
