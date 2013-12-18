package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractTupleFromMannualData {

    public static void main(String[] args) throws IOException {
        String path = "data/muc34/TASK/CORPORA/dev/";
        File f = new File(path);
        if (!f.exists())
            throw new IllegalArgumentException();
        
        File[] files = f.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File arg0, String arg1) {
                return arg1.endsWith(".muc4");
            }
        });
        
//        Pattern incidentDatePat = Pattern.compile("\\d+\\.\\s+(INCIDENT: DATE)\\s+(.*)");
        Pattern incidentLocationPat = Pattern.compile("\\d+\\.\\s+(INCIDENT: LOCATION)\\s+(.*)");
        Pattern incidentTypePat = Pattern.compile("\\d+\\.\\s+(INCIDENT: TYPE)\\s+(.*)");
        Pattern incidentInstrumentPat = Pattern.compile("\\d+\\.\\s+(INCIDENT: INSTRUMENT ID)\\s+(.*)");
        
        Pattern perpIndividualPat = Pattern.compile("\\d+\\.\\s+(PERP: INDIVIDUAL ID)\\s+(.*)");
        Pattern perpOrganizationPat = Pattern.compile("\\d+\\.\\s+(PERP: ORGANIZATION ID)\\s+(.*)");
        
        Pattern physTgtPat = Pattern.compile("\\d+\\.\\s+(PHYS TGT: ID)\\s+(.*)");
        Pattern humTgtPat = Pattern.compile("\\d+\\.\\s+(HUM TGT: NAME)\\s+(.*)");
        Pattern humTgtDescPat = Pattern.compile("\\d+\\.\\s+(HUM TGT: DESCRIPTION)\\s+(.*)");
        
//        System.out.print("incident_date\t");
        System.out.print("incident_loc\t");
        System.out.print("incident_type\t");
        System.out.print("incident_instr\t");
        System.out.print("perp_ind_id\t");
        System.out.print("perp_org_id\t");
        System.out.print("phys_tgt_id\t");
        System.out.print("hum_tgt_name\t");
        System.out.println("hum_tgt_desc\t");
        for (int i = 0; i < files.length; i++) {
            File name = files[i];
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(name), "UTF-8"));
            while (br.ready()) {
                String line = br.readLine();
//                Matcher incidentDateMat = incidentDatePat.matcher(line);
//                if (incidentDateMat.find())
//                    System.out.print(incidentDateMat.group(2) + "\t");
                Matcher incidentTypeMat = incidentTypePat.matcher(line);
                if (incidentTypeMat.find())
                    System.out.print(incidentTypeMat.group(2) + "\t");
                Matcher incidentLocationMat = incidentLocationPat.matcher(line);
                if (incidentLocationMat.find())
                    System.out.print(incidentLocationMat.group(2) + "\t");
                Matcher incidentInstrumentMat = incidentInstrumentPat.matcher(line);
                if (incidentInstrumentMat.find())
                    System.out.print(incidentInstrumentMat.group(2) + "\t");
                Matcher perpIndividualMat = perpIndividualPat.matcher(line);
                if (perpIndividualMat.find())
                    System.out.print(perpIndividualMat.group(2) + "\t");
                Matcher perpOrganizationMat = perpOrganizationPat.matcher(line);
                if (perpOrganizationMat.find())
                    System.out.print(perpOrganizationMat.group(2) + "\t");
                Matcher physTgtMat = physTgtPat.matcher(line);
                if (physTgtMat.find())
                    System.out.print(physTgtMat.group(2) + "\t");
                Matcher humTgtMat = humTgtPat.matcher(line);
                if (humTgtMat.find())
                    System.out.print(humTgtMat.group(2) + "\t");
                Matcher humTgtDescMat = humTgtDescPat.matcher(line);
                if (humTgtDescMat.find())
                    System.out.println(humTgtDescMat.group(2) + "\t");
            }
            br.close();
        }
    }
    
}
