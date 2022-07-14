package com.regnosys.rosetta.common.postprocess.qualify;

import com.google.inject.Inject;
import com.regnosys.rosetta.common.util.SimpleProcessor;
import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.PostProcessStep;
import com.rosetta.model.lib.process.Processor.Report;
import com.rosetta.model.lib.qualify.Qualified;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class QualifyProcessorStep implements PostProcessStep {
	
	@Inject QualifyFunctionFactory qualifyFunctionFactory;
	
	@Override
	public Integer getPriority() {
		return 2;
	}

	@Override
	public String getName() {
		return "Qualification PostProcessor";
	}

	@Override
	public <T extends RosettaModelObject> PostProcessorReport runProcessStep(Class<? extends T> topClass,
			T builder) {
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		QualificationProcessReport report = new QualificationProcessReport(builder);
		QualifiableFinder processor = new QualifiableFinder(report);
		processor.processRosetta(path, topClass, builder, null);
		builder.process(path, processor);
		return convertReport(report);
	}
	
	private QualificationReport convertReport(QualificationProcessReport report) {
		RosettaModelObject build = report.getResultObject().build();
		Collection<QualificationResult> results = report.getResultQualifications();
		return new QualificationReport(build, results);
	}

	static class QualificationProcessReport implements Report, PostProcessorReport {

		private final List<QualificationResult> qualifications = new ArrayList<>();
		private final RosettaModelObject resultObject;

		public QualificationProcessReport(RosettaModelObject resultObject) {
			super();
			this.resultObject = resultObject;
		}

		@Override
		public RosettaModelObject getResultObject() {
			return resultObject;
		}

		public Collection<QualificationResult> getResultQualifications() {
			return qualifications;
		}
	}
	
	class QualifiableFinder extends SimpleProcessor {
		private final QualificationProcessReport report;

		public QualifiableFinder(QualificationProcessReport report) {
			this.report = report;
		}

		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType,
				R instance, RosettaModelObject parent, AttributeMeta... metas) {
			if (instance instanceof Qualified) {
				Qualified qualified = (Qualified)instance;
				QualificationFinder finder = new QualificationFinder();
				finder.processQual(path, rosettaType, instance);
				instance.process(path, finder);
				QualificationFinder.QaulificationFinderReport finderReport = (QualificationFinder.QaulificationFinderReport)finder.report();
				report.qualifications.addAll(finderReport.results);
				for (QualificationResult result:finderReport.results) {
					result.getUniqueSuccessQualifyResult().ifPresent(r->qualified.setQualification(r.getName()));
				}
			}
			return true;
		}

		@Override
		public Report report() {
			return report;
		}
		
	}
	
	class QualificationFinder extends SimpleProcessor {
		
		QaulificationFinderReport report = new QaulificationFinderReport();
		
		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType,
				R instance, RosettaModelObject parent, AttributeMeta... metas) {
			if (instance instanceof Qualified || instance==null) {
				return false;
			}
			processQual(path, rosettaType, instance);
			return true;
		}

		private <R extends RosettaModelObject> void processQual(RosettaPath path, Class<?> type, R builder) {
			@SuppressWarnings("unchecked")
			RosettaMetaData<R> metaData = (RosettaMetaData<R>) builder.metaData();
			List<Function<? super R, QualifyResult>> qualFuncs = metaData.getQualifyFunctions(qualifyFunctionFactory);
			if (qualFuncs.isEmpty()) return;
			List<QualifyResult> allQualifyResults = new ArrayList<>();
			Optional<QualifyResult> uniqueSuccessQualifyResult = null;
			for (Function<? super R, QualifyResult> func:qualFuncs) {
				@SuppressWarnings("unchecked")
				R built = (R) builder.build();
				QualifyResult qualResult = func.apply(built);
				allQualifyResults.add(qualResult);
				if (qualResult.isSuccess()) {
					if (uniqueSuccessQualifyResult==null) uniqueSuccessQualifyResult = Optional.of(qualResult);
					else uniqueSuccessQualifyResult = Optional.empty();
				}
			}
			if (uniqueSuccessQualifyResult==null) uniqueSuccessQualifyResult = Optional.empty();
			QualificationResult q = new QualificationResult(Optional.of(path), type, uniqueSuccessQualifyResult , allQualifyResults);
			report.results.add(q);
		}

		@Override
		public Report report() {
			return report;
		}
		
		class QaulificationFinderReport implements Report {
			List<QualificationResult> results = new ArrayList<>();
		}
	}
}
