public class CityWatchOntologyAlignmentMain {
    public static void main(String[] args) {
        // Define file paths
        String cityWatchOntologyPath = "files/CityWatch_Ontology.ttl";
        String roadAccidentOntologyPath = "files/Onto_Lecture.ttl"; // The file name is misleading - it's actually a road accident ontology
        String computedAlignmentPath = "files/computed_alignment.ttl";
        String referenceAlignmentPath = "files/reference_alignment.ttl";
        String sparqlResultsPath = "files/sparql_results.csv";
        
        // Step 1: Generate alignment between ontologies
        System.out.println("Generating alignment between CityWatch Ontology and Road Accident Ontology...");
        AlignmentComputation.generateAlignment(cityWatchOntologyPath, roadAccidentOntologyPath, computedAlignmentPath);
        
        // Step 2: Evaluate alignment against reference
        System.out.println("\nEvaluating computed alignment against reference alignment...");
        OntologyAligner aligner = new OntologyAligner();
        aligner.computePrecisionRecall(computedAlignmentPath, referenceAlignmentPath);
        
        // Step 3: Use the alignment with SPARQL
        System.out.println("\nExecuting SPARQL query over merged ontologies...");
        SPARQLQueryUtil sparqlQueryUtil = new SPARQLQueryUtil(
            cityWatchOntologyPath, roadAccidentOntologyPath, computedAlignmentPath);
        
        // Display model content for debugging (new)
        sparqlQueryUtil.displayModelContent();
        
        // Execute a SPARQL query using road accident ontology vocabulary
        System.out.println("\nExecuting SPARQL query using road accident ontology vocabulary...");
        sparqlQueryUtil.executeLecturerVocabularyQuery(sparqlResultsPath);
    }
}