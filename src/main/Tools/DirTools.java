package Tools;


import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("ConstantConditions")
public class DirTools {
    private static List<File> dirFiles=new ArrayList<>();






    @SuppressWarnings("ConstantConditions")
    public static void exploreDirSub(File path, Console c){
        if(!path.exists() || !path.isDirectory()){
            System.err.println("Path is invalid.");
        }
        else{
            assert path.listFiles() !=null;
            //ensures every file/path gets called through the recursive method in order
            for(File f: path.listFiles()){
                try {
                    travelDir(f,c);
                }catch(IOException ioe){
                    //System.out.println(ioe.getLocalizedMessage());
                }

            }
        }
    }







    private static void travelDir(File path, Console c) throws IOException{
        File dir=null; boolean isDirFound=false;
        int fileCount=0;

        try{
            for(int x=0;x<path.listFiles().length;x++) {
                //System.out.println(path.listFiles()[x]);//debug method
                dirFiles.add(path.listFiles()[x]);
                fileCount++;

                if(c ==null) {
                    System.out.println("Indexing Progress-> " + fileCount + "/" + path.listFiles().length +
                            " in: " + path.getAbsolutePath());
                }
                if(c !=null) {
                    c.append("Indexing Progress-> " + fileCount + "/" + path.listFiles().length +
                            " in: " + path.getAbsolutePath());
                }



                if (path.listFiles()[x].isDirectory() && !isDirFound) {
                    dir = path.listFiles()[x];
                    isDirFound = true;

                }
                if (isDirFound) {
                    isDirFound = false;
                    if (dir != null) {
                        travelDir(dir,c);
                    }

                }
            }

        }catch(NullPointerException npe){
            if(c ==null) {
                System.err.println("Error has occured ->" + npe.getLocalizedMessage() + " on File ->" + path.getAbsolutePath() + " Skipping");
            }
            else{
                c.append( "Error has occured ->" + npe.getLocalizedMessage() + " on File ->" + path.getAbsolutePath() + " Skipping");
            }

        }







    }


    private static FileOutputStream fileOut;
    private static ZipOutputStream zipOut;


    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    public static void zipDir(File pathIn, File out, Console logOut) throws IOException{
        //User friendly stuff
        JOptionPane.showMessageDialog(null,"Once you hit OK, I will Begin Finding and then Zipping all your files for Backup! \nDo not close out of the window!"+
        "\n ", "Re-Image Prep: @SAEM Tech Support",JOptionPane.INFORMATION_MESSAGE);


        //check permissions
        if(logOut==null)
        System.out.println(pathIn.canRead() +"< read? write? >"+pathIn.canWrite());

        else
            logOut.append(pathIn.canRead() +"< read? write? >"+pathIn.canWrite());


        //instance i/o

        fileOut=new FileOutputStream(out);
        zipOut=new ZipOutputStream(fileOut);


        exploreDirSub(pathIn,logOut); //find all files and store into array (of course as paths lol)

        String fullDir=dirFiles.get(0).getAbsolutePath();
        StringBuilder test=new StringBuilder();

        StringTokenizer pathSep=new StringTokenizer(fullDir,"\\");
        test.append(pathSep.nextToken()+"\\"); //go ahead and append C;\ or whatever it could be
        System.out.println("Root Drive ->"+test);

        //find highest directory;
        boolean noParent=true;
        String parentDir=null;
        while(noParent) {
            int index=0;
            for (File f : dirFiles) {
                if (f.getAbsolutePath().contains(test.toString())) {
                    index++;
                }
            }
            if(index==dirFiles.size()) {
                parentDir=test.toString();
                test.append(pathSep.nextToken()+"\\");
            }
            else{
                if(logOut ==null)
                System.out.println("Found parent->"+parentDir);
                else
                    logOut.append("Found parent->"+parentDir);
                break;
            }

        }

        File Parent=new File(parentDir);

        //zip files
        System.out.println("\n\n\n Zipping");
        int index=0;
        for(File f: dirFiles){
            index++;
            if (index % 10 == 0) {
                System.out.print(".");
            }
            if (index %1000 ==0){
                System.out.println("");
            }
            if(index %10000 ==0){
                System.out.println("Please Wait...");
            }


            String truePath=f.getAbsolutePath().substring(
                    f.getAbsolutePath().indexOf(Parent.getName()),f.getAbsolutePath().length()
            );
            if(!f.isDirectory()){
                ZipEntry ze=new ZipEntry(truePath);
                ze.setSize(f.length());
                 System.out.print(ze.getSize()+ "<-size");
                zipOut.putNextEntry(ze);
                //get CRC and set
                CRC32 crc=new CRC32();
                crc.update(Files.readAllBytes(f.toPath()));
                ze.setCrc(crc.getValue());


                zipOut.write(Files.readAllBytes(f.toPath()));

                zipOut.closeEntry();



            }
        }

        //clean up, Files Are Extracted

        zipOut.close();
        JOptionPane.showMessageDialog(null,"Files zipped!","ReImage-Prep @SAEM Technical Support",JOptionPane.INFORMATION_MESSAGE);








    }





}
