import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class OntologyAligner {

    /**
     * Computes precision, recall, and F1 score by comparing computed alignment with reference alignment
     */
    public void computePrecisionRecall(String computedAlignmentPath, String referenceAlignmentPath) {
        // Load the computed alignment and reference alignment
        Model computedModel = ModelFactory.createDefaultModel();
        Model referenceModel = ModelFactory.createDefaultModel();
        
        try {
            File computedFile = new File(computedAlignmentPath);
            File referenceFile = new File(referenceAlignmentPath);
            
            if (!computedFile.exists()) {
                System.err.println("ERROR: Computed alignment file not found: " + computedAlignmentPath);
                return;
            }
            
            if (!referenceFile.exists()) {
                System.err.println("ERROR: Reference alignment file not found: " + referenceAlignmentPath);
                return;
            }
            
            System.out.println("Loading computed alignment from: " + computedFile.getAbsolutePath());
            computedModel.read(computedFile.toURI().toString(), "TURTLE");
            System.out.println("Computed alignment size: " + computedModel.size());
            
            System.out.println("Loading reference alignment from: " + referenceFile.getAbsolutePath());
            referenceModel.read(referenceFile.toURI().toString(), "TURTLE");
            System.out.println("Reference alignment size: " + referenceModel.size());
            
        } catch (Exception e) {
            System.err.println("Error loading alignment models: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        // Create sets to store alignment statements for comparison
        Set<AlignmentPair> computedAlignments = new HashSet<>();
        Set<AlignmentPair> referenceAlignments = new HashSet<>();
        
        // Extract all types of equivalence statements from computed alignment
        collectAlignments(computedModel, computedAlignments);
        collectAlignments(referenceModel, referenceAlignments);
        
        // Count the number of correct alignments (true positives)
        Set<AlignmentPair> truePositives = new HashSet<>();
        
        for (AlignmentPair computedPair : computedAlignments) {
            for (AlignmentPair referencePair : referenceAlignments) {
                if (computedPair.equals(referencePair)) {
                    truePositives.add(computedPair);
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
        for (AlignmentPair pair : truePositives) {
            System.out.println(pair);
        }
        
        // Display missing alignments from reference (false negatives)
        System.out.println("\n=== Missing Alignments (False Negatives) ===");
        for (AlignmentPair pair : referenceAlignments) {
            if (!truePositives.contains(pair)) {
                System.out.println(pair);
            }
        }
        
        // Display incorrect alignments in computed (false positives)
        System.out.println("\n=== Incorrect Alignments (False Positives) ===");
        for (AlignmentPair pair : computedAlignments) {
            if (!truePositives.contains(pair)) {
                System.out.println(pair);
            }
        }
    }
    
    /**
     * Collects all alignment statements from a model and stores them as AlignmentPair objects
     */
    private void collectAlignments(Model model, Set<AlignmentPair> alignments) {
        // Get all equivalentClass statements
        StmtIterator classIter = model.listStatements(null, OWL.equivalentClass, (RDFNode)null);
        while (classIter.hasNext()) {
            Statement stmt = classIter.next();
            if (stmt.getObject().isResource()) {
                AlignmentPair pair = new AlignmentPair(stmt.getSubject(), stmt.getObject().asResource());
                alignments.add(pair);
            }
        }
        
        // Get all equivalentProperty statements
        StmtIterator propIter = model.listStatements(null, OWL.equivalentProperty, (RDFNode)null);
        while (propIter.hasNext()) {
            Statement stmt = propIter.next();
            if (stmt.getObject().isResource()) {
                AlignmentPair pair = new AlignmentPair(stmt.getSubject(), stmt.getObject().asResource());
                alignments.add(pair);
            }
        }
        
        // Get all sameAs statements
        StmtIterator sameAsIter = model.listStatements(null, OWL.sameAs, (RDFNode)null);
        while (sameAsIter.hasNext()) {
            Statement stmt = sameAsIter.next();
            if (stmt.getObject().isResource()) {
                AlignmentPair pair = new AlignmentPair(stmt.getSubject(), stmt.getObject().asResource());
                alignments.add(pair);
            }
        }
    }
    
    /**
     * Helper class to represent an alignment pair and provide proper equals/hashCode
     */
    private static class AlignmentPair {
        private final String source;
        private final String target;
        
        public AlignmentPair(Resource source, Resource target) {
            // Store canonicalized URIs (always have source lexicographically before target)
            if (source.getURI().compareTo(target.getURI()) <= 0) {
                this.source = source.getURI();
                this.target = target.getURI();
            } else {
                this.source = target.getURI();
                this.target = source.getURI();
            }
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof AlignmentPair)) return false;
            AlignmentPair other = (AlignmentPair) obj;
            return source.equals(other.source) && target.equals(other.target);
        }
        
        @Override
        public int hashCode() {
            return source.hashCode() * 31 + target.hashCode();
        }
        
        @Override
        public String toString() {
            return source + " <-> " + target;
        }
    }
    
    /**
     * Debug method to print the content of an alignment model
     */
    public void debugAlignmentModel(String alignmentPath) {
        try {
            Model model = ModelFactory.createDefaultModel();
            model.read(alignmentPath, "TURTLE");
            
            System.out.println("=== Alignment Model Content ===");
            System.out.println("Size: " + model.size());
            
            System.out.println("\nequivalentClass statements:");
            StmtIterator classIter = model.listStatements(null, OWL.equivalentClass, (RDFNode)null);
            int classCount = 0;
            while (classIter.hasNext()) {
                Statement stmt = classIter.next();
                System.out.println(stmt.getSubject().getURI() + " <-> " + stmt.getObject());
                classCount++;
            }
            System.out.println("Total equivalentClass: " + classCount);
            
            System.out.println("\nequivalentProperty statements:");
            StmtIterator propIter = model.listStatements(null, OWL.equivalentProperty, (RDFNode)null);
            int propCount = 0;
            while (propIter.hasNext()) {
                Statement stmt = propIter.next();
                System.out.println(stmt.getSubject().getURI() + " <-> " + stmt.getObject());
                propCount++;
            }
            System.out.println("Total equivalentProperty: " + propCount);
            
            System.out.println("\nsameAs statements:");
            StmtIterator sameAsIter = model.listStatements(null, OWL.sameAs, (RDFNode)null);
            int sameAsCount = 0;
            while (sameAsIter.hasNext()) {
                Statement stmt = sameAsIter.next();
                System.out.println(stmt.getSubject().getURI() + " <-> " + stmt.getObject());
                sameAsCount++;
            }
            System.out.println("Total sameAs: " + sameAsCount);
            
        } catch (Exception e) {
            System.err.println("Error debugging alignment model: " + e.getMessage());
            e.printStackTrace();
        }
    }
}