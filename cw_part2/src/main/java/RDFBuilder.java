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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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



    /**
     * Adds an object property triple to the RDF model.
     * Depending on the field name and current KG source setting, it may reuse an external KG URI
     * (from Google or Wikidata) or fall back to a local URI.
     */
    private void addObjectProperty(Model model, String subjectURI, String[] row, int rowIndex,
                                   String colName, String propertyName, String className) {
        // Get the column index; skip if invalid or missing value
        Integer col = columnIndex.get(colName);
        if (col == null || col >= row.length || row[col].isEmpty()) return;

        // Clean and extract the raw value from the current CSV row
        String rawValue = row[col].trim();
        String cleanedValue = clean(rawValue);
        String objectURI = null;

        // Try to reuse external KG URI based on field and source settings
        if ((kgSource == KGSource.GOOGLE && colName.equals("weather_condition")) ||
                (kgSource == KGSource.WIKIDATA && colName.equals("lighting_condition"))) {
            objectURI = kgCache.getOrDefault(rawValue, null);
        }


        // Fallback: construct a local URI if no KG URI was found
        if (objectURI == null) {
            objectURI = namespace + className.toLowerCase() + "_" + cleanedValue;
        }

        // Create RDF resources and property
        Resource subjectRes = model.createResource(subjectURI);
        Resource objectRes = model.createResource(objectURI);
        Property property = model.createProperty(namespace + propertyName);

        // Add the object property triple
        model.add(subjectRes, property, objectRes);

        // If the URI is local (not from external KG), add rdf:type triple
        if (!objectURI.startsWith("http://g.co/kg/") &&
                !objectURI.startsWith("http://www.wikidata.org/entity/")) {
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
//                System.out.println("-Compare: " + query + " <-> " + entity.getName() + " | sim = " + sim);

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

    public void processBatch(List<String[]> rows, int batchIndex, String batchFolderPath) throws IOException {

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
            addLiteral(model, uri, row, rowIndex, "crash_hour", "crashHour", XSDDatatype.XSDinteger);//not in ontology originally, added manually
            addLiteral(model, uri, row, rowIndex, "crash_day_of_week", "crashDayOfWeek", XSDDatatype.XSDinteger);//not in ontology originally, added manually
            addLiteral(model, uri, row, rowIndex, "crash_month", "crashMonth", XSDDatatype.XSDinteger);//not in ontology originally, added manually

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
            addObjectProperty(model, uri, row, rowIndex, "crash_type", "hasTrafficAccidentTypeCategory", "TrafficAccidentTypeCategory");//not in ontology originally, added manually
            addObjectProperty(model, uri, row, rowIndex, "weather_condition", "hasWeatherCondition", "WeatherCondition");
            addObjectProperty(model, uri, row, rowIndex, "lighting_condition", "hasLightingCondition", "LightingCondition");
            addObjectProperty(model, uri, row, rowIndex, "prim_contributory_cause", "hasTrafficAccidentCause", "TrafficAccidentCause");
            addObjectProperty(model, uri, row, rowIndex, "traffic_control_device", "hasTrafficControlDevice", "TrafficControlDevice");
            addObjectProperty(model, uri, row, rowIndex, "trafficway_type", "occurAt", "Road");
            addObjectProperty(model, uri, row, rowIndex, "alignment", "hasAlignment", "RoadAlignnment");
            addObjectProperty(model, uri, row, rowIndex, "roadway_surface_cond", "hasRoadCondition", "RoadCondition");
            addObjectProperty(model, uri, row, rowIndex, "road_defect", "hasRoadDefect", "RoadDefect");
            addObjectProperty(model, uri, row, rowIndex, "most_severe_injury", "hasMostSevereInjury", "TrafficAccidentSeverity");
            addObjectProperty(model, uri, row, rowIndex, "intersection_related_i", "isIntersectionRelated", "IntersectionRelation");//not in ontology originally, added manually

        }

        String outFile = batchFolderPath + "/batch_" + batchIndex + ".ttl";
        RDFDataMgr.write(new FileOutputStream(outFile), model, RDFFormat.TURTLE);
    }

    public void mergeBatches(int totalBatches,String batchFolder, String outputPath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));
        boolean prefixWritten = false;

        for (int i = 0; i < totalBatches; i++) {
            String filePath = batchFolder + "/batch_" + i + ".ttl";
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

    public static void clearOutputDirectory(String folderPath) {
        File dir = new File(folderPath);
        if (dir.exists() && dir.isDirectory()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file.isFile()) file.delete();
            }
        }
    }

    public static void ensureOutputDirectoriesExist(String folderPath) {
        String[] paths = {
                folderPath,
                folderPath + "/batch"
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

    public void saveKGCacheToJSON(String path) throws IOException {
        Gson gson = new Gson();
        String json = gson.toJson(kgCache);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(json);
        }
        System.out.println("Saved KG cache to: " + path);
    }

    public void loadKGCacheFromJSON(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) return;

        Gson gson = new Gson();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                kgCache.putAll(loaded);
                System.out.println("Loaded KG cache from: " + path);
            }
        }
    }

    private String getKGURIFromWikidata(String query) {
        if (kgCache.containsKey(query)) return kgCache.get(query);

        try {
            WikidataLookup lookup = new WikidataLookup();
            Set<KGEntity> results = lookup.getKGEntities(query, 5, "en");

            I_Sub isub = new I_Sub();
            double bestSim = -1.0;
            String bestURI = null;

            for (KGEntity entity : results) {
                double sim = isub.score(query, entity.getName());
                if (sim > bestSim) {
                    bestSim = sim;
                    bestURI = entity.getId();  // e.g. https://www.wikidata.org/entity/Q180524
                }
            }

            if (bestURI != null) {
                kgCache.put(query, bestURI);
                System.out.println(">>> MAPPED Wikidata URI for [" + query + "] = " + bestURI);
                return bestURI;
            }

        } catch (Exception e) {
            System.err.println("Wikidata lookup failed for: " + query);
            e.printStackTrace();
        }

        return null;
    }

    // quick test -- eg. maxrows=20  full test: maxrows = 0
    public static void GoogleKGReuse(String inputCSV, String ontologyFile, int... optionalRowLimit) throws IOException {
        int rowLimit = (optionalRowLimit.length > 0) ? optionalRowLimit[0] : 0;
        int batchSize = 1000;

        // Define output paths and filenames
        String outputFolder = "cw_part2/files/output/output_gkg_r3_1";
        String batchFolder = outputFolder + "/batch";
        String cacheFile = "cw_part2/files/cache/kg_cache.json";
        String outputFile = (rowLimit > 0)
                ? outputFolder + "/CityWatch_GKG_R3_1_QuickTest" + rowLimit + ".ttl"
                : outputFolder + "/CityWatch_GKG.ttl";

        // Prepare output directories
        ensureOutputDirectoriesExist(outputFolder);
        clearOutputDirectory(batchFolder);

        // Read CSV headers and store column index
        Map<String, Integer> colIndex = new HashMap<>();
        CSVReader reader = new CSVReader(new FileReader(inputCSV));
        String[] headers = reader.readNext();
        for (int i = 0; i < headers.length; i++) {
            colIndex.put(headers[i], i);
        }

        // Initialize RDFBuilder with ontology and column mapping
        RDFBuilder builder = new RDFBuilder(inputCSV, ontologyFile, colIndex);
        builder.setKGSource(RDFBuilder.KGSource.GOOGLE);
        builder.loadKGCacheFromJSON(cacheFile);

        // === Collect all CSV rows and extract unique values for KG lookup ===
        List<String[]> allRows = new ArrayList<>();
        Set<String> uniqueWeather = new HashSet<>();
        Set<String> uniqueLighting = new HashSet<>();

        String[] line;
        int count = 0;
        while ((line = reader.readNext()) != null) {
            allRows.add(line);
            count++;

            // Collect unique values for weather_condition and lighting_condition
            if (colIndex.containsKey("weather_condition")) {
                String val = line[colIndex.get("weather_condition")].trim();
                if (!val.isEmpty()) uniqueWeather.add(val);
            }

            if (colIndex.containsKey("lighting_condition")) {
                String val = line[colIndex.get("lighting_condition")].trim();
                if (!val.isEmpty()) uniqueLighting.add(val);
            }

            // If a row limit is set, stop when reached
            if (rowLimit > 0 && count >= rowLimit) break;
        }
        reader.close();

        // === Perform KG URI mapping only once ===
        builder.buildKGURIMap(uniqueWeather, uniqueLighting);

        // === Process rows in batches for RDF triple generation ===
        int batchIndex = 0;
        for (int i = 0; i < allRows.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allRows.size());
            List<String[]> batch = allRows.subList(i, end);
            builder.processBatch(batch, batchIndex++, batchFolder);
        }

        // Merge batch files and save final output and cache
        builder.mergeBatches(batchIndex, batchFolder, outputFile);
        builder.saveKGCacheToJSON(cacheFile);

        System.out.println("Google KG RDF generation complete: " + outputFile);
    }



    // quick test -- e.g. maxrows=20  full test: maxrows = 0
    public static void WikidataKGReuse(String inputCSV, String ontologyFile, int... optionalRowLimit) throws IOException {
        int rowLimit = (optionalRowLimit.length > 0) ? optionalRowLimit[0] : 0;
        int batchSize = 1000;

        // Define output paths and filenames
        String outputFolder = "cw_part2/files/output/output_wikidata_r3_2";
        String batchFolder = outputFolder + "/batch";
        String cacheFile = "cw_part2/files/cache/kg_cache_wikidata.json";
        String outputFile = (rowLimit > 0)
                ? outputFolder + "/CityWatch_Wikidata_R3_2_QuickTest" + rowLimit + ".ttl"
                : outputFolder + "/CityWatch_Wikidata.ttl";

        // Prepare output directories
        ensureOutputDirectoriesExist(outputFolder);
        clearOutputDirectory(batchFolder);

        // Read CSV headers and store column index
        Map<String, Integer> colIndex = new HashMap<>();
        CSVReader reader = new CSVReader(new FileReader(inputCSV));
        String[] headers = reader.readNext();
        for (int i = 0; i < headers.length; i++) {
            colIndex.put(headers[i], i);
        }

        // Initialize RDFBuilder
        RDFBuilder builder = new RDFBuilder(inputCSV, ontologyFile, colIndex);
        builder.setKGSource(RDFBuilder.KGSource.WIKIDATA);
        builder.loadKGCacheFromJSON(cacheFile);

        // === Load CSV once and collect unique values ===
        List<String[]> allRows = new ArrayList<>();
        Set<String> uniqueWeather = new HashSet<>();
        Set<String> uniqueLighting = new HashSet<>();

        String[] line;
        int count = 0;
        while ((line = reader.readNext()) != null) {
            allRows.add(line);
            count++;

            if (colIndex.containsKey("weather_condition")) {
                String val = line[colIndex.get("weather_condition")].trim();
                if (!val.isEmpty()) uniqueWeather.add(val);
            }

            if (colIndex.containsKey("lighting_condition")) {
                String val = line[colIndex.get("lighting_condition")].trim();
                if (!val.isEmpty()) uniqueLighting.add(val);
            }

            if (rowLimit > 0 && count >= rowLimit) break;
        }
        reader.close();

        // === Perform KG URI mapping once ===
        builder.buildKGURIMap(uniqueWeather, uniqueLighting);

        // === Process RDF triples in batches ===
        int batchIndex = 0;
        for (int i = 0; i < allRows.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allRows.size());
            List<String[]> batch = allRows.subList(i, end);
            builder.processBatch(batch, batchIndex++, batchFolder);
        }

        // Merge TTL files and save the cache
        builder.mergeBatches(batchIndex, batchFolder, outputFile);
        builder.saveKGCacheToJSON(cacheFile);

        System.out.println("Wikidata KG RDF generation complete: " + outputFile);
    }




    public static void DefaultRDFGeneration(String inputCSV, String ontologyFile, int... optionalRowLimit) throws IOException {
        int rowLimit = (optionalRowLimit.length > 0) ? optionalRowLimit[0] : 0;
        int batchSize = 1000;

        String outputFolder = "cw_part2/files/output/output_default";
        String batchFolder = outputFolder + "/batch";
        String outputFile = (rowLimit > 0)
                ? outputFolder + "/CityWatch_Default_QuickTest" + rowLimit + ".ttl"
                : outputFolder + "/CityWatch_Default.ttl";

        ensureOutputDirectoriesExist(outputFolder);
        clearOutputDirectory(batchFolder);

        Map<String, Integer> colIndex = new HashMap<>();
        CSVReader reader = new CSVReader(new FileReader(inputCSV));
        String[] headers = reader.readNext();
        for (int i = 0; i < headers.length; i++) {
            colIndex.put(headers[i], i);
        }

        RDFBuilder builder = new RDFBuilder(inputCSV, ontologyFile, colIndex);
        builder.setKGSource(RDFBuilder.KGSource.NONE);

        List<String[]> batch = new ArrayList<>();
        String[] line;
        int count = 0;
        int batchIndex = 0;

        while ((line = reader.readNext()) != null) {
            batch.add(line);
            count++;

            if (rowLimit > 0 && count >= rowLimit) break;

            if (batch.size() == batchSize) {
                builder.processBatch(batch, batchIndex++, batchFolder);
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            builder.processBatch(batch, batchIndex++, batchFolder);
        }

        reader.close();
        builder.mergeBatches(batchIndex, batchFolder, outputFile);

        System.out.println("Default RDF generation complete: " + outputFile);
    }



    /**
     * Performs ontology reasoning using an existing RDF data file and an ontology file.
     * The reasoning is based on RDFS semantics (OWL 2 RL compatible).
     * The inferred triples are written to an output TTL file.
     *
     * @param inputTTL    Path to the input RDF data file (e.g., CityWatch_Default.ttl)
     * @param ontologyTTL Path to the ontology file (e.g., CityWatch_Ontology.ttl)
     * @param outputTTL   Path to the output TTL file that will contain the inferred triples
     */
    public static void performReasoning(String inputTTL, String ontologyTTL, String outputTTL) {
        try {
            long start = System.currentTimeMillis();

            // 1. Load the complete RDF data model from the input TTL file
            Model dataModel = RDFDataMgr.loadModel(inputTTL);

            // 2. Load the ontology model and bind it to an RDFS reasoner
            Model ontologyModel = RDFDataMgr.loadModel(ontologyTTL);
            Reasoner reasoner = ReasonerRegistry.getRDFSReasoner().bindSchema(ontologyModel);

            // 3. Create an inference model using the data and the reasoner
            InfModel infModel = ModelFactory.createInfModel(reasoner, dataModel);

            // 4. Write the inferred model to the output TTL file
            RDFDataMgr.write(new FileOutputStream(outputTTL), infModel, RDFFormat.TURTLE_PRETTY);

            long end = System.currentTimeMillis();
            System.out.println("Reasoning completed in " + (end - start) + " ms");
            System.out.println("Output written to: " + outputTTL);

            infModel.close();
            dataModel.close();
        } catch (Exception e) {
            System.err.println("Reasoning failed:");
            e.printStackTrace();
        }
    }

//    public void buildKGURIMap(List<String[]> rows) {
//        Set<String> uniqueWeather = new HashSet<>();
//        Set<String> uniqueLighting = new HashSet<>();
//
//        for (String[] row : rows) {
//            if (columnIndex.containsKey("weather_condition")) {
//                String val = row[columnIndex.get("weather_condition")].trim();
//                if (!val.isEmpty()) uniqueWeather.add(val);
//            }
//            if (columnIndex.containsKey("lighting_condition")) {
//                String val = row[columnIndex.get("lighting_condition")].trim();
//                if (!val.isEmpty()) uniqueLighting.add(val);
//            }
//        }
//
//        System.out.println("Unique weather: " + uniqueWeather.size() + ", lighting: " + uniqueLighting.size());
//
//        for (String value : uniqueWeather) {
//            if (!kgCache.containsKey(value)) {
//                String uri = getKGURIFromGoogle(value, "weather_condition");
//                if (uri != null) kgCache.put(value, uri);
//            }
//        }
//
//        for (String value : uniqueLighting) {
//            if (!kgCache.containsKey(value)) {
//                String uri = getKGURIFromWikidata(value);
//                if (uri != null) kgCache.put(value, uri);
//            }
//        }
//    }

    public void buildKGURIMap(Set<String> weatherValues, Set<String> lightingValues) {
        for (String value : weatherValues) {
            if (!kgCache.containsKey(value)) {
                String uri = getKGURIFromGoogle(value, "weather_condition");
                if (uri != null) kgCache.put(value, uri);
            }
        }

        for (String value : lightingValues) {
            if (!kgCache.containsKey(value)) {
                String uri = getKGURIFromWikidata(value);
                if (uri != null) kgCache.put(value, uri);
            }
        }
    }

    public static void main(String[] args) {
        try {

//            ensureOutputDirectoriesExist(outputFolder);
            String inputCSV = "cw_part2/files/CityWatch_Dataset.csv";
            String ontologyFile = "cw_part2/files/CityWatch_Ontology.ttl";
            String outputFolder = "cw_part2/files/output";
            clearOutputDirectory(outputFolder);

//    //             RDF 1& RDF2
//            long startDefault = System.currentTimeMillis();
//            DefaultRDFGeneration(inputCSV, ontologyFile);
//            long endDefault = System.currentTimeMillis();
//            System.out.println("DefaultRDFGeneration finished in " + (endDefault - startDefault) + " ms\n");

////             RDF 3.1
//            long startGoogle = System.currentTimeMillis();
////            GoogleKGReuse(inputCSV, ontologyFile, 20);
//           GoogleKGReuse(inputCSV, ontologyFile);
//            long endGoogle = System.currentTimeMillis();
//            System.out.println("GoogleKGReuse finished in " + (endGoogle - startGoogle) + " ms\n");
//
////             RDF3.2
//            long startWikidata = System.currentTimeMillis();
//            WikidataKGReuse(inputCSV, ontologyFile);
////           WikidataKGReuse(inputCSV, ontologyFile,20);
//            long endWikidata = System.currentTimeMillis();
//            System.out.println("WikidataKGReuse finished in " + (endWikidata - startWikidata) + " ms\n");
//
//            // RDF.4
            String inputTTL = "cw_part2/files/output/output_default/CityWatch_Default.ttl";
            String outputTTL = "cw_part2/files/output/CityWatch_Reasoned.ttl";
            performReasoning(inputTTL, ontologyFile, outputTTL);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
