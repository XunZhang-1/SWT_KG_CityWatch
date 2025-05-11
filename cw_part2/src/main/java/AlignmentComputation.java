import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AlignmentComputation {

    // This method will generate the alignment automatically
    public static void generateAlignment(String cityWatchOntologyPath, String roadAccidentOntologyPath, String alignmentOutputPath) {
        // Load CityWatch and Road Accident Ontologies
        Model cityWatchModel = ModelFactory.createDefaultModel();
        Model roadAccidentModel = ModelFactory.createDefaultModel();

        cityWatchModel.read(cityWatchOntologyPath, "TURTLE");
        roadAccidentModel.read(roadAccidentOntologyPath, "TURTLE");

        // Create an empty alignment model
        Model alignmentModel = ModelFactory.createDefaultModel();

        // Get all classes from CityWatch ontology
        StmtIterator cityWatchClasses = cityWatchModel.listStatements(null, RDF.type, OWL.Class);
        // Get all classes from Road Accident ontology
        StmtIterator roadAccidentClasses = roadAccidentModel.listStatements(null, RDF.type, OWL.Class);

        // Debugging: Print out CityWatch and Road Accident Classes
        System.out.println("CityWatch Classes:");
        while (cityWatchClasses.hasNext()) {
            Statement stmt = cityWatchClasses.next();
            Resource resource = stmt.getSubject();
            System.out.println("Class: " + resource.getURI());
        }

        System.out.println("\nRoad Accident Classes:");
        while (roadAccidentClasses.hasNext()) {
            Statement stmt = roadAccidentClasses.next();
            Resource resource = stmt.getSubject();
            System.out.println("Class: " + resource.getURI());
        }

        // Iterate over CityWatch classes and try to find matching classes in Road Accident ontology
        cityWatchClasses = cityWatchModel.listStatements(null, RDF.type, OWL.Class);  // Reiterate as we need to reset iterator
        while (cityWatchClasses.hasNext()) {
            Statement cityWatchClassStmt = cityWatchClasses.next();
            Resource cityWatchClass = cityWatchClassStmt.getSubject();
            String cityWatchClassName = cityWatchClass.getLocalName();

            // Now compare with all Road Accident classes
            roadAccidentClasses = roadAccidentModel.listStatements(null, RDF.type, OWL.Class); // Reiterate
            while (roadAccidentClasses.hasNext()) {
                Statement roadAccidentClassStmt = roadAccidentClasses.next();
                Resource roadAccidentClass = roadAccidentClassStmt.getSubject();
                String roadAccidentClassName = roadAccidentClass.getLocalName();

                // Debugging: Check class names before matching
                System.out.println("Comparing: " + cityWatchClassName + " with " + roadAccidentClassName);

                // If class names are the same, create an alignment
                if (cityWatchClassName != null && roadAccidentClassName != null && 
                    cityWatchClassName.equalsIgnoreCase(roadAccidentClassName)) {
                    // Using owl:equivalentClass for classes
                    alignmentModel.add(cityWatchClass, OWL.equivalentClass, roadAccidentClass);
                    System.out.println("Aligned class: " + cityWatchClassName + " -> " + roadAccidentClassName);
                }
            }
        }

        // Reset the iterators to iterate over properties (like object properties, datatype properties, etc.)
        StmtIterator cityWatchProperties = cityWatchModel.listStatements(null, RDF.type, RDF.Property);
        StmtIterator roadAccidentProperties = roadAccidentModel.listStatements(null, RDF.type, RDF.Property);

        // Debugging: Print out CityWatch and Road Accident Properties
        System.out.println("\nCityWatch Properties:");
        while (cityWatchProperties.hasNext()) {
            Statement stmt = cityWatchProperties.next();
            Resource resource = stmt.getSubject();
            System.out.println("Property: " + resource.getURI());
        }

        System.out.println("\nRoad Accident Properties:");
        while (roadAccidentProperties.hasNext()) {
            Statement stmt = roadAccidentProperties.next();
            Resource resource = stmt.getSubject();
            System.out.println("Property: " + resource.getURI());
        }

        // Iterate over CityWatch properties and try to find matching properties in Road Accident ontology
        cityWatchProperties = cityWatchModel.listStatements(null, RDF.type, RDF.Property);  // Reiterate
        while (cityWatchProperties.hasNext()) {
            Statement cityWatchPropertyStmt = cityWatchProperties.next();
            Resource cityWatchProperty = cityWatchPropertyStmt.getSubject();
            String cityWatchPropertyName = cityWatchProperty.getLocalName();

            // Now compare with all Road Accident properties
            roadAccidentProperties = roadAccidentModel.listStatements(null, RDF.type, RDF.Property); // Reiterate
            while (roadAccidentProperties.hasNext()) {
                Statement roadAccidentPropertyStmt = roadAccidentProperties.next();
                Resource roadAccidentProperty = roadAccidentPropertyStmt.getSubject();
                String roadAccidentPropertyName = roadAccidentProperty.getLocalName();

                // Debugging: Check property names before matching
                System.out.println("Comparing: " + cityWatchPropertyName + " with " + roadAccidentPropertyName);

                // If property names are the same, create an alignment
                if (cityWatchPropertyName != null && roadAccidentPropertyName != null && 
                    cityWatchPropertyName.equalsIgnoreCase(roadAccidentPropertyName)) {
                    // Using owl:equivalentProperty for properties
                    alignmentModel.add(cityWatchProperty, OWL.equivalentProperty, roadAccidentProperty);
                    System.out.println("Aligned property: " + cityWatchPropertyName + " -> " + roadAccidentPropertyName);
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