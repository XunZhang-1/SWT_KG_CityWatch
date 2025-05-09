import com.opencsv.CSVReader;
import java.io.*;
import java.util.*;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.reasoner.*;
import org.apache.jena.reasoner.rulesys.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;


public class RDFBuilder {
    String inputFile;
    String ontologyFile;
    String namespace = "http://www.city.ac.uk/inm713-in3067/2025/CityWatch#";
    Map<String, Integer> columnIndex;
    Map<String, String> stringToURI = new HashMap<>();
    Map<String, String> kgCache = new HashMap<>();

    public enum KGSource { GOOGLE, WIKIDATA, NONE }
    private KGSource kgSource = KGSource.NONE;
    public void setKGSource(KGSource source) { this.kgSource = source; }

    public RDFBuilder(String inputFile, String ontologyFile, Map<String, Integer> columnIndex) {
        this.inputFile = inputFile;
        this.ontologyFile = ontologyFile;
        this.columnIndex = columnIndex;
    }

    private String clean(String value) {
        return value.trim().replaceAll("[\\s()\"/]", "_").toLowerCase();
    }

    //Subtask RDF.1: URI Generation
    private String getOrCreateURI(String name, int rowIndex, String accident) {
        name = clean(name);
        String uriStr = namespace + "accident_" + rowIndex + "_" + name;
        if (!stringToURI.containsKey(name + rowIndex)) {
            stringToURI.put(name + rowIndex, uriStr);
        }
        return stringToURI.get(name + rowIndex);
    }

    private void addLiteral(Model model, String uri, String[] row, int rowIndex, String colName, String propName, XSDDatatype datatype) {
        Integer col = columnIndex.get(colName);
        if (col == null || col >= row.length || row[col].isEmpty()) return;

        String rawValue = row[col].trim();
        try {
            Property property = model.createProperty(namespace + propName);
            Literal literal;
            if (datatype.equals(XSDDatatype.XSDinteger)) {
                int intValue = (int) Float.parseFloat(rawValue);
                literal = model.createTypedLiteral(intValue);
            } else {
                literal = model.createTypedLiteral(rawValue, datatype);
            }
            model.add(model.createResource(uri), property, literal);
        } catch (NumberFormatException e) {
            System.err.println("Unable to parse value '" + rawValue + "' from column '" + colName + "' at row index: " + rowIndex);
        }
    }

    private void addObjectProperty(Model model, String subjectURI, String[] row, int rowIndex, String colName, String propertyName, String className) {
        Integer col = columnIndex.get(colName);
        if (col == null || col >= row.length || row[col].isEmpty()) return;

        String rawValue = row[col].trim();
        String cleanedValue = clean(rawValue);
        String objectURI = null;

        if (kgSource == KGSource.WIKIDATA) {
            objectURI = getKGURIFromWikidata(rawValue);
        } else if (kgSource == KGSource.GOOGLE) {
            objectURI = getKGURIFromGoogle(rawValue, colName);
        }

        if (objectURI == null) {
            objectURI = namespace + className.toLowerCase() + "_" + cleanedValue;
        }

        Resource subjectRes = model.createResource(subjectURI);
        Resource objectRes = model.createResource(objectURI);
        Property property = model.createProperty(namespace + propertyName);

        model.add(subjectRes, property, objectRes);


        if (!objectURI.startsWith("http://g.co/kg/") && !objectURI.startsWith("http://www.wikidata.org/entity/")) {
            model.add(objectRes, RDF.type, model.createResource(namespace + className));
        }
    }


    /*
     * Updated RDFBuilder.java with Google KG keyword-based entity matching
     * based on previous reference to Lab4_Solution for modularity and better KG reuse.
     *

     */
    private String getKGURIFromGoogle(String query, String columnName) {
        if (kgCache.containsKey(query)) return kgCache.get(query);

        try {
            GoogleKGLookup lookup = new GoogleKGLookup();
            Set<String> types = new HashSet<>();
            Set<String> languages = new HashSet<>();
            languages.add("en");

            TreeSet<KGEntity> results = lookup.getEntities(query, "5", types, languages, 0.0);

            I_Sub isub = new I_Sub();
            double bestSim = -1.0;
            String bestURI = null;

            for (KGEntity entity : results) {
                double sim = isub.score(query, entity.getName());
                System.out.println("-Compare: " + query + " <-> " + entity.getName() + " | sim = " + sim);

                if (sim > bestSim) {
                    bestSim = sim;
                    String id = entity.getId().trim();
                    if (id.contains("/m/") || id.contains("/g/")) {
                        int start = id.indexOf("/m/") != -1 ? id.indexOf("/m/") : id.indexOf("/g/");
                        id = id.substring(start);
                    } else {
                        return null;
                    }
                    bestURI = "http://g.co/kg" + id;
                }
            }

            if (bestURI != null) {
                kgCache.put(query, bestURI);
                System.out.println(">>> MAPPED KG URI for [" + query + "] = " + bestURI);
                return bestURI;
            }

        } catch (Exception e) {
            System.err.println("Google KG lookup failed for: " + query);
            e.printStackTrace();
        }

        return null;
    }

    // Optional: generalized lexical URI cleaning (moved to method for reuse)
    private String processLexicalName(String name) {
        return name.trim().replaceAll("[\s()\"/,:]", "_").toLowerCase();
    }


    private String getKGURIFromWikidata(String query) {
        return query;
    }

    public void processBatch(List<String[]> rows, int batchIndex) throws IOException {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("cw", namespace);
        model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema");

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            int rowIndex = batchIndex * rows.size() + i;
            if (row.length < columnIndex.size()) continue;
            String subject = row[columnIndex.get("crash_date")];
            if (subject.isEmpty()) continue;

            String uri = getOrCreateURI(subject, rowIndex, "accident");
            model.add(model.createResource(uri), RDF.type, model.createResource(namespace + "TrafficAccident"));

            addLiteral(model, uri, row, rowIndex, "crash_date", "crashDate", XSDDatatype.XSDstring);
            addLiteral(model, uri, row, rowIndex, "crash_hour", "crashHour", XSDDatatype.XSDinteger);//Ontology does not exist, manual mapping is required
            addLiteral(model, uri, row, rowIndex, "crash_day_of_week", "crashDayOfWeek", XSDDatatype.XSDinteger);//Ontology does not exist, manual mapping is required
            addLiteral(model, uri, row, rowIndex, "crash_month", "crashMonth", XSDDatatype.XSDinteger);//Ontology does not exist, manual mapping is required

            addLiteral(model, uri, row, rowIndex, "injuries_total", "injuriesTotal", XSDDatatype.XSDinteger);
            addLiteral(model, uri, row, rowIndex, "injuries_fatal", "injuriesFatal", XSDDatatype.XSDinteger);
            addLiteral(model, uri, row, rowIndex, "injuries_incapacitating", "injuriesIncapacitating", XSDDatatype.XSDinteger);
            addLiteral(model, uri, row, rowIndex, "injuries_non_incapacitating", "injuriesNonIncapacitating", XSDDatatype.XSDinteger);
            addLiteral(model, uri, row, rowIndex, "injuries_reported_not_evident", "injuriesReportedNotEvident", XSDDatatype.XSDinteger);
            addLiteral(model, uri, row, rowIndex, "injuries_no_indication", "injuriesNoIndication", XSDDatatype.XSDinteger);
            addLiteral(model, uri, row, rowIndex, "num_units", "hasNumberOfVehicles", XSDDatatype.XSDinteger);
            addLiteral(model, uri, row, rowIndex, "damage", "hasDamageAmount",XSDDatatype.XSDstring );

            // object properties
            addObjectProperty(model, uri, row, rowIndex, "first_crash_type", "hasFirstCrashType", "TrafficAccidentType");
            addObjectProperty(model, uri, row, rowIndex, "crash_type", "hasTrafficAccidentTypeCategory", "TrafficAccidentTypeCategory");//Ontology does not exist, manual mapping is required
            addObjectProperty(model, uri, row, rowIndex, "weather_condition", "hasWeatherCondition", "WeatherCondition");
            addObjectProperty(model, uri, row, rowIndex, "lighting_condition", "hasLightingCondition", "LightingCondition");
            addObjectProperty(model, uri, row, rowIndex, "prim_contributory_cause", "hasTrafficAccidentCause", "TrafficAccidentCause");
            addObjectProperty(model, uri, row, rowIndex, "traffic_control_device", "hasTrafficControlDevice", "TrafficControlDevice");
            addObjectProperty(model, uri, row, rowIndex, "trafficway_type", "occurAt", "Road");
            addObjectProperty(model, uri, row, rowIndex, "alignment", "hasAlignment", "RoadAlignnment");
            addObjectProperty(model, uri, row, rowIndex, "roadway_surface_cond", "hasRoadCondition", "RoadCondition");
            addObjectProperty(model, uri, row, rowIndex, "road_defect", "hasRoadDefect", "RoadDefect");
            addObjectProperty(model, uri, row, rowIndex, "most_severe_injury", "hasMostSevereInjury", "TrafficAccidentSeverity");
            addObjectProperty(model, uri, row, rowIndex, "intersection_related_i", "isIntersectionRelated", "IntersectionRelation");//Ontology does not exist, manual mapping is required

        }

        String outFile = "cw_part2/files/output/batch/batch_" + batchIndex + ".ttl";
        RDFDataMgr.write(new FileOutputStream(outFile), model, RDFFormat.TURTLE);
    }

    public void mergeBatches(int totalBatches, String outputPath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));
        boolean prefixWritten = false;

        for (int i = 0; i < totalBatches; i++) {
            String filePath = "cw_part2/files/output/batch/batch_" + i + ".ttl";
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;

            while ((line = reader.readLine()) != null) {
                if (!prefixWritten) {
                    writer.write(line);
                    writer.newLine();
                    if (!line.startsWith("@prefix") && !line.trim().isEmpty()) {
                        prefixWritten = true;
                    }
                } else {
                    if (!line.startsWith("@prefix") && !line.trim().isEmpty()) {
                        writer.write(line);
                        writer.newLine();
                    }
                }
            }

            reader.close();
        }

        writer.close();
        System.out.println("Merged all TTL batches to: " + outputPath);
    }



    /**
     * Perform OWL 2 RL-style reasoning on a set of TTL batch files using the given ontology.
     * Merges all inferred data into a single output TTL file.
     *
     * @param batchesDirPath     the directory containing the batch TTL files
     * @param ontologyPath       the path to the ontology file (onto_cw2)
     * @param outputMergedPath   the path to save the merged inferred TTL file
     */
    public void performInMemoryReasoningInBatches(String batchesDirPath, String ontologyPath, String outputMergedPath) {
        File batchesDir = new File(batchesDirPath);
        File[] batchFiles = batchesDir.listFiles((dir, name) -> name.endsWith(".ttl"));
        Arrays.sort(batchFiles, Comparator.comparingInt(f ->
                Integer.parseInt(f.getName().replaceAll("[^0-9]", ""))
        ));
        if (batchFiles == null) return;

        // Create a model to hold all inferred triples
        Model finalModel = ModelFactory.createDefaultModel();

        // Load the ontology model
        Model ontologyModel = RDFDataMgr.loadModel(ontologyPath);

        // Use an OWL mini reasoner (suitable for OWL 2 RL style reasoning)
        Reasoner reasoner = ReasonerRegistry.getOWLMiniReasoner().bindSchema(ontologyModel);

        long totalStart = System.currentTimeMillis();
        for (File file : batchFiles) {
            System.out.println("Reasoning on batch: " + file.getName());
            long start = System.currentTimeMillis();
            // Load the current batch model
            Model dataModel = RDFDataMgr.loadModel(file.getAbsolutePath());

            // Create an inference model with the reasoner and data
            InfModel infModel = ModelFactory.createInfModel(reasoner, dataModel);

            // Merge the inferred triples into the final model
            finalModel.add(infModel);

            // Clean up
            infModel.close();
            dataModel.close();
            long end = System.currentTimeMillis();
            System.out.println("Finished " + file.getName() + " in " + (end - start) + " ms");
        }
        long totalEnd = System.currentTimeMillis();
        // Write the merged inferred model to a TTL file
        try (FileOutputStream out = new FileOutputStream(outputMergedPath)) {
            RDFDataMgr.write(out, finalModel, RDFFormat.TURTLE_PRETTY);
            System.out.println("Reasoning complete. Output saved to: " + outputMergedPath);
            System.out.println("Total reasoning time: " + (totalEnd - totalStart) + " ms");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void clearOutputDirectory(String folderPath) {
        File dir = new File(folderPath);
        if (dir.exists() && dir.isDirectory()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file.isFile()) file.delete();
            }
        }
    }


    public static void quickCheckRDF(String filePath) {
        Model model = RDFDataMgr.loadModel(filePath);
        Property prop = model.createProperty("http://www.city.ac.uk/inm713-in3067/2025/CityWatch#hasTrafficAccidentTypeCategory");

        System.out.println("\n=== Quick Check: hasTrafficAccidentTypeCategory triples ===");
        StmtIterator iter = model.listStatements(null, prop, (RDFNode) null);
        int count = 0;

        while (iter.hasNext() && count < 10) {
            Statement stmt = iter.next();
            System.out.println("√ " + stmt.getSubject().getLocalName()
                    + " --> " + stmt.getPredicate().getLocalName()
                    + " --> " + stmt.getObject().asResource().getLocalName());
            count++;
        }

        if (count == 0) {
            System.err.println("No hasTrafficAccidentTypeCategory triple found, please check your mapping logic!");
        }
    }

    public static void ensureOutputDirectoriesExist() {
        String[] paths = {
                "cw_part2/files/output",
                "cw_part2/files/output/batch"
        };

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (created) {
                    System.out.println("Created missing directory: " + path);
                } else {
                    System.err.println("Failed to create directory: " + path);
                }
            }
        }
    }


    public static void main(String[] args) {
        try {

            ensureOutputDirectoriesExist();
            String inputCSV = "files/CityWatch_Dataset.csv";
            String ontologyFile = "files/CityWatch_Ontology.ttl";
            String ontologyLectureFile = "files/Onto_Lecture.ttl";
            String outputFolder = "cw_part2/files/output";
            clearOutputDirectory(outputFolder);

            Map<String, Integer> colIndex = new HashMap<>();
            CSVReader reader = new CSVReader(new FileReader(inputCSV));
            String[] headers = reader.readNext();
            for (int i = 0; i < headers.length; i++) {
                colIndex.put(headers[i], i);
            }

            int batchSize = 1000;
            List<String[]> batch = new ArrayList<>();
            String[] line;
            System.out.println("=== Generating DEFAULT RDF (no KG reuse) ===");
            RDFBuilder builderDefault = new RDFBuilder(inputCSV, ontologyFile, colIndex);
            builderDefault.setKGSource(KGSource.NONE);

            int batchIndex = 0;
// full-version
//            reader = new CSVReader(new FileReader(inputCSV));
//            reader.readNext();  // skip header
//            batch.clear();
//
//
//
//            while ((line = reader.readNext()) != null) {
//                batch.add(line);
//                if (batch.size() >= batchSize) {
//                    builderDefault.processBatch(batch, batchIndex++);
//                    batch.clear();
//                }
//            }
//            if (!batch.isEmpty()) {
//                builderDefault.processBatch(batch, batchIndex);
//            }
//            reader.close();
//
//            String mergedDefault = "cw_part2/files/output/CityWatch_Default.ttl";
//            builderDefault.mergeBatches(batchIndex + 1, mergedDefault);


            // quick test:    Only the first 20 rows of data are mapped
            int maxRows = 20;
            int currentCount = 0;

            reader = new CSVReader(new FileReader(inputCSV));
            reader.readNext();  // skip header
            batch.clear();

            while ((line = reader.readNext()) != null && currentCount < maxRows) {
                batch.add(line);
                currentCount++;
            }
            reader.close();

            builderDefault.processBatch(batch, 0);

            String mergedDefault = "cw_part2/files/output/CityWatch_Default_Test20.ttl";
            builderDefault.mergeBatches(1, mergedDefault);

            quickCheckRDF(mergedDefault);

            // quick test:    Only the first 20 rows of data are mapped
            System.out.println("=== Starting KG mode: Google Knowledge Graph ===");
            RDFBuilder builderGKG = new RDFBuilder(inputCSV, ontologyFile, colIndex);
            builderGKG.setKGSource(KGSource.GOOGLE);

            // quick test:    Only the first 20 rows of data are mapped
            int maxRows2 = 20;
            int currentCount2 = 0;
            reader = new CSVReader(new FileReader(inputCSV));
            reader.readNext();
            batch.clear();

            while ((line = reader.readNext()) != null && currentCount2 < maxRows2) {
                batch.add(line);
                currentCount2++;
            }
            reader.close();

            builderGKG.processBatch(batch, 0);
            String mergedGKG = "cw_part2/files/output/CityWatch_GKG_Test20.ttl";
            builderGKG.mergeBatches(1, mergedGKG);



//        // Subtask RDF.4
//        String reasonedOutput = "cw_part2/files/output/CityWatch_Reasoned.ttl";
//        builderDefault.performInMemoryReasoningInBatches("cw_part2/files/output/batch", ontologyFile, reasonedOutput);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
