import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;

import java.io.FileOutputStream;
import java.io.IOException;

public class SPARQLQueryUtil {
    private Model mergedModel;
    
    public SPARQLQueryUtil(String cityWatchOntologyPath, String lecturerOntologyPath, String alignmentPath) {
        // Load all ontologies and the alignment
        mergedModel = ModelFactory.createDefaultModel();
        
        try {
            // Load CityWatch ontology
            RDFDataMgr.read(mergedModel, cityWatchOntologyPath);
            
            // Load Lecturer ontology
            RDFDataMgr.read(mergedModel, lecturerOntologyPath);
            
            // Load alignment
            RDFDataMgr.read(mergedModel, alignmentPath);
            
            System.out.println("Loaded ontologies and alignment into merged model.");
        } catch (Exception e) {
            System.err.println("Error loading ontologies or alignment: " + e.getMessage());
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
            
            // Write results to file using a FileOutputStream instead of FileWriter
            // as ResultSetFormatter.outputAsCSV expects an OutputStream
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(outputPath);
            ResultSetFormatter.outputAsCSV(outputStream, results);
            outputStream.close();
            
            System.out.println("Query results saved to: " + outputPath);
            
            // Also print results to console
            System.out.println("\nQuery Results:");
            
            // Need to re-execute the query as the previous result set was consumed
            qexec = QueryExecutionFactory.create(query, mergedModel);
            results = qexec.execSelect();
            
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
        // Based on our understanding, the file is actually a road accident ontology
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
}