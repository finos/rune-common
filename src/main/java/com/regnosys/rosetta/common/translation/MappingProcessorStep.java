package com.regnosys.rosetta.common.translation;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.PostProcessStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("unused") // Used in rosetta-translate
public class MappingProcessorStep implements PostProcessStep {

	private static final Logger LOGGER = LoggerFactory.getLogger(MappingProcessorStep.class);
	private final int mappingMaxTimeout;
	private final List<MappingDelegate> mappingDelegates;
	private final ExecutorService executor;
	private final List<CompletableFuture<?>> invokedTasks;
	private final MappingContext context;

	public MappingProcessorStep(Collection<MappingProcessor> mappingProcessors, MappingContext context) {
		this.context = context;
		this.mappingDelegates = new ArrayList<>(mappingProcessors);
		this.mappingDelegates.sort(MAPPING_DELEGATE_COMPARATOR);
		this.executor = context.getExecutor();
		this.invokedTasks = context.getInvokedTasks();
		this.mappingMaxTimeout = 800;
	}

	public MappingProcessorStep(Collection<MappingProcessor> mappingProcessors, MappingContext context, int mappingMaxTimeout) {
		this.context = context;
		this.mappingDelegates = new ArrayList<>(mappingProcessors);
		this.mappingDelegates.sort(MAPPING_DELEGATE_COMPARATOR);
		this.executor = context.getExecutor();
		this.invokedTasks = context.getInvokedTasks();
		this.mappingMaxTimeout = mappingMaxTimeout;
	}


	@Override
	public Integer getPriority() {
		return 1;
	}

	@Override
	public String getName() {
		return "Mapping Processor";
	}

	@Override
	public <T extends RosettaModelObject> PostProcessorReport runProcessStep(Class<T> topClass, RosettaModelObjectBuilder builder) {
		LOGGER.debug("About to run {} mappingDelegates", mappingDelegates.size());
		Stopwatch stopwatch = Stopwatch.createStarted();

		Future<?> mappingsFuture = executor.submit(() -> {
			RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
			for (MappingDelegate mapper : mappingDelegates) {
				LOGGER.info("Running mapper {} for model path {}", mapper.getClass().getSimpleName(), mapper.getModelPath());
				MappingBuilderProcessor processor = new MappingBuilderProcessor(mapper);
				processor.processRosetta(path, topClass, builder, null);
				builder.process(path, processor);
			}
			// Mapper thread waits for invoked tasks to complete before continuing (subject to timeout before)
			awaitCompletion(invokedTasks);
		});

		LOGGER.debug("Main thread waits for the mappers to complete before continuing");
		try {
			Uninterruptibles.getUninterruptibly(mappingsFuture, mappingMaxTimeout, TimeUnit.MILLISECONDS);
		} catch (ExecutionException e1) {
			LOGGER.error("Error running mapping processor", e1);
			this.context.getMappingErrors().add("Error running mapping processors: " + e1.getMessage());
		} catch (TimeoutException e1) {
			LOGGER.error("Timeout running mapping processor");
			this.context.getMappingErrors().add("Timeout running mapping processors");
		}

		LOGGER.info("Mappers completed in {}", stopwatch.stop().toString());

		LOGGER.debug("Shutdown mapper thread pool");
		executor.shutdown();
		try {
			if (!executor.awaitTermination(200, TimeUnit.MILLISECONDS)) {
				LOGGER.info("Failed to shutdown mapper executor in 200ms, force shutdown now");
				executor.shutdownNow();
			} else {
				LOGGER.debug("All mapper threads terminated");
			}
		} catch (InterruptedException e) {
			LOGGER.warn("Caught interrupted exception whilst running shutdownNow");
			executor.shutdownNow();
		}

		// Nothing to return
		return null;
	}

	private void awaitCompletion(List<CompletableFuture<?>> invokedTasks) {
		try {
			CompletableFuture.allOf(invokedTasks.toArray(new CompletableFuture[invokedTasks.size()])).get();
		} catch (InterruptedException e) {
			LOGGER.debug("Interrupt during mapping invokedTasks", e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause().getMessage(), e.getCause());
		}
	}

	/**
	 * Sort by model path so mapping processors are invoked in a consistent logical order.
	 * If there are payouts, always process the cashflow payout after the other payouts because
	 * third party cashflow payouts can breaks the Counterparty mappings if processed first.
	 *
	 * TODO: move this to the CDM.
	 */
	private static class PathComparator implements Comparator<MappingDelegate> {

		private static final String CASHFLOW_PAYOUT_SUB_PATH = ".payout.cashflow";

		@Override
		public int compare(MappingDelegate o1, MappingDelegate o2) {
			String path1 = o1.getModelPath().buildPath();
			String path2 = o2.getModelPath().buildPath();

			if (getPayoutSubPath(path1).equals(getPayoutSubPath(path2))) {
				if (path1.contains(CASHFLOW_PAYOUT_SUB_PATH)) {
					if (path2.contains(CASHFLOW_PAYOUT_SUB_PATH)) {
						return path1.compareToIgnoreCase(path2);
					}
					return 1;
				} else if (path2.contains(CASHFLOW_PAYOUT_SUB_PATH)) {
					return -1;
				}
			}
			return o1.getModelPath().compareTo(o2.getModelPath());
		}

		private Optional<String> getPayoutSubPath(String path) {
			if (path.contains(".tradableProduct.product.contractualProduct.economicTerms.payout.")) {
				return Optional.of(path.split("\\.payout\\.")[0]);
			}
			return Optional.empty();
		}
	}

	// Sort by path, then if there's multiple mappers on the same path, sort by mapper name.
	static final Comparator<MappingDelegate> MAPPING_DELEGATE_COMPARATOR = new PathComparator().thenComparing(p -> p.getClass().getName());



	/**
	 * Implements BuilderProcessor and delegates to the given MappingProcessor when the path matches.
	 */
	private static class MappingBuilderProcessor implements BuilderProcessor {

		private final MappingDelegate delegate;
		private final RosettaPath modelPath;
		private final List<Path> synonymPaths;

		MappingBuilderProcessor(MappingDelegate delegate) {
			this.delegate = delegate;
			this.modelPath = delegate.getModelPath();
			this.synonymPaths = delegate.getSynonymPaths();
		}

		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath currentPath,
				Class<R> rosettaType,
				RosettaModelObjectBuilder builder,
				RosettaModelObjectBuilder parent,
				AttributeMeta... meta) {
			if (currentPath.equals(modelPath)) {
				synonymPaths.forEach(p -> delegate.map(p, Optional.ofNullable(builder), parent));
			}
			return true;
		}

		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath currentPath,
				Class<R> rosettaType,
				List<? extends RosettaModelObjectBuilder> builder,
				RosettaModelObjectBuilder parent,
				AttributeMeta... meta) {
			if (matchesProcessorPathForMultipleCardinality(currentPath, rosettaType)) {
				synonymPaths.forEach(p -> delegate.map(p, Optional.ofNullable(builder).orElse(Collections.emptyList()), parent));
			}
			return true;
		}

		@Override
		public <T> void processBasic(RosettaPath currentPath, Class<T> rosettaType, T instance, RosettaModelObjectBuilder parent, AttributeMeta... meta) {
			if (currentPath.equals(modelPath)) {
				synonymPaths.forEach(p -> delegate.mapBasic(p, Optional.ofNullable(instance), parent));
			}
		}

		@Override
		public <T> void processBasic(RosettaPath currentPath, Class<T> rosettaType, List<T> instance, RosettaModelObjectBuilder parent, AttributeMeta... meta) {
			if (currentPath.equals(modelPath)) {
				synonymPaths.forEach(p -> delegate.mapBasic(p, Optional.ofNullable(instance).orElse(Collections.emptyList()), parent));
			}
		}

		@Override
		public BuilderProcessor.Report report() {
			return null;
		}

		private boolean matchesProcessorPathForMultipleCardinality(RosettaPath currentPath, Class<?> rosettaType) {
			return ReferenceWithMeta.class.isAssignableFrom(rosettaType) ?
					// so the parse handlers match on the list rather than each list item
					currentPath.equals(modelPath.getParent()) :
					currentPath.equals(modelPath);
		}
	}
}
