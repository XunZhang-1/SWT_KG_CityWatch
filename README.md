# SWT_KG_CityWatch

This project contains two main Java programs:

- **RDFBuilder**: Converts CSV data into RDF triples using a predefined ontology.
- **QueryLocalRDFKnowledgeGraphs**: Runs SPARQL queries over the *reasoned* RDF data.
- **OWL2Vec Embedding (Vector Tasks)**: Generates ontology embeddings using OWL2Vec*.
---

## Requirements

- Java 17  
- IntelliJ IDEA or any Java IDE  
- Minimum 8 GB RAM (recommended: 16 GB)  

---

## Run Instructions

1. **RDFBuilder**  
   - Converts input CSV to RDF (.ttl) using the CityWatch ontology.  
   - Output saved in `files/output/`.  
   - Run from IntelliJ: Right-click → `Run 'RDFBuilder'`.

2. **QueryLocalRDFKnowledgeGraphs**  
   - Queries RDF data **after RDFS reasoning**.  
   - Ensure RDF reasoning is applied before this step.  
   - Before running, edit the following line in the code to select the desired query:
    ```java
        // Change this to run SPARQL 1 to 5
        int queryId = 5;
    ```
   - Run from IntelliJ: Right-click → `Run 'QueryLocalRDFKnowledgeGraphs'`.

3. **Vector Tasks (OWL2Vec*)**  
   - Generates ontology embeddings for alignment tasks.  
   - Open and run the notebook:
     ```
     cw_part2/files/vector/OWL2Vec-Star-IN3067-INM713/jupyter_notebook_owl2vec.ipynb
     ```


