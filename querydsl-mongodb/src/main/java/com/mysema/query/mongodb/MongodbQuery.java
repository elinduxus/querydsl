/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.query.mongodb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.mapping.cache.DefaultEntityCache;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mysema.commons.lang.Assert;
import com.mysema.commons.lang.CloseableIterator;
import com.mysema.query.QueryMetadata;
import com.mysema.query.QueryModifiers;
import com.mysema.query.SearchResults;
import com.mysema.query.SimpleProjectable;
import com.mysema.query.SimpleQuery;
import com.mysema.query.support.QueryMixin;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.ParamExpression;
import com.mysema.query.types.Predicate;

/**
 * MongoDb query
 * 
 * @author laimw
 * 
 * @param <K>
 */
public class MongodbQuery<K> implements SimpleQuery<MongodbQuery<K>>,
        SimpleProjectable<K> {

    private final QueryMixin<MongodbQuery<K>> queryMixin;
    
    private final EntityPath<K> ePath;
    
    private final Morphia morphia;
    
    private final Datastore ds;
    
    private final DBCollection coll;
    
    private final MongodbSerializer serializer = new MongodbSerializer();
    
    private final DefaultEntityCache cache = new DefaultEntityCache();
    
    public MongodbQuery(Morphia morphiaParam, Datastore datastore,
            EntityPath<K> entityPath) {
        queryMixin = new QueryMixin<MongodbQuery<K>>(this);
        ePath = entityPath;
        ds = datastore;
        morphia = morphiaParam;
        coll = ds.getCollection(ePath.getType());
    }

    @Override
    public MongodbQuery<K> where(Predicate... e) {
        return queryMixin.where(e);
    }

    @Override
    public MongodbQuery<K> limit(long limit) {
        //TODO Add support
        return queryMixin.limit(limit);
    }

    @Override
    public MongodbQuery<K> offset(long offset) {
        //TODO Add support
        return queryMixin.offset(offset);
    }

    @Override
    public MongodbQuery<K> restrict(QueryModifiers modifiers) {
        //TODO Implement this
        return queryMixin.restrict(modifiers);
    }

    @Override
    public MongodbQuery<K> orderBy(OrderSpecifier<?>... o) {
        return queryMixin.orderBy(o);
    }

    @Override
    public <T> MongodbQuery<K> set(ParamExpression<T> param, T value) {
        return queryMixin.set(param, value);
    }

    public CloseableIterator<K> iterate() {
        final DBCursor cursor = createCursor();
        return new CloseableIterator<K>() {
            @Override
            public boolean hasNext() {
                return cursor.hasNext();
            }

            @Override
            public K next() {
                DBObject dbObject = cursor.next();
                return morphia.fromDBObject(ePath.getType(), dbObject, cache);
            }

            @Override
            public void remove() {
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    @Override
    public List<K> list() {
        DBCursor cursor = createCursor();
        List<K> results = new ArrayList<K>(cursor.size());
        for (DBObject dbObject : cursor) {
            results.add(morphia.fromDBObject(ePath.getType(), dbObject, cache));
        }
        return results;
    }

    private DBCursor createCursor() {
        QueryMetadata metadata = queryMixin.getMetadata();
        // Long queryLimit = metadata.getModifiers().getLimit();
        // Long queryOffset = metadata.getModifiers().getOffset();
        DBCursor cursor = coll.find(createQuery());
        if (metadata.getOrderBy().size() > 0) {
            cursor.sort(serializer.toSort(metadata.getOrderBy()));
        }
        return cursor;
    }

    @Override
    public List<K> listDistinct() {
        throw new UnsupportedOperationException();
    }

    @Override
    public K uniqueResult() {
        //TODO Do this
        throw new UnsupportedOperationException();
    }

    @Override
    public SearchResults<K> listResults() {
        //TODO Do this
        throw new UnsupportedOperationException();
    }

    @Override
    public SearchResults<K> listDistinctResults() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long count() {
        return coll.count(createQuery());
    }

    @Override
    public long countDistinct() {
        throw new UnsupportedOperationException();
    }

    private DBObject createQuery() {
        QueryMetadata metadata = queryMixin.getMetadata();
        Assert.notNull(metadata.getWhere(), "where needs to be set");

        return (DBObject) serializer.handle(metadata.getWhere());
    }

    @Override
    public String toString() {
        return createQuery().toString();
    }

}