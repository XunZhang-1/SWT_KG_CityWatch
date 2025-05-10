import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.query.*;
import org.apache.jena.riot.RDFWriter;

import java.io.*;
import java.util.List;

public class SPARQLQueryUtil {
    private Model cityWatchModel;
    private Model lecturerModel;
    private Model alignmentModel;
    private String baseUri;

    public SPARQLQueryUtil(String cityWatchPath, String lecturerPath, String alignmentPath, String baseUri) {
        this.baseUri = baseUri;
    }

    // Load the RDF models
    public void loadModels(String cityWatchPath, String lecturerPath, String alignmentPath) {
        cityWatchModel = RDFDataMgr.loadModel(cityWatchPath);
        lecturerModel = RDFDataMgr.loadModel(lecturerPath);
        alignmentModel = RDFDataMgr.loadModel(alignmentPath);
    }

    // Get the combined model
    public Model getModel() {
        Model combinedModel = ModelFactory.createDefaultModel();
        combinedModel.add(cityWatchModel);
        combinedModel.add(lecturerModel);
        combinedModel.add(alignmentModel);
        return combinedModel;
    }

    // Save the model in the specified format (TURTLE, RDF/XML, JSON-LD, etc.)
    public void saveModel(String outputPath, String format) {
        try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
            Model model = getModel();
            if ("TURTLE".equalsIgnoreCase(format)) {
                model.write(outputStream, "TURTLE");  // Write in Turtle format
            } else if ("RDFXML".equalsIgnoreCase(format)) {
                model.write(outputStream, "RDF/XML");  // Write in RDF/XML format
            } else if ("JSONLD".equalsIgnoreCase(format)) {
                model.write(outputStream, "JSON-LD");  // Write in JSON-LD format
            } else if ("NTRIPLES".equalsIgnoreCase(format)) {
                model.write(outputStream, "N-TRIPLE");  // Write in N-Triples format
            } else {
                System.out.println("Unsupported RDF format: " + format);  // Ensure only valid formats
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Remove duplicate statements
    public void removeDuplicates() {
        Model model = getModel();
        // Removing duplicates by reloading the model, could be optimized further
        Model uniqueModel = ModelFactory.createDefaultModel();
        model.listStatements().forEachRemaining(st -> uniqueModel.add(st));
    }

    // Execute a SPARQL query and save the results to CSV
    public void executeQuery(String sparqlQuery, String outputPath) {
        Query query = QueryFactory.create(sparqlQuery);
        QueryExecution qe = QueryExecutionFactory.create(query, getModel());
        ResultSet results = qe.execSelect();

        // Writing query results to a CSV file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            // Write header (variable names)
            List<String> varNames = results.getResultVars();
            writer.write(String.join(",", varNames));
            writer.newLine();

            // Write the results
            while (results.hasNext()) {
                QuerySolution sol = results.nextSolution();
                for (String varName : varNames) {
                    writer.write(sol.get(varName).toString() + ",");
                }
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            qe.close();
        }
    }
}
