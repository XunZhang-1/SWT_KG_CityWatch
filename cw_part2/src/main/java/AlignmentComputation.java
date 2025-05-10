import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AlignmentComputation {

    // This method will generate the alignment automatically
    public static void generateAlignment(String cityWatchOntologyPath, String lecturerOntologyPath, String alignmentOutputPath) {
        // Load CityWatch and Lecturer Ontologies
        Model cityWatchModel = ModelFactory.createDefaultModel();
        Model lecturerModel = ModelFactory.createDefaultModel();

        cityWatchModel.read(cityWatchOntologyPath, "TURTLE");
        lecturerModel.read(lecturerOntologyPath, "TURTLE");

        // Create an empty alignment model
        Model alignmentModel = ModelFactory.createDefaultModel();
        Property sameAs = alignmentModel.createProperty("http://www.w3.org/2002/07/owl#sameAs");

        // Get all classes from CityWatch ontology
        StmtIterator cityWatchClasses = cityWatchModel.listStatements(null, RDF.type, OWL.Class);
        // Get all classes from Lecturer ontology
        StmtIterator lecturerClasses = lecturerModel.listStatements(null, RDF.type, OWL.Class);

        // Debugging: Print out CityWatch and Lecturer Classes
        System.out.println("CityWatch Classes:");
        while (cityWatchClasses.hasNext()) {
            Statement stmt = cityWatchClasses.next();
            Resource resource = stmt.getSubject();
            System.out.println("Class: " + resource.getURI());
        }

        System.out.println("\nLecturer Classes:");
        while (lecturerClasses.hasNext()) {
            Statement stmt = lecturerClasses.next();
            Resource resource = stmt.getSubject();
            System.out.println("Class: " + resource.getURI());
        }

        // Iterate over CityWatch classes and try to find matching classes in Lecturer ontology
        cityWatchClasses = cityWatchModel.listStatements(null, RDF.type, OWL.Class);  // Reiterate as we need to reset iterator
        while (cityWatchClasses.hasNext()) {
            Statement cityWatchClassStmt = cityWatchClasses.next();
            Resource cityWatchClass = cityWatchClassStmt.getSubject();
            String cityWatchClassName = cityWatchClass.getLocalName();

            // Now compare with all Lecturer classes
            lecturerClasses = lecturerModel.listStatements(null, RDF.type, OWL.Class); // Reiterate
            while (lecturerClasses.hasNext()) {
                Statement lecturerClassStmt = lecturerClasses.next();
                Resource lecturerClass = lecturerClassStmt.getSubject();
                String lecturerClassName = lecturerClass.getLocalName();

                // Debugging: Check class names before matching
                System.out.println("Comparing: " + cityWatchClassName + " with " + lecturerClassName);

                // If class names are the same, create an alignment
                if (cityWatchClassName.equalsIgnoreCase(lecturerClassName)) {
                    alignmentModel.add(cityWatchClass, sameAs, lecturerClass);
                    System.out.println("Aligned class: " + cityWatchClassName + " -> " + lecturerClassName);
                }
            }
        }

        // Reset the iterators to iterate over properties (like object properties, datatype properties, etc.)
        StmtIterator cityWatchProperties = cityWatchModel.listStatements(null, RDF.type, RDF.Property);
        StmtIterator lecturerProperties = lecturerModel.listStatements(null, RDF.type, RDF.Property);

        // Debugging: Print out CityWatch and Lecturer Properties
        System.out.println("\nCityWatch Properties:");
        while (cityWatchProperties.hasNext()) {
            Statement stmt = cityWatchProperties.next();
            Resource resource = stmt.getSubject();
            System.out.println("Property: " + resource.getURI());
        }

        System.out.println("\nLecturer Properties:");
        while (lecturerProperties.hasNext()) {
            Statement stmt = lecturerProperties.next();
            Resource resource = stmt.getSubject();
            System.out.println("Property: " + resource.getURI());
        }

        // Iterate over CityWatch properties and try to find matching properties in Lecturer ontology
        cityWatchProperties = cityWatchModel.listStatements(null, RDF.type, RDF.Property);  // Reiterate
        while (cityWatchProperties.hasNext()) {
            Statement cityWatchPropertyStmt = cityWatchProperties.next();
            Resource cityWatchProperty = cityWatchPropertyStmt.getSubject();
            String cityWatchPropertyName = cityWatchProperty.getLocalName();

            // Now compare with all Lecturer properties
            lecturerProperties = lecturerModel.listStatements(null, RDF.type, RDF.Property); // Reiterate
            while (lecturerProperties.hasNext()) {
                Statement lecturerPropertyStmt = lecturerProperties.next();
                Resource lecturerProperty = lecturerPropertyStmt.getSubject();
                String lecturerPropertyName = lecturerProperty.getLocalName();

                // Debugging: Check property names before matching
                System.out.println("Comparing: " + cityWatchPropertyName + " with " + lecturerPropertyName);

                // If property names are the same, create an alignment
                if (cityWatchPropertyName.equalsIgnoreCase(lecturerPropertyName)) {
                    alignmentModel.add(cityWatchProperty, sameAs, lecturerProperty);
                    System.out.println("Aligned property: " + cityWatchPropertyName + " -> " + lecturerPropertyName);
                }
            }
        }

        // Save the alignment to a TTL file
        try {
            alignmentModel.write(new FileOutputStream(new File(alignmentOutputPath)), "TURTLE");
            System.out.println("Alignment saved to: " + alignmentOutputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        generateAlignment("files/CityWatch_Ontology.ttl", "files/Onto_Lecture.ttl", "files/reference_alignment.ttl");
    }
}
