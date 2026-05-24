"""Permission-aware RAG — injects tenant and user filters into retrieval."""


class PermissionAwareRAG:
    """Wraps the retriever to enforce tenant-level data isolation in RAG."""

    def build_permission_filters(
        self,
        tenant_id: str,
        user_id: str | None = None,
        department: str | None = None,
        role: str | None = None,
    ) -> dict:
        """Build metadata filters that enforce data isolation.

        These filters are passed to the retriever to ensure only
        authorized chunks are returned.
        """
        filters = {"tenant_id": tenant_id}
        if department:
            filters["department"] = department
        if role:
            filters["allowed_roles"] = role
        return filters
