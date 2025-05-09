import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.reasoner.*;
import org.apache.jena.riot.Lang;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SPARQLQueryUtil {
    private Model model;
    private String lecturerNamespace;
    private String cwNamespace = "http://www.city.ac.uk/inm713-in3067/2025/CityWatch#";

    /**
     * Constructor that loads models from the CityWatch and Lecturer ontology files,
     * and optionally an alignment file if it exists.
     */
    public SPARQLQueryUtil(String citywatchOntologyPath, String lecturerOntologyPath,
                           String alignmentPath, String lecturerNamespace) {
        this.lecturerNamespace = lecturerNamespace;
        loadModels(citywatchOntologyPath, lecturerOntologyPath, alignmentPath);
    }

    /**
     * Creates a backward-compatible constructor that initializes without alignment.
     */
    public SPARQLQueryUtil(String citywatchOntologyPath, String lecturerOntologyPath, String lecturerNamespace) {
        this(citywatchOntologyPath, lecturerOntologyPath, null, lecturerNamespace);
    }

    /**
     * Loads both CityWatch and Lecturer RDF models from files and combines them,
     * optionally adding the computed alignment if available.
     */
    void loadModels(String citywatchPath, String lecturerPath, String alignmentPath) {
        model = ModelFactory.createDefaultModel();

        try {
            // Load CityWatch ontology
            System.out.println("Loading CityWatch ontology from: " + citywatchPath);
            FileInputStream fisCw = new FileInputStream(citywatchPath);
            RDFDataMgr.read(model, fisCw, null, Lang.TURTLE);  // Specify Turtle format
            fisCw.close();
            System.out.println("CityWatch ontology loaded with " + model.size() + " statements");

            // Load Lecturer ontology
            System.out.println("Loading Lecturer ontology from: " + lecturerPath);
            FileInputStream fisLec = new FileInputStream(lecturerPath);
            RDFDataMgr.read(model, fisLec, null, Lang.TURTLE);  // Specify Turtle format
            fisLec.close();
            System.out.println("Combined ontologies with " + model.size() + " statements");

            // Try to load alignment if provided
            if (alignmentPath != null) {
                try {
                    System.out.println("Loading alignment from: " + alignmentPath);
                    FileInputStream fisAlign = new FileInputStream(alignmentPath);
                    RDFDataMgr.read(model, fisAlign, null, Lang.TURTLE);  // Specify Turtle format
                    fisAlign.close();
                    System.out.println("Alignment added, total statements: " + model.size());
                } catch (FileNotFoundException e) {
                    System.out.println("Alignment file not found at: " + alignmentPath + " - continuing without alignment");
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading models: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Executes a SPARQL query and returns the results as a list of strings.
     */
    public List<String> executeQuery(String queryString) {
        List<String> results = new ArrayList<>();
        try {
            Query query = QueryFactory.create(queryString);
            QueryExecution qexec = QueryExecutionFactory.create(query, model);

            if (query.isSelectType()) {
                ResultSet resultSet = qexec.execSelect();
                while (resultSet.hasNext()) {
                    QuerySolution solution = resultSet.nextSolution();
                    StringBuilder resultRow = new StringBuilder();
                    for (String varName : resultSet.getResultVars()) {
                        if (solution.get(varName) != null) {
                            resultRow.append(varName).append(": ")
                                    .append(solution.get(varName).toString())
                                    .append(" | ");
                        }
                    }
                    if (resultRow.length() > 0) {
                        results.add(resultRow.substring(0, resultRow.length() - 3)); // Remove last " | "
                    }
                }
            } else if (query.isAskType()) {
                boolean askResult = qexec.execAsk();
                results.add("Ask query result: " + askResult);
            } else if (query.isConstructType() || query.isDescribeType()) {
                Model resultModel = query.isConstructType() ? qexec.execConstruct() : qexec.execDescribe();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                resultModel.write(outputStream, "TURTLE");
                results.add(outputStream.toString());
            }
            qexec.close();
        } catch (Exception e) {
            System.err.println("Error executing query: " + e.getMessage());
            e.printStackTrace();
            results.add("Error: " + e.getMessage());
        }
        return results;
    }

    /**
     * Returns a list of prefixes that can be used in SPARQL queries.
     */
    public String getPrefixDeclarations() {
        return "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
               "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
               "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
               "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
               "PREFIX lec: <" + lecturerNamespace + ">\n" +
               "PREFIX cw: <" + cwNamespace + ">\n";
    }

    /**
     * Removes duplicate triples from the model.
     */
    public void removeDuplicates() {
        Model uniqueModel = ModelFactory.createDefaultModel();
        
        // Iterate over the triples in the original model and add them to the uniqueModel
        StmtIterator stmtIterator = model.listStatements();
        while (stmtIterator.hasNext()) {
            Statement stmt = stmtIterator.nextStatement();
            uniqueModel.add(stmt);
        }

        // Update the original model with the unique triples
        model = uniqueModel;

        System.out.println("Removed duplicates. Model size: " + model.size());
    }

    /**
     * Saves the current model to a file.
     */
    public void saveModel(String outputPath, String format) {
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            // Write the model to the specified format (e.g., TURTLE, RDF/XML, etc.)
            if (format == null || format.isEmpty()) {
                format = "TURTLE"; // Default format if none is provided
            }
            model.write(fos, format);
            System.out.println("Model saved to: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error saving model: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Returns the loaded model.
     */
    public Model getModel() {
        return model;
    }
}
