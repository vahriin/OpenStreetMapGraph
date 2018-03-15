package com.company;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    private Map all_nodes = new HashMap<Long, Node_Coordinates>(); // <node_id, node_coordinates>
    private Map nodes = new HashMap<Long, Node_Coordinates>();
    private Map<Long, ArrayList<Long>> adjacency_list = new HashMap<>(); // <node_id, adjacency_node_ids>
    private HashSet roads = new HashSet<>(Arrays.asList("motorway", "motorway_link", "trunk", "trunk_link", "secondary", "secondary_link",
            "primary", "primary_link", "tertiary", "tertiary_link", "unclassified", "residential", "service", "living_street", "road"));

    public static void main(String[] args) throws Exception {
        long start_time = System.currentTimeMillis();
        new Main().run();
        long finish_time = System.currentTimeMillis();
        System.out.printf("\n" + (finish_time - start_time) + " ms");
    }

    private void run() throws Exception {
        try(StreamProcessor processor = new StreamProcessor(Files.newInputStream(Paths.get("Krasnoyarsk.osm")))) {
            while (processor.startElement("node", "way"))  // Maped all nodes to all_nodes
                all_nodes.put(Long.parseLong(processor.getAttribute("id")), new Node_Coordinates(Double.parseDouble(processor.getAttribute("lat")), Double.parseDouble(processor.getAttribute("lon"))));
            while (processor.startElement("way", "relation")) { // Maped all ways
                ArrayList temp_list = new ArrayList<Long>();
                while (processor.startElement("nd", "tag"))
                    if(all_nodes.containsKey(Long.parseLong(processor.getAttribute("ref"))))
                        temp_list.add(Long.parseLong(processor.getAttribute("ref")));

                while (processor.startElement("tag", "way"))
                    if ("highway".equals(processor.getAttribute("k")) && roads.contains(processor.getAttribute("v")) && temp_list.size() > 1){
                        for(int i = 0; i < temp_list.size(); ++i){
                            if (adjacency_list.containsKey((Long) temp_list.get(i))) {
                                if (i - 1 < 0)
                                    adjacency_list.get(temp_list.get(i)).add((Long) temp_list.get(i + 1));
                                else if (i + 1 == temp_list.size())
                                    adjacency_list.get(temp_list.get(i)).add((Long) temp_list.get(i - 1));
                                else
                                    adjacency_list.get(temp_list.get(i)).addAll(Arrays.asList((Long) temp_list.get(i - 1), (Long) temp_list.get(i + 1)));
                                } else {
                                    if (i - 1 < 0)
                                        adjacency_list.put((Long) temp_list.get(i), new ArrayList<>(Arrays.asList((Long) temp_list.get(i + 1))));
                                    else if (i + 1 == temp_list.size())
                                        adjacency_list.put((Long) temp_list.get(i), new ArrayList<>(Arrays.asList((Long) temp_list.get(i - 1))));
                                    else
                                        adjacency_list.put((Long) temp_list.get(i), new ArrayList<>(Arrays.asList((Long) temp_list.get(i - 1), (Long) temp_list.get(i + 1))));
                                }
                                nodes.put(temp_list.get(i), all_nodes.get(temp_list.get(i)));
                       }
                   }
           }
           System.out.printf(all_nodes.size() + "    " + nodes.size());
            for (Object key: nodes.keySet())
                ((Node_Coordinates) nodes.get(key)).set_coordinates();


            printCSV();
            createSVG();
        }
    }

    private void printCSV() throws Exception{
        String CSV_Nodes_File = "Nodes.csv";
        String CSV_AdjList_File = "AdjList.csv";

        FileWriter writer = new FileWriter(CSV_Nodes_File);
        CSVUtils.writeLine(writer, Arrays.asList("Node_id", "'lat' and 'lon'"), ';');
        for(Object key : nodes.keySet())
            CSVUtils.writeLine(writer, Arrays.asList(key.toString(), nodes.get(key).toString()), '>');

        writer = new FileWriter(CSV_AdjList_File);
        CSVUtils.writeLine(writer, Arrays.asList("Node_id", "adjacency_list"), ';');
        for(Object key : adjacency_list.keySet())
            CSVUtils.writeLine(writer, Arrays.asList(key.toString(), adjacency_list.get(key).toString()), '>');

        writer.flush();
        writer.close();
    }

    private void createSVG() throws Exception{
        String SVG_File = "visualization.svg";
        PrintWriter out = new PrintWriter(new FileWriter(SVG_File));
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<svg version = \"1.1\"\n" +
                "     baseProfile=\"full\"\n" +
                "     xmlns = \"http://www.w3.org/2000/svg\" \n" +
                "     xmlns:xlink = \"http://www.w3.org/1999/xlink\"\n" +
                "     xmlns:ev = \"http://www.w3.org/2001/xml-events\"\n" +
                "     height = \"100%\"  width = \"100%\">\n" +
                "     <g fill-opacity=\"0.6\" stroke=\"black\" stroke-width=\"0.5px\">");

        for(Object key : nodes.keySet()) // cx="-1035700" cy="-755900"
            out.println("       <circle r=\"1px\" fill=\"blue\"   transform=\"translate(" + ((Node_Coordinates) nodes.get(key)).get_variable("euclid_X")  + "," + ((Node_Coordinates) nodes.get(key)).get_variable("euclid_Y") + ")\" />");

        for(Object key : adjacency_list.keySet())
            for(Object value : adjacency_list.get(key))
                out.println("       <line x1=\"" + ((Node_Coordinates) nodes.get(key)).get_variable("euclid_X") + "\" y1=\"" + ((Node_Coordinates) nodes.get(key)).get_variable("euclid_Y") + "\" x2=\"" + ((Node_Coordinates) nodes.get(value)).get_variable("euclid_X") + "\" y2=\"" + ((Node_Coordinates) nodes.get(value)).get_variable("euclid_Y") + "\" style=\"stroke:rgb(0,64,255);stroke-width:0.0009\" />");

        out.print("     </g>\n" +
                "</svg>");
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
    double lat, lon, euclid_X, euclid_Y;
    final private static double R_MAJOR = 6378137.0;
    final private static double R_MINOR = 6356752.3142;
    static double X = Double.MAX_VALUE, Y = Double.MAX_VALUE;

    private double  mercX(double lon) {
        return R_MAJOR * Math.toRadians(lon);
    }

    private double mercY(double lat) {
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
        if(X > euclid_X && Y > euclid_Y) {
            X = euclid_X;
            Y = euclid_Y;
        }
    }

    void set_coordinates(){
        euclid_X = (euclid_X - X) * 1000000000000000000L;
        euclid_Y = (euclid_Y - Y) * 1000000000000000000L;
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

    StreamProcessor(InputStream is) throws XMLStreamException {
        reader = FACTORY.createXMLStreamReader(is);
    }

    boolean startElement(String element, String stop_tag) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (stop_tag != null && event == XMLEvent.END_ELEMENT && stop_tag.equals(reader.getLocalName()))
                return false;
            if (event == XMLEvent.START_ELEMENT && element.equals(reader.getLocalName()))
                return true;
        }
        return false;
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
