import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;

import java.util.HashSet;
import java.util.Set;

public class OntologyAligner {

    public void computePrecisionRecall(String computedAlignmentPath, String referenceAlignmentPath) {
        // Load the computed alignment and reference alignment
        Model computedModel = ModelFactory.createDefaultModel();
        Model referenceModel = ModelFactory.createDefaultModel();
        
        try {
            computedModel.read(computedAlignmentPath, "TURTLE");
            referenceModel.read(referenceAlignmentPath, "TURTLE");
        } catch (Exception e) {
            System.err.println("Error loading alignment models: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        // Create sets to store alignment statements for comparison
        Set<Statement> computedAlignments = new HashSet<>();
        Set<Statement> referenceAlignments = new HashSet<>();
        
        // Extract equivalence statements from computed alignment
        StmtIterator computedIter = computedModel.listStatements(null, OWL.sameAs, (RDFNode)null);
        while (computedIter.hasNext()) {
            computedAlignments.add(computedIter.next());
        }
        
        // Also collect equivalentClass statements
        StmtIterator computedClassIter = computedModel.listStatements(null, OWL.equivalentClass, (RDFNode)null);
        while (computedClassIter.hasNext()) {
            computedAlignments.add(computedClassIter.next());
        }
        
        // Also collect equivalentProperty statements
        StmtIterator computedPropIter = computedModel.listStatements(null, OWL.equivalentProperty, (RDFNode)null);
        while (computedPropIter.hasNext()) {
            computedAlignments.add(computedPropIter.next());
        }
        
        // Extract equivalence statements from reference alignment
        StmtIterator referenceIter = referenceModel.listStatements(null, OWL.sameAs, (RDFNode)null);
        while (referenceIter.hasNext()) {
            referenceAlignments.add(referenceIter.next());
        }
        
        // Also collect equivalentClass statements from reference
        StmtIterator referenceClassIter = referenceModel.listStatements(null, OWL.equivalentClass, (RDFNode)null);
        while (referenceClassIter.hasNext()) {
            referenceAlignments.add(referenceClassIter.next());
        }
        
        // Also collect equivalentProperty statements from reference
        StmtIterator referencePropIter = referenceModel.listStatements(null, OWL.equivalentProperty, (RDFNode)null);
        while (referencePropIter.hasNext()) {
            referenceAlignments.add(referencePropIter.next());
        }
        
        // Count the number of correct alignments (true positives)
        // We need to compare the subject-object pairs rather than the exact statements
        // as the same alignment might be represented differently in both models
        Set<Statement> truePositives = new HashSet<>();
        
        for (Statement computedStmt : computedAlignments) {
            Resource computedSubject = computedStmt.getSubject();
            RDFNode computedObject = computedStmt.getObject();
            
            for (Statement referenceStmt : referenceAlignments) {
                Resource referenceSubject = referenceStmt.getSubject();
                RDFNode referenceObject = referenceStmt.getObject();
                
                // If the subject and object URIs match (ignoring the property used)
                if (computedSubject.getURI().equals(referenceSubject.getURI()) &&
                    computedObject.toString().equals(referenceObject.toString())) {
                    truePositives.add(computedStmt);
                    break;
                }
            }
        }
        
        // Calculate precision and recall
        double precision = 0.0;
        double recall = 0.0;
        double f1Score = 0.0;
        
        if (!computedAlignments.isEmpty()) {
            precision = (double) truePositives.size() / computedAlignments.size();
        }
        
        if (!referenceAlignments.isEmpty()) {
            recall = (double) truePositives.size() / referenceAlignments.size();
        }
        
        // Calculate F1 score
        if (precision + recall > 0) {
            f1Score = 2 * precision * recall / (precision + recall);
        }
        
        // Print the results
        System.out.println("=== Alignment Evaluation Results ===");
        System.out.println("True Positives: " + truePositives.size());
        System.out.println("Total Computed Alignments: " + computedAlignments.size());
        System.out.println("Total Reference Alignments: " + referenceAlignments.size());
        System.out.println("Precision: " + precision);
        System.out.println("Recall: " + recall);
        System.out.println("F1 Score: " + f1Score);
        
        // Display the matching alignments for verification
        System.out.println("\n=== Matching Alignments ===");
        for (Statement stmt : truePositives) {
            System.out.println(stmt.getSubject().getURI() + " <-> " + stmt.getObject().toString());
        }
        
        // Display missing alignments from reference (false negatives)
        System.out.println("\n=== Missing Alignments (False Negatives) ===");
        Set<String> matchedPairs = new HashSet<>();
        
        // First, collect all the matched subject-object pairs
        for (Statement stmt : truePositives) {
            matchedPairs.add(stmt.getSubject().getURI() + " <-> " + stmt.getObject().toString());
        }
        
        // Then find the alignments in reference that don't match any of these pairs
        for (Statement referenceStmt : referenceAlignments) {
            String pair = referenceStmt.getSubject().getURI() + " <-> " + referenceStmt.getObject().toString();
            if (!matchedPairs.contains(pair)) {
                System.out.println(pair);
            }
        }
        
        // Display incorrect alignments in computed (false positives)
        System.out.println("\n=== Incorrect Alignments (False Positives) ===");
        for (Statement computedStmt : computedAlignments) {
            String pair = computedStmt.getSubject().getURI() + " <-> " + computedStmt.getObject().toString();
            if (!matchedPairs.contains(pair)) {
                System.out.println(pair);
            }
        }
    }
}