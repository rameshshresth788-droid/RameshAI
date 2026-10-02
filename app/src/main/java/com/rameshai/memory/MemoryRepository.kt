package com.rameshai.memory

/**
 * Two memory tiers, per spec:
 *  - SHORT_TERM: recent conversation turns, used to give the AI provider
 *    context for follow-up questions. Trimmed aggressively (see [recentContext]).
 *  - LONG_TERM_PREFERENCE: explicit user preferences the assistant should
 *    remember across sessions (e.g. "mujhe Hindi me jawab do").
 *
 * Nothing is written here automatically from arbitrary conversation content —
 * only turns actually exchanged, and only preferences the user's own commands
 * establish. The in-app Memory screen lets the user view/edit/delete/clear all,
 * satisfying the "don't blindly save everything" + user-control requirement.
 */
class MemoryRepository(private val dao: MemoryDao) {

    suspend fun addShortTerm(content: String) {
        dao.insert(MemoryEntity(kind = MemoryKind.SHORT_TERM, content = content))
    }

    suspend fun addPreference(content: String) {
        dao.insert(MemoryEntity(kind = MemoryKind.LONG_TERM_PREFERENCE, content = content))
    }

    suspend fun recentContext(turns: Int = 6): List<String> =
        dao.recentShortTerm(turns).reversed().map { it.content }

    suspend fun preferences(): List<String> = dao.allPreferences().map { it.content }

    fun observeAll() = dao.observeAll()

    suspend fun delete(entry: MemoryEntity) = dao.delete(entry)

    suspend fun clearAll() = dao.clearAll()
}
