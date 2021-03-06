package edu.bgsu.rna.jar3d;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import edu.bgsu.rna.jar3d.io.loaders.QueryLoader;
import edu.bgsu.rna.jar3d.io.loaders.QueryLoadingFailed;
import edu.bgsu.rna.jar3d.io.writers.ResultSaver;
import edu.bgsu.rna.jar3d.io.writers.SaveFailed;
import edu.bgsu.rna.jar3d.loop.Loop;
import edu.bgsu.rna.jar3d.loop.LoopType;
import edu.bgsu.rna.jar3d.query.Query;
import edu.bgsu.rna.jar3d.results.LoopResult;

/**
 * An Application is simple way to load sequences and models, run the models over the sequences and save the results.
 * This can only load from the file system currently and uses only bp models at the moment. In addition, it will only
 * run internal loops.
 */
public class Application {

	/** Default range limit for how far to look left and right in the sequence, mostly for alignments */
	// TODO 2013-11-05 CLZ The user needs a way to override the default range limit
	// 100 will be adequate for motifs, but not for large alignments
	public static final int DEFAULT_RANGE_LIMIT = 100;

	/** Default model type */
	// 2013-11-05 CLZ This should no longer be used.  No default.
	public static final String DEFAULT_MODEL_TYPE = "bp";

	/** Default model version */
	// 2013-11-05 CLZ This should no longer be used.  No default.
	public static final String DEFAULT_VERSION = "0.6";

	/** The query loader to use. */
	private final QueryLoader loader;

	/** The object to save results with. */
	private final ResultSaver saver;

	/** The model type to use. */
	private final String modelType;

	/** The model version to use. */
	private final String version;

	/** The range limit to scan. */
	private final int rangeLimit;

	/**
	 * Build a new Application.
	 * 
	 * @param loader The query loader to use.
	 * @param saver The result saver to use.
	 * @param modelType The model type to use.
	 * @param version The version to use.
	 * @param rangeLimit The range limit to scan.
	 */
	public Application(QueryLoader loader, ResultSaver saver, String modelType, String version, int rangeLimit) {
		this.loader = loader;
		this.saver = saver;
		this.modelType = modelType;
		this.version = version;
		this.rangeLimit = rangeLimit;
	}

	/**
	 * Create a new Application. The modelType, version, and rangeLimit are set to the default ones.
	 * 
	 * @param loader The query loader to use.
	 * @param saver The results saver to use.
	 */
	public Application(QueryLoader loader, ResultSaver saver) {
		this(loader, saver, DEFAULT_MODEL_TYPE, DEFAULT_VERSION, DEFAULT_RANGE_LIMIT);
	}

	/**
	 * Load then run a query and return the results.
	 * 
	 * @param queryId Query id to load.
	 * @param base Base path to the models.
	 * @return The results.
	 * @throws QueryLoadingFailed
	 */
	public List<List<LoopResult>> runQuery(String queryId, String base) throws QueryLoadingFailed {
		Query query = loader.load(queryId);
		return runQuery(query, base);
	}
	
	/**
	 * Load then run a query and return the results.
	 * 
	 * @param queryId Query id to load.
	 * @param ILbase Base path to the IL models.
	 * @param HLbase Base path to the HL models.
	 * @return The results.
	 * @throws QueryLoadingFailed
	 */
	
	public List<List<LoopResult>> runQuery(String queryId, String ILbase, String HLbase) throws QueryLoadingFailed {
		Query query = loader.load(queryId);
		return runQuery(query, ILbase, HLbase);
	}

	/**
	 * Run a query and return the results.
	 * 
	 * @param base The base bath to the models.
	 * @param query The query to run.
	 * @return The results.
	 */
	public List<List<LoopResult>> runQuery(Query query, String base) {
		List<List<LoopResult>> allResults = new ArrayList<List<LoopResult>>();
		for(Loop loop: query) {
			List<LoopResult> results = motifParse(base, loop); 
			allResults.add(results);
		}
		return allResults;
	}

	/**
	 * Run a query and return the results.
	 * 
	 * @param ILbase The base path to the IL models.
	 * @param HLbase The base path to the HL models
	 * @param query The query to run.
	 * @return The results.
	 */
	
	public List<List<LoopResult>> runQuery(Query query, String ILbase, String HLbase) {
		List<List<LoopResult>> allResults = new ArrayList<List<LoopResult>>();
		for(Loop loop: query) {
			String type = loop.getLoopType().getShortName();
			List<LoopResult> results;
			if(type.equals("IL")){
				results = motifParse(ILbase, loop); 
			}else {
				results = motifParse(HLbase, loop);
			}
			allResults.add(results);
		}
		return allResults;
	}

	/**
	 * Run a loop against a single loop and return the results. This will only score internal loops, all other loop
	 * types will give empty results.
	 * 
	 * @param modellist The path to a file telling what models to use
	 * @param loop The loop.
	 * @return Results of running the loop.
	 */
	private List<LoopResult> motifParse(String modellist, Loop loop) {
		List<LoopResult> result = new ArrayList<LoopResult>();

		// String folder = base + File.separator + loop.getTypeString() + File.separator + version;
		// 2013-11-07 CLZ user specifies complete path to file telling what models to use
		
		File f = new File(modellist);
		String folder = f.getParent();
		
		System.out.println("Looking for a list of motifs to use in "+modellist);
		System.out.println("Looking for motifs in "+folder);
		
		System.setProperty("user.dir", folder);

		// 2013-11-05 CLZ The third argument on the next line makes the choice between structured or all models 
		// 2013-11-05 CLZ But now that is ignored because the user specifies the path to the models
		
		Vector<String> modelNames = Sequence.getModelNames(modellist, modelType, false);		
		HashMap<String,MotifGroup> groupData = webJAR3D.loadMotifGroups(modellist, modelType);

		if (modelNames.size() == 0) {
			System.out.println("Found " + modelNames.size() + " model files");
		}

//		System.out.println("Application.motifParse: " +loop.getLoopType());
		
/**
		// 2013-11-05 CLZ Old code only ran for internal loops.  Others returned empty results and saving crashed.
		if (loop.getLoopType() == LoopType.INTERNAL) {
			result = Alignment.doLoopDBQuerey(loop, modelNames, groupData, rangeLimit);
		} else {
			result = new ArrayList<LoopResult>();
		}
*/
		
		result = Alignment.doLoopDBQuery(loop, modelNames, groupData, rangeLimit);

		return result;
	}

	/**
	 * Save results using the ResultSaver. 
	 * 
	 * @param results The results to save.
	 * @throws SaveFailed
	 */
	public void saveResults(List<List<LoopResult>> results) throws SaveFailed {
		for(List<LoopResult> res: results) {
			saver.save(res);
		}
		saver.cleanUp();
	}

	/**
	 * Load a query, run the query and then save the results.
	 * 
	 * @param queryId Query to load.
	 * @param base Base path to the models.
	 * @throws SaveFailed
	 * @throws QueryLoadingFailed
	 */
	public void runAndSave(String queryId, String base) throws SaveFailed, QueryLoadingFailed {
		List<List<LoopResult>> results = this.runQuery(queryId, base);
		saver.writeHeader();
		saveResults(results);
	}
	
	/**
	 * Load a query, run the query and then save the results.
	 * 
	 * @param queryId Query to load.
	 * @param ILbase Base path to the IL models.
	 * @param HLbase Base path to the HL models.
	 * @throws SaveFailed
	 * @throws QueryLoadingFailed
	 */
	
	public void runAndSave(String queryId, String ILbase, String HLbase) throws SaveFailed, QueryLoadingFailed {
		List<List<LoopResult>> results = this.runQuery(queryId, ILbase, HLbase);
		saver.writeHeader();
		saveResults(results);
	}
}
