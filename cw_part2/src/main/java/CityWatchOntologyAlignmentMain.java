import java.io.File;

public class CityWatchOntologyAlignmentMain {
	public static void main(String[] args) {
        // Define file paths
        String cityWatchOntologyPath = "files/CityWatch_Ontology.ttl";
        String roadAccidentOntologyPath = "files/Onto_Lecture.ttl"; 
        String computedAlignmentPath = "files/output/computed_alignment.ttl";
        String referenceAlignmentPath = "files/reference_alignment.ttl";
        String sparqlResultsPath = "files/output/sparql_results.csv";
        
        // Ensure output directory exists
        File outputDir = new File("files/output");
        if (!outputDir.exists()) {
            System.out.println("Creating output directory: " + outputDir.getAbsolutePath());
            if (outputDir.mkdirs()) {
                System.out.println("Output directory created successfully.");
            } else {
                System.err.println("Failed to create output directory!");
                return; // Exit if we can't create the output directory
            }
        } else {
            System.out.println("Output directory already exists: " + outputDir.getAbsolutePath());
        }
        
        // Step 0: Create reference alignment if it doesn't exist
        System.out.println("\nChecking reference alignment...");
        java.io.File referenceFile = new java.io.File(referenceAlignmentPath);
        if (!referenceFile.exists()) {
            System.out.println("Reference alignment not found. Creating it...");
            ReferenceAlignmentCreator.createReferenceAlignment(
                cityWatchOntologyPath, roadAccidentOntologyPath, referenceAlignmentPath);
        } else {
            System.out.println("Reference alignment found at: " + referenceAlignmentPath);
        }
        
        // Step 1: Generate alignment between ontologies
        System.out.println("\nGenerating alignment between CityWatch Ontology and Road Accident Ontology...");
        try {
            AlignmentComputation.generateAlignment(cityWatchOntologyPath, roadAccidentOntologyPath, computedAlignmentPath);
        } catch (Exception e) {
            System.err.println("Error during alignment computation:");
            e.printStackTrace();
            return;
        }
        
        // Verify the computed alignment file exists
        File computedFile = new File(computedAlignmentPath);
        if (!computedFile.exists()) {
            System.err.println("ERROR: Computed alignment file was not created: " + computedAlignmentPath);
            return;
        } else {
            System.out.println("Verified: Computed alignment file exists at: " + computedFile.getAbsolutePath());
            System.out.println("File size: " + computedFile.length() + " bytes");
        }
        
        // Step 2: Evaluate alignment against reference
        System.out.println("\nEvaluating computed alignment against reference alignment...");
        OntologyAligner aligner = new OntologyAligner();
        aligner.computePrecisionRecall(computedAlignmentPath, referenceAlignmentPath);
        
        // Step 3: Use the alignment with SPARQL
        System.out.println("\nExecuting SPARQL query over merged ontologies...");
        SPARQLQueryUtil sparqlQueryUtil = new SPARQLQueryUtil(
            cityWatchOntologyPath, roadAccidentOntologyPath, computedAlignmentPath);
        
        // Display model content for debugging
        sparqlQueryUtil.displayModelContent();
        
        // Execute SPARQL queries using road accident ontology vocabulary
        System.out.println("\nExecuting SPARQL query using road accident ontology vocabulary...");
        sparqlQueryUtil.ensureOutputDirectoryExists(sparqlResultsPath);
        sparqlQueryUtil.executeDriversAndVehiclesQuery(sparqlResultsPath);
        
        // Execute additional queries to test alignment
        System.out.println("\nExecuting accident severity query...");
        sparqlQueryUtil.executeAccidentSeverityQuery("files/output/accident_severity_results.csv");
        
        System.out.println("\nExecuting query for people and vehicles involved in accidents...");
        sparqlQueryUtil.executeAccidentInvolvedQuery("files/output/accident_involved_results.csv");
        
        System.out.println("\nAll SPARQL queries executed successfully.");
    }
}