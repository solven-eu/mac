/*
 * (C) Quartet FS 2016
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.mac.statistic.memory.visitor.impl;

import java.time.Instant;
import java.util.Objects;
import java.util.logging.Logger;

import com.activeviam.mac.Loggers;
import com.activeviam.mac.memory.DatastoreConstants;
import com.activeviam.mac.memory.MemoryAnalysisDatastoreDescription;
import com.activeviam.mac.memory.MemoryAnalysisDatastoreDescription.StringArrayObject;
import com.qfs.monitoring.statistic.IStatisticAttribute;
import com.qfs.monitoring.statistic.memory.IMemoryStatistic;
import com.qfs.monitoring.statistic.memory.MemoryStatisticConstants;
import com.qfs.monitoring.statistic.memory.impl.ChunkSetStatistic;
import com.qfs.monitoring.statistic.memory.impl.ChunkStatistic;
import com.qfs.monitoring.statistic.memory.impl.DefaultMemoryStatistic;
import com.qfs.monitoring.statistic.memory.impl.DictionaryStatistic;
import com.qfs.monitoring.statistic.memory.impl.IndexStatistic;
import com.qfs.monitoring.statistic.memory.impl.ReferenceStatistic;
import com.qfs.monitoring.statistic.memory.visitor.IMemoryStatisticVisitor;
import com.qfs.store.IDatastoreSchemaMetadata;
import com.qfs.store.IRecordSet;
import com.qfs.store.impl.ChunkSet;
import com.qfs.store.record.IRecordFormat;
import com.qfs.store.transaction.IOpenedTransaction;


/**
 * This visitor is not reusable.
 * <p>
 * This visits memory statistics and commits them into the given datastore. The datastore must have
 * the schema defined by {@link MemoryAnalysisDatastoreDescription}.
 *
 * @author Quartet FS
 */
public class DatastoreFeederVisitor implements IMemoryStatisticVisitor<Void> {

	/** Class logger */
	private static final Logger logger = Logger.getLogger(Loggers.DATASTORE_LOADING);

	/** Type name if the memory usage comes from an {@link IRecordSet} */
	public static final String TYPE_RECORD = "Record";
	/** Type name if the memory usage comes from references */
	public static final String TYPE_REFERENCE = "Reference";
	/** Type name if the memory usage comes from indexes */
	public static final String TYPE_INDEX = "Index";
	/** Type name if the memory usage comes from dictionaries */
	public static final String TYPE_DICTIONARY = "Dictionary";
	/** Type name if the memory usage comes from the version chunks */
	public static final String TYPE_VERSION_COLUMN = "VersionColumn";

	/**
	 * A boolean that if true tells us that the currently visited component is responsible for
	 * storing versioning data.
	 */
	protected boolean isVersionColumn = false; // FIXME find a cleaner way to do that. (specific stat for instance).

	private final IDatastoreSchemaMetadata storageMetadata;
	private final IOpenedTransaction tm;
	/** The record format of the store that stores the chunks. */
	protected final IRecordFormat chunkRecordFormat;
	/** The name of the off-heap dump. Can be null */
	protected final String dumpName;

//	/** The current record, that will be added to the transaction manager when visiting a leaf. */
//	protected Object[] currentChunkRecord;
	/** The export date, found on the first statistics we read */
	protected Instant current = null;
	/** The epoch id we are currently reading statistics for */
	protected Long epochId = null;
	protected String branch = null;

	/** ID of the current {@link ChunkSet}. */
	protected Long chunkSetId = null;
	/** ID of the current reference. */
	protected Long referenceId = null;
	/** ID of the current index. */
	protected Long indexId = null;
	/** ID of the current dictionary. */
	protected Long dictionaryId = null;

	private String store = null;
	private Integer partition = null;
	private IndexType indexType = null;

	/**
	 * Constructor.
	 *
	 * @param storageMetadata structure of the metadata
	 * @param transaction the open transaction to use to fill the datastore with the visited data
	 * @param dumpName The name of the off-heap dump. Can be null.
	 */
	public DatastoreFeederVisitor(
			final IDatastoreSchemaMetadata storageMetadata,
			final IOpenedTransaction transaction,
			final String dumpName) {
		this.storageMetadata = storageMetadata;
		this.tm = transaction;
		this.chunkRecordFormat = this.storageMetadata
				.getStoreMetadata(DatastoreConstants.CHUNK_STORE)
				.getStoreFormat()
				.getRecordFormat();
		this.dumpName = dumpName;
	}

	public void startFrom(final IMemoryStatistic stat) {
		if (this.current == null) {
			final IStatisticAttribute dateAtt = stat.getAttribute(DatastoreConstants.CHUNK__EXPORT_DATE);
			if (dateAtt == null) {
				throw new IllegalStateException("First level statistic should contain the export date.");
			}
			this.current = Instant.ofEpochSecond(dateAtt.asLong());

			readEpochAndBranchIfAny(stat);
			assert this.epochId != null;

			FeedVisitor.includeApplicationInfoIfAny(
					this.tm,
					this.current,
					this.epochId,
					this.dumpName,
					stat);

			stat.accept(this);
		} else {
			throw new RuntimeException("Cannot reuse a feed instance");
		}
	}

	@Override
	public Void visit(final ChunkStatistic chunkStatistic) {
		final Object[] tuple = FeedVisitor.buildChunkTupleFrom(this.storageMetadata, chunkStatistic);
		tuple[chunkRecordFormat.getFieldIndex(DatastoreConstants.EPOCH_ID)] = this.epochId;
		tuple[chunkRecordFormat.getFieldIndex(DatastoreConstants.CHUNK__DUMP_NAME)] = this.dumpName;
		tuple[chunkRecordFormat.getFieldIndex(DatastoreConstants.CHUNK__EXPORT_DATE)] = this.current;

		final String type;
		if (chunkSetId != null) {
			tuple[chunkRecordFormat.getFieldIndex(DatastoreConstants.CHUNKSET_ID)] = chunkSetId;
			type = TYPE_RECORD;
		} else if (referenceId != null) {
			tuple[chunkRecordFormat.getFieldIndex(DatastoreConstants.REFERENCE_ID)] = referenceId;
			type = TYPE_REFERENCE;
		} else if (indexId != null) {
			tuple[chunkRecordFormat.getFieldIndex(DatastoreConstants.INDEX_ID)] = indexId;
			type = TYPE_INDEX;
		} else if (dictionaryId != null) {
			tuple[chunkRecordFormat.getFieldIndex(DatastoreConstants.DICTIONARY_ID)] = dictionaryId;
			type = TYPE_DICTIONARY;
		} else if (isVersionColumn) {
			type = TYPE_VERSION_COLUMN;
		} else {
			type = null;
		}
		tuple[chunkRecordFormat.getFieldIndex(DatastoreConstants.CHUNK__TYPE)] = type;

		tm.add(DatastoreConstants.CHUNK_STORE, tuple);

		visitChildren(chunkStatistic);

		// Reset
//		tuple[chunkRecordFormat.getFieldIndex(DatastoreConstants.CHUNK_ID)] = null;
//		tuple[chunkRecordFormat.getFieldIndex(DatastoreConstants.CHUNKSET_ID)] = null;
//		tuple[chunkRecordFormat.getFieldIndex(DatastoreConstants.REFERENCE_ID)] = null;
//		tuple[chunkRecordFormat.getFieldIndex(DatastoreConstants.INDEX_ID)] = null;
//		tuple[chunkRecordFormat.getFieldIndex(DatastoreConstants.DICTIONARY_ID)] = null;
//		tuple[chunkRecordFormat.getFieldIndex(DatastoreConstants.CHUNK__TYPE)] = null;
//		tuple[chunkRecordFormat.getFieldIndex(DatastoreConstants.CHUNK__CLASS)] = null;
//		tuple[chunkRecordFormat.getFieldIndex(DatastoreConstants.CHUNK__OFF_HEAP_SIZE)] = null;
//		tuple[chunkRecordFormat.getFieldIndex(DatastoreConstants.CHUNK__ON_HEAP_SIZE)] = null;

		return null;
	}

	@Override
	public Void visit(final DefaultMemoryStatistic stat) {
		readEpochAndBranchIfAny(stat);

		switch (stat.getName()) {
		case MemoryStatisticConstants.STAT_NAME_STORE:
			processStoreStat(stat);
			break;
		case MemoryStatisticConstants.STAT_NAME_STORE_PARTITION:
			processStorePartition(stat);
			break;
		case MemoryStatisticConstants.STAT_NAME_PRIMARY_INDICES:
			processPrimaryIndices(stat);
			break;
		case MemoryStatisticConstants.STAT_NAME_SECONDARY_INDICES:
			processSecondaryIndices(stat);
			break;
		default:
			recordStatAndExplore(stat);
		}

		this.epochId = null;

		return null;
	}

	@Override
	public Void visit(final ChunkSetStatistic stat) {
		final Object[] tuple = FeedVisitor.buildChunksetTupleFrom(this.storageMetadata, stat);
		final IRecordFormat format = FeedVisitor.getChunksetFormat(this.storageMetadata);

		this.chunkSetId = (Long) tuple[format.getFieldIndex(DatastoreConstants.CHUNKSET_ID)];

		assert this.epochId != null;
		tuple[format.getFieldIndex(DatastoreConstants.EPOCH_ID)] = this.epochId;
		if (this.partition != null) {
			assert this.store != null;

			tuple[format.getFieldIndex(DatastoreConstants.CHUNKSET__PARTITION)] = this.partition;
			tuple[format.getFieldIndex(DatastoreConstants.CHUNK__STORE_NAME)] = this.store;
		} else if (this.dictionaryId != null) {
			tuple[format.getFieldIndex(DatastoreConstants.CHUNKSET__DICTIONARY_ID)] = this.dictionaryId;
		} else {
			throw new RuntimeException("Cannot process this stat. A chunkset is not attached to a dictionary or a store. Faulty stat: " + stat);
		}

		tm.add(DatastoreConstants.CHUNKSET_STORE, tuple);

		if (stat.getChildren() != null) {
			for (final IMemoryStatistic child : stat.getChildren()) {
				// Could be the rowMapping if SparseChunkSet
//				final IStatisticAttribute keyAtt = child.getAttribute(MemoryStatisticConstants.ATTR_NAME_FIELD);
				// TODO(ope) use the field attributes of the chunkset
//				if (keyAtt != null) {
//					currentChunkRecord[chunkRecordFormat.getFieldIndex(DatastoreConstants.FIELDS)] =
//							new StringArrayObject(keyAtt.asText());
//				} else {
//					currentChunkRecord[chunkRecordFormat.getFieldIndex(DatastoreConstants.FIELDS)] = null;
//				}
				child.accept(this);
			}
		}

		// Reset
//		currentChunkRecord[chunkRecordFormat.getFieldIndex(DatastoreConstants.FIELDS)] = null;
		this.chunkSetId = null;

		return null;
	}

	@Override
	public Void visit(final ReferenceStatistic referenceStatistic) {
		final Object[] tuple = buildPartOfReferenceStatisticTuple(this.storageMetadata, referenceStatistic);
		final IRecordFormat refStoreFormat = getReferenceFormat(this.storageMetadata);

		// fill out the tuple
		this.referenceId = referenceStatistic.getAttribute(MemoryStatisticConstants.ATTR_NAME_REFERENCE_ID).asLong();

		tuple[refStoreFormat.getFieldIndex(DatastoreConstants.REFERENCE_ID)] = referenceId;
		tuple[refStoreFormat.getFieldIndex(DatastoreConstants.REFERENCE_NAME)] =
				referenceStatistic.getAttribute(DatastoreConstants.REFERENCE_NAME).asText();
		tuple[refStoreFormat.getFieldIndex(DatastoreConstants.REFERENCE_CLASS)] =
				referenceStatistic.getAttribute(DatastoreConstants.REFERENCE_CLASS).asText();
		tm.add(DatastoreConstants.REFERENCE_STORE, tuple);

		visitChildren(referenceStatistic);

		//Reset
		this.referenceId = null;

		return null;
	}

	@Override
	public Void visit(final IndexStatistic stat) {
		final Object[] tuple = buildIndexTupleFrom(this.storageMetadata, stat);
		final IRecordFormat format = getIndexFormat(this.storageMetadata);

		this.indexId = stat.getAttribute(MemoryStatisticConstants.ATTR_NAME_INDEX_ID).asLong();
		final boolean isKeyIndex = stat.getName().equals(MemoryStatisticConstants.STAT_NAME_KEY_INDEX);
		if (isKeyIndex) {
			this.indexType = IndexType.KEY;
		}

		assert this.store != null;
		tuple[format.getFieldIndex(DatastoreConstants.INDEX__STORE_NAME)] = this.store;
		assert this.partition != null;
		tuple[format.getFieldIndex(DatastoreConstants.INDEX__STORE_PARTITION)] = this.partition;
		tuple[format.getFieldIndex(DatastoreConstants.INDEX_TYPE)] = this.indexType;

		this.tm.add(DatastoreConstants.INDEX_STORE, tuple);

		visitChildren(stat);

		// Reset
//		currentChunkRecord[chunkRecordFormat.getFieldIndex(DatastoreConstants.FIELDS)] = null;
		indexId = null;
		if (isKeyIndex) {
			this.indexType = null;
		}

		return null;
	}

	@Override
	public Void visit(final DictionaryStatistic stat) {
//		if (!dictionaryStatistic.getName().equals(MemoryStatisticConstants.STAT_NAME_DICTIONARY)) {
//			// The stat was constructed as a dictionary stat, but in fact belongs to another structure, like the keys of an index
//			visitChildren(dictionaryStatistic);
//			return null;
//		}
		final IRecordFormat format = FeedVisitor.getDictionaryFormat(this.storageMetadata);
		final Object[] tuple = FeedVisitor.buildDictionaryTupleFrom(format, stat);
		this.dictionaryId = (Long) tuple[format.getFieldIndex(DatastoreConstants.DICTIONARY_ID)];
//		// Init. chunk record
//		currentChunkRecord = new Object[chunkRecordFormat.getFieldCount()];

//		currentChunkRecord[chunkRecordFormat.getFieldIndex(DatastoreConstants.EPOCH_ID)] = epochId;

		tuple[format.getFieldIndex(DatastoreConstants.EPOCH_ID)] = this.epochId;
		if (this.indexId != null) {
			tuple[format.getFieldIndex(DatastoreConstants.DIC__INDEX_ID)] = this.indexId;
		}
		// Nothing to do for the provider. There are dealt in another visitor

		tm.add(DatastoreConstants.DICTIONARY_STORE, tuple);

		visitChildren(stat);

		// Reset
//		currentChunkRecord = null;
		dictionaryId = null;
		return null;
	}

	/**
	 * Visits all the children of the given {@link IMemoryStatistic}.
	 *
	 * @param statistic The statistics whose children to visit.
	 */
	protected void visitChildren(final IMemoryStatistic statistic) {
		if (statistic.getChildren() != null) {
			for (final IMemoryStatistic child : statistic.getChildren()) {
				child.accept(this);
			}
		}
	}

	private void processStoreStat(final IMemoryStatistic stat) {
		final IStatisticAttribute nameAttr = Objects.requireNonNull(
				stat.getAttribute(MemoryStatisticConstants.ATTR_NAME_STORE_NAME),
				() -> "No store name in stat " + stat);
		this.store = nameAttr.asText();

		// Explore the store children
		visitChildren(stat);

		this.store = null;
	}

	private void processStorePartition(final IMemoryStatistic stat) {
		final IStatisticAttribute partitionAttr = Objects.requireNonNull(
				stat.getAttribute(MemoryStatisticConstants.ATTR_NAME_PARTITION_ID),
				() -> "No partition attribute in stat" + stat);
		this.partition = partitionAttr.asInt();

		visitChildren(stat);

		this.partition = null;
	}

	private void processPrimaryIndices(final IMemoryStatistic stat) {
		processIndexList(stat, IndexType.PRIMARY);
	}

	private void processSecondaryIndices(final IMemoryStatistic stat) {
		processIndexList(stat, IndexType.SECONDARY);
	}

	private void processIndexList(final IMemoryStatistic stat, final IndexType type) {
		this.indexType = type;
		visitChildren(stat);
		this.indexType = null;
	}

	private void recordStatAndExplore(final DefaultMemoryStatistic stat) {
//		if (stat.getAttributes().containsKey(DatastoreConstants.CHUNK__STORE_NAME)) {
//			currentChunkRecord = new Object[chunkRecordFormat.getFieldCount()];
//			// Set the store name
//			currentChunkRecord[chunkRecordFormat
//					.getFieldIndex(DatastoreConstants.CHUNK__STORE_NAME)] = stat.getAttributes()
//					.get(DatastoreConstants.CHUNK__STORE_NAME)
//					.asText();
//			currentChunkRecord[chunkRecordFormat.getFieldIndex(DatastoreConstants.CHUNK__EXPORT_DATE)] = current;
//		} else if (stat.getAttributes().containsKey(DatastoreConstants.CHUNK__PARTITION_ID)) {
//			// Set the partition id
//			currentChunkRecord[chunkRecordFormat
//					.getFieldIndex(DatastoreConstants.CHUNK__PARTITION_ID)] = stat.getAttributes()
//					.get(DatastoreConstants.CHUNK__PARTITION_ID)
//					.asInt();
//		}

		isVersionColumn = MemoryStatisticConstants.STAT_NAME_VERSIONS_COLUMN.equals(stat.getName());

		visitChildren(stat);

		isVersionColumn = false;

		// Reset
//		if (stat.getAttributes().containsKey(DatastoreConstants.CHUNK__STORE_NAME)) {
//			currentChunkRecord = null;
//		} else if (stat.getAttributes().containsKey(DatastoreConstants.CHUNK__PARTITION_ID)) {
//			currentChunkRecord[chunkRecordFormat.getFieldIndex(DatastoreConstants.CHUNK__PARTITION_ID)] = null;
//		}
	}

	private void readEpochAndBranchIfAny(final IMemoryStatistic stat) {
		final IStatisticAttribute epochAttr = stat.getAttribute(MemoryStatisticConstants.ATTR_NAME_EPOCH);
		if (epochAttr != null) {
			final Long epoch = epochAttr.asLong();
			assert this.epochId == null || epoch.equals(this.epochId);
			this.epochId = epoch;
		}
		final IStatisticAttribute branchAttr = stat.getAttribute(MemoryStatisticConstants.ATTR_NAME_BRANCH);
		if (branchAttr != null) {
			final String branch = branchAttr.asText();
			assert this.branch == null || this.branch.equals(branch);
			this.branch = branch;
		}
	}

	/**
	 * Build an object array that represents a reference.
	 *
	 * @param referenceStatistic the {@link ReferenceStatistic} from which the array must be built.
	 * @return the object array.
	 */
	protected Object[] buildPartOfReferenceStatisticTuple(
			final IDatastoreSchemaMetadata storageMetadata,
			final ReferenceStatistic referenceStatistic) {
		final IRecordFormat refStoreFormat = getReferenceFormat(storageMetadata);

		final Object[] tuple = new Object[refStoreFormat.getFieldCount()];
		tuple[refStoreFormat.getFieldIndex(
				DatastoreConstants.REFERENCE_FROM_STORE)] = referenceStatistic.getAttribute(DatastoreConstants.REFERENCE_FROM_STORE).asText();
		tuple[refStoreFormat.getFieldIndex(DatastoreConstants.REFERENCE_FROM_STORE_PARTITION_ID)] =
				referenceStatistic.getAttribute(DatastoreConstants.REFERENCE_FROM_STORE_PARTITION_ID).asInt();
		tuple[refStoreFormat.getFieldIndex(DatastoreConstants.REFERENCE_TO_STORE)] =
				referenceStatistic.getAttribute(DatastoreConstants.REFERENCE_TO_STORE).asText();
		tuple[refStoreFormat.getFieldIndex(DatastoreConstants.REFERENCE_TO_STORE_PARTITION_ID)] =
				referenceStatistic.getAttribute(DatastoreConstants.REFERENCE_TO_STORE_PARTITION_ID).asInt();

		return tuple;
	}

	private static Object[] buildIndexTupleFrom(
			final IDatastoreSchemaMetadata storageMetadata,
			final IndexStatistic stat) {
		final IRecordFormat storeFormat = getIndexFormat(storageMetadata);

		final Object[] tuple = new Object[storeFormat.getFieldCount()];
		tuple[storeFormat.getFieldIndex(DatastoreConstants.INDEX_ID)] =
				stat.getAttribute(MemoryStatisticConstants.ATTR_NAME_INDEX_ID).asLong();

		final String[] fieldNames = stat.getAttribute(DatastoreConstants.FIELDS)
				.asStringArray();
		assert fieldNames != null && fieldNames.length > 0 : "Cannot find fields in the attributes of " + stat;
		tuple[storeFormat.getFieldIndex(DatastoreConstants.FIELDS)] = new StringArrayObject(fieldNames);

		tuple[storeFormat.getFieldIndex(DatastoreConstants.INDEX_CLASS)] =
				stat.getAttribute(DatastoreConstants.INDEX_CLASS).asText();

		return tuple;
	}

	private static IRecordFormat getIndexFormat(IDatastoreSchemaMetadata storageMetadata) {
		return FeedVisitor.getRecordFormat(storageMetadata, DatastoreConstants.INDEX_STORE);
	}

	private static IRecordFormat getReferenceFormat(IDatastoreSchemaMetadata storageMetadata) {
		return FeedVisitor.getRecordFormat(storageMetadata, DatastoreConstants.REFERENCE_STORE);
	}

}
