package com.company;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


public class Main {

    private Map all_nodes = new HashMap<Long, Node_Coordinates>(); // <node_id, node_coordinates>
    private Map nodes = new HashMap<Long, Node_Coordinates>();
    private Map<Long, ArrayList<Long>> adjacency_list = new HashMap<>(); // <node_id, adjacency_node_ids>
    private HashSet roads = new HashSet<>(Arrays.asList("motorway", "motorway_link", "trunk", "trunk_link", "secondary", "secondary_link",
            "primary", "primary_link", "tertiary", "tertiary_link", "unclassified", "road"));

    private void run() throws Exception {
        //Scanner input = new Scanner(new File("input.txt"));
        try(StreamProcessor processor = new StreamProcessor(Files.newInputStream(Paths.get("Krasnoyarsk.osm")))) {
            XMLStreamReader reader = processor.getReader();
            while (processor.startElement("node", "way")) { // Maped all nodes
                long mid = Long.parseLong(processor.getAttribute("id"));
                //System.out.printf("id = " + processor.getAttribute("id") + "; lat= " + processor.getAttribute("lat") + "; lon= " + processor.getAttribute("lon") + "\n");
                all_nodes.put(Long.parseLong(processor.getAttribute("id")), new Node_Coordinates(Double.parseDouble(processor.getAttribute("lat")), Double.parseDouble(processor.getAttribute("lon"))));
            }
           while (processor.startElement("way", "relation")) { // Maped all ways
               ArrayList temp_list = new ArrayList<Long>();
               while (processor.startElement("nd", "tag"))
                   temp_list.add(Long.parseLong(processor.getAttribute("ref")));
               while (processor.startElement("tag", "way"))
                   if ("highway".equals(processor.getAttribute("k")) && roads.contains(processor.getAttribute("v"))){
                       for(int i = 0; i < temp_list.size(); ++i){
                           if(adjacency_list.containsKey((Long) temp_list.get(i))){
                               if(i - 1 < 0)
                                   adjacency_list.get(temp_list.get(i)).add((Long) temp_list.get(i+1));
                               else if(i + 1 == temp_list.size())
                                   adjacency_list.get(temp_list.get(i)).add((Long) temp_list.get(i-1));
                               else
                                   adjacency_list.get(temp_list.get(i)).addAll(Arrays.asList((Long) temp_list.get(i-1), (Long) temp_list.get(i+1)));
                           }else{
                               if(i - 1 < 0)
                                   adjacency_list.put((Long) temp_list.get(i), new ArrayList<>(Arrays.asList((Long) temp_list.get(i+1))));
                               else if(i + 1 == temp_list.size())
                                   adjacency_list.put((Long) temp_list.get(i), new ArrayList<>(Arrays.asList((Long) temp_list.get(i-1))));
                               else
                                   adjacency_list.put((Long) temp_list.get(i), new ArrayList<>(Arrays.asList((Long) temp_list.get(i-1),(Long) temp_list.get(i+1))));
                           }
                           nodes.put(temp_list.get(i), all_nodes.get(temp_list.get(i)));
                       }
                   }
           }
            System.out.printf(all_nodes.size() + "    " + nodes.size());
            printCSV();
            createSVG();

        }
    }

    private void printCSV() throws Exception{
        String CSV_Nodes_File = "C:/Program Files/osmosis-latest/bin/GraphOfCity/Nodes.csv";
        String CSV_AdjList_File = "C:/Program Files/osmosis-latest/bin/GraphOfCity/AdjList.csv";

        FileWriter writer = new FileWriter(CSV_Nodes_File);
        CSVUtils.writeLine(writer, Arrays.asList("Node_id", "'lat' and 'lon'"), ';', ' ');
        for(Object key : nodes.keySet())
            try {
                CSVUtils.writeLine(writer, Arrays.asList(key.toString(), nodes.get(key).toString()), ';', ' ');
            }catch (Exception e){
            }


        writer = new FileWriter(CSV_AdjList_File);
        CSVUtils.writeLine(writer, Arrays.asList("Node_id", "adjacency_list"), ';', ' ');
        for(Object key : adjacency_list.keySet()){
            CSVUtils.writeLine(writer, Arrays.asList(key.toString(), adjacency_list.get(key).toString()), ';', ' ');
        }
        writer.flush();
        writer.close();
    }

    private void createSVG() throws Exception{
        String SVG_File = "C:/Program Files/osmosis-latest/bin/GraphOfCity/visualization.svg";
        PrintWriter out = new PrintWriter(new FileWriter(SVG_File));
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<svg version = \"1.1\"\n" +
                "     baseProfile=\"full\"\n" +
                "     xmlns = \"http://www.w3.org/2000/svg\" \n" +
                "     xmlns:xlink = \"http://www.w3.org/1999/xlink\"\n" +
                "     xmlns:ev = \"http://www.w3.org/2001/xml-events\"\n" +
                "     height = \"100%\"  width = \"100%\">\n" +
                "     <g fill-opacity=\"0.6\" stroke=\"black\" stroke-width=\"0.5px\">");

        for(Object key : nodes.keySet())
            try {
                out.println("       <circle cx=\"-1035700\" cy=\"-755900\" r=\"2px\" fill=\"blue\"   transform=\"translate(" + ((Node_Coordinates) nodes.get(key)).get_variable("euclid_X") + "," + ((Node_Coordinates) nodes.get(key)).get_variable("euclid_Y") + ")\" />");
            }catch (Exception e){
            }

        for(Object key : adjacency_list.keySet())
            for(Object value : adjacency_list.get(key))
                try {
                    out.println("       <line x1=\"" + (-1035700 + ((Node_Coordinates) nodes.get(key)).get_variable("euclid_X")) + "\" y1=\"" + (-755900 + ((Node_Coordinates) nodes.get(key)).get_variable("euclid_Y")) + "\" x2=\"" + (-1035700 + ((Node_Coordinates) nodes.get(value)).get_variable("euclid_X")) +"\" y2=\"" + (-755900 + ((Node_Coordinates) nodes.get(value)).get_variable("euclid_Y")) + "\" style=\"stroke:rgb(255,0,0);stroke-width:1\" />");
                }catch (Exception e){
                }

        out.print("     </g>\n" +
                "</svg>");

        out.close();
    }

    public static void main(String[] args) throws Exception {
        long start_time = System.currentTimeMillis();
        new Main().run();
        long finish_time = System.currentTimeMillis();
        System.out.printf("\n" + (finish_time - start_time) + " ms");
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

    static void writeLine(Writer w, List<String> values, char separators, char customQuote) throws IOException {

        boolean first = true;

        if (separators == ' ')
            separators = DEFAULT_SEPARATOR;

        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (!first)
                sb.append(separators);
            if (customQuote == ' ')
                sb.append(followCVSformat(value));
            else
                sb.append(customQuote).append(followCVSformat(value)).append(customQuote);

            first = false;
        }
        sb.append("\n");
        w.append(sb.toString());
    }
}


class Node_Coordinates{
    double lat, lon, euclid_X, euclid_Y;

    Node_Coordinates(double input_lat, double input_lon){
        lat = input_lat;
        lon = input_lon;
        euclid_coordinates(lat, lon);
    }

    void euclid_coordinates(double input_lat, double input_lon){
        final double Multiplier = 100, Radius = 6378;
        double latRad, lonRad;

        latRad = input_lat * Math.PI / 180;
        lonRad = input_lon * Math.PI / 180;

        euclid_X = Multiplier * Radius * lonRad;
        euclid_Y = Multiplier * Radius * Math.log(Math.tan(Math.PI/4+latRad/2));
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

    XMLStreamReader getReader() {
        return reader;
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

    public String getAttribute(String name) throws XMLStreamException {
        return reader.getAttributeValue(null, name);
    }
    public String getText() throws XMLStreamException {
        return reader.getElementText();
    }

    @Override
    public void close() {
        if (reader != null)
            try {
                reader.close();
            }catch (XMLStreamException e) {
                System.out.printf("Here is err");
            }
    }
}
