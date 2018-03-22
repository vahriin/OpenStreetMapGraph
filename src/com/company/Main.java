package com.company;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Main {

    private Map<Long, Node_Coordinates> all_nodes = new HashMap<>(); // <node_id, node_coordinates>
    private Map<Long, Node_Coordinates> nodes = new HashMap<>();
    private Map<Long, LinkedHashSet<Long>> adjacency_list = new HashMap<>(); // <node_id, adjacency_node_ids>
    private HashSet<String> roads = new HashSet<>();
    private static final String FILENAME = "Krasnoyarsk.osm";

    public static void main(String[] args) throws Exception {
        long start_time = System.currentTimeMillis();
        new Main().run();
        long finish_time = System.currentTimeMillis();
        System.out.printf("\n" + (finish_time - start_time) + " ms");
    }

    private void run() throws Exception {

        BufferedReader input = new BufferedReader(new FileReader("RoadOfInterest.txt"));
        StringTokenizer tokenizer = new StringTokenizer(input.readLine(), " ");
        while(tokenizer.hasMoreTokens())
            roads.add(tokenizer.nextToken());
        input.close();

        try(StreamProcessor processor = new StreamProcessor(Files.newInputStream(Paths.get("Krasnoyarsk.osm")))) {
            processor.startElement("bounds", "node");
            Node_Coordinates.X = Node_Coordinates.mercX(Double.parseDouble(processor.getAttribute("minlon")));
            Node_Coordinates.Y = Node_Coordinates.mercY(Double.parseDouble(processor.getAttribute("minlat")));

            while (processor.startElement("node", "way"))   // Maped all nodes to all_nodes
                all_nodes.put(Long.parseLong(processor.getAttribute("id")), new Node_Coordinates(Double.parseDouble(processor.getAttribute("lat")), Double.parseDouble(processor.getAttribute("lon"))));

            int count = 0;
            while (processor.startElement("way", "relation")){ // Maped all ways
                ArrayList<Long> temp_list = new ArrayList<>();

                while (processor.startElement("nd", "tag"))
                    if (all_nodes.containsKey(Long.parseLong(processor.getAttribute("ref"))))
                        temp_list.add(Long.parseLong(processor.getAttribute("ref")));

                while (processor.startElement("tag", "way"))
                    if ("highway".equals(processor.getAttribute("k")) && roads.contains(processor.getAttribute("v")) && temp_list.size() > 1) {
                        count++;
                        for (int i = 0; i < temp_list.size(); ++i) {
                            adjacency_list.putIfAbsent(temp_list.get(i), new LinkedHashSet<>((i - 1 < 0) ? Collections.singletonList(temp_list.get(i + 1)) : (i + 1 == temp_list.size()) ? Collections.singletonList(temp_list.get(i - 1)) : Arrays.asList(temp_list.get(i - 1), temp_list.get(i + 1))));
                            nodes.put(temp_list.get(i), all_nodes.get(temp_list.get(i)));
                        }
                    }
            }
            System.out.printf(all_nodes.size() + "    " + nodes.size() + "             count = " + count);
            nodes.forEach((k, v) -> v.set_coordinates());

            printCSV();
            createSVG();
        }
    }


    private void printCSV() throws Exception{
        String CSV_Nodes_File = "Nodes.csv";
        String CSV_AdjList_File = "AdjList.csv";

        PrintWriter writer = new PrintWriter(new FileWriter(CSV_Nodes_File));
        CSVUtils.writeLine(writer, Arrays.asList("Node_id", "'lat' and 'lon'"), ';');
        for(Long key : nodes.keySet())
            CSVUtils.writeLine(writer, Arrays.asList(key.toString(), nodes.get(key).toString()), '>');

        writer = new PrintWriter(new FileWriter(CSV_AdjList_File));
        CSVUtils.writeLine(writer, Arrays.asList("Node_id", "adjacency_list"), ';');
        for(Long key : adjacency_list.keySet())
            CSVUtils.writeLine(writer, Arrays.asList(key.toString(), adjacency_list.get(key).toString()), '>');

        writer.flush();
        writer.close();
    }

    private void createSVG() throws Exception{
        // input file
        BufferedReader input = new BufferedReader(new FileReader("PreparationOfSVG.txt"));
        String str = input.readLine();
        Pattern regular_exp = Pattern.compile("\\s*<?/\\w*>");
        Matcher cheker = regular_exp.matcher(str);
        // output file
        String SVG_File = "visualization.svg";
        PrintWriter out = new PrintWriter(new FileWriter(SVG_File));
        while (!cheker.matches()){ // general rules
            out.println(str);
            str = input.readLine();
            cheker = regular_exp.matcher(str);
        }
        nodes.forEach((k,v) -> out.println("\t\t<circle r=\"0.5px\" fill=\"blue\"   transform=\"translate(" + v.get_variable("euclid_X") + "," + v.get_variable("euclid_Y") + ")\"/>"));
        adjacency_list.forEach((k1, v1) -> v1.forEach(v2 -> { out.println("\t\t<line x1=\"" + nodes.get(k1).get_variable("euclid_X") + "\" y1=\"" + nodes.get(k1).get_variable("euclid_Y") + "\" x2=\"" + nodes.get(v2).get_variable("euclid_X") + "\" y2=\"" + nodes.get(v2).get_variable("euclid_Y") + "\" style=\"stroke:rgb(255,0,0);stroke-width:0.5\"/>"); adjacency_list.get(v2).remove(k1);}));

        do { // close tags of general rules
            out.println(str);
        }while((str = input.readLine()) != null);

        input.close();
        out.close();
    }
}

class CSVUtils {

    private static final char DEFAULT_SEPARATOR = ',';

    private static String followCVSformat(String value) {
        String result = value;
        if (result.contains("\""))
            result = result.replace("\"", "\"\"");
        return result;

    }
    static void writeLine(Writer w, List<String> values, char separators) throws IOException {
        boolean first = true;

        if (separators == ' ')
            separators = DEFAULT_SEPARATOR;

        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (!first)
                sb.append(" ").append(separators).append(" ");
            sb.append(followCVSformat(value));
            first = false;
        }
        sb.append("\n");
        w.append(sb.toString());
    }
}

class Node_Coordinates{
    private double lat, lon, euclid_X, euclid_Y;
    final private static double R_MAJOR = 6378137.0;
    final private static double R_MINOR = 6356752.3142;
    final private static double Multiplier = 1E-2;
    static double X, Y;

    static double  mercX(double lon) { return R_MAJOR * Math.toRadians(lon); }

    static double mercY(double lat) {
        if (lat > 89.5) {
            lat = 89.5;
        }
        if (lat < -89.5) {
            lat = -89.5;
        }
        double temp = R_MINOR / R_MAJOR;
        double es = 1.0 - (temp * temp);
        double eccent = Math.sqrt(es);
        double phi = Math.toRadians(lat);
        double sinphi = Math.sin(phi);
        double con = eccent * sinphi;
        double com = 0.5 * eccent;
        con = Math.pow(((1.0-con)/(1.0+con)), com);
        double ts = Math.tan(0.5 * ((Math.PI*0.5) - phi))/con;
        double y = 0 - R_MAJOR * Math.log(ts);
        return y;
    }

    Node_Coordinates(double input_lat, double input_lon){
        lat = input_lat;
        lon = input_lon;
        euclid_X = mercX(input_lon);
        euclid_Y = mercY(input_lat);
    }

    void set_coordinates(){
        euclid_X = (euclid_X - X) * Multiplier;
        euclid_Y = (euclid_Y - Y) * Multiplier;
    }

    double get_variable(String variable_name){
        switch (variable_name){
            case "lat":
                return lat;
            case "lon":
                return lon;
            case "euclid_X":
                return euclid_X;
            case "euclid_Y":
                return euclid_Y;
        }
        return 0;
    }

    @Override
    public String toString() {
        return " lat = " + lat + " lon = " + lon;
    }
}

class StreamProcessor implements AutoCloseable {
    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    private final XMLStreamReader reader;
    private boolean flag = true;
    private int event;

    StreamProcessor(InputStream is) throws XMLStreamException {
        reader = FACTORY.createXMLStreamReader(is);
    }

    boolean startElement(String element, String stop_tag) throws XMLStreamException {
        while (reader.hasNext()) {
            event = reader.next();
            if (stop_tag != null && event == XMLEvent.END_ELEMENT && stop_tag.equals(reader.getLocalName())) {
                flag = false;
            }
            if (event == XMLEvent.START_ELEMENT && element.equals(reader.getLocalName())) {
                return true;
            }
            if(event == XMLEvent.CHARACTERS){
                if(!flag){
                    flag = true;
                    return false;
                }
            }
        }
        return false;
    }

    void checker(){

    }
    String getAttribute(String name) throws XMLStreamException {
        return reader.getAttributeValue(null, name);
    }

    @Override
    public void close() {
        if (reader != null)
            try {
                reader.close();
            }catch (XMLStreamException e) {}
    }
}
