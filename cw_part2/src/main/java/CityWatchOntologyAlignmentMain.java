import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.RDFDataMgr;

import static org.apache.jena.enhanced.BuiltinPersonalities.model;

public class CityWatchOntologyAlignmentMain {
    public static void main(String[] args) {
        // New constructor: allows direct Model input (for reasoned model use)

        // === Path Setup ===
        String cityWatchOntologyPath = "cw_part2/files/CityWatch_Ontology.ttl";
        String lecturerOntologyPath = "cw_part2/files/Onto_Lecture.ttl";
        String referenceAlignmentPath = "cw_part2/files/reference_alignment.ttl";
        String computedAlignmentPath = "cw_part2/files/output/computed_alignment.ttl";
        String combinedModelPath = "cw_part2/files/output/combined_model_after_reasoning.ttl";
        String sparqlResultsPath = "cw_part2/files/output/sparql_query_results.csv";

        // === Generate Alignment (OA.1) ===
        System.out.println("Computing equivalences between the ontologies...");
        AlignmentComputation.generateAlignment(cityWatchOntologyPath, lecturerOntologyPath, computedAlignmentPath);

        // === Evaluate Alignment (OA.2) ===
        System.out.println("Evaluating precision and recall...");
        OntologyAligner aligner = new OntologyAligner(cityWatchOntologyPath, lecturerOntologyPath, computedAlignmentPath);
        aligner.computePrecisionRecall(referenceAlignmentPath);

        // === Reasoning with All Sources (OA.3) ===
        System.out.println("Performing reasoning with all sources...");
        Model cityWatchModel = RDFDataMgr.loadModel(cityWatchOntologyPath);
        Model lecturerModel = RDFDataMgr.loadModel(lecturerOntologyPath);
        Model alignmentModel = RDFDataMgr.loadModel(computedAlignmentPath);

        Model mergedModel = ModelFactory.createDefaultModel();
        mergedModel.add(cityWatchModel);
        mergedModel.add(lecturerModel);
        mergedModel.add(alignmentModel);

        Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
        Model reasonedModel = ModelFactory.createInfModel(reasoner, mergedModel);

        // Save reasoned model
        try {
            reasonedModel.write(new java.io.FileOutputStream(combinedModelPath), "TURTLE");
            System.out.println("✅ Saved reasoned model to: " + combinedModelPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // === SPARQL Query (OA.4) using Lecturer Vocabulary ===
        System.out.println("Executing SPARQL query using lecturer vocabulary...");
        String sparqlQuery =
                "PREFIX lec: <http://example.com/lecturer#>\n" +
                        "SELECT ?accident ?lightCondition\n" +
                        "WHERE {\n" +
                        "  ?accident lec:hasLightCondition ?lightCondition .\n" +
                        "}\n" +
                        "LIMIT 10";

        SPARQLQueryUtil sparqlUtil = new SPARQLQueryUtil(reasonedModel);
        sparqlUtil.executeQuery(sparqlQuery, sparqlResultsPath);
        System.out.println("✅ SPARQL query results saved to: " + sparqlResultsPath);

    }
}
