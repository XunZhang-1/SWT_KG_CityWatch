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
            objectURI = getKGURIFromGoogle(rawValue);
        }

        if (objectURI == null) {
            objectURI = namespace + className.toLowerCase() + "_" + cleanedValue;
        }

        Resource objectRes = model.createResource(objectURI);
        Property property = model.createProperty(namespace + propertyName);
        model.add(model.createResource(subjectURI), property, objectRes);
    }

    private String getKGURIFromGoogle(String query) {
        return null;
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
        Model mergedModel = ModelFactory.createDefaultModel();
        for (int i = 0; i < totalBatches; i++) {
            String filePath = "cw_part2/files/output/batch/batch_" + i + ".ttl";
            Model batchModel = RDFDataMgr.loadModel(filePath);
            mergedModel.add(batchModel);
        }
        RDFDataMgr.write(new FileOutputStream(outputPath), mergedModel, RDFFormat.TURTLE);
        System.out.println("merge all files：" + outputPath);
    }

    public void performInMemoryReasoning(String mergedPath, String outputPath, String ontologyPath) {
        try {
            Model dataModel = RDFDataMgr.loadModel(mergedPath);
            Model ontologyModel = RDFDataMgr.loadModel(ontologyPath);
            Model base = ModelFactory.createUnion(dataModel, ontologyModel);
            Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
            reasoner = reasoner.bindSchema(ontologyModel);
            InfModel infModel = ModelFactory.createInfModel(reasoner, base);
            RDFDataMgr.write(new FileOutputStream(outputPath), infModel, RDFFormat.TURTLE);
            System.out.println("reasoning output :" + outputPath);
        } catch (Exception e) {
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


    public static void main(String[] args) {
        try {
            String inputCSV = "cw_part2/files/CityWatch_Dataset.csv";
            String ontologyFile = "cw_part2/files/CityWatch_Ontology.ttl";
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
            builderDefault.performInMemoryReasoning("cw_part2/files/output/CityWatch_Default_Test20.ttl", "cw_part2/files/output/CityWatch_Reasoned.ttl", ontologyFile);

            // quick test:    Only the first 20 rows of data are mapped

//            String reasonedOutput = "cw_part2/files/output/CityWatch_Reasoned.ttl";
//            builderDefault.performInMemoryReasoning(mergedDefault, reasonedOutput, ontologyFile);

//            System.out.println("=== Starting KG mode: Google Knowledge Graph ===");
//            RDFBuilder builderGKG = new RDFBuilder(inputCSV, ontologyFile, colIndex);
//            builderGKG.setKGSource(KGSource.GOOGLE);
//
//            int batchIndex = 0;
//            reader = new CSVReader(new FileReader(inputCSV));
//            reader.readNext();
//            while ((line = reader.readNext()) != null) {
//                batch.add(line);
//                if (batch.size() >= batchSize) {
//                    builderGKG.processBatch(batch, batchIndex++);
//                    batch.clear();
//                }
//            }
//            if (!batch.isEmpty()) {
//                builderGKG.processBatch(batch, batchIndex);
//            }
//            reader.close();
//
//            String mergedGKG = "cw_part2/files/output/CityWatch_GKG.ttl";
//            builderGKG.mergeBatches(batchIndex + 1, mergedGKG);

//            System.out.println("=== Starting KG mode: Wikidata ===");
//            RDFBuilder builderWD = new RDFBuilder(inputCSV, ontologyFile, colIndex);
//            builderWD.setKGSource(KGSource.WIKIDATA);
//
//            reader = new CSVReader(new FileReader(inputCSV));
//            reader.readNext();
//            batchIndex = 0;
//            batch.clear();
//            while ((line = reader.readNext()) != null) {
//                batch.add(line);
//                if (batch.size() >= batchSize) {
//                    builderWD.processBatch(batch, batchIndex++);
//                    batch.clear();
//                }
//            }
//            if (!batch.isEmpty()) {
//                builderWD.processBatch(batch, batchIndex);
//            }
//            reader.close();
//
//            String mergedWD = "cw_part2/files/output/CityWatch_Wikidata.ttl";
//            builderWD.mergeBatches(batchIndex + 1, mergedWD);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
