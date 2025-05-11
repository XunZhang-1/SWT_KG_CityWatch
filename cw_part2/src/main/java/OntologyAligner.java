import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.RDFDataMgr;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class OntologyAligner {

    private Model cw2Model;  // CityWatch ontology model
    private Model lecturerModel;  // Lecturer ontology model
    private Model model; // Make sure 'model' is a class-level variable

    // Constructor
    public OntologyAligner(String cityWatchOntologyPath, String lecturerOntologyPath, String alignmentPath) {
        model = ModelFactory.createDefaultModel();
        loadModels(cityWatchOntologyPath, lecturerOntologyPath, alignmentPath);
    }
    
 // Method to load the ontologies and combine them into the 'model'
    private void loadModels(String citywatchPath, String lecturerPath, String alignmentPath) {
        try {
            // Load CityWatch ontology
            RDFDataMgr.read(model, citywatchPath);

            // Load Lecturer ontology
            RDFDataMgr.read(model, lecturerPath);

            // Load alignment if available
            if (alignmentPath != null) {
                RDFDataMgr.read(model, alignmentPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load ontologies
    public void loadOntologies(String cityWatchPath, String lecturerPath) {
        Model cityWatchModel = ModelFactory.createDefaultModel();
        cityWatchModel.read(cityWatchPath, "TURTLE");

        Model lecturerModel = ModelFactory.createDefaultModel();
        lecturerModel.read(lecturerPath, "TURTLE");

        // Perform any necessary actions like loading models or doing preliminary checks
        System.out.println("Ontologies loaded.");
    }


    // Generate reference alignment
    public void generateReferenceAlignment(String cityWatchOntologyPath, String lecturerOntologyPath, String referenceAlignmentPath) {
        // For simplicity, create a basic reference alignment file (can be extended to more complex cases)
        try (FileWriter writer = new FileWriter(referenceAlignmentPath)) {
            writer.write("@prefix owl: <http://www.w3.org/2002/07/owl#> .\n");
            writer.write("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n");
            writer.write("@prefix : <http://example.org/> .\n");

            writer.write(":CityWatchClass1 owl:equivalentClass :LecturerClass1 .\n");
            writer.write(":CityWatchProperty1 owl:equivalentProperty :LecturerProperty1 .\n");
            // Add more equivalences as needed

            System.out.println("Reference alignment generated successfully and saved to: " + referenceAlignmentPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error generating reference alignment.");
        }

    }
    public void computePrecisionRecall(String referenceAlignmentPath) {
        // Load models
        Model referenceModel = ModelFactory.createDefaultModel();
        referenceModel.read(referenceAlignmentPath, "TURTLE");

        Model computedModel = ModelFactory.createDefaultModel();
        computedModel.read("cw_part2/files/output/computed_alignment.ttl", "TURTLE");

        Set<Statement> referenceStatements = referenceModel.listStatements().toSet();
        Set<Statement> computedStatements = computedModel.listStatements().toSet();

        Set<Statement> correctMatches = new HashSet<>(computedStatements);
        correctMatches.retainAll(referenceStatements); // intersection

        int correct = correctMatches.size();
        int computedTotal = computedStatements.size();
        int referenceTotal = referenceStatements.size();

        double precision = computedTotal > 0 ? (double) correct / computedTotal : 0;
        double recall = referenceTotal > 0 ? (double) correct / referenceTotal : 0;
        double f1 = (precision + recall) > 0 ? 2 * (precision * recall) / (precision + recall) : 0;

        System.out.println("Total mappings in computed alignment: " + computedTotal);
        System.out.println("Total mappings in reference alignment: " + referenceTotal);
        System.out.println("Correct matches found: " + correct);
        System.out.println("Precision: " + precision);
        System.out.println("Recall:    " + recall);
        System.out.println("F1 Score:  " + f1);
    }

//    // Compute equivalences and save the alignment to a file
//    public void computeEquivalences(String cityWatchOntologyPath, String lecturerOntologyPath, String computedAlignmentPath) {
//        Model cityWatchModel = ModelFactory.createDefaultModel();
//        Model lecturerModel = ModelFactory.createDefaultModel();
//
//        // Load CityWatch and Lecturer ontologies
//        RDFDataMgr.read(cityWatchModel, cityWatchOntologyPath);
//        RDFDataMgr.read(lecturerModel, lecturerOntologyPath);
//
//        // Logic for computing equivalences between the two models
//        // Example of computing equivalences and adding them to the alignment model
//        Model computedModel = ModelFactory.createDefaultModel();
//
//        // Example of creating equivalences
//        Resource cityWatchClass = computedModel.createResource("http://example.org/CityWatchClass");
//        Resource lecturerClass = computedModel.createResource("http://example.org/LecturerClass");
//        computedModel.add(cityWatchClass, OWL.equivalentClass, lecturerClass);
//
//        // Write the computed equivalences to the output file
//        try (FileWriter writer = new FileWriter(computedAlignmentPath)) {
//            computedModel.write(writer, "TURTLE");
//            System.out.println("Equivalences computed and saved to: " + computedAlignmentPath);
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.err.println("Error saving computed alignment.");
//        }
//    }



//
//    // Compute precision and recall of the mappings
//    public void computePrecisionRecall(String referenceAlignmentPath) {
//        // Load the reference alignment
//        Model referenceModel = ModelFactory.createDefaultModel();
//        referenceModel.read(referenceAlignmentPath);
//
//        // Placeholder: Compute precision and recall by comparing generated alignments with the reference alignment
//        // You can use a more detailed comparison logic here to measure precision and recall based on common triples
//
//        System.out.println("Precision and recall computation is done (placeholder).");
//    }

 // Reasoning method where 'model' is used
    public void performReasoning(String outputPath) {
        // Ensure that 'model' is available
        if (model == null) {
            System.err.println("Model is not initialized.");
            return;
        }

        // Create a reasoner and apply reasoning to the 'model'
        Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();  // Or another reasoner depending on your needs
        InfModel infModel = ModelFactory.createInfModel(reasoner, model);  // Here, 'model' is the variable

        // Save the inferred model to the output path
        try (FileWriter writer = new FileWriter(outputPath)) {
            infModel.write(writer, "TURTLE");
            System.out.println("Reasoning performed, combined model saved to: " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error saving the combined model after reasoning.");
        }
    }

    // Execute a SPARQL query on the combined model
    public void executeSparqlQuery(String combinedModelPath, String resultsPath) {
        // Read combined model
        Model combinedModel = ModelFactory.createDefaultModel();
        combinedModel.read(combinedModelPath);

        // Example SPARQL query (adjust based on your ontologies)
        String sparqlQuery = "SELECT ?subject ?predicate ?object WHERE { ?subject ?predicate ?object . } LIMIT 10";

        Query query = QueryFactory.create(sparqlQuery);
        QueryExecution qExec = QueryExecutionFactory.create(query, combinedModel);
        ResultSet results = qExec.execSelect();

        // Save results to CSV
        try (FileWriter writer = new FileWriter(resultsPath)) {
            writer.write("Subject,Predicate,Object\n");
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String subject = soln.getResource("subject").toString();
                String predicate = soln.getResource("predicate").toString();
                String object = soln.get("object").toString();
                writer.write(subject + "," + predicate + "," + object + "\n");
            }
            System.out.println("SPARQL query results saved to: " + resultsPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error executing SPARQL query.");
        }
    }
}
