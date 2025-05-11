import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;

import java.io.FileOutputStream;
import java.io.IOException;

public class SPARQLQueryUtil {
    private Model mergedModel;
    
    public SPARQLQueryUtil(String cityWatchOntologyPath, String roadAccidentOntologyPath, String alignmentPath) {
        mergedModel = ModelFactory.createDefaultModel();
        
        System.out.println("Loading CityWatch ontology from: " + cityWatchOntologyPath);
        mergedModel.read(cityWatchOntologyPath, "TURTLE");
        System.out.println("CityWatch ontology loaded. Size: " + mergedModel.size());
        
        System.out.println("Loading Road Accident ontology from: " + roadAccidentOntologyPath);
        mergedModel.read(roadAccidentOntologyPath, "TURTLE");
        System.out.println("Road Accident ontology loaded. Size: " + mergedModel.size());
        
        System.out.println("Loading alignment from: " + alignmentPath);
        mergedModel.read(alignmentPath, "TURTLE");
        System.out.println("Alignment loaded. Size: " + mergedModel.size());
        
        // Verify namespaces
        System.out.println("\nNamespaces in merged model:");
        mergedModel.getNsPrefixMap().forEach((prefix, uri) -> 
            System.out.println(prefix + ": " + uri));
        
        addRoadAccidentData();
    }
    
    private void addRoadAccidentData() {
        // Create namespace for Road Accident ontology
        String accidentNS = "http://www.gosemantic.com/ontologies/roadaccident.owl#";
        String exNS = "http://example.org/";
        
        try {
            System.out.println("Adding Road Accident test data to the merged model...");
            
            // Create classes from Road Accident ontology
            Resource personClass = mergedModel.createResource(accidentNS + "Person");
            Resource driverClass = mergedModel.createResource(accidentNS + "Driver");
            Resource pedestrianClass = mergedModel.createResource(accidentNS + "Pedestrian");
            Resource vehicleClass = mergedModel.createResource(accidentNS + "Vehicle");
            Resource carClass = mergedModel.createResource(accidentNS + "Car");
            Resource truckClass = mergedModel.createResource(accidentNS + "Truck");
            Resource roadClass = mergedModel.createResource(accidentNS + "Road");
            Resource junctionClass = mergedModel.createResource(accidentNS + "Junction");
            Resource accidentClass = mergedModel.createResource(accidentNS + "Accident");
            
            // Create properties from Road Accident ontology
            Property drivesProperty = mergedModel.createProperty(accidentNS + "drives");
            Property crossesRoadProperty = mergedModel.createProperty(accidentNS + "crossesRoad");
            Property involvedInProperty = mergedModel.createProperty(accidentNS + "involvedIn");
            Property hasJunctionProperty = mergedModel.createProperty(accidentNS + "hasJunction");
            Property hasLocationProperty = mergedModel.createProperty(accidentNS + "hasLocation");
            Property hasSeverityProperty = mergedModel.createProperty(accidentNS + "hasSeverity");
            Property hasNameProperty = mergedModel.createProperty(accidentNS + "hasName");
            Property hasAgeProperty = mergedModel.createProperty(accidentNS + "hasAge");
            
            // Create some individuals (instances)
            // People
            Resource person1 = mergedModel.createResource(exNS + "person1");
            mergedModel.add(person1, RDF.type, personClass);
            mergedModel.add(person1, RDF.type, driverClass);
            mergedModel.add(person1, hasNameProperty, mergedModel.createLiteral("John Smith"));
            mergedModel.add(person1, hasAgeProperty, mergedModel.createTypedLiteral(35));
            
            Resource person2 = mergedModel.createResource(exNS + "person2");
            mergedModel.add(person2, RDF.type, personClass);
            mergedModel.add(person2, RDF.type, pedestrianClass);
            mergedModel.add(person2, hasNameProperty, mergedModel.createLiteral("Sarah Johnson"));
            mergedModel.add(person2, hasAgeProperty, mergedModel.createTypedLiteral(28));
            
            Resource person3 = mergedModel.createResource(exNS + "person3");
            mergedModel.add(person3, RDF.type, personClass);
            mergedModel.add(person3, RDF.type, driverClass);
            mergedModel.add(person3, hasNameProperty, mergedModel.createLiteral("Michael Chen"));
            mergedModel.add(person3, hasAgeProperty, mergedModel.createTypedLiteral(42));
            
            // Vehicles
            Resource vehicle1 = mergedModel.createResource(exNS + "vehicle1");
            mergedModel.add(vehicle1, RDF.type, vehicleClass);
            mergedModel.add(vehicle1, RDF.type, carClass);
            
            Resource vehicle2 = mergedModel.createResource(exNS + "vehicle2");
            mergedModel.add(vehicle2, RDF.type, vehicleClass);
            mergedModel.add(vehicle2, RDF.type, truckClass);
            
            // Roads and Junctions
            Resource road1 = mergedModel.createResource(exNS + "road1");
            mergedModel.add(road1, RDF.type, roadClass);
            mergedModel.add(road1, hasNameProperty, mergedModel.createLiteral("High Street"));
            
            Resource road2 = mergedModel.createResource(exNS + "road2");
            mergedModel.add(road2, RDF.type, roadClass);
            mergedModel.add(road2, hasNameProperty, mergedModel.createLiteral("Main Road"));
            
            Resource junction1 = mergedModel.createResource(exNS + "junction1");
            mergedModel.add(junction1, RDF.type, junctionClass);
            mergedModel.add(road1, hasJunctionProperty, junction1);
            mergedModel.add(road2, hasJunctionProperty, junction1);
            
            // Accidents
            Resource accident1 = mergedModel.createResource(exNS + "accident1");
            mergedModel.add(accident1, RDF.type, accidentClass);
            mergedModel.add(accident1, hasLocationProperty, junction1);
            mergedModel.add(accident1, hasSeverityProperty, mergedModel.createLiteral("Serious"));
            
            // Relationships
            mergedModel.add(person1, drivesProperty, vehicle1);
            mergedModel.add(person2, crossesRoadProperty, road1);
            mergedModel.add(person3, drivesProperty, vehicle2);
            
            mergedModel.add(person1, involvedInProperty, accident1);
            mergedModel.add(person2, involvedInProperty, accident1);
            mergedModel.add(vehicle1, involvedInProperty, accident1);
            
            System.out.println("Road Accident test data added successfully.");
            System.out.println("Total statements in merged model: " + mergedModel.size());
            
        } catch (Exception e) {
            System.err.println("Error adding test data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void executeQuery(String sparqlQueryString, String outputPath) {
        try {
            // Create a query and execution
            Query query = QueryFactory.create(sparqlQueryString);
            QueryExecution qexec = QueryExecutionFactory.create(query, mergedModel);
            
            // Execute query and output results
            ResultSet results = qexec.execSelect();
            
            // Write results to file
            FileOutputStream outputStream = new FileOutputStream(outputPath);
            ResultSetFormatter.outputAsCSV(outputStream, results);
            outputStream.close();
            
            System.out.println("Query results saved to: " + outputPath);
            
            // Also print results to console
            System.out.println("\nQuery Results:");
            
            // Need to re-execute the query as the previous result set was consumed
            qexec = QueryExecutionFactory.create(query, mergedModel);
            results = qexec.execSelect();
            
            if (!results.hasNext()) {
                System.out.println("No results found for the query.");
            }
            
            // Print results in a more readable format
            ResultSetFormatter.out(System.out, results, query);
            
        } catch (Exception e) {
            System.err.println("Error executing SPARQL query: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void executeAccidentInvolvedQuery(String outputPath) {
        // This query finds all people and vehicles involved in accidents, 
        // along with accident severity and location information
        String roadAccidentQuery = 
            "PREFIX rdf: <" + RDF.getURI() + ">\n" +
            "PREFIX accident: <http://www.gosemantic.com/ontologies/roadaccident.owl#>\n" +
            "SELECT ?person ?personName ?personType ?vehicle ?accident ?severity ?location ?roadName\n" +
            "WHERE {\n" +
            "  # Person must be involved in an accident (first triple pattern)\n" +
            "  ?person rdf:type accident:Person .\n" +
            "  ?person accident:involvedIn ?accident .\n" +
            "  ?accident rdf:type accident:Accident .\n" +
            "  \n" +
            "  # Get person properties\n" +
            "  ?person accident:hasName ?personName .\n" +
            "  \n" +
            "  # Get person type (Driver or Pedestrian)\n" +
            "  OPTIONAL {\n" +
            "    ?person rdf:type ?personType .\n" +
            "    FILTER (?personType = accident:Driver || ?personType = accident:Pedestrian)\n" +
            "  }\n" +
            "  \n" +
            "  # Get accident severity and location (second triple pattern)\n" +
            "  ?accident accident:hasSeverity ?severity .\n" +
            "  ?accident accident:hasLocation ?location .\n" +
            "  \n" +
            "  # Get road information if available\n" +
            "  OPTIONAL {\n" +
            "    ?road accident:hasJunction ?location .\n" +
            "    ?road accident:hasName ?roadName .\n" +
            "  }\n" +
            "  \n" +
            "  # Get vehicle information if the person is a driver\n" +
            "  OPTIONAL {\n" +
            "    ?person accident:drives ?vehicle .\n" +
            "    ?vehicle rdf:type accident:Vehicle .\n" +
            "  }\n" +
            "}";
        
        executeQuery(roadAccidentQuery, outputPath);
    }
    
    public void executeDriversAndVehiclesQuery(String outputPath) {
        // This query finds all drivers and the vehicles they drive
        // Only using Road Accident ontology vocabulary
        String roadAccidentQuery =
            "PREFIX rdf: <" + RDF.getURI() + ">\n" +
            "PREFIX accident: <http://www.gosemantic.com/ontologies/roadaccident.owl#>\n" +
            "SELECT ?driver ?driverName ?driverAge ?vehicle ?vehicleType\n" +
            "WHERE {\n" +
            "  # Find drivers (first triple pattern)\n" +
            "  ?driver rdf:type accident:Driver .\n" +
            "  \n" +
            "  # Get driver properties\n" +
            "  ?driver accident:hasName ?driverName .\n" +
            "  ?driver accident:hasAge ?driverAge .\n" +
            "  \n" +
            "  # Get vehicle information (second triple pattern)\n" +
            "  ?driver accident:drives ?vehicle .\n" +
            "  ?vehicle rdf:type accident:Vehicle .\n" +
            "  \n" +
            "  # Get specific vehicle type\n" +
            "  OPTIONAL {\n" +
            "    ?vehicle rdf:type ?vehicleType .\n" +
            "    FILTER (?vehicleType != accident:Vehicle)\n" +
            "  }\n" +
            "}";
        
        executeQuery(roadAccidentQuery, outputPath);
    }
    
    public void displayModelContent() {
        System.out.println("\n=== Road Accident Ontology Model Content ===");
        String accidentNS = "http://www.gosemantic.com/ontologies/roadaccident.owl#";
        
        // Display classes
        System.out.println("\nClasses:");
        StmtIterator classStmts = mergedModel.listStatements(null, RDF.type, mergedModel.createResource("http://www.w3.org/2002/07/owl#Class"));
        while (classStmts.hasNext()) {
            Statement stmt = classStmts.next();
            if (stmt.getSubject().getURI() != null && stmt.getSubject().getURI().contains(accidentNS)) {
                System.out.println("- " + stmt.getSubject().getURI());
            }
        }
        
        // Display properties
        System.out.println("\nProperties:");
        StmtIterator propStmts = mergedModel.listStatements(null, RDF.type, RDF.Property);
        while (propStmts.hasNext()) {
            Statement stmt = propStmts.next();
            if (stmt.getSubject().getURI() != null && stmt.getSubject().getURI().contains(accidentNS)) {
                System.out.println("- " + stmt.getSubject().getURI());
            }
        }
        
        // Display a few instances
        System.out.println("\nSample Instances:");
        String[] types = {"Person", "Vehicle", "Road", "Accident"};
        for (String type : types) {
            StmtIterator instances = mergedModel.listStatements(null, RDF.type, mergedModel.createResource(accidentNS + type));
            System.out.println("- " + type + " instances:");
            int count = 0;
            while (instances.hasNext() && count < 5) {
                Statement stmt = instances.next();
                System.out.println("  * " + stmt.getSubject().getURI());
                count++;
            }
        }

System.out.println("Merged Model Size: " + mergedModel.size());

    }
    
    public void executeAccidentSeverityQuery(String outputPath) {
        String query = 
            "PREFIX accident: <http://www.gosemantic.com/ontologies/roadaccident.owl#>\n" +
            "SELECT ?accident ?severity ?location ?roadName\n" +
            "WHERE {\n" +
            "  ?accident a accident:Accident .\n" +
            "  ?accident accident:hasSeverity ?severity .\n" +
            "  ?accident accident:hasLocation ?location .\n" +
            "  OPTIONAL {\n" +
            "    ?road accident:hasJunction ?location .\n" +
            "    ?road accident:hasName ?roadName .\n" +
            "  }\n" +
            "}";
        
        executeQuery(query, outputPath);
    }
    
    public void executeLecturerVocabularyQuery(String outputPath) {
        // This method was commented out in the original code
        // Implementing it to execute a query using road accident ontology vocabulary
        executeDriversAndVehiclesQuery(outputPath);
    }

    // Ensure your file paths are correct by adding this helper method
    public void ensureOutputDirectoryExists(String outputPath) {
        try {
            // Extract directory path from file path
            String dirPath = outputPath.substring(0, outputPath.lastIndexOf('/'));
            java.io.File dir = new java.io.File(dirPath);
            
            // Create directory if it doesn't exist
            if (!dir.exists()) {
                System.out.println("Creating output directory: " + dirPath);
                if (dir.mkdirs()) {
                    System.out.println("Output directory created successfully.");
                } else {
                    System.out.println("Failed to create output directory, may already exist.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error ensuring output directory exists: " + e.getMessage());
        }
    }

   

    
    public static void main(String[] args) {
        String cityWatchOntologyPath = "files/CityWatch_Ontology.ttl";
        String roadAccidentOntologyPath = "files/Onto_Lecture.ttl";
        String alignmentPath = "files/computed_alignment.ttl";
        String outputPath = "files/road_accident_query_results.csv";
        
        SPARQLQueryUtil util = new SPARQLQueryUtil(
            cityWatchOntologyPath, roadAccidentOntologyPath, alignmentPath);
        
        // Display model content for understanding the data structure
        util.displayModelContent();
        
        // Execute the query that uses only Road Accident ontology vocabulary
        System.out.println("\nExecuting query with only Road Accident ontology vocabulary...");
        util.executeDriversAndVehiclesQuery(outputPath);
    }
}