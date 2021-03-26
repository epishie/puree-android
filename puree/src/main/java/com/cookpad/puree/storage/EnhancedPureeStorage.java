package com.cookpad.puree.storage;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public abstract class EnhancedPureeStorage implements PureeStorage {

    public abstract void insert(String type, String jsonLog, int priority);

    public abstract Records select(QueryBuilder queryBuilder);

    public abstract void delete(Condition... conditions);

    public interface Condition {}

    protected static class OfType implements Condition {
        private final String type;

        OfType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    protected static class WithIds implements Condition {
        private final String ids;

        WithIds(String ids) {
            this.ids = ids;
        }

        public String getIds() {
            return ids;
        }
    }

    protected static class WithAge implements Condition {
        private final long ageMillis;

        WithAge(long ageMillis) {
            this.ageMillis = ageMillis;
        }

        public long getAgeMillis() {
            return ageMillis;
        }
    }

    public static Condition ofType(String type) {
        return new OfType(type);
    }

    public static Condition withIds(String ids) {
        return new WithIds(ids);
    }

    public static Condition withAge(long ageMillis) {
        return new WithAge(ageMillis);
    }

    @ParametersAreNonnullByDefault
    public static class Sort {
        public enum Field { ID, PRIORITY }
        public enum Order { ASCENDING, DESCENDING }

        private final Field field;
        private final Order order;

        public Sort(Field field, Order order) {
            this.field = field;
            this.order = order;
        }
        public Field getField() {
            return field;
        }

        public Order getOrder() {
            return order;
        }
    }

    @ParametersAreNonnullByDefault
    public static class Query {
        private Condition[] conditions = new Condition[] {};
        private Sort[] sorting = new Sort[] {};
        private Integer count = null;

        public void setConditions(Condition... conditions) {
            this.conditions = conditions;
        }

        public Condition[] getConditions() {
            return conditions;
        }

        public void setSorting(Sort[] sorting) {
            this.sorting = sorting;
        }

        public Sort[] getSorting() {
            return sorting;
        }

        public void setCount(int count) {
            this.count = count;
        }

        @Nullable
        public Integer getCount() {
            return count;
        }
    }

    @ParametersAreNonnullByDefault
    public interface QueryBuilder {
        Query build(Query query);
    }
}
