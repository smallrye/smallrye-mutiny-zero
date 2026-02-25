@org.jspecify.annotations.NullMarked
module io.smallrye.mutiny.zero.vertxpublishers {
    exports mutiny.zero.vertxpublishers;
    requires transitive io.smallrye.mutiny.zero;
    requires transitive org.jspecify;
    requires transitive io.vertx.core;
}