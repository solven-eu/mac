/*
 * (C) Quartet FS 2016
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.mac.memory;

import com.qfs.monitoring.statistic.memory.MemoryStatisticConstants;
import com.qfs.monitoring.statistic.memory.PivotMemoryStatisticConstants;

/**
 * List of constants used by stores storing the off-heap memory analysis results.
 * @author ActiveViam
 */
public class DatastoreConstants {

	// Default values
	public static final long LONG_IF_NOT_EXIST = -1L;
	public static final int INT_IF_NOT_EXIST = -1;

	// ALL STORES
	public static final String CHUNK_STORE = MemoryStatisticConstants.STAT_NAME_CHUNK;
	public static final String CHUNKSET_STORE = MemoryStatisticConstants.STAT_NAME_CHUNKSET;
	public static final String REFERENCE_STORE = MemoryStatisticConstants.STAT_NAME_REFERENCE;
	public static final String INDEX_STORE = MemoryStatisticConstants.STAT_NAME_INDEX;
	public static final String DICTIONARY_STORE = MemoryStatisticConstants.STAT_NAME_DICTIONARY;
//	public static final String STORE_PARTITION_STORE = "StorePartitions";
	public static final String STORE_FIELD_STORE = "StoreFields";
//	public static final String STORE_STORE = "Stores";
	public static final String LEVEL_STORE = "Levels";
	public static final String PROVIDER_COMPONENT_STORE = "ProviderComponent";
	public static final String PROVIDER_STORE = "Provider";
	public static final String PIVOT_STORE = "ActivePivot";
	public static final String APPLICATION_STORE = "Application";

	// Field names

	public static final String CHUNK__EXPORT_DATE = MemoryStatisticConstants.ATTR_NAME_DATE;
	public static final String CHUNK__DEBUG_TREE = "chunkDebugTree";
	public static final String DATE_PATTERN = MemoryStatisticConstants.DATE_PATTERN;

	// Ids of different stores used as key.
//	public static final String EPOCH_ID = MemoryStatisticConstants.ATTR_NAME_EPOCH;
	public static final String CHUNK_ID = MemoryStatisticConstants.ATTR_NAME_CHUNK_ID;
	public static final String CHUNKSET_ID = MemoryStatisticConstants.ATTR_NAME_CHUNKSET_ID;
	public static final String REFERENCE_ID = "referenceId";
	public static final String INDEX_ID = "indexId";
	public static final String FIELDS = "fields";
	public static final String DICTIONARY_ID = "dicId";
	public static final String CHUNK__TYPE = "chunkType"; // is it an index, ref, record, dic...

	public static final String CHUNK__OWNER = "owner";
	public static final String CHUNK__COMPONENT = "component";
	public static final String CHUNK__PARENT_ID = "parentId";
	public static final String CHUNK__PARENT_TYPE = "parentType";
	public static final String CHUNK__SIZE = "size";
	public static final String CHUNK__NON_WRITTEN_ROWS = "nonWrittenRows";
	public static final String CHUNK__FREE_ROWS = "freeRows";

	public static final String PROVIDER__PROVIDER_ID = PivotMemoryStatisticConstants.ATTR_NAME_PROVIDER_ID;
	public static final String PROVIDER__PIVOT_ID = "pivotId";
	public static final String PROVIDER__MANAGER_ID = "managerId";
	public static final String PROVIDER__INDEX = "index";
	public static final String PROVIDER__TYPE = "type";
	public static final String PROVIDER__CATEGORY = "category";

	public static final String CHUNK_AND_STORE__STORE_NAME = "chunkAndStore";
	public static final String CHUNK_AND_STORE__CHUNK_ID = MemoryStatisticConstants.ATTR_NAME_CHUNK_ID;
	public static final String CHUNK_AND_STORE__STORE = MemoryStatisticConstants.STAT_NAME_STORE;

	// store and partition info.
	public static final String CHUNK__STORE_NAME = MemoryStatisticConstants.ATTR_NAME_STORE_NAME;
	public static final String CHUNK__PARTITION_ID = MemoryStatisticConstants.ATTR_NAME_PARTITION_ID;
	public static final String CHUNK__PROVIDER_ID = PROVIDER__PROVIDER_ID;

	// ## CHUNK_STORE ## Field names of the chunk store
	public static final String CHUNK__CLASS = "class";
	public static final String CHUNK__OFF_HEAP_SIZE = "offHeapMemorySize";
	public static final String CHUNK__ON_HEAP_SIZE = "onHeapMemorySize";
	public static final String CHUNK__DUMP_NAME = "dumpName"; // The name of the off-heap dump
	public static final String CHUNK__FIELD = "field";

	// ## CHUNK_SET_STORE ## Field names of the ChunkSet store
	public static final String CHUNK_SET_CLASS = MemoryStatisticConstants.ATTR_NAME_CLASS;
	public static final String CHUNK_SET_FREE_ROWS = "freeRows";
	public static final String CHUNK_SET_PHYSICAL_CHUNK_SIZE = MemoryStatisticConstants.ATTR_NAME_LENGTH;
	public static final String CHUNKSET__DICTIONARY_ID = "dictionaryId";
	public static final String CHUNKSET__TYPE = "chunksetType";
	public static final String CHUNKSET__FREED_ROWS = "freedRows";

	// ## REFERENCES_STORE ## Field names of the Reference store
	public static final String REFERENCE_NAME = MemoryStatisticConstants.ATTR_NAME_REFERENCE_NAME;
	public static final String REFERENCE_FROM_STORE = MemoryStatisticConstants.ATTR_NAME_FROM_STORE;
	public static final String REFERENCE_FROM_STORE_PARTITION_ID = MemoryStatisticConstants.ATTR_NAME_FROM_STORE_PARTITION_ID;
	public static final String REFERENCE_TO_STORE = MemoryStatisticConstants.ATTR_NAME_TO_STORE;
	public static final String REFERENCE_TO_STORE_PARTITION_ID = MemoryStatisticConstants.ATTR_NAME_TO_STORE_PARTITION_ID;
	public static final String REFERENCE_CLASS = MemoryStatisticConstants.ATTR_NAME_CLASS;
	public static final String REFERENCE__FIELDS = "fields";

	// ## INDEX_STORE ## Field names of the Index store
	public static final String INDEX_TYPE = "type";
	public static final String INDEX_CLASS = MemoryStatisticConstants.ATTR_NAME_CLASS;
	public static final String INDEX__FIELDS = "fields";

	// ## DICTIONARY_STORE ## Field names of the Dictionary store
	public static final String DICTIONARY_SIZE = MemoryStatisticConstants.ATTR_NAME_LENGTH;
	public static final String DICTIONARY_ORDER = "order";
	public static final String DICTIONARY_CLASS = MemoryStatisticConstants.ATTR_NAME_CLASS;
	public static final String DIC__INDEX_ID = "indexId";

	public static final String STORE_FIELD__STORE_NAME = "storeName";
	public static final String STORE_FIELD__FIELD = "field";
	public static final String STORE_FIELD__DICTIONARY_ID = "dictionaryId";

	public static final String LEVEL__MANAGER_ID = "managerId";
	public static final String LEVEL__PIVOT_ID = "pivotId";
	public static final String LEVEL__DIMENSION = "dimension";
	public static final String LEVEL__HIERARCHY = "hierarchy";
	public static final String LEVEL__LEVEL = "level";
	public static final String LEVEL__DICTIONARY_ID = "dictionaryId";
	public static final String LEVEL__ON_HEAP_SIZE = "onHeap";
	public static final String LEVEL__OFF_HEAP_SIZE = "offHeap";
	public static final String LEVEL__MEMBER_COUNT = "memberCount";

	public static final String PROVIDER_COMPONENT__PROVIDER_ID = PROVIDER__PROVIDER_ID;
	public static final String PROVIDER_COMPONENT__CLASS = "class";
	public static final String PROVIDER_COMPONENT__TYPE = "providerComponentType";

	public static final String PIVOT__PIVOT_ID = "pivotId";
	public static final String PIVOT__MANAGER_ID = "managerId";

	public static final String APPLICATION__DATE = "date";
	public static final String APPLICATION__DUMP_NAME = "dumpName";
	public static final String APPLICATION__USED_ON_HEAP = "usedOnHeap";
	public static final String APPLICATION__MAX_ON_HEAP = "maxOnHeap";
	public static final String APPLICATION__USED_OFF_HEAP = "usedOffHeap";
	public static final String APPLICATION__MAX_OFF_HEAP = "maxOffHeap";

	private DatastoreConstants() {}

}
