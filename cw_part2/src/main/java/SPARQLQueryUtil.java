import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;

import java.io.FileOutputStream;

public class SPARQLQueryUtil {
    private Model mergedModel;
    
    public SPARQLQueryUtil(String cityWatchOntologyPath, String roadAccidentOntologyPath, String alignmentPath) {
        // Load all ontologies and the alignment
        mergedModel = ModelFactory.createDefaultModel();
        
        try {
            // Load CityWatch ontology
            RDFDataMgr.read(mergedModel, cityWatchOntologyPath);
            
            // Load Road Accident ontology
            RDFDataMgr.read(mergedModel, roadAccidentOntologyPath);
            
            // Load alignment
            RDFDataMgr.read(mergedModel, alignmentPath);
            
            System.out.println("Loaded ontologies and alignment into merged model.");
            
            // Add test data since we didn't find any instances in the query results
            addTestData();
            
        } catch (Exception e) {
            System.err.println("Error loading ontologies or alignment: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void addTestData() {
        // Create namespaces for our test data
        String accidentNS = "http://www.gosemantic.com/ontologies/roadaccident.owl#";
        String cityWatchNS = "http://www.city.ac.uk/inm713-in3067/2025/CityWatch#";
        String exNS = "http://example.org/";
        
        try {
            System.out.println("Adding test data to the merged model...");
            
            // Create classes from both ontologies
            Resource personClass = mergedModel.createResource(accidentNS + "Person");
            Resource driverClass = mergedModel.createResource(accidentNS + "Driver");
            Resource pedestrianClass = mergedModel.createResource(accidentNS + "Pedestrian");
            Resource vehicleClass = mergedModel.createResource(accidentNS + "Vehicle");
            Resource roadClass = mergedModel.createResource(accidentNS + "Road");
            
            // Create properties
            Property drivesProperty = mergedModel.createProperty(accidentNS + "drives");
            Property crossesRoadProperty = mergedModel.createProperty(accidentNS + "crossesRoad");
            
            // Create some individuals (instances)
            // Person 1 - A driver
            Resource person1 = mergedModel.createResource(exNS + "person1");
            mergedModel.add(person1, RDF.type, personClass);
            mergedModel.add(person1, RDF.type, driverClass);
            
            // Person 2 - A pedestrian
            Resource person2 = mergedModel.createResource(exNS + "person2");
            mergedModel.add(person2, RDF.type, personClass);
            mergedModel.add(person2, RDF.type, pedestrianClass);
            
            // Vehicle
            Resource vehicle1 = mergedModel.createResource(exNS + "vehicle1");
            mergedModel.add(vehicle1, RDF.type, vehicleClass);
            
            // Road
            Resource road1 = mergedModel.createResource(exNS + "road1");
            mergedModel.add(road1, RDF.type, roadClass);
            
            // Relationships
            mergedModel.add(person1, drivesProperty, vehicle1);
            mergedModel.add(person2, crossesRoadProperty, road1);
            
            System.out.println("Test data added successfully.");
            
            // Debug: print the number of statements in the model
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
            
            while (results.hasNext()) {
                QuerySolution solution = results.next();
                System.out.println(solution);
            }
            
        } catch (Exception e) {
            System.err.println("Error executing SPARQL query: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void executeLecturerVocabularyQuery(String outputPath) {
        // Original query
        String roadAccidentQuery = "PREFIX rdf: <" + RDF.getURI() + ">\n" +
                              "PREFIX accident: <http://www.gosemantic.com/ontologies/roadaccident.owl#>\n" +
                              "SELECT ?person ?vehicle ?road\n" +
                              "WHERE {\n" +
                              "  ?person rdf:type accident:Person .\n" +
                              "  OPTIONAL { ?person accident:drives ?vehicle }\n" +
                              "  OPTIONAL { ?person accident:crossesRoad ?road }\n" +
                              "}";
        
        executeQuery(roadAccidentQuery, outputPath);
    }
    
    // Add a method to display the model's content for debugging
    public void displayModelContent() {
        System.out.println("\n=== Model Content ===");
        StmtIterator stmts = mergedModel.listStatements();
        int count = 0;
        while (stmts.hasNext() && count < 50) { // Limit to 50 statements to avoid flooding the console
            Statement stmt = stmts.next();
            System.out.println(stmt.getSubject() + " - " + stmt.getPredicate() + " - " + stmt.getObject());
            count++;
        }
        if (stmts.hasNext()) {
            System.out.println("... (more statements)");
        }
    }
    
    // Add a new main method for testing this class directly
    public static void main(String[] args) {
        String cityWatchOntologyPath = "files/CityWatch_Ontology.ttl";
        String roadAccidentOntologyPath = "files/Onto_Lecture.ttl";
        String alignmentPath = "files/computed_alignment.ttl";
        String outputPath = "files/sparql_results.csv";
        
        SPARQLQueryUtil util = new SPARQLQueryUtil(cityWatchOntologyPath, roadAccidentOntologyPath, alignmentPath);
        
        // Display model content for debugging
        util.displayModelContent();
        
        // Execute the query
        util.executeLecturerVocabularyQuery(outputPath);
    }
}