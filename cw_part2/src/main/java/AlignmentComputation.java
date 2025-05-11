import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class AlignmentComputation {
    
    // Known namespaces to exclude
    private static final Set<String> EXCLUDED_NAMESPACES = Set.of(
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        "http://www.w3.org/2000/01/rdf-schema#",
        "http://www.w3.org/2002/07/owl#"
    );

    public static void generateAlignment(String cityWatchOntologyPath, 
                                      String roadAccidentOntologyPath, 
                                      String alignmentOutputPath) {
        try {
            // Load models
            Model cityWatchModel = ModelFactory.createDefaultModel();
            Model roadAccidentModel = ModelFactory.createDefaultModel();
            
            System.out.println("Loading CityWatch ontology from: " + cityWatchOntologyPath);
            File cityWatchFile = new File(cityWatchOntologyPath);
            cityWatchModel.read(cityWatchFile.toURI().toString(), "TURTLE");
            System.out.println("CityWatch ontology loaded. Size: " + cityWatchModel.size());
            
            System.out.println("Loading Road Accident ontology from: " + roadAccidentOntologyPath);
            File roadAccidentFile = new File(roadAccidentOntologyPath);
            roadAccidentModel.read(roadAccidentFile.toURI().toString(), "TURTLE");
            System.out.println("Road Accident ontology loaded. Size: " + roadAccidentModel.size());
            
            // Create alignment model
            Model alignmentModel = ModelFactory.createDefaultModel();
            
            // Get namespaces
            String cityWatchNS = findMainNamespace(cityWatchModel);
            String roadAccidentNS = findMainNamespace(roadAccidentModel);
            
            System.out.println("CityWatch Namespace: " + cityWatchNS);
            System.out.println("Road Accident Namespace: " + roadAccidentNS);
            
            // Add explicit namespace declarations
            alignmentModel.setNsPrefix("owl", OWL.getURI());
            alignmentModel.setNsPrefix("rdf", RDF.getURI());
            alignmentModel.setNsPrefix("rdfs", RDFS.getURI());
            
            // Add ontology namespaces
            if (!cityWatchNS.isEmpty()) {
                String prefix = "cw";
                alignmentModel.setNsPrefix(prefix, cityWatchNS);
            }
            
            if (!roadAccidentNS.isEmpty()) {
                String prefix = "ra";
                alignmentModel.setNsPrefix(prefix, roadAccidentNS);
            }
            
            // Add ontology declaration to make the file a valid OWL ontology
            Resource alignmentOntology = alignmentModel.createResource(
                "http://example.org/alignment#",
                OWL.Ontology
            );
            
            // Add imports for both ontologies
            if (cityWatchFile.toURI().toString().startsWith("file:")) {
                alignmentOntology.addProperty(
                    OWL.imports, 
                    alignmentModel.createResource(cityWatchFile.toURI().toString())
                );
            }
            
            if (roadAccidentFile.toURI().toString().startsWith("file:")) {
                alignmentOntology.addProperty(
                    OWL.imports, 
                    alignmentModel.createResource(roadAccidentFile.toURI().toString())
                );
            }
    
            // Find classes and properties
            List<Resource> cityWatchClasses = findAllClasses(cityWatchModel, cityWatchNS);
            List<Resource> roadAccidentClasses = findAllClasses(roadAccidentModel, roadAccidentNS);
            
            List<Resource> cityWatchProps = findAllProperties(cityWatchModel, cityWatchNS);
            List<Resource> roadAccidentProps = findAllProperties(roadAccidentModel, roadAccidentNS);
    
            // Debug output
            System.out.println("\nCityWatch Classes (" + cityWatchClasses.size() + "):");
            cityWatchClasses.forEach(c -> System.out.println(" - " + c.getURI()));
            
            System.out.println("\nRoad Accident Classes (" + roadAccidentClasses.size() + "):");
            roadAccidentClasses.forEach(c -> System.out.println(" - " + c.getURI()));
            
            System.out.println("\nCityWatch Properties (" + cityWatchProps.size() + "):");
            cityWatchProps.forEach(p -> System.out.println(" - " + p.getURI()));
            
            System.out.println("\nRoad Accident Properties (" + roadAccidentProps.size() + "):");
            roadAccidentProps.forEach(p -> System.out.println(" - " + p.getURI()));
    
            // Align classes
            alignResources(cityWatchClasses, roadAccidentClasses, OWL.equivalentClass, alignmentModel);
            
            // Align properties
            alignResources(cityWatchProps, roadAccidentProps, OWL.equivalentProperty, alignmentModel);
    
            // Create output directory if it doesn't exist
            File outputFile = new File(alignmentOutputPath);
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
                System.out.println("Created directory: " + outputFile.getParentFile().getAbsolutePath());
            }
            
            // Check if alignment model is empty
            if (alignmentModel.isEmpty()) {
                System.out.println("WARNING: Alignment model is empty! No alignments were found.");
                
                // Add at least one statement to avoid empty file
                alignmentModel.add(
                    alignmentModel.createResource("http://example.org/alignment#EmptyAlignment"), 
                    RDFS.comment, 
                    "No alignments were found between the ontologies."
                );
            }
            
            // Save alignment
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                alignmentModel.write(fos, "TURTLE");
                System.out.println("\nAlignment saved to: " + outputFile.getAbsolutePath());
                System.out.println("Total alignments created: " + alignmentModel.size());
            }
            
            // Debug - print alignment to console
            System.out.println("\nAlignment Model Content:");
            alignmentModel.write(System.out, "TURTLE");
            
            // Report alignment statistics
            int classAlignments = alignmentModel.listStatements(null, OWL.equivalentClass, (RDFNode)null).toList().size();
            int propAlignments = alignmentModel.listStatements(null, OWL.equivalentProperty, (RDFNode)null).toList().size();
            
            System.out.println("\nAlignment Statistics:");
            System.out.println("Class alignments: " + classAlignments);
            System.out.println("Property alignments: " + propAlignments);
            System.out.println("Total alignments: " + (classAlignments + propAlignments));
            
        } catch (Exception e) {
            System.err.println("Error generating alignment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void alignResources(List<Resource> resources1, 
                                     List<Resource> resources2, 
                                     Property relation, 
                                     Model alignmentModel) {
        int alignmentCount = 0;
        
        for (Resource r1 : resources1) {
            String name1 = getLocalName(r1);
            if (name1 == null) continue;
            
            for (Resource r2 : resources2) {
                String name2 = getLocalName(r2);
                if (name2 == null) continue;

                if (compareNames(name1, name2)) {
                    alignmentModel.add(r1, relation, r2);
                    alignmentCount++;
                    System.out.println("Aligned: " + name1 + " <-> " + name2 + " (" + r1.getURI() + " <-> " + r2.getURI() + ")");
                }
            }
        }
        
        if (alignmentCount == 0) {
            System.out.println("WARNING: No alignments found for this resource type!");
        } else {
            System.out.println(alignmentCount + " alignments found.");
        }
    }

    private static List<Resource> findAllClasses(Model model, String namespace) {
        Set<Resource> classes = new HashSet<>();
        
        // Find explicit class declarations
        model.listStatements(null, RDF.type, OWL.Class)
             .mapWith(Statement::getSubject)
             .filterKeep(r -> r.isURIResource() && r.getURI().startsWith(namespace))
             .forEach(classes::add);
        
        // Find RDFS class declarations
        model.listStatements(null, RDF.type, RDFS.Class)
             .mapWith(Statement::getSubject)
             .filterKeep(r -> r.isURIResource() && r.getURI().startsWith(namespace))
             .forEach(classes::add);

        // Find resources used as types
        model.listStatements(null, RDF.type, (RDFNode)null)
             .mapWith(Statement::getObject)
             .filterKeep(RDFNode::isURIResource)
             .mapWith(RDFNode::asResource)
             .filterDrop(r -> EXCLUDED_NAMESPACES.stream().anyMatch(ns -> r.getURI().startsWith(ns)))
             .filterKeep(r -> r.getURI().startsWith(namespace))
             .forEach(classes::add);

        return new ArrayList<>(classes);
    }

    private static List<Resource> findAllProperties(Model model, String namespace) {
        Set<Resource> properties = new HashSet<>();
        
        // Find all properties (Object, Datatype, Annotation)
        model.listStatements(null, RDF.type, (RDFNode)null)
             .filterKeep(stmt -> 
                 stmt.getObject().equals(RDF.Property) ||
                 stmt.getObject().equals(OWL.ObjectProperty) ||
                 stmt.getObject().equals(OWL.DatatypeProperty) ||
                 stmt.getObject().equals(OWL.AnnotationProperty))
             .mapWith(Statement::getSubject)
             .filterKeep(r -> r.isURIResource() && r.getURI().startsWith(namespace))
             .forEach(properties::add);

        // Find properties used in statements
        model.listStatements()
             .mapWith(Statement::getPredicate)
             .filterKeep(p -> p.isURIResource() && p.getURI().startsWith(namespace))
             .filterDrop(p -> EXCLUDED_NAMESPACES.stream().anyMatch(ns -> p.getURI().startsWith(ns)))
             .forEach(properties::add);

        return new ArrayList<>(properties);
    }

    private static String findMainNamespace(Model model) {
        // First try to find owl:Ontology declaration
        ResIterator ontologies = model.listSubjectsWithProperty(RDF.type, OWL.Ontology);
        if (ontologies.hasNext()) {
            String uri = ontologies.next().getURI();
            if (uri.contains("#")) {
                return uri.substring(0, uri.lastIndexOf('#') + 1);
            } else if (uri.endsWith("/")) {
                return uri;
            } else {
                return uri + "#";
            }
        }

        // Fallback to most common namespace
        Map<String, Integer> nsCounts = new HashMap<>();
        model.listStatements().forEachRemaining(stmt -> {
            countNamespace(stmt.getSubject(), nsCounts);
            countNamespace(stmt.getPredicate(), nsCounts);
            countNamespace(stmt.getObject(), nsCounts);
        });

        return nsCounts.entrySet().stream()
            .filter(e -> !EXCLUDED_NAMESPACES.contains(e.getKey()))
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("");
    }

    private static void countNamespace(RDFNode node, Map<String, Integer> counts) {
        if (node.isURIResource()) {
            String uri = node.asResource().getURI();
            String ns;
            if (uri.contains("#")) {
                ns = uri.substring(0, uri.lastIndexOf('#') + 1);
            } else {
                int lastSlash = uri.lastIndexOf('/');
                ns = lastSlash > 0 ? uri.substring(0, lastSlash + 1) : uri;
            }
            counts.put(ns, counts.getOrDefault(ns, 0) + 1);
        }
    }

    private static String getLocalName(Resource resource) {
        if (!resource.isURIResource()) return null;
        String uri = resource.getURI();
        int lastHash = uri.lastIndexOf('#');
        int lastSlash = uri.lastIndexOf('/');
        int index = Math.max(lastHash, lastSlash);
        return index > 0 ? uri.substring(index + 1) : uri;
    }

    private static boolean compareNames(String name1, String name2) {
        // Normalize names
        name1 = normalizeName(name1);
        name2 = normalizeName(name2);

        // Exact match
        if (name1.equalsIgnoreCase(name2)) return true;

        // Common synonyms for ontology alignment
        // Using Map.ofEntries instead of Map.of due to the size limit of Map.of
        Map<String, Set<String>> synonyms = Map.ofEntries(
            Map.entry("traffic", Set.of("road", "transportation")),
            Map.entry("accident", Set.of("crash", "incident", "collision")),
            Map.entry("vehicle", Set.of("car", "automobile", "transport")),
            Map.entry("person", Set.of("individual", "human", "people")),
            Map.entry("driver", Set.of("motorist", "operator")),
            Map.entry("pedestrian", Set.of("walker", "passerby")),
            Map.entry("junction", Set.of("intersection", "crossing")),
            Map.entry("road", Set.of("street", "highway", "lane", "route")),
            Map.entry("severity", Set.of("seriousness", "intensity", "degree")),
            Map.entry("location", Set.of("place", "position", "site")),
            Map.entry("name", Set.of("label", "title", "identifier")),
            Map.entry("age", Set.of("years", "lifetime"))
        );

        // Check direct synonyms
        for (Map.Entry<String, Set<String>> entry : synonyms.entrySet()) {
            String key = entry.getKey();
            Set<String> values = entry.getValue();

            if ((name1.equals(key) && values.contains(name2)) ||
                (name2.equals(key) && values.contains(name1)) ||
                (values.contains(name1) && values.contains(name2))) {
                return true;
            }
        }

        // Check if one name contains the other
        if (name1.contains(name2) || name2.contains(name1)) {
            return true;
        }
        
        return false;
    }

    private static String normalizeName(String name) {
        // Convert to lowercase
        String normalized = name.toLowerCase();
        
        // Remove common prefixes often used in ontologies
        String[] prefixesToRemove = {"has", "is", "get", "set"};
        for (String prefix : prefixesToRemove) {
            if (normalized.startsWith(prefix) && normalized.length() > prefix.length() && 
                Character.isUpperCase(name.charAt(prefix.length()))) {
                normalized = normalized.substring(prefix.length());
            }
        }
        
        // Remove non-alphabetic characters
        normalized = normalized.replaceAll("[^a-z]", "");
        
        // Remove common plurals
        if (normalized.endsWith("s") && normalized.length() > 2) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        
        return normalized;
    }
    
    public static void main(String[] args) {
        generateAlignment(
            "files/CityWatch_Ontology.ttl", 
            "files/Onto_Lecture.ttl", 
            "files/output/computed_alignment.ttl"
        );
    }
}