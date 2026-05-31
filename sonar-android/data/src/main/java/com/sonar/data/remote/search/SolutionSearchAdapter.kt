package com.sonar.data.remote.search

import com.sonar.domain.entities.Solution
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Search Adapter - searches the web for solutions.
 * Uses a simple SERP API or falls back to mock results in dev mode.
 */
@Singleton
class SolutionSearchAdapter @Inject constructor(
    private val httpClient: HttpClient
) {
    /**
     * Search for solutions to the given problem.
     * Returns a list of relevant articles/links.
     */
    suspend fun search(problem: String, background: String, apiKey: String?): Result<List<Solution>> {
        // In dev mode without API key, return mock solutions
        return if (apiKey.isNullOrBlank()) {
            Result.success(getMockSolutions(problem))
        } else {
            try {
                // Using a simple search API (SerpAPI, Google Custom Search, etc.)
                // For now, use SerpAPI as example
                val query = "$problem $background".take(200)
                val response = httpClient.get("https://serpapi.com/search") {
                    parameter("q", query)
                    parameter("api_key", apiKey)
                    parameter("num", 5)
                }

                if (response.status.isSuccess()) {
                    val solutions = parseSearchResponse(response.bodyAsText())
                    Result.success(solutions)
                } else {
                    // Fallback to mock
                    Result.success(getMockSolutions(problem))
                }
            } catch (e: Exception) {
                // Fallback to mock on any error
                Result.success(getMockSolutions(problem))
            }
        }
    }

    private fun parseSearchResponse(body: String): List<Solution> {
        // Parse SerpAPI JSON response
        // This is a simplified implementation
        return try {
            // In production, parse the actual JSON response from your search provider
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getMockSolutions(problem: String): List<Solution> = listOf(
        Solution(
            title = "敏捷项目管理：分阶段发布策略",
            summary = "采用敏捷方法论，将产品发布分为多个阶段，每个阶段交付核心功能，有效降低风险并快速获得用户反馈。",
            url = "https://www.atlassian.com/agile/release-management"
        ),
        Solution(
            title = "MVP 最小可行产品策略",
            summary = "先推出最小可行产品验证市场需求，再根据用户反馈迭代开发，是创业公司和新品发布的推荐做法。",
            url = "https://en.wikipedia.org/wiki/Minimum_viable_product"
        ),
        Solution(
            title = "跨部门协作：研发与市场对齐法",
            summary = "建立定期的技术方案评审会议机制，确保研发进度与市场预期有效对齐，避免信息不对称导致的延误。",
            url = "https://hbr.org/2019/05/connecting-development-and-business-strategy"
        )
    )
}