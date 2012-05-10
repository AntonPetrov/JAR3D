package edu.bgsu.rna.jar3d.results;

import java.util.List;

import edu.bgsu.rna.jar3d.query.Loop;

public interface ResultsSaver {

	public void save(List<LoopResult> results) throws SaveFailed;

	public void save(LoopResult results) throws SaveFailed;
	
	public void markFailure(Loop loop);

	public void cleanUp();
}
