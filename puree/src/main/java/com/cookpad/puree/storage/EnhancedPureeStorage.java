package com.cookpad.puree.storage;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public abstract class EnhancedPureeStorage implements PureeStorage {
    public abstract Records select(QueryBuilder queryBuilder);

    public abstract void delete(Predicate... predicates);

    public interface Predicate {}

    protected static class OfType implements Predicate {
        private final String type;

        OfType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    protected static class WithIds implements Predicate {
        private final String ids;

        WithIds(String ids) {
            this.ids = ids;
        }

        public String getIds() {
            return ids;
        }
    }

    public static Predicate ofType(String type) {
        return new OfType(type);
    }

    public static Predicate withIds(String ids) {
        return new WithIds(ids);
    }

    @ParametersAreNonnullByDefault
    public static class Sort {
        public enum Field { ID }
        public enum Order { ASCENDING, DESCENDING }

        private final Field field;
        private final Order order;

        public Sort() {
            this(Field.ID, Order.ASCENDING);
        }

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
        private Predicate[] predicates = new Predicate[] {};
        private Sort sort = new Sort();
        private Integer count = null;

        public void setPredicates(Predicate... predicates) {
            this.predicates = predicates;
        }

        public Predicate[] getPredicates() {
            return predicates;
        }

        public void setSort(Sort sort) {
            this.sort = sort;
        }

        public Sort getSort() {
            return sort;
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
