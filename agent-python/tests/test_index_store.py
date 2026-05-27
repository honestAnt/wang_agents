"""Test Qdrant index store CRUD operations."""

from app.rag.index_store import IndexStore


class TestIndexStore:
    def test_collection_name_format(self):
        store = IndexStore()
        name = store._collection_name("kb1", "tenant1")
        assert name == "tenant1_kb1"

    def test_create_returns_bool(self):
        store = IndexStore()
        result = store.create("kb-test", "t1")
        assert isinstance(result, bool)

    def test_delete_is_safe(self):
        store = IndexStore()
        store.delete("kb-test", "t1")

    def test_stats_returns_dict(self):
        store = IndexStore()
        stats = store.stats("kb-test", "t1")
        assert isinstance(stats, dict)

    def test_collection_exists_returns_bool(self):
        store = IndexStore()
        result = store.collection_exists("kb-nonexistent", "t1")
        assert isinstance(result, bool)

    def test_safe_collection_name(self):
        store = IndexStore()
        name = store._collection_name("kb/with slashes", "tenant/with spaces")
        assert "/" not in name
        assert " " not in name
