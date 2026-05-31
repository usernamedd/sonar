package com.sonar.data.search

import com.sonar.domain.entities.Solution
import com.sonar.domain.ports.SearchPort
import com.sonar.domain.ports.SolutionDomain
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SolutionSearchAdapter @Inject constructor() : SearchPort {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun search(problem: String, background: String, apiKey: String?): Result<List<SolutionDomain>> {
        return if (apiKey.isNullOrBlank()) {
            Result.success(getMockSolutions(problem))
        } else {
            try {
                val query = "$problem $background".take(200)
                val request = Request.Builder()
                    .url("https://serpapi.com/search?q=${URLEncoder.encode(query, "UTF-8")}&api_key=$apiKey&num=5")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val solutions = parseSearchResponse(response.body?.string() ?: "")
                        if (solutions.isNotEmpty()) Result.success(solutions)
                        else Result.success(getMockSolutions(problem))
                    } else {
                        Result.success(getMockSolutions(problem))
                    }
                }
            } catch (e: Exception) {
                Result.success(getMockSolutions(problem))
            }
        }
    }

    private fun parseSearchResponse(body: String): List<SolutionDomain> {
        // Parse SerpAPI response - simplified implementation
        return try {
            val titleRegex = """"title"\s*:\s*"([^"]+)""".toRegex()
            val snippetRegex = """"snippet"\s*:\s*"([^"]+)""".toRegex()
            val linkRegex = """"link"\s*:\s*"([^"]+)""".toRegex()

            val titles = titleRegex.findAll(body).map { it.groupValues[1] }.take(5).toList()
            val snippets = snippetRegex.findAll(body).map { it.groupValues[1] }.take(5).toList()
            val links = linkRegex.findAll(body).map { it.groupValues[1] }.take(5).toList()

            titles.mapIndexed { i, title ->
                SolutionDomain(
                    title = title,
                    summary = snippets.getOrElse(i) { "" },
                    url = links.getOrNull(i)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getMockSolutions(problem: String): List<SolutionDomain> = listOf(
        SolutionDomain(
            title = "敏捷项目管理：分阶段发布策略",
            summary = "采用敏捷方法论，将产品发布分为多个阶段，每个阶段交付核心功能，有效降低风险并快速获得用户反馈。",
            url = "https://www.atlassian.com/agile/release-management"
        ),
        SolutionDomain(
            title = "MVP 最小可行产品策略",
            summary = "先推出最小可行产品验证市场需求，再根据用户反馈迭代开发，是创业公司和新品发布的推荐做法。",
            url = "https://en.wikipedia.org/wiki/Minimum_viable_product"
        ),
        SolutionDomain(
            title = "跨部门协作：研发与市场对齐法",
            summary = "建立定期的技术方案评审会议机制，确保研发进度与市场预期有效对齐，避免信息不对称导致的延误。",
            url = "https://hbr.org/2019/05/connecting-development-and-business-strategy"
        )
    )
}
