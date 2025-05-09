import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;

import java.io.File;
import java.io.IOException;

public class CityWatchOntologyAlignmentMain {

    public static void main(String[] args) throws IOException {
        // Paths to input and output files
		String cityWatchOntologyPath = "files/CityWatch_Ontology.ttl";
		String lecturerOntologyPath = "files/Onto_Lecture.ttl";
		String alignmentPath = "files/reference_alignment.ttl";
		String computedAlignmentPath = "files/output/computed_alignment.ttl";
		String combinedModelPath = "files/output/combined_model_after_reasoning.ttl";
		String sparqlResultsPath = "files/output/sparql_query_results.csv";

		// Initialize the SPARQLQueryUtil with the paths
		SPARQLQueryUtil sparqlQueryUtil = new SPARQLQueryUtil(cityWatchOntologyPath, lecturerOntologyPath, alignmentPath, "http://example.com/lecturer");

		// Load the ontologies and alignment
		sparqlQueryUtil.loadModels(cityWatchOntologyPath, lecturerOntologyPath, alignmentPath);

		// Compute equivalences (alignment), for now, we'll just simulate the equivalence computation
		System.out.println("Computing equivalences between the ontologies...");
		// You can replace this part with actual equivalence computation
		// Example: simulate some equivalences between the models
		File alignmentFile = new File(computedAlignmentPath);
		// Save simulated alignment
		sparqlQueryUtil.saveModel(computedAlignmentPath, "TURTLE");

		// Perform reasoning on the combined model
		System.out.println("Performing reasoning with all sources...");
		Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
		Model reasonedModel = ModelFactory.createInfModel(reasoner, sparqlQueryUtil.getModel());
		sparqlQueryUtil.saveModel(combinedModelPath, "TURTLE");

		// After reasoning, remove duplicates to avoid redundancy
		sparqlQueryUtil.removeDuplicates();
		System.out.println("Duplicates removed. Model size: " + sparqlQueryUtil.getModel().size());

		// Execute SPARQL query to get some results
		System.out.println("Executing SPARQL query...");
		String sparqlQuery = "SELECT ?subject ?predicate ?object WHERE { ?subject ?predicate ?object . } LIMIT 10";
		sparqlQueryUtil.executeQuery(sparqlQuery);

		// Save results to CSV
		// Save model after reasoning in TURTLE format (or any other format you prefer)
		sparqlQueryUtil.saveModel("files/output/combined_model_after_reasoning.ttl", "TURTLE");

		System.out.println("Ontology alignment process completed successfully!");
    }
}
