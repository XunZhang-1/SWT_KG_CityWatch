import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Creates a reference alignment between the CityWatch ontology and the Road Accident ontology.
 * This reference alignment can be used to evaluate the quality of automatically generated alignments.
 */
public class ReferenceAlignmentCreator {

    /**
     * Creates a reference alignment with manually identified mappings between the two ontologies.
     * 
     * @param cityWatchOntologyPath Path to the CityWatch ontology file
     * @param roadAccidentOntologyPath Path to the Road Accident ontology file
     * @param outputPath Path to save the reference alignment
     */
    public static void createReferenceAlignment(String cityWatchOntologyPath, 
                                              String roadAccidentOntologyPath, 
                                              String outputPath) {
        try {
            // Load the ontologies to get the correct URIs
            Model cityWatchModel = ModelFactory.createDefaultModel();
            Model roadAccidentModel = ModelFactory.createDefaultModel();
            
            System.out.println("Loading CityWatch ontology for reference alignment...");
            cityWatchModel.read(cityWatchOntologyPath, "TURTLE");
            System.out.println("CityWatch ontology loaded. Size: " + cityWatchModel.size());
            
            System.out.println("Loading Road Accident ontology for reference alignment...");
            roadAccidentModel.read(roadAccidentOntologyPath, "TURTLE");
            System.out.println("Road Accident ontology loaded. Size: " + roadAccidentModel.size());
            
            // Create the alignment model
            Model alignmentModel = ModelFactory.createDefaultModel();
            
            // Get namespaces
            String cityWatchNS = findNamespace(cityWatchModel);
            String roadAccidentNS = findNamespace(roadAccidentModel);
            
            System.out.println("CityWatch Namespace: " + cityWatchNS);
            System.out.println("Road Accident Namespace: " + roadAccidentNS);
            
            // Manual mappings between classes
            Map<String, String> classAlignments = new HashMap<>();
            classAlignments.put("Person", "Person");
            classAlignments.put("Driver", "Driver");
            classAlignments.put("Pedestrian", "Pedestrian");
            classAlignments.put("Vehicle", "Vehicle");
            classAlignments.put("Car", "Car");
            classAlignments.put("Truck", "Truck");
            classAlignments.put("Road", "Road");
            classAlignments.put("Junction", "Junction");
            classAlignments.put("Accident", "Accident");
            classAlignments.put("Location", "Location");
            classAlignments.put("TrafficObject", "TrafficObject");
            classAlignments.put("RoadUser", "RoadUser");
            
            // Manual mappings between properties
            Map<String, String> propertyAlignments = new HashMap<>();
            propertyAlignments.put("drives", "drives");
            propertyAlignments.put("crossesRoad", "crossesRoad");
            propertyAlignments.put("involvedIn", "involvedIn");
            propertyAlignments.put("hasJunction", "hasJunction");
            propertyAlignments.put("hasLocation", "hasLocation");
            propertyAlignments.put("hasSeverity", "hasSeverity");
            propertyAlignments.put("hasName", "hasName");
            propertyAlignments.put("hasAge", "hasAge");
            
            // Add class alignments
            for (Map.Entry<String, String> entry : classAlignments.entrySet()) {
                String cityWatchClassUri = cityWatchNS + entry.getKey();
                String roadAccidentClassUri = roadAccidentNS + entry.getValue();
                
                Resource cityWatchClass = cityWatchModel.getResource(cityWatchClassUri);
                Resource roadAccidentClass = roadAccidentModel.getResource(roadAccidentClassUri);
                
                if (resourceExists(cityWatchModel, cityWatchClass) && 
                    resourceExists(roadAccidentModel, roadAccidentClass)) {
                    alignmentModel.add(cityWatchClass, OWL.equivalentClass, roadAccidentClass);
                    System.out.println("Added class alignment: " + entry.getKey() + " <-> " + entry.getValue());
                } else {
                    System.out.println("Warning: Could not create class alignment for " + 
                                      entry.getKey() + " <-> " + entry.getValue() + 
                                      " (one or both resources not found)");
                }
            }
            
            // Add property alignments
            for (Map.Entry<String, String> entry : propertyAlignments.entrySet()) {
                String cityWatchPropUri = cityWatchNS + entry.getKey();
                String roadAccidentPropUri = roadAccidentNS + entry.getValue();
                
                Resource cityWatchProp = cityWatchModel.getResource(cityWatchPropUri);
                Resource roadAccidentProp = roadAccidentModel.getResource(roadAccidentPropUri);
                
                if (resourceExists(cityWatchModel, cityWatchProp) && 
                    resourceExists(roadAccidentModel, roadAccidentProp)) {
                    alignmentModel.add(cityWatchProp, OWL.equivalentProperty, roadAccidentProp);
                    System.out.println("Added property alignment: " + entry.getKey() + " <-> " + entry.getValue());
                } else {
                    System.out.println("Warning: Could not create property alignment for " + 
                                      entry.getKey() + " <-> " + entry.getValue() + 
                                      " (one or both resources not found)");
                }
            }
            
            // Create the output directory if it doesn't exist
            File outputFile = new File(outputPath);
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
            
            // Save the alignment
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                alignmentModel.write(fos, "TURTLE");
                System.out.println("Reference alignment saved to: " + outputPath);
                System.out.println("Total alignments created: " + alignmentModel.size());
            }
            
            // Debug - print alignment to console
            System.out.println("\nReference Alignment Content:");
            alignmentModel.write(System.out, "TURTLE");
            
        } catch (Exception e) {
            System.err.println("Error creating reference alignment: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Checks if a resource exists in the model
     */
    private static boolean resourceExists(Model model, Resource resource) {
        return model.contains(resource, null) || model.contains(null, null, resource);
    }
    
    /**
     * Finds the main namespace of an ontology
     */
    private static String findNamespace(Model model) {
        // First try to find owl:Ontology declaration
        ResIterator ontologies = model.listSubjectsWithProperty(null, model.createResource("http://www.w3.org/2002/07/owl#Ontology"));
        if (ontologies.hasNext()) {
            String uri = ontologies.next().getURI();
            if (uri.contains("#")) {
                return uri.substring(0, uri.lastIndexOf('#') + 1);
            } else if (uri.endsWith("/")) {
                return uri;
            } else {
                return uri + "#";
            }
        }
        
        // Fallback to looking at statement URIs
        Set<String> namespaces = new HashSet<>();
        model.listStatements().forEachRemaining(stmt -> {
            if (stmt.getSubject().isURIResource()) {
                String uri = stmt.getSubject().getURI();
                if (uri.contains("#")) {
                    namespaces.add(uri.substring(0, uri.lastIndexOf('#') + 1));
                }
            }
        });
        
        // Return the first namespace found or a default
        return namespaces.isEmpty() ? 
               "http://www.semanticweb.org/ontology#" : 
               namespaces.iterator().next();
    }
}