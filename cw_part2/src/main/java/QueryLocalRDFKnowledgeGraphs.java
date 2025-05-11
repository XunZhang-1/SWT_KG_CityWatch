import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.RDFDataMgr;
import util.ReadFile;
import org.apache.jena.query.ResultSetFormatter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Iterator;

/**
 * Author: Ernesto Jimenez-Ruiz
 * Modified in 2025
 */
public class QueryLocalRDFKnowledgeGraphs {

	// Field to track the current query number (used to name CSV files)
	private int queryId;

	public QueryLocalRDFKnowledgeGraphs(String file_onto, String file_data, String query_file, int queryId) throws FileNotFoundException {
		this.queryId = queryId;

		// Load RDF data file
		Dataset dataset = RDFDataMgr.loadDataset(file_data);
		Model model = dataset.getDefaultModel();

		// Optionally load ontology and merge with data (not required if reasoning already done)
		if (file_onto != null) {
			Dataset dataset_onto = RDFDataMgr.loadDataset(file_onto);
			model.add(dataset_onto.getDefaultModel().listStatements().toList());
		}
		System.out.println("The input graph contains '" + model.listStatements().toSet().size() + "' triples.");

		// No reasoning needed here – assuming input is already reasoned
		Model inf_model = model;

		// Load SPARQL query from file
		ReadFile qfile = new ReadFile(query_file);
		String queryStr = qfile.readFileIntoString();

		System.out.println("Query:");
		System.out.println(queryStr);

		// Execute SPARQL query
		Query q = QueryFactory.create(queryStr);
		QueryExecution qe = QueryExecutionFactory.create(q, inf_model);

		try {
			// First run: print results to console
			ResultSet results1 = qe.execSelect();
			ResultSetFormatter.out(System.out, results1);

			// Second run: save results to CSV file
			qe = QueryExecutionFactory.create(q, inf_model);
			ResultSet results2 = qe.execSelect();

			String csvPath = String.format("cw_part2/files/output/result_sparql%d.csv", queryId);
			FileOutputStream out = new FileOutputStream(csvPath);
			ResultSetFormatter.outputAsCSV(out, results2);

			System.out.println("CSV result written to: " + csvPath);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			qe.close();
		}
	}

	// Main method
	public static void main(String[] args) {
		// Dataset already contains reasoning results
		String dataset = "cw_part2/files/output/CityWatch_Reasoned.ttl";
		String ontology_file = null;

		// Change this to run SPARQL 1 to 5
		int queryId = 5;
		String query_file = String.format("cw_part2/files/queries/query_sparql%d.txt", queryId);

		try {
			new QueryLocalRDFKnowledgeGraphs(ontology_file, dataset, query_file, queryId);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
