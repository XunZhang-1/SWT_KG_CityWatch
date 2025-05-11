import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AlignmentComputation {

    public static void generateAlignment(String cityWatchOntologyPath, String lecturerOntologyPath, String alignmentOutputPath) {
        // Load ontologies
        Model cityWatchModel = ModelFactory.createDefaultModel();
        Model lecturerModel = ModelFactory.createDefaultModel();

        cityWatchModel.read(cityWatchOntologyPath, "TURTLE");
        lecturerModel.read(lecturerOntologyPath, "TURTLE");

        // Create alignment model
        Model alignmentModel = ModelFactory.createDefaultModel();
        Property equivalentClass = OWL.equivalentClass;
        Property equivalentProperty = OWL.equivalentProperty;

        // Threshold for name length (to avoid short ambiguous terms)
        final int NAME_MIN_LENGTH = 4;

        // ----------------- Align Classes -----------------
        StmtIterator cityWatchClasses = cityWatchModel.listStatements(null, RDF.type, OWL.Class);
        while (cityWatchClasses.hasNext()) {
            Resource cwClass = cityWatchClasses.next().getSubject();
            String cwName = cwClass.getLocalName();

            if (cwName == null || cwName.length() < NAME_MIN_LENGTH) continue;

            StmtIterator lecturerClasses = lecturerModel.listStatements(null, RDF.type, OWL.Class);
            while (lecturerClasses.hasNext()) {
                Resource lecturerClass = lecturerClasses.next().getSubject();
                String lecturerName = lecturerClass.getLocalName();

                if (lecturerName == null || lecturerName.length() < NAME_MIN_LENGTH) continue;

                if (cwName.equalsIgnoreCase(lecturerName)) {
                    alignmentModel.add(cwClass, equivalentClass, lecturerClass);
                    System.out.println("✔️ Aligned class: " + cwName + " ≡ " + lecturerName);
                }
            }
        }

        // ----------------- Align Properties -----------------
        StmtIterator cityWatchProps = cityWatchModel.listStatements(null, RDF.type, RDF.Property);
        while (cityWatchProps.hasNext()) {
            Resource cwProp = cityWatchProps.next().getSubject();
            String cwName = cwProp.getLocalName();

            if (cwName == null || cwName.length() < NAME_MIN_LENGTH) continue;

            StmtIterator lecturerProps = lecturerModel.listStatements(null, RDF.type, RDF.Property);
            while (lecturerProps.hasNext()) {
                Resource lecturerProp = lecturerProps.next().getSubject();
                String lecturerName = lecturerProp.getLocalName();

                if (lecturerName == null || lecturerName.length() < NAME_MIN_LENGTH) continue;

                if (cwName.equalsIgnoreCase(lecturerName)) {
                    alignmentModel.add(cwProp, equivalentProperty, lecturerProp);
                    System.out.println("✔️ Aligned property: " + cwName + " ≡ " + lecturerName);
                }
            }
        }

        // ----------------- Save alignment model -----------------
        try {
            alignmentModel.write(new FileOutputStream(new File(alignmentOutputPath)), "TURTLE");
            System.out.println("✅ Alignment saved to: " + alignmentOutputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Optional main method for testing independently
    public static void main(String[] args) {
        generateAlignment(
                "cw_part2/files/CityWatch_Ontology.ttl",
                "cw_part2/files/Onto_Lecture.ttl",
                "cw_part2/files/output/computed_alignment.ttl"
        );
    }
}
