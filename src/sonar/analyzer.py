"""
LLM conversation analysis module (MOCK implementation).
Extracts the core question, background, and potential solutions from a transcript.
"""
from __future__ import annotations

from typing import List, Optional

from .models import AnalysisResult, Solution


# Realistic mock analysis templates keyed by topic keywords
_MOCK_ANALYSES = [
    {
        "keywords": ["部署", "上线", "发布", "deploy", "release", "kubernetes", "k8s", "docker", "容器"],
        "core_question": "如何优化微服务的自动化部署流程以缩短发布周期并提高稳定性？",
        "background": "团队正在使用基于 Kubernetes 的微服务架构，当前部署流程需要手动执行多个步骤，包括构建镜像、更新配置、滚动升级等。每次发布大约需要 2-3 小时，且经常因为配置错误或依赖问题导致回滚。团队希望引入 GitOps 工作流和自动化 CI/CD 管道来减少人为错误并加快迭代速度。",
        "solutions": [
            Solution(
                title="使用 ArgoCD 实现 GitOps 自动化部署",
                summary="ArgoCD 是一个声明式的 GitOps 持续交付工具，能够自动同步 Git 仓库中的配置状态与集群实际状态。支持多集群管理、自动回滚和策略驱动的部署，可显著减少手动操作失误。",
                url="https://argo-cd.readthedocs.io/"
            ),
            Solution(
                title="Flux CD + Kustomize 多环境管理方案",
                summary="Flux CD 是另一款 GitOps 工具，与 Kustomize 结合可实现开发、测试、生产等多环境的配置管理。其 Image Update 功能可自动检测新镜像并触发部署。",
                url="https://fluxcd.io/"
            ),
            Solution(
                title="GitHub Actions + Helm 渐进式交付",
                summary="使用 GitHub Actions 构建 CI/CD 管道，结合 Helm Chart 管理应用版本，并利用 Flagger 实现金丝雀发布和蓝绿部署，实现自动化、安全的发布流程。",
                url="https://github.com/features/actions"
            ),
        ]
    },
    {
        "keywords": ["API", "接口", "性能", "慢", "延迟", "latency", "slow", "响应", "response", "timeout"],
        "core_question": "如何优化高并发场景下的 API 响应时间以解决频繁超时问题？",
        "background": "当前系统的核心业务 API 在峰值并发达到 5000 QPS 时，P99 延迟从正常的 200ms 飙升到 3s 以上，导致大量请求超时。后端使用 Python FastAPI + PostgreSQL，已启用连接池但效果不佳。数据库查询耗时占比最高，部分复杂查询需要扫描全表。",
        "solutions": [
            Solution(
                title="引入 Redis 多级缓存架构",
                summary="在 API 层和数据访问层之间引入 Redis 缓存，使用 Cache-Aside 模式缓存热点数据。对低频更新的数据设置较长的 TTL，并利用 Redis 集群水平扩展以应对高并发。",
                url="https://redis.io/docs/manual/"
            ),
            Solution(
                title="数据库查询优化与索引重构",
                summary="通过慢查询日志定位瓶颈 SQL，使用 EXPLAIN ANALYZE 分析执行计划，添加合适的复合索引。对复杂聚合查询考虑使用物化视图或 Elasticsearch 做搜索层。",
                url="https://www.postgresql.org/docs/current/indexes.html"
            ),
            Solution(
                title="异步处理与消息队列削峰填谷",
                summary="对非实时性要求的写操作通过 Celery / RabbitMQ 异步处理，将同步请求转化为后台任务。同时使用 FastAPI 的异步路由和连接复用技术提升吞吐量。",
                url="https://docs.celeryq.dev/"
            ),
        ]
    },
    {
        "keywords": ["数据库", "迁移", "migrate", "数据", "同步", "主从", "备份", "灾备", "backup", "replica"],
        "core_question": "如何在不停机的情况下安全完成 MySQL 从 5.7 到 8.0 的大版本升级？",
        "background": "现有生产环境使用 MySQL 5.7，需要升级到 8.0 以使用窗口函数、CTE 等新特性，并修复多个已知安全漏洞。数据库大小约 1.2TB，涉及 200+ 张表，部分表超过 5 亿行。业务要求零停机迁移，且需要有完整的回滚方案。",
        "solutions": [
            Solution(
                title="基于 GTID 的主从复制在线升级方案",
                summary="搭建 MySQL 8.0 从库，通过 GTID 复制从 5.7 主库同步数据。待追平延迟后，主从切换使 8.0 成为新主库。整个过程对应用透明，支持随时回切。",
                url="https://dev.mysql.com/doc/refman/8.0/en/replication-gtids.html"
            ),
            Solution(
                title="使用 Dual-Master 做蓝绿切换升级",
                summary="利用 MySQL 双主架构配合 ProxySQL 或 HAProxy 做流量切换。先升级备用节点，验证通过后逐步切换读/写流量，实现无感升级。",
                url="https://proxysql.com/documentation/"
            ),
            Solution(
                title="逻辑复制 + pt-online-schema-change 渐进迁移",
                summary="使用 Percona Toolkit 的 pt-online-schema-change 在线变更表结构，配合 canal/debezium 捕获增量变更写入新库，最终短时间锁表完成割接。",
                url="https://docs.percona.com/percona-toolkit/pt-online-schema-change.html"
            ),
        ]
    },
    {
        "keywords": ["测试", "自动化", "test", "单元测试", "覆盖率", "coverage", "集成测试", "CI", "pytest"],
        "core_question": "如何在保持开发速度的同时建立有效的自动化测试体系？",
        "background": "创业团队目前只有少量手动测试，CI 流程中没有任何自动化测试。代码迭代快，经常出现回归 bug，线上事故频发。团队希望引入测试文化，但担心写测试会导致开发周期变长，且不清楚应该优先覆盖哪些类型的测试。",
        "solutions": [
            Solution(
                title="测试金字塔与风险驱动策略",
                summary="按 70% 单元测试 / 20% 集成测试 / 10% E2E 测试的比例搭建测试体系。先为核心业务逻辑编写单元测试，用 pytest 加参数化覆盖边界情况。对关键路径编写集成测试，使用 Docker Compose 管理依赖服务。",
                url="https://martinfowler.com/articles/practical-test-pyramid.html"
            ),
            Solution(
                title="在 CI 中强制执行测试与覆盖率门禁",
                summary="在 GitHub Actions 中配置 pytest + coverage，设置增量代码覆盖率门禁（>80%）。合并 PR 前必须通过全部测试，利用 pytest-xdist 并行执行保持构建速度。",
                url="https://docs.pytest.org/en/stable/"
            ),
            Solution(
                title="引入契约测试确保微服务间兼容性",
                summary="使用 Pact 框架实现消费者驱动的契约测试，让每个微服务的 API 变更都能被上游消费者验证，避免接口断裂导致的回归问题。",
                url="https://docs.pact.io/"
            ),
        ]
    },
]

# Generic fallback when no keywords match
_FALLBACK_ANALYSIS = {
    "core_question": "如何系统地分析和解决当前讨论的核心问题？",
    "background": "会议中讨论了多个技术方案和业务需求，团队成员提出了不同的观点和建议。需要从讨论中提炼出最关键的问题，并参考行业最佳实践来制定可行的解决方案。",
    "solutions": [
        Solution(
            title="建立问题跟踪与决策记录文档",
            summary="创建 RFC 文档 (Request for Comments)，系统化记录问题背景、方案对比和决策理由。使用 ADR (Architecture Decision Records) 记录关键架构决策，方便后续追溯。",
            url="https://adr.github.io/"
        ),
        Solution(
            title="使用鱼骨图分析法定位根因",
            summary="对复杂问题使用鱼骨图（因果图）从人员、流程、技术、工具等维度系统性分析根本原因，避免停留在表面症状。",
            url="https://asq.org/quality-resources/fishbone"
        ),
        Solution(
            title="制定分阶段实施计划",
            summary="将大问题拆解为可执行的小步骤，按优先级和依赖关系排期。采用 MVP 思路快速验证假设，通过迭代持续优化方案。",
            url="https://www.atlassian.com/work-management/project-management/phases"
        ),
    ]
}


def _match_keywords(transcript: str) -> Optional[dict]:
    """
    Find the best-matching mock analysis based on keywords in the transcript.

    Returns the analysis dict that matches the most keywords.
    """
    transcript_lower = transcript.lower()
    best_match = None
    best_count = 0

    for analysis in _MOCK_ANALYSES:
        count = sum(1 for kw in analysis["keywords"] if kw.lower() in transcript_lower)
        if count > best_count:
            best_count = count
            best_match = analysis

    return best_match


def analyze(
    transcript: str,
    provider: str = "mock",
    api_key: str = "mock-key",
    model: str = "gpt-4",
) -> AnalysisResult:
    """
    Analyze a conversation transcript to extract core question, background, and solutions.

    Args:
        transcript: Full text of the transcribed conversation.
        provider: LLM provider name ("mock" for mock implementation).
        api_key: API key for the provider.
        model: Model name.

    Returns:
        AnalysisResult with core_question, background, and list of Solutions.
    """
    if provider != "mock":
        print(f"⚠️  非 mock 提供者 '{provider}' 暂未实现，使用 mock 模式")

    print(f"🔍 正在分析对话...")

    matched = _match_keywords(transcript)
    if matched:
        result_data = matched
        print(f"   匹配到主题: {result_data['core_question'][:60]}...")
    else:
        result_data = _FALLBACK_ANALYSIS
        print(f"   使用通用分析模板")

    result = AnalysisResult(
        core_question=result_data["core_question"],
        background=result_data["background"],
        solutions=result_data["solutions"],
    )

    print(f"✅ 分析完成: 1 个核心问题, {len(result.solutions)} 个解决方案")
    return result
