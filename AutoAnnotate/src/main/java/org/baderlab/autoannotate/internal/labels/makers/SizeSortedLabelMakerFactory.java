package org.baderlab.autoannotate.internal.labels.makers;

import org.baderlab.autoannotate.internal.labels.LabelMaker;
import org.baderlab.autoannotate.internal.labels.LabelMakerFactory;
import org.baderlab.autoannotate.internal.labels.LabelMakerUI;
import org.baderlab.autoannotate.internal.labels.WordCloudAdapter;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class SizeSortedLabelMakerFactory implements LabelMakerFactory<SizeSortedOptions> {

	@Inject private Provider<WordCloudAdapter> wordCloudProvider; 
	
	public static final int DEFAULT_MAX_WORDS = 4;
	
	
	@Override
	public String getName() {
		return "WordCloud: Biggest Words";
	}

	@Override
	public SizeSortedOptions getDefaultContext() {
		return new SizeSortedOptions(DEFAULT_MAX_WORDS);
	}

	@Override
	public LabelMakerUI<SizeSortedOptions> createUI(SizeSortedOptions context) {
		return new SizeSortedLabelMakerUI(context);
	}

	@Override
	public LabelMaker createLabelMaker(SizeSortedOptions context) {
		return new SizeSortedLabelMaker(wordCloudProvider.get(), context);
	}

}
