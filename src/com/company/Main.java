package com.company;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private Map<Long, Node_Coordinates> all_nodes = new HashMap<>(); // <node_id, node_coordinates>
    private Map<Long, Node_Coordinates> nodes = new HashMap<>();
    private Map<Long, LinkedHashSet<Long>> adjacency_list = new HashMap<>(); // <node_id, adjacency_node_ids>
    private Map<String, RoadParameters> roads = new HashMap<>();

    public static void main(String[] args) throws Exception {
        long start_time = System.currentTimeMillis();
        new Main().run();
        long finish_time = System.currentTimeMillis();
        System.out.format("%d ms", finish_time - start_time);
    }

    private void run() throws Exception {

        BufferedReader input = new BufferedReader(new FileReader("Input/RoadOfInterest.txt"));
        StringTokenizer tokenizer = new StringTokenizer(input.readLine(), " ");
        while(tokenizer.hasMoreTokens())
            roads.put(tokenizer.nextToken(), new RoadParameters(tokenizer.nextToken(), Double.parseDouble(tokenizer.nextToken())));
        input.close();

        try(StreamProcessor processor = new StreamProcessor(Files.newInputStream(Paths.get("Input/Krasnoyarsk.osm")))) {
            processor.startElement("bounds", "node");
            Node_Coordinates.X = Node_Coordinates.mercX(Double.parseDouble(processor.getAttribute("minlon")));
            Node_Coordinates.Y = Node_Coordinates.mercY(Double.parseDouble(processor.getAttribute("minlat")));

            while (processor.startElement("node", "way"))   // Maped all nodes to all_nodes
                all_nodes.put(Long.parseLong(processor.getAttribute("id")), new Node_Coordinates(Double.parseDouble(processor.getAttribute("lat")), Double.parseDouble(processor.getAttribute("lon"))));

            do{ // Maped all ways
                ArrayList<Long> temp_list = new ArrayList<>();

                while (processor.startElement("nd", "tag"))
                    if (all_nodes.containsKey(Long.parseLong(processor.getAttribute("ref"))))
                        temp_list.add(Long.parseLong(processor.getAttribute("ref")));

                do {
                    if ("highway".equals(processor.getAttribute("k")) && roads.containsKey(processor.getAttribute("v")) && temp_list.size() > 1)
                        for (int i = 0; i < temp_list.size(); ++i) {
                            adjacency_list.putIfAbsent(temp_list.get(i), new LinkedHashSet<>((i - 1 < 0) ? Collections.singletonList(temp_list.get(i + 1)) : (i + 1 == temp_list.size()) ? Collections.singletonList(temp_list.get(i - 1)) : Arrays.asList(temp_list.get(i - 1), temp_list.get(i + 1))));
                            nodes.put(temp_list.get(i), all_nodes.get(temp_list.get(i)));
                        }
                }while (processor.startElement("tag", "way"));
            }while (processor.startElement("way", "relation"));

            nodes.forEach((k, v) -> v.set_coordinates());
            printCSV();
            createSVG();
        }
    }

    private void printCSV() throws Exception{
        String CSV_Nodes_File = "Output/Nodes.csv";
        String CSV_AdjList_File = "Output/AdjList.csv";

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
        BufferedReader input = new BufferedReader(new FileReader("Input/PreparationOfSVG.txt"));
        String str = input.readLine();
        Pattern regular_exp = Pattern.compile("\\s*<?/\\w*>");
        Matcher cheker = regular_exp.matcher(str);
        // output file
        String SVG_File = "Output/visualization.svg";
        PrintWriter out = new PrintWriter(new FileWriter(SVG_File));
        while (!cheker.matches()){ // general rules
            out.println(str);
            str = input.readLine();
            cheker = regular_exp.matcher(str);
        }
        nodes.forEach((k,v) -> out.format(Locale.US, "\t\t<circle r=\"0.2px\" fill=\"blue\" transform=\"translate(%f,%f)\"/>\n", v.get_variable("euclid_X") , v.get_variable("euclid_Y")));
        adjacency_list.forEach((k1, v1) -> v1.forEach(v2 -> { out.format(Locale.US, "\t\t<line x1=\"%f\" y1=\"%f\" x2=\"%f\" y2=\"%f\" style=\"stroke:rgb(255,0,0);stroke-width:0.4\"/>\n", nodes.get(k1).get_variable("euclid_X"), nodes.get(k1).get_variable("euclid_Y"), nodes.get(v2).get_variable("euclid_X"), nodes.get(v2).get_variable("euclid_Y")); adjacency_list.get(v2).remove(k1);}));

        do { // close tags of general rules
            out.println(str);
        }while((str = input.readLine()) != null);

        input.close();
        out.close();
    }
}

